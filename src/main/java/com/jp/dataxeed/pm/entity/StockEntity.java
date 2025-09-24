package com.jp.dataxeed.pm.entity;

import java.time.LocalDate;

import lombok.Data;

@Data
public class StockEntity {
    private Integer id;
    private Integer productId;
    private Integer quantity;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}