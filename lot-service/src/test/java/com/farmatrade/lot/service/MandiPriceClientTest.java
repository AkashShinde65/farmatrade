package com.farmatrade.lot.service;

import com.farmatrade.lot.dto.MandiPriceRecord;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Covers selectBestMatch()/extractState() in isolation, without hitting the real Mandi API --
 * added 2026-08-01 after finding the reference price was never actually filtered by the lot's
 * location despite accepting a locationName parameter. The live government API was rate-limited
 * (HTTP 429) while verifying this fix, so this test proves the new matching logic is correct
 * independent of that external API being reachable.
 */
class MandiPriceClientTest {

    private final MandiPriceClient client = new MandiPriceClient(null);

    @Test
    void extractState_parsesLastCommaSeparatedSegment() {
        assertThat(client.extractState("Ludhiana, Punjab")).isEqualTo("Punjab");
        assertThat(client.extractState("Amritsar,  Punjab")).isEqualTo("Punjab");
    }

    @Test
    void extractState_returnsNullWhenNoCommaPresent() {
        assertThat(client.extractState("Punjab")).isNull();
        assertThat(client.extractState(null)).isNull();
        assertThat(client.extractState("Ludhiana, ")).isNull();
    }

    @Test
    void selectBestMatch_prefersRecordMatchingTheLotsState() {
        MandiPriceRecord punjabRecord = record("Punjab", BigDecimal.valueOf(2000));
        MandiPriceRecord keralaRecord = record("Kerala", BigDecimal.valueOf(3500));

        MandiPriceRecord result = client.selectBestMatch(
                List.of(keralaRecord, punjabRecord), "Ludhiana, Punjab");

        assertThat(result).isSameAs(punjabRecord);
    }

    @Test
    void selectBestMatch_matchIsCaseInsensitive() {
        MandiPriceRecord punjabRecord = record("PUNJAB", BigDecimal.valueOf(2000));

        MandiPriceRecord result = client.selectBestMatch(
                List.of(punjabRecord), "Ludhiana, punjab");

        assertThat(result).isSameAs(punjabRecord);
    }

    @Test
    void selectBestMatch_fallsBackToFirstRecordWhenNoStateMatches() {
        MandiPriceRecord keralaRecord = record("Kerala", BigDecimal.valueOf(3500));
        MandiPriceRecord goaRecord = record("Goa", BigDecimal.valueOf(4000));

        MandiPriceRecord result = client.selectBestMatch(
                List.of(keralaRecord, goaRecord), "Ludhiana, Punjab");

        assertThat(result).isSameAs(keralaRecord);
    }

    @Test
    void selectBestMatch_fallsBackToFirstRecordWhenLocationNameHasNoState() {
        MandiPriceRecord onlyRecord = record("Kerala", BigDecimal.valueOf(3500));

        MandiPriceRecord result = client.selectBestMatch(List.of(onlyRecord), "Ludhiana");

        assertThat(result).isSameAs(onlyRecord);
    }

    private MandiPriceRecord record(String state, BigDecimal modalPrice) {
        MandiPriceRecord record = new MandiPriceRecord();
        record.setState(state);
        record.setModal_price(modalPrice);
        return record;
    }
}
