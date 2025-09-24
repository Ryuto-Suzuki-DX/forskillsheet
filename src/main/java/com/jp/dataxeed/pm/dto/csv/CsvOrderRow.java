package com.jp.dataxeed.pm.dto.csv;

import lombok.Data;

@Data
public class CsvOrderRow {
    private long rowNum; // CSVレコード番号
    private String importKey; // 注文グルーピングキー
    private String mode; // IN / OUT
    private Integer partyId; // 必須
    private String trackingNumber;
    private String deliveryDate; // yyyy-MM-dd（文字で受けて後でparse）
    private String situation; // "完了" 禁止
    private Integer locationId; // 任意
    private Integer adminId; // 必須（ADMIN権限チェック）
    private Integer workerId; // 任意
    private Integer inspectorId; // 任意
    private String adminNote;
    private String workerNote;
    private String inspectorNote;
    private String productCode; // 必須（ユニーク前提 → ID解決）
    private Integer quantity; // 必須 >= 1
}
