package com.farmatrade.bidding.service;

import com.farmatrade.bidding.entity.Auction;
import com.farmatrade.bidding.exception.AuctionClosedException;
import com.farmatrade.bidding.exception.BidValidationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BidValidationServiceTest {

    private final BidValidationService validationService = new BidValidationService();
    private Auction auction;

    @BeforeEach
    void setUp() {
        auction = Auction.builder()
                .id(1L)
                .lotId(1001L)
                .farmerId(55L)
                .lotType(Auction.LotType.AUCTION)
                .status(Auction.AuctionStatus.ACTIVE)
                .startingPrice(BigDecimal.valueOf(1000))
                .currentHighestBid(null)
                .endTime(Instant.now().plusSeconds(600))
                .createdAt(Instant.now())
                .build();
    }

    @Test
    void validatePlaceBid_passes_whenBidAboveStartingPriceAndAuctionActive() {
        validationService.validatePlaceBid(auction, BigDecimal.valueOf(1500));
        // no exception = pass
    }

    @Test
    void validatePlaceBid_rejects_whenBidNotGreaterThanCurrentHighest() {
        auction.setCurrentHighestBid(BigDecimal.valueOf(2000));

        assertThatThrownBy(() -> validationService.validatePlaceBid(auction, BigDecimal.valueOf(2000)))
                .isInstanceOf(BidValidationException.class)
                .hasMessageContaining("must be greater than");
    }

    @Test
    void validatePlaceBid_rejects_whenAuctionAlreadyClosed() {
        auction.setStatus(Auction.AuctionStatus.CLOSED);

        assertThatThrownBy(() -> validationService.validatePlaceBid(auction, BigDecimal.valueOf(1500)))
                .isInstanceOf(AuctionClosedException.class);
    }

    @Test
    void validatePlaceBid_rejects_whenEndTimeHasPassed() {
        auction.setEndTime(Instant.now().minusSeconds(5));

        assertThatThrownBy(() -> validationService.validatePlaceBid(auction, BigDecimal.valueOf(1500)))
                .isInstanceOf(AuctionClosedException.class)
                .hasMessageContaining("expired");
    }

    @Test
    void isWithinAntiSnipeWindow_true_whenEndTimeLessThan30SecondsAway() {
        auction.setEndTime(Instant.now().plusSeconds(10));

        boolean result = validationService.isWithinAntiSnipeWindow(auction, 30);

        assertThat(result).isTrue();
    }

    @Test
    void isWithinAntiSnipeWindow_false_whenEndTimeFarAway() {
        auction.setEndTime(Instant.now().plusSeconds(600));

        boolean result = validationService.isWithinAntiSnipeWindow(auction, 30);

        assertThat(result).isFalse();
    }

    @Test
    void validateBuyNow_rejects_whenLotIsNotFixedPrice() {
        assertThatThrownBy(() -> validationService.validateBuyNow(auction))
                .isInstanceOf(BidValidationException.class)
                .hasMessageContaining("FIXED_PRICE");
    }

    @Test
    void validateBuyNow_rejects_whenLotAlreadySold() {
        auction.setLotType(Auction.LotType.FIXED_PRICE);
        auction.setStatus(Auction.AuctionStatus.SOLD);

        assertThatThrownBy(() -> validationService.validateBuyNow(auction))
                .isInstanceOf(AuctionClosedException.class);
    }

    @Test
    void validateBuyNow_passes_forActiveFixedPriceLot() {
        auction.setLotType(Auction.LotType.FIXED_PRICE);
        auction.setFixedPrice(BigDecimal.valueOf(2500));

        validationService.validateBuyNow(auction);
        // no exception = pass
    }
}
