package com.jp.dataxeed.pm.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

public class PasswordHashGenerator {
    public static void main(String[] args) {
        String rawPassword = "1";
        String hashed = new BCryptPasswordEncoder().encode(rawPassword);
        System.out.println("Hashed password: " + hashed);
    }
}
