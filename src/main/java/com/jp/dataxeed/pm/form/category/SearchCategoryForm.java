package com.jp.dataxeed.pm.form.category;

import java.time.LocalDate;

import lombok.Data;

@Data
public class SearchCategoryForm {
    private Integer id;
    private String name;

    // 日付系
    private LocalDate createdAtFrom;
    private LocalDate createdAtTo;
    private LocalDate updatedAtFrom;
    private LocalDate updatedAtTo;
}
