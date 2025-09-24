package com.jp.dataxeed.pm.dto;

import lombok.Data;

@Data
public class PictureDto {
    private Integer id;
    private String fileName; // ← name から変更
    private String fileType;
    private Long fileSize;
    private String filePath;
}
