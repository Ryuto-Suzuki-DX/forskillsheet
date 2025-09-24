package com.jp.dataxeed.pm.entity;

import lombok.Data;

@Data
public class PictureEntity {
    private Integer id; // 主キー
    private String fileName; // 保存した実ファイル名（ユニーク名）
    private String fileType; // MIMEタイプ
    private Long fileSize; // バイト
    private String filePath; // 保存パス（配信URL or FSパス）
}
