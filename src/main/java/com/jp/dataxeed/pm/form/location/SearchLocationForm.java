package com.jp.dataxeed.pm.form.location;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class SearchLocationForm {
    private Integer id;
    private String name;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtTo;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtTo;

}
