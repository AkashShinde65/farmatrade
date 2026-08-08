package com.farmatrade.auth.dto;

import jakarta.validation.constraints.NotNull;

public record AccountStatusRequest(@NotNull Boolean enabled) {
}
