package com.farmatrade.lot.dto;

import java.math.BigDecimal;

public record MarkSoldRequest(Long buyerId, BigDecimal amount) {
}
