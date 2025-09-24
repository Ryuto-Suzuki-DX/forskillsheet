package com.jp.dataxeed.pm.form.order;

import java.time.LocalDate;

import lombok.Data;

@Data
public class OrderForm {
    private Integer id;
    private String orderCode;
    private Integer partyId;
    private String trackingNumber;
    private LocalDate deliveryDate; // 追加: 配送日
    private String adminNote;
    private String warehouseWorkerNote;
    private String qualityInspectorNote;
    private Integer adminId;
    private Integer warehouseWorkerId;
    private Integer qualityInspectorId;
    private String situation;
    private Integer LocationId;
    private LocalDate createdAt;
    private LocalDate updatedAt;
}
