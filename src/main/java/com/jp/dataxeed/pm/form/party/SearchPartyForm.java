package com.jp.dataxeed.pm.form.party;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class SearchPartyForm {
    private Integer id;
    private String partyCode;
    private String name;
    private String address;
    private String detail;
    private String attention;

    // 日付用
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtTo;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtTo;
}
