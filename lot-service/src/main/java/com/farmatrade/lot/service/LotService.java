package com.farmatrade.lot.service;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.farmatrade.lot.dto.CreateLotRequest;
import com.farmatrade.lot.dto.DailyForecast;
import com.farmatrade.lot.dto.LotResponse;
import com.farmatrade.lot.dto.WeatherSnapshot;
import com.farmatrade.lot.entity.Lot;
import com.farmatrade.lot.enums.LotStatus;
import com.farmatrade.lot.enums.SaleType;
import com.farmatrade.lot.exception.InvalidLotException;
import com.farmatrade.lot.exception.ResourceNotFoundException;
import com.farmatrade.lot.repository.LotRepository;


@Service
public class LotService {

	private static final Logger log = LoggerFactory.getLogger(LotService.class);

	private final LotRepository lotRepository;
	private final MandiPriceClient mandiPriceClient;
	private final ImageStorageService imageStorageService;
	private final WeatherClient weatherClient;
	private final ObjectMapper objectMapper;

	public LotService(LotRepository lotRepository,MandiPriceClient mandiPriceClient,ImageStorageService imageStorageService,WeatherClient weatherClient,ObjectMapper objectMapper) {

	    this.lotRepository = lotRepository;
	    this.mandiPriceClient = mandiPriceClient;
	    this.imageStorageService = imageStorageService;
	    this.weatherClient = weatherClient;
	    this.objectMapper = objectMapper;
	}

    // ==========================
    // INTERNAL - called by bidding-service (see InternalLotController)
    // ==========================

    public void updateHighestBid(Long id, BigDecimal amount, Long buyerId) {
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found with ID : " + id));

        lot.setCurrentHighestBid(amount);
        lot.setCurrentHighestBidderId(buyerId);

        lotRepository.save(lot);
    }

    // buyerId/amount are passed explicitly rather than trusting currentHighestBid/
    // currentHighestBidderId, since a fixed-price Buy-Now never calls updateHighestBid first.
    public void markSold(Long id, Long buyerId, BigDecimal amount) {
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found with ID : " + id));

        lot.setStatus(LotStatus.SOLD);
        lot.setWinningBuyerId(buyerId);
        lot.setCurrentHighestBid(amount);
        lot.setCurrentHighestBidderId(buyerId);

        lotRepository.save(lot);
    }

    // Called for every auction close, including no-bid expiries. Idempotent against a
    // preceding markSold() call in the same close-out sequence -- only expires a lot that
    // wasn't actually sold.
    public void closeAuction(Long id) {
        Lot lot = lotRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Lot not found with ID : " + id));

        if (lot.getStatus() != LotStatus.SOLD && lot.getStatus() != LotStatus.CANCELLED) {
            lot.setStatus(LotStatus.EXPIRED);
            lotRepository.save(lot);
        }
    }

