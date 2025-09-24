package com.jp.dataxeed.pm.mapper;

import java.util.List;

import com.jp.dataxeed.pm.entity.LocationEntity;
import com.jp.dataxeed.pm.form.location.SearchLocationForm;

public interface LocationMapper {

    // findAll
    List<LocationEntity> findAll();

    // IDで取得
    LocationEntity findById(int locationId);

    // 保存用insert
    void insertToSave(LocationEntity locationEntity);

    // 保存用update
    void updateToSave(LocationEntity locationEntity);

    // 理論削除
    void deleteLocation(int id);

    // 検索
    List<LocationEntity> searchLocation(SearchLocationForm searchLocationForm);

}
