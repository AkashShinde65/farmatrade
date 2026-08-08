package com.farmatrade.logistics.service;

import com.farmatrade.logistics.dto.ColdStorageMatch;
import com.farmatrade.logistics.dto.GeoPoint;
import com.farmatrade.logistics.dto.LogisticsAcceptResult;
import com.farmatrade.logistics.dto.LogisticsChoiceResponse;
import com.farmatrade.logistics.dto.OptInRequest;
import com.farmatrade.logistics.dto.SaleCompletedRequest;
import com.farmatrade.logistics.dto.TruckBookingResult;
import com.farmatrade.logistics.dto.WeatherSnapshot;
import com.farmatrade.logistics.entity.LogisticsRequest;
import com.farmatrade.logistics.entity.LogisticsRequestStatus;
import com.farmatrade.logistics.entity.TruckBookingStatus;
import com.farmatrade.logistics.repository.LogisticsRequestRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;

@Service
public class LogisticsRequestService {

    private static final Logger log = LoggerFactory.getLogger(LogisticsRequestService.class);

    private final LogisticsRequestRepository logisticsRequestRepository;
    private final GeocodingClient geocodingClient;
    private final WeatherClient weatherClient;
    private final ColdStorageMatchingService coldStorageMatchingService;
    private final TruckMockService truckMockService;
    private final BillingServiceClient billingServiceClient;

    public LogisticsRequestService(
            LogisticsRequestRepository logisticsRequestRepository,
            GeocodingClient geocodingClient,
            WeatherClient weatherClient,
            ColdStorageMatchingService coldStorageMatchingService,
            TruckMockService truckMockService,
            BillingServiceClient billingServiceClient) {
        this.logisticsRequestRepository = logisticsRequestRepository;
        this.geocodingClient = geocodingClient;
        this.weatherClient = weatherClient;
        this.coldStorageMatchingService = coldStorageMatchingService;
        this.truckMockService = truckMockService;
        this.billingServiceClient = billingServiceClient;
    }

    /**
     * Geocodes the pickup address once here (and stores the result) so accept() never has to
     * geocode again -- and so the weather snapshot shown at decision time uses the real pickup
     * coordinates rather than a placeholder.
     */
    @Transactional
    public LogisticsChoiceResponse createFromLotWin(OptInRequest optInRequest) {
        LogisticsRequest request = new LogisticsRequest();
        request.setLotId(optInRequest.lotId());
        request.setFarmerId(optInRequest.farmerId());
        request.setBuyerId(optInRequest.buyerId());
        request.setFarmerAddress(optInRequest.farmerAddress());
        request.setSaleId(optInRequest.saleId());
        request.setAmount(optInRequest.amount());
        request.setCropName(optInRequest.cropName());
        request.setQuantity(optInRequest.quantity());
        request.setGst(optInRequest.gst());
        request.setPlatformFee(optInRequest.platformFee());
        request.setTotalAmount(optInRequest.totalAmount());
        request.setPaymentMethod(optInRequest.paymentMethod());
        request.setStatus(LogisticsRequestStatus.PENDING_CHOICE);

        GeoPoint origin = geocodingClient.geocode(optInRequest.farmerAddress());
        request.setLatitude(origin.latitude());
        request.setLongitude(origin.longitude());
        logisticsRequestRepository.save(request);

        WeatherSnapshot weatherSnapshot = weatherClient.getSnapshot(origin, optInRequest.farmerAddress());

        return new LogisticsChoiceResponse(request.getId(), request.getLotId(), request.getStatus(), weatherSnapshot);
    }

    public LogisticsRequest getById(Long id) {
        return logisticsRequestRepository.findById(id)
                .orElseThrow(() -> new NoSuchElementException("No logistics request found with id " + id));
    }

    public List<LogisticsRequest> findByBuyer(Long buyerId) {
        return logisticsRequestRepository.findByBuyerId(buyerId);
    }

    /**
     * Previously accept()/decline() trusted the path {id} alone -- any authenticated user who
     * knew (or guessed) another buyer's request id could accept or decline it on their behalf.
     * Both callers now pass the caller's own id (from the JWT "sub" claim), checked here against
     * the request's real buyerId.
     */
    private void requireOwnership(LogisticsRequest request, Long callerId) {
        if (!request.getBuyerId().equals(callerId)) {
            throw new AccessDeniedException(
                    "Logistics request " + request.getId() + " does not belong to the authenticated user");
        }
    }

    @Transactional
    public LogisticsAcceptResult accept(Long id, Long callerId) {
        LogisticsRequest request = getById(id);
        requireOwnership(request, callerId);
        if (request.getStatus() != LogisticsRequestStatus.PENDING_CHOICE) {
            throw new IllegalStateException(
                    "Logistics request " + id + " is " + request.getStatus() + ", expected PENDING_CHOICE.");
        }

        GeoPoint origin = new GeoPoint(request.getLatitude(), request.getLongitude());
        List<ColdStorageMatch> matches = coldStorageMatchingService.findNearby(origin);
        WeatherSnapshot weatherSnapshot = weatherClient.getSnapshot(origin, request.getFarmerAddress());
        TruckBookingResult truckBooking = truckMockService.bookNearestAvailable(origin, request.getId());

        request.setStatus(LogisticsRequestStatus.REQUESTED);
        request.setTruckId(truckBooking.truckId());
        request.setTruckBookingStatus(TruckBookingStatus.BOOKED);
        logisticsRequestRepository.save(request);

        notifyBilling(request, true);

        return new LogisticsAcceptResult(request.getId(), request.getStatus(), matches, weatherSnapshot, truckBooking);
    }

    @Transactional
    public LogisticsRequest decline(Long id, Long callerId) {
        LogisticsRequest request = getById(id);
        requireOwnership(request, callerId);
        if (request.getStatus() != LogisticsRequestStatus.PENDING_CHOICE) {
            throw new IllegalStateException(
                    "Logistics request " + id + " is " + request.getStatus() + ", expected PENDING_CHOICE.");
        }
        request.setStatus(LogisticsRequestStatus.DECLINED);
        logisticsRequestRepository.save(request);

        notifyBilling(request, false);

        return request;
    }

    /**
     * Single trigger point for invoice creation -- Bidding Service intentionally does not call
     * Billing Service directly at sale-close time (see BillingServiceClient Javadoc). Best-effort:
     * a failure here doesn't undo the buyer's already-persisted accept/decline choice, but is
     * tracked on the request row (billingNotified) for manual reconciliation.
     */
    private void notifyBilling(LogisticsRequest request, boolean logisticsAccepted) {
        SaleCompletedRequest saleRequest = new SaleCompletedRequest(
                request.getSaleId(),
                request.getBuyerId(),
                request.getFarmerId(),
                request.getLotId(),
                request.getCropName(),
                request.getQuantity(),
                request.getAmount(),
                request.getGst(),
                request.getPlatformFee(),
                request.getTotalAmount(),
                request.getPaymentMethod(),
                logisticsAccepted
        );

        boolean notified = billingServiceClient.notifySaleCompleted(saleRequest);
        request.setBillingNotified(notified);
        logisticsRequestRepository.save(request);

        if (!notified) {
            log.error("Billing notification FAILED for logistics request {} (saleId={}) - flagged for manual reconciliation",
                    request.getId(), request.getSaleId());
        }
    }
}
