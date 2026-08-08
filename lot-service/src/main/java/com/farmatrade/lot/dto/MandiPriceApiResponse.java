package com.farmatrade.lot.dto;

import java.util.List;

import lombok.Data;

@Data
public class MandiPriceApiResponse {

    private Integer total;

    private Integer count;

    private List<MandiPriceRecord> records;
}