package com.jp.dataxeed.pm.form.product;

import java.time.LocalDate;

import lombok.Data;

@Data
public class ProductForm {
    private Integer id;
    private String productCode;
    private String name;
    private Boolean deleteFlag;
    private LocalDate createdAt;
    private LocalDate updatedAt;

}
