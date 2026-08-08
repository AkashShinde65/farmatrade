package com.farmatrade.lot.dto;

import java.math.BigDecimal;

import lombok.Data;

@Data
public class MandiPriceRecord {

    private String state;

    private String district;

    private String market;

    private String commodity;

    private String variety;

    private String grade;

    private BigDecimal min_price;

    private BigDecimal max_price;

    private BigDecimal modal_price;

    private String arrival_date;
}