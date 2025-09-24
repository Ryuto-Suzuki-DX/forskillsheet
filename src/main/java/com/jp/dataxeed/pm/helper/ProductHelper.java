package com.jp.dataxeed.pm.helper;

import org.springframework.stereotype.Component;

import com.jp.dataxeed.pm.dto.ProductWithDetailsDto;
import com.jp.dataxeed.pm.entity.ProductEntity;

@Component
public class ProductHelper {

    // Entity → Form

    // Form → Entity

    // productWithDetailsDto → productEntity
    public ProductEntity dtoToEntity(ProductWithDetailsDto productWithDetailsDto) {
        ProductEntity productEntity = new ProductEntity();
        productEntity.setId(productWithDetailsDto.getId());
        productEntity.setProductCode(productWithDetailsDto.getProductCode());
        productEntity.setName(productWithDetailsDto.getName());
        productEntity.setCreatedAt(productWithDetailsDto.getCreatedAt());
        productEntity.setUpdatedAt(productWithDetailsDto.getUpdatedAt());
        return productEntity;
    }

}
