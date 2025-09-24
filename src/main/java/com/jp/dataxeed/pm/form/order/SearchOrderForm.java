package com.jp.dataxeed.pm.form.order;

import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import lombok.Data;

@Data
public class SearchOrderForm {
    private Integer id;
    private String orderCode;
    private String partyCode;
    private String partyName; // paryName → partyName（修正）
    private String trackingNumber;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate deliveryDate; // 配送日

    // “Name”は文字列なのでStringに（検索で部分一致できるように）
    private String adminName;
    private String qualityInspectorName;
    private String warehouseWorkerName;

    private String adminNote;
    private String qualityInspectorNote;
    private String warehouseWorkerNote;

    private String locationName; // Integer → String（名称検索なら文字列）

    private String situation;

    // 製品名で検索するため
    private String productName1;
    private String productName2;

    // how_csvを見る
    private Boolean howCsv;

    // 画像の有無
    private Boolean hasImage;

    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate createdAtTo;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtFrom;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate updatedAtTo;
}