    // ==========================
    // CREATE LOT
    // ==========================
    public LotResponse createLot(CreateLotRequest request, MultipartFile image) {

        // Validate Request
        validateRequest(request);

        Lot lot = new Lot();

	    // Manual Mapping
	    lot.setFarmerId(request.getFarmerId());
	    lot.setCropName(request.getCropName());
	    lot.setGrade(request.getGrade());
	    lot.setQuantity(request.getQuantity());
	    lot.setUnit(request.getUnit());
	    lot.setSaleType(request.getSaleType());
	    
	    lot.setBasePrice(request.getBasePrice());
	    lot.setFixedPrice(request.getFixedPrice());
	    lot.setAuctionDurationMinutes(request.getAuctionDurationMinutes());
	    
	    lot.setLatitude(request.getLatitude());
	    lot.setLongitude(request.getLongitude());
	    lot.setLocationName(request.getLocationName().trim());
	    
	    WeatherSnapshot weather = weatherClient.getWeather(
	                    request.getLatitude(),
	                    request.getLongitude(),
	                    request.getLocationName()
	            );

	    applyWeatherToLot(lot, weather);

	    BigDecimal mandiPrice = mandiPriceClient.getReferencePrice(
			    		request.getCropName(),
			            request.getLocationName()
	    );

	    lot.setMandiReferencePrice(mandiPrice);
	    
	    
	    // IMAGE
	    try {

	        if (image != null && !image.isEmpty()) {

	            String imageUrl =
	                    imageStorageService.saveImage(image);

	            lot.setImageUrl(imageUrl);
	        }

	    } catch (Exception e) {

	        throw new InvalidLotException(
	                "Unable to upload crop image.");
	    }


        // Default Status
        lot.setStatus(LotStatus.LISTED);

        // Auction Logic
        if (request.getSaleType() == SaleType.AUCTION) {

            lot.setCurrentHighestBid(request.getBasePrice());

            // Automatically calculate auction end time
            lot.setAuctionEndsAt(LocalDateTime.now().plusMinutes(request.getAuctionDurationMinutes()));

        } else {

            lot.setCurrentHighestBid(BigDecimal.ZERO);

            lot.setAuctionEndsAt(null);
        }

        Lot savedLot = lotRepository.save(lot);

        return convertToResponse(savedLot);
    }

    
    // ==========================
    // VALIDATION METHOD
    // ==========================
    private void validateRequest(CreateLotRequest request) {

        
        // Auction Validation
        if (request.getSaleType() == SaleType.AUCTION) {

        	if (request.getBasePrice() == null) {

        	    throw new InvalidLotException(
        	            "Base Price is required for Auction.");
        	}

        	if (request.getBasePrice().compareTo(BigDecimal.ZERO) <= 0) {

        	    throw new InvalidLotException(
        	            "Base Price must be greater than zero.");
        	}

            if (request.getAuctionDurationMinutes() == null) {

                throw new InvalidLotException(
                        "Auction Duration is required.");
            }

            if (request.getAuctionDurationMinutes() < 5
                    || request.getAuctionDurationMinutes() > 180) {

                throw new InvalidLotException(
                        "Auction Duration must be between 5 and 180 minutes.");
            }
        }

        // Fixed Price Validation
        if (request.getSaleType() == SaleType.FIXED_PRICE) {

            if (request.getFixedPrice() == null) {

                throw new InvalidLotException(
                        "Fixed Price is required.");
            }

            if (request.getFixedPrice().compareTo(BigDecimal.ZERO) <= 0) {

                throw new InvalidLotException(
                        "Fixed Price must be greater than zero.");
            }
        }
    }
    
    
    // =====================================
    // UPDATE LOT
    // =====================================

    public LotResponse updateLot(
            Long id,
            CreateLotRequest request,
            MultipartFile image
            )throws IOException {

        Lot lot = lotRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Lot not found with ID : " + id));
        
            
        
        // Validate updated data
        validateRequest(request);
        
        
	     // Protect active auction
	        if (lot.getStatus() == LotStatus.ACTIVE &&
	                lot.getSaleType() == SaleType.AUCTION &&
	                request.getSaleType() != SaleType.AUCTION) {
	
	            throw new InvalidLotException(
	                    "Cannot change an active auction to fixed price.");
	        }
	           

        // ---------------------------------
        // Update basic fields
        // ---------------------------------


        lot.setCropName(request.getCropName());
        lot.setGrade(request.getGrade());
        lot.setQuantity(request.getQuantity());
        lot.setUnit(request.getUnit());
        lot.setSaleType(request.getSaleType());

        lot.setBasePrice(request.getBasePrice());
        lot.setFixedPrice(request.getFixedPrice());

        lot.setAuctionDurationMinutes(
                request.getAuctionDurationMinutes());

        lot.setLatitude(request.getLatitude());
        lot.setLongitude(request.getLongitude());
        lot.setLocationName(request.getLocationName());

          
        
        
     String oldImageUrl = lot.getImageUrl();

     // Update image if provided
     if (image != null && !image.isEmpty()) {

         try {

             // Save NEW image
             String newImageUrl =
                     imageStorageService.saveImage(image);

             // Set new image URL
             lot.setImageUrl(newImageUrl);

         } catch (Exception e) {

             throw new InvalidLotException(
                     "Unable to update crop image.");
         }
     }
                

	     // ---------------------------------
	     // Auction logic during UPDATE
	     // ---------------------------------
	
	     if (request.getSaleType() == SaleType.AUCTION) {
	
	         // IMPORTANT:
	         // Do NOT reset currentHighestBid
	         // Do NOT reset auctionEndsAt
	         //
	         // Existing auction values remain unchanged.
	
	     } else if (request.getSaleType() == SaleType.FIXED_PRICE) {
	
	         lot.setCurrentHighestBid(BigDecimal.ZERO);
	         lot.setAuctionEndsAt(null);
	     }

        Lot updatedLot = lotRepository.save(lot);
        
        if (image != null && !image.isEmpty()
                && oldImageUrl != null
                && !oldImageUrl.isBlank()) {

            try {

                imageStorageService.deleteImage(oldImageUrl);

            } catch (IOException e) {

                System.out.println(
                        "Warning: Old image could not be deleted: "
                        + oldImageUrl);
            }
        }

        return convertToResponse(updatedLot);
    }
   
	
    // =====================================
 	// CANCEL LOT
 	// =====================================

