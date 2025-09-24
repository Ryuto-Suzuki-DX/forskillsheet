package com.jp.dataxeed.pm.entity;

import java.time.LocalDate;

import lombok.Data;

@Data
public class LocationEntity {
    private Integer id;
    private String name;
    private Boolean deleteFlag;
    private LocalDate createdAt;
    private LocalDate updatedAt;

}
