package com.farmatrade.bidding.service;

import com.farmatrade.bidding.dto.BidResponse;
import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.exception.BidValidationException;
import com.farmatrade.bidding.repository.AuctionRepository;
import com.farmatrade.bidding.repository.SaleRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuctionServiceTest {

    @Mock
    private AuctionRepository auctionRepository;
    @Mock
    private SaleRepository saleRepository;
    @Mock
    private BidPlacementExecutor bidPlacementExecutor;

    private AuctionService auctionService;

    @BeforeEach
    void setUp() {
        auctionService = new AuctionService(auctionRepository, saleRepository, bidPlacementExecutor);
    }

    @Test
    void createAuction_persistsNewAuction_whenLotIdNotAlreadyUsed() {
        when(auctionRepository.findByLotId(1001L)).thenReturn(Optional.empty());
        when(auctionRepository.save(any(Auction.class))).thenAnswer(inv -> inv.getArgument(0));

        Auction result = auctionService.createAuction(
                1001L, 55L, Auction.LotType.AUCTION,
                BigDecimal.valueOf(1000), null, Instant.now().plusSeconds(600));

        assertThat(result.getLotId()).isEqualTo(1001L);
        assertThat(result.getStatus()).isEqualTo(Auction.AuctionStatus.ACTIVE);
        verify(auctionRepository).save(any(Auction.class));
    }

    @Test
    void createAuction_rejects_whenLotIdAlreadyHasAnAuction() {
        Auction existing = Auction.builder().lotId(1001L).build();
        when(auctionRepository.findByLotId(1001L)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> auctionService.createAuction(
                1001L, 55L, Auction.LotType.AUCTION,
                BigDecimal.valueOf(1000), null, Instant.now().plusSeconds(600)))
                .isInstanceOf(BidValidationException.class)
                .hasMessageContaining("already exists");

        verify(auctionRepository, never()).save(any());
    }

    @Test
    void placeBid_returnsImmediately_whenFirstAttemptSucceeds() {
        BidResponse expected = new BidResponse(1L, 1001L, 101L, BigDecimal.valueOf(1500),
                Instant.now(), BigDecimal.valueOf(1500), Instant.now().plusSeconds(600), false);
        when(bidPlacementExecutor.execute(1001L, 101L, BigDecimal.valueOf(1500))).thenReturn(expected);

        BidResponse result = auctionService.placeBid(1001L, 101L, BigDecimal.valueOf(1500));

        assertThat(result).isEqualTo(expected);
        verify(bidPlacementExecutor, times(1)).execute(anyLong(), anyLong(), any(BigDecimal.class));
    }

    @Test
    void placeBid_retriesAndSucceeds_afterOneOptimisticLockConflict() {
        BidResponse expected = new BidResponse(1L, 1001L, 101L, BigDecimal.valueOf(1500),
                Instant.now(), BigDecimal.valueOf(1500), Instant.now().plusSeconds(600), false);

        when(bidPlacementExecutor.execute(anyLong(), anyLong(), any(BigDecimal.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Auction.class, 1001L))
                .thenReturn(expected);

        BidResponse result = auctionService.placeBid(1001L, 101L, BigDecimal.valueOf(1500));

        assertThat(result).isEqualTo(expected);
        verify(bidPlacementExecutor, times(2)).execute(anyLong(), anyLong(), any(BigDecimal.class));
    }

    @Test
    void placeBid_givesUp_afterExhaustingAllRetries() {
        when(bidPlacementExecutor.execute(anyLong(), anyLong(), any(BigDecimal.class)))
                .thenThrow(new ObjectOptimisticLockingFailureException(Auction.class, 1001L));

        assertThatThrownBy(() -> auctionService.placeBid(1001L, 101L, BigDecimal.valueOf(1500)))
                .isInstanceOf(BidValidationException.class)
                .hasMessageContaining("Too many concurrent bids");

        verify(bidPlacementExecutor, times(3)).execute(anyLong(), anyLong(), any(BigDecimal.class));
    }
}
