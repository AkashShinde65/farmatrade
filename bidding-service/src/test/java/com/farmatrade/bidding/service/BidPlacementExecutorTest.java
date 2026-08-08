package com.farmatrade.bidding.service;

import com.farmatrade.bidding.dto.BidResponse;
import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.entity.Bid;
import com.farmatrade.bidding.repository.AuctionRepository;
import com.farmatrade.bidding.repository.BidRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidPlacementExecutorTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private BidRepository bidRepository;
    @Mock
    private LotServiceClient lotServiceClient;

    private BidPlacementExecutor executor;
    private Auction auction;

    @BeforeEach
    void setUp() {
        // Real (non-mocked) collaborators - pure business logic, no need to mock.
        BidValidationService validationService = new BidValidationService();
        BidBroadcastService broadcastService = mock(BidBroadcastService.class);

        executor = new BidPlacementExecutor(
                auctionRepository, bidRepository, validationService, broadcastService, lotServiceClient);

        ReflectionTestUtils.setField(executor, "antiSnipeWindowSeconds", 30);
        ReflectionTestUtils.setField(executor, "antiSnipeExtensionMinutes", 2);

        auction = Auction.builder()
                .id(1L)
                .lotId(1001L)
                .farmerId(55L)
                .lotType(Auction.LotType.AUCTION)
                .status(Auction.AuctionStatus.ACTIVE)
                .startingPrice(BigDecimal.valueOf(1000))
                .endTime(Instant.now().plusSeconds(600))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void execute_acceptsBid_andReturnsNotExtended_whenFarFromEndTime() {
        when(auctionRepository.findByLotId(1001L)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> {
            Bid b = inv.getArgument(0);
            b.setId(99L);
            return b;
        });

        BidResponse response = executor.execute(1001L, 101L, BigDecimal.valueOf(1500));

        assertThat(response.bidId()).isEqualTo(99L);
        assertThat(response.buyerId()).isEqualTo(101L);
        assertThat(response.amount()).isEqualTo(BigDecimal.valueOf(1500));
        assertThat(response.auctionExtended()).isFalse();
        verify(lotServiceClient).updateHighestBid(1001L, BigDecimal.valueOf(1500), 101L);
    }

    @Test
    void execute_extendsAuction_whenBidArrivesWithinAntiSnipeWindow() {
        auction.setEndTime(Instant.now().plusSeconds(10)); // inside 30s window
        Instant originalEndTime = auction.getEndTime();

        when(auctionRepository.findByLotId(1001L)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));

        BidResponse response = executor.execute(1001L, 101L, BigDecimal.valueOf(1500));

        assertThat(response.auctionExtended()).isTrue();
        assertThat(auction.getStatus()).isEqualTo(Auction.AuctionStatus.EXTENDED);
        assertThat(auction.getEndTime()).isAfter(originalEndTime);
    }

    @Test
    void execute_doesNotFailBid_whenLotServiceSyncThrows() {
        when(auctionRepository.findByLotId(1001L)).thenReturn(Optional.of(auction));
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));
        when(bidRepository.save(any(Bid.class))).thenAnswer(inv -> inv.getArgument(0));
        doThrow(new RuntimeException("Lot Service unreachable"))
                .when(lotServiceClient).updateHighestBid(any(), any(), any());

        BidResponse response = executor.execute(1001L, 101L, BigDecimal.valueOf(1500));

        // The bid itself must still succeed even though the downstream sync failed.
        assertThat(response.amount()).isEqualTo(BigDecimal.valueOf(1500));
    }
}
