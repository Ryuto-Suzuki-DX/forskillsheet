package com.jp.dataxeed.pm.form.category;

import java.time.LocalDate;

import lombok.Data;

@Data
public class CategoryForm {
    private Integer id;
    private String name;
    private Boolean deleteFlag;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