 	public LotResponse cancelLot(Long id) {

 	    Lot lot = lotRepository.findById(id)
 	            .orElseThrow(() ->
 	                    new ResourceNotFoundException(
 	                            "Lot not found with ID : " + id));

 	    // Check if already cancelled
 	    if (lot.getStatus() == LotStatus.CANCELLED) {

 	        throw new InvalidLotException(
 	                "Lot is already cancelled.");
 	    }

 	    // Check if lot is already sold
 	    if (lot.getStatus() == LotStatus.SOLD) {

 	        throw new InvalidLotException(
 	                "Sold lot cannot be cancelled.");
 	    }

 	    // Change status
 	    lot.setStatus(LotStatus.CANCELLED);

 	    // Save updated lot
 	    Lot cancelledLot = lotRepository.save(lot);

 	    return convertToResponse(cancelledLot);
 	}
 	
 	
	 // =====================================
	 // UPDATE LOT STATUS
	 // =====================================
	
	 public LotResponse updateLotStatus(Long id, LotStatus newStatus) {
	
	     Lot lot = lotRepository.findById(id)
	             .orElseThrow(() ->
	                     new ResourceNotFoundException(
	                             "Lot not found with ID : " + id));
	
	     // Cannot change a cancelled lot
	     if (lot.getStatus() == LotStatus.CANCELLED) {
	
	         throw new InvalidLotException(
	                 "Cancelled lot status cannot be changed.");
	     }
	
	     // Cannot change a sold lot
	     if (lot.getStatus() == LotStatus.SOLD) {
	
	         throw new InvalidLotException(
	                 "Sold lot status cannot be changed.");
	     }
	
	     // Update status
	     lot.setStatus(newStatus);
	
	     // Save to database
	     Lot updatedLot = lotRepository.save(lot);
	
	     // Convert Entity → DTO
	     return convertToResponse(updatedLot);
	 }
 	
    //getAllLots
    public List<LotResponse> getAllLots() {

        List<Lot> lots = lotRepository.findAll();

        return lots.stream().map(this::convertToResponse).collect(Collectors.toList());
    }
    
    
    //getLotById
    
	public LotResponse getLotById(Long id) {
		Lot lot = lotRepository.findById(id)
                .orElseThrow(() ->
                        new ResourceNotFoundException("Lot not found with ID : " + id));

        return convertToResponse(lot);
	}
	
	//getLotsByFarmer
	public List<LotResponse> getLotsByFarmer(Long farmerId) {

	    List<Lot> lots = lotRepository.findByFarmerId(farmerId);

	    if (lots.isEmpty()) {
	        throw new ResourceNotFoundException(
	                "No lots found for Farmer ID : " + farmerId);
	    }

	    return lots.stream()
	            .map(this::convertToResponse)
	            .toList();
	}
	
	//Search Lots by Crop
	
	public List<LotResponse> searchLotsByCrop(String cropName) {

	    if (cropName == null || cropName.trim().isEmpty()) {

	        throw new InvalidLotException("Crop name is required for search.");
	    }

	    List<Lot> lots =
	            lotRepository.findByCropNameContainingIgnoreCase(cropName.trim());

	    if (lots.isEmpty()) {
	        throw new ResourceNotFoundException(
	                "No lots found for crop : " + cropName);
	    }

	    return lots.stream()
	            .map(this::convertToResponse)
	            .toList();
	}
	

	//Search Lots by Status
	public List<LotResponse> getLotsByStatus(LotStatus status) {

	    List<Lot> lots = lotRepository.findByStatus(status);
	    
	    if (lots.isEmpty()) {
	        throw new ResourceNotFoundException(
	                "No lots found with status : " + status
	        );
	    }

	    return lots.stream()
	            .map(this::convertToResponse)
	            .toList();
	}
	
	
	// GET LOTS BY SALE TYPE
	public List<LotResponse> getLotsBySaleType(SaleType saleType) {

	    List<Lot> lots = lotRepository.findBySaleType(saleType);

	    if (lots.isEmpty()) {
	        throw new ResourceNotFoundException(
	                "No lots found with sale type : " + saleType);
	    }

	    return lots.stream()
	            .map(this::convertToResponse)
	            .toList();
	}
	
