package com.jp.dataxeed.pm.form.user;

import java.time.LocalDate;

import lombok.Data;

@Data
public class UserForm {
    private Integer id;
    private String username;
    private String name;
    private String password;
    private String role; // 例: "ADMIN", "GENERAL"
    private Boolean deleteFlag;
    private LocalDate createdAt;
    private LocalDate updatedAt;

}
