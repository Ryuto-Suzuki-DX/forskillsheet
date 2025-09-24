package com.jp.dataxeed.pm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.jp.dataxeed.pm.dto.CategoryDto;
import com.jp.dataxeed.pm.dto.LocationDto;
import com.jp.dataxeed.pm.dto.ProductWithDetailsDto;
import com.jp.dataxeed.pm.entity.ProductEntity;
import com.jp.dataxeed.pm.form.product.SearchProductForm;

public interface ProductMapper {

    /* ========== CUD ========== */

    /** INSERT（useGeneratedKeys=true で entity.id に採番） */
    int insertToSave(ProductEntity productEntity);

    /** UPDATE（論理項目の更新含む） */
    void updateToSave(ProductEntity productEntity);

    /** 論理削除（products.delete_flag = true） */
    void deleteProduct(int id);

    /* ========== 取得系（ResultMap + ネストselect） ========== */

    /** 全件（delete_flag = false のみ） */
    List<ProductWithDetailsDto> findAllWithDetails();

    /** 単体（id指定） */
    ProductWithDetailsDto findByIdWithDetails(int id);

    /** 複数ID（IN句）。XML 側は collection="list" を使うため @Param("list") を付与 */
    List<ProductWithDetailsDto> findByIdsWithDetails(@Param("list") List<Integer> ids);

    /** 検索（条件は SearchProductForm） */
    List<ProductWithDetailsDto> searchWithDetails(SearchProductForm searchProductForm);

    /* ========== ネストselect用（XML の <collection select="..."> が呼ぶ） ========== */

    /** 指定 productId のカテゴリ一覧 */
    List<CategoryDto> findCategoriesByProductId(int id);

    /** 指定 productId のロケーション一覧 */
    List<LocationDto> findLocationsByProductId(int id);

    // コードでid
    Integer findIdByProductCode(@org.apache.ibatis.annotations.Param("productCode") String productCode);
}
