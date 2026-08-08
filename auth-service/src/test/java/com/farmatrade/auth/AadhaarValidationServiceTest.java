package com.farmatrade.auth;

import com.farmatrade.auth.service.AadhaarValidationService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AadhaarValidationServiceTest {
    private final AadhaarValidationService service = new AadhaarValidationService();

    @Test
    void acceptsAnyTwelveDigitValueRegardlessOfChecksum() {
        assertThatCode(() -> service.validate("900000000003")).doesNotThrowAnyException();
        assertThatCode(() -> service.validate("111111111111")).doesNotThrowAnyException();
    }

    @Test
    void rejectsValuesThatAreNotExactlyTwelveDigits() {
        assertThatThrownBy(() -> service.validate("123"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("12 digits");
        assertThatThrownBy(() -> service.validate("not-aadhaar"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void normalizeReturnsTheRawValueUnchanged() {
        assertThat(service.normalize("900000000002")).isEqualTo("900000000002");
    }
}
