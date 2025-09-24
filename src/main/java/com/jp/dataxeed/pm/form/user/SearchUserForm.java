package com.jp.dataxeed.pm.form.user;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class SearchUserForm {
    private Integer id;
    private String username;
    private String name;
    private String role;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtTo;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtTo;
}
