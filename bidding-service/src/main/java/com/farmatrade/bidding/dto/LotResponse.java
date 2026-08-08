package com.farmatrade.bidding.dto;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Shape of the data returned by Lot Service's "Get Lot" API. This is a
 * local DTO owned by the Bidding Service - it intentionally mirrors only
 * the fields Bidding Service needs and must NEVER be replaced with (or
 * mapped 1:1 against) Lot Service's actual JPA entity.
 *
 * Only id/farmerId/fixedPrice/cropName/quantity/locationName are
 * actually consumed today (see AuctionClosingService) and match Lot
 * Service's real response field names. lotType/startingPrice/lotStatus/
 * auctionEndTime do NOT match Lot Service's actual field names
 * (saleType/basePrice/status/auctionEndsAt) and will always deserialize
 * as null - fix these if this DTO grows a real use for them.
 *
 * CORRECTED 2026-07-30 -- this record's id field was previously named
 * "lotId", and the comment above claimed it matched Lot Service's real
 * response too. It didn't: Lot Service's actual field is "id" (confirmed
 * while diagnosing the internal-token GET /api/lots/{id} auth bug), so it
 * always silently deserialized as null. Harmless today since nothing
 * reads it (AuctionClosingService only reads cropName/quantity/
 * locationName), but a landmine for whoever adds the next field that
 * does. Renamed to match reality instead of just documenting the gap.
 */
public record LotResponse(
        Long id,
        Long farmerId,
        String lotType,
        BigDecimal startingPrice,
        BigDecimal fixedPrice,
        String lotStatus,
        Instant auctionEndTime,
        String cropName,
        BigDecimal quantity,
        String locationName
) {
}