	//Update image only
	public LotResponse updateLotImage(
	        Long id,
	        MultipartFile image) throws IOException {

	    // 1. Find existing lot
	    Lot lot = lotRepository.findById(id)
	            .orElseThrow(() ->
	                    new ResourceNotFoundException(
	                            "Lot not found with ID : " + id));

	    // 2. Validate image
	    if (image == null || image.isEmpty()) {

	        throw new InvalidLotException(
	                "Image is required.");
	    }

	    // 3. Keep old image URL
	    String oldImageUrl = lot.getImageUrl();

	    try {

	        // 4. Save new image
	        String newImageUrl =
	                imageStorageService.saveImage(image);

	        // 5. Update only image URL
	        lot.setImageUrl(newImageUrl);

	        // 6. Save database
	        Lot updatedLot =
	                lotRepository.save(lot);

	        // 7. Delete old image
	        if (oldImageUrl != null &&
	                !oldImageUrl.isBlank()) {

	            imageStorageService.deleteImage(
	                    oldImageUrl);
	        }

	        // 8. Return updated response
	        return convertToResponse(updatedLot);

	    } catch (Exception e) {

	        throw new InvalidLotException(
	                "Unable to update crop image.");
	    }
	}
	
	
	 private LotResponse convertToResponse(Lot lot) {

	        LotResponse response = new LotResponse();

	        response.setId(lot.getId());
	        response.setFarmerId(lot.getFarmerId());
	        response.setCropName(lot.getCropName());
	        response.setGrade(lot.getGrade());
	        response.setQuantity(lot.getQuantity());
	        response.setUnit(lot.getUnit());
	        response.setSaleType(lot.getSaleType());
	        
	        response.setStatus(lot.getStatus());
	        
	        response.setBasePrice(lot.getBasePrice());
	        response.setFixedPrice(lot.getFixedPrice());
	        
	        response.setCurrentHighestBid(lot.getCurrentHighestBid());
	        response.setCurrentHighestBidderId(lot.getCurrentHighestBidderId());
	        response.setMandiReferencePrice(lot.getMandiReferencePrice());
	        response.setAuctionEndsAt(lot.getAuctionEndsAt());
	        
	        response.setWinningBuyerId(lot.getWinningBuyerId());

	        response.setLatitude(lot.getLatitude());
	        response.setLongitude(lot.getLongitude());
	        
	        response.setLocationName(lot.getLocationName());
	        
	        response.setImageUrl(lot.getImageUrl());

	        response.setWeather(readWeatherFromLot(lot));

	        return response;
	    }

	/**
	 * Splits a fetched WeatherSnapshot into the Lot entity's plain scalar columns plus one JSON
	 * column for the 3-day forecast list. weather is null when the external API call failed or
	 * coordinates were missing (see WeatherClient) -- clears any stale weather fields in that
	 * case rather than leaving old data behind.
	 */
	private void applyWeatherToLot(Lot lot, WeatherSnapshot weather) {

	    if (weather == null) {
	        lot.setWeatherTempC(null);
	        lot.setWeatherHumidityPct(null);
	        lot.setWeatherRiskLevel(null);
	        lot.setWeatherForecastJson(null);
	        return;
	    }

	    lot.setWeatherTempC(weather.getCurrentTempC());
	    lot.setWeatherHumidityPct(weather.getCurrentHumidityPct());
	    lot.setWeatherRiskLevel(weather.getRiskLevel());

	    try {
	        lot.setWeatherForecastJson(objectMapper.writeValueAsString(weather.getForecast()));
	    } catch (Exception e) {
	        log.error("Unable to serialize weather forecast for lot {}", lot.getId(), e);
	        lot.setWeatherForecastJson(null);
	    }
	}

	/**
	 * Rebuilds the WeatherSnapshot shown on every read (list/search/get-by-id) from the fields
	 * captured once at listing time, so the same 3-day window keeps being shown regardless of
	 * when the lot is viewed afterward -- rather than re-querying Open-Meteo (which would return
	 * a different 3-day window each day the lot sits unsold). Returns null if no weather was ever
	 * captured (e.g. missing coordinates at listing time), matching the DTO's existing contract.
	 */
	private WeatherSnapshot readWeatherFromLot(Lot lot) {

	    if (lot.getWeatherTempC() == null || lot.getWeatherForecastJson() == null) {
	        return null;
	    }

	    List<DailyForecast> forecast;
	    try {
	        forecast = objectMapper.readValue(
	                lot.getWeatherForecastJson(),
	                new TypeReference<List<DailyForecast>>() {});
	    } catch (Exception e) {
	        log.error("Unable to deserialize weather forecast for lot {}", lot.getId(), e);
	        forecast = Collections.emptyList();
	    }

	    return new WeatherSnapshot(
	            lot.getLocationName(),
	            lot.getWeatherTempC(),
	            lot.getWeatherHumidityPct(),
	            lot.getWeatherRiskLevel(),
	            forecast);
	}

}