package com.jp.dataxeed.pm.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

public interface ProductLocationsMapper {

    void delete(int id);

    void insert(@Param("productId") int productId, @Param("locationIds") List<Integer> locationIds);
}
