package com.farmatrade.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(max = 150) String fullName,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Pattern(regexp = "^[0-9]{10,15}$", message = "mobile must contain 10 to 15 digits") String mobile,
        @NotBlank @Size(min = 10, max = 72) String password,
        @NotBlank @Pattern(regexp = "^[0-9]{12}$", message = "aadhaar must contain exactly 12 digits") String aadhaar
) {
}
