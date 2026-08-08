package com.farmatrade.bidding.repository;

import com.farmatrade.bidding.entity.Auction;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Uses the real AuctionRepository/Auction entity against an in-memory H2
 * database (see src/test/resources/application.properties) rather than
 * mocks, so the actual JPQL/derived-query method names are exercised.
 */
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class AuctionRepositoryTest {

    @Autowired
    private AuctionRepository auctionRepository;

    @Test
    void findByLotId_returnsAuction_whenItExists() {
        Auction auction = persistAuction(2001L, Auction.AuctionStatus.ACTIVE, Instant.now().plusSeconds(300));

        Optional<Auction> found = auctionRepository.findByLotId(2001L);

        assertThat(found).isPresent();
        assertThat(found.get().getId()).isEqualTo(auction.getId());
    }

    @Test
    void findByLotId_returnsEmpty_whenItDoesNotExist() {
        assertThat(auctionRepository.findByLotId(999999L)).isEmpty();
    }

    @Test
    void findByStatusInAndEndTimeLessThanEqual_returnsOnlyExpiredActiveAuctions() {
        persistAuction(3001L, Auction.AuctionStatus.ACTIVE, Instant.now().minusSeconds(10));
        persistAuction(3002L, Auction.AuctionStatus.ACTIVE, Instant.now().plusSeconds(600));
        persistAuction(3003L, Auction.AuctionStatus.SOLD, Instant.now().minusSeconds(10));

        List<Auction> expired = auctionRepository.findByStatusInAndEndTimeLessThanEqual(
                List.of(Auction.AuctionStatus.ACTIVE, Auction.AuctionStatus.EXTENDED),
                Instant.now()
        );

        assertThat(expired).hasSize(1);
        assertThat(expired.get(0).getLotId()).isEqualTo(3001L);
    }

    private Auction persistAuction(Long lotId, Auction.AuctionStatus status, Instant endTime) {
        Auction auction = Auction.builder()
                .lotId(lotId)
                .farmerId(55L)
                .lotType(Auction.LotType.AUCTION)
                .status(status)
                .startingPrice(BigDecimal.valueOf(1000))
                .endTime(endTime)
                .createdAt(Instant.now())
                .build();
        return auctionRepository.save(auction);
    }
}
