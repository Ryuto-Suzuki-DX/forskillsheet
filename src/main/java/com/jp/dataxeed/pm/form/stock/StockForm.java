package com.jp.dataxeed.pm.form.stock;

import java.time.LocalDate;

import lombok.Data;

@Data
public class StockForm {
    private Integer id;
    private Integer productId;
    private Integer quantity;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
