package com.jp.dataxeed.pm.dto;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;

import lombok.Data;

@Data
public class OrderWithDetailsDto {
    private Integer id;
    private String orderCode;

    private Integer partyId;
    private String partyCode;
    private String partyName;

    private String trackingNumber;

    @DateTimeFormat(pattern = "yyyy-MM-dd") // 追加
    private LocalDate deliveryDate; // 配送日

    private Integer adminId;
    private Integer qualityInspectorId;
    private Integer warehouseWorkerId;
    private String adminName;
    private String qualityInspectorName;
    private String warehouseWorkerName;
    private String adminNote;
    private String qualityInspectorNote;
    private String warehouseWorkerNote;

    private Integer locationId;

    private String situation;

    private LocalDate createdAt;
    private LocalDate updatedAt;

    private List<Integer> productIds;

    private List<ProductWithDetailsDto> productWithDetailsDtos = new ArrayList<>(); // こいつは重たくならないように、後でjavaで処理時に乗せる

    private List<PictureDto> pictures;
}
