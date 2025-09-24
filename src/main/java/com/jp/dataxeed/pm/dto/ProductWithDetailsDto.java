package com.jp.dataxeed.pm.dto;

import java.time.LocalDate;
import java.util.List;

import lombok.Data;

@Data
public class ProductWithDetailsDto {

    private Integer id;
    private String productCode;
    private String name;
    private LocalDate createdAt;
    private LocalDate updatedAt;

    // 既存の詳細情報
    private List<CategoryDto> categories;
    private List<LocationDto> locations;

    // IDだけを持つリスト（フォームバインド用）
    private List<Integer> categoryIds;
    private List<Integer> locationIds;

    // Order用
    private Integer quantity;

    // 在庫合計（表示/検索用）
    private Integer totalQuantity;
}
