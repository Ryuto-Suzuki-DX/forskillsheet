package com.jp.dataxeed.pm.form.location;

import java.time.LocalDate;

import lombok.Data;

@Data
public class LocationForm {
    private Integer id;
    private String name;
    private Boolean deleteFlag;
    private LocalDate createdAt;
    private LocalDate updatedAt;

}
