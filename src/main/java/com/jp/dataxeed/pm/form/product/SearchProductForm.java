package com.jp.dataxeed.pm.form.product;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class SearchProductForm {
    private Integer id;
    private String productCode;
    private String name;

    private List<Integer> categoryIds;
    private List<Integer> locationIds;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtTo;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtTo;

    private Integer totalQuantityFrom; // 下限
    private Integer totalQuantityTo; // 上限
}
