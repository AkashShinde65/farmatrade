package com.farmatrade.auth.service;

import org.springframework.stereotype.Service;

/**
 * Per project decision, this only checks that an Aadhaar value is 12 digits -- it no longer runs
 * a Verhoeff checksum, so any well-formed 12-digit number is accepted. normalize() also no longer
 * hashes the value; the raw 12-digit number is what gets persisted (see User.aadhaarHash, whose
 * name predates this change).
 */
@Service
public class AadhaarValidationService {

    public void validate(String aadhaar) {
        if (aadhaar == null || !aadhaar.matches("^[0-9]{12}$")) {
            throw new IllegalArgumentException("Aadhaar must contain exactly 12 digits");
        }
    }

    public String normalize(String aadhaar) {
        validate(aadhaar);
        return aadhaar;
    }
}
