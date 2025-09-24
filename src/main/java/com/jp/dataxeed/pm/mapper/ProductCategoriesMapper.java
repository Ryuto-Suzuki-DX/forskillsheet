package com.jp.dataxeed.pm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface ProductCategoriesMapper {

    void delete(int id);

    void insert(@Param("productId") int productId, @Param("categoryIds") List<Integer> categoryIds);
}
