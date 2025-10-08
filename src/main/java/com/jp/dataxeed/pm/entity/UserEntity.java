package com.jp.dataxeed.pm.entity;

import java.time.LocalDate;

import lombok.Data;

@Data
public class UserEntity {
    private Integer id;
    private String username;
    private String name;
    private String password;
    private String role; // : "ADMIN", "GENERAL"
    private Boolean deleteFlag;
    private LocalDate createdAt;
    private LocalDate updatedAt;

}
