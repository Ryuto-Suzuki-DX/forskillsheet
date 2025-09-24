package com.jp.dataxeed.pm.entity;

import java.time.LocalDate;

import lombok.Data;

@Data
public class PartyEntity {
    private Integer id;
    private String partyCode;
    private String name;
    private String address;
    private String detail;
    private String attention;
    private Boolean deleteFlag;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
