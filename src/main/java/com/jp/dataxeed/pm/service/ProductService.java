package com.jp.dataxeed.pm.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jp.dataxeed.pm.dto.CategoryDto;
import com.jp.dataxeed.pm.dto.LocationDto;
import com.jp.dataxeed.pm.dto.ProductWithDetailsDto;
import com.jp.dataxeed.pm.entity.ProductEntity;
import com.jp.dataxeed.pm.form.product.SearchProductForm;
import com.jp.dataxeed.pm.helper.ProductHelper;
import com.jp.dataxeed.pm.mapper.ProductCategoriesMapper;
import com.jp.dataxeed.pm.mapper.ProductLocationsMapper;
import com.jp.dataxeed.pm.mapper.ProductMapper;

@Service
public class ProductService {

    private final ProductMapper productMapper;
    private final ProductHelper productHelper;
    private final ProductCategoriesMapper productCategoriesMapper;
    private final ProductLocationsMapper productLocationsMapper;

    @Autowired
    public ProductService(ProductMapper productMapper,
            ProductHelper productHelper,
            ProductCategoriesMapper productCategoriesMapper,
            ProductLocationsMapper productLocationsMapper) {
        this.productMapper = productMapper;
        this.productHelper = productHelper;
        this.productCategoriesMapper = productCategoriesMapper;
        this.productLocationsMapper = productLocationsMapper;
    }

    /*
     * =========================
     * 単体取得（既存）
     * =========================
     */
    // ID → ProductWithDetailsDto
    public ProductWithDetailsDto getProductWithDetailsDtoById(int id) {
        ProductWithDetailsDto dto = productMapper.findByIdWithDetails(id);
        if (dto == null)
            return null;
        fillIdLists(dto); // ← categoryIds / locationIds を詰める共通処理
        return dto;
    }

    /*
     * =========================
     * 検索（既存）
     * =========================
     */
    public List<ProductWithDetailsDto> searchProductWithDetailsDto(SearchProductForm form) {
        List<ProductWithDetailsDto> list = productMapper.searchWithDetails(form);
        // 画面側で categoryIds/locationIds を使う可能性があるなら詰めておく
        if (list != null) {
            list.forEach(this::fillIdLists);
        }
        return list;
    }

    /*
     * =========================
     * 保存（既存）
     * =========================
     */
    // 保存 更新 OR 新規登録
    public void saveProduct(ProductWithDetailsDto dto) {
        if (dto == null)
            return;

        if (dto.getId() == null) {
            // 新規
            ProductEntity entity = productHelper.dtoToEntity(dto);
            productMapper.insertToSave(entity);
            int newId = entity.getId();
            forProductdetails(newId, dto);
        } else {
            // 更新
            ProductEntity entity = productHelper.dtoToEntity(dto);
            productMapper.updateToSave(entity);
            int alreadyId = dto.getId();
            forProductdetails(alreadyId, dto);
        }
    }

    // 追加＆更新補助
    public void forProductdetails(int id, ProductWithDetailsDto dto) {
        // カテゴリのリセット＆追加
        productCategoriesMapper.delete(id);
        List<Integer> categoryIds = dto.getCategoryIds();
        if (categoryIds != null && !categoryIds.isEmpty()) {
            productCategoriesMapper.insert(id, categoryIds);
        }

        // ロケーションのリセット＆追加
        productLocationsMapper.delete(id);
        List<Integer> locationIds = dto.getLocationIds();
        if (locationIds != null && !locationIds.isEmpty()) {
            productLocationsMapper.insert(id, locationIds);
        }
    }

    /*
     * =========================
     * 削除（既存）
     * =========================
     */
    // Productは理論削除 ほかは完全削除
    public void deleteProduct(int id) {
        productMapper.deleteProduct(id);
        productCategoriesMapper.delete(id);
        productLocationsMapper.delete(id);
    }

    /*
     * =========================
     * 追加：複数ID一括取得（入力順で返す）
     * =========================
     */
    public List<ProductWithDetailsDto> getProductsWithDetailsByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty())
            return List.of();

        // 重複を除いたクエリ用ID（順序は問わない）
        List<Integer> distinct = ids.stream().filter(Objects::nonNull).distinct().toList();

        // ResultMap（ネストselect）でまとめて取得
        List<ProductWithDetailsDto> rows = productMapper.findByIdsWithDetails(distinct);

        // id -> DTO にマップ化
        Map<Integer, ProductWithDetailsDto> byId = new LinkedHashMap<>();
        for (ProductWithDetailsDto d : rows) {
            if (d == null || d.getId() == null)
                continue;
            fillIdLists(d); // ← categoryIds / locationIds を詰める（単体と同じ）
            byId.put(d.getId(), d);
        }

        // 入力順を維持して返す（存在しないIDはスキップ）
        return ids.stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /*
     * =========================
     * 共通ヘルパ
     * =========================
     */
    private void fillIdLists(ProductWithDetailsDto dto) {
        if (dto == null)
            return;

        // カテゴリ → categoryIds
        if (dto.getCategories() != null && !dto.getCategories().isEmpty()) {
            dto.setCategoryIds(dto.getCategories().stream()
                    .map(CategoryDto::getId)
                    .filter(Objects::nonNull)
                    .toList());
        } else {
            dto.setCategoryIds(List.of());
        }

        // ロケーション → locationIds
        if (dto.getLocations() != null && !dto.getLocations().isEmpty()) {
            dto.setLocationIds(dto.getLocations().stream()
                    .map(LocationDto::getId)
                    .filter(Objects::nonNull)
                    .toList());
        } else {
            dto.setLocationIds(List.of());
        }
    }

    // 既存のサービスに次を追加（実体は ProductMapper に実装）
    public Integer getProductIdByCode(String productCode) {
        if (productCode == null || productCode.isBlank())
            return null;
        return productMapper.findIdByProductCode(productCode);
    }

}
