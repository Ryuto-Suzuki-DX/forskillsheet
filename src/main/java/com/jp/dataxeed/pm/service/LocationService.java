package com.jp.dataxeed.pm.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jp.dataxeed.pm.entity.LocationEntity;
import com.jp.dataxeed.pm.form.location.LocationForm;
import com.jp.dataxeed.pm.form.location.SearchLocationForm;
import com.jp.dataxeed.pm.helper.LocationHelper;
import com.jp.dataxeed.pm.mapper.LocationMapper;

@Service
public class LocationService {

    private final LocationMapper locationMapper;
    private final LocationHelper locationHelper;

    @Autowired
    public LocationService(LocationMapper locationMapper, LocationHelper locationHelper) {
        this.locationMapper = locationMapper;
        this.locationHelper = locationHelper;
    }

    // findAll
    public List<LocationForm> findAll() {
        return locationMapper.findAll().stream()
                .map(locationHelper::entityToForm)
                .collect(Collectors.toList());
    }

    // id → LocationEntity → LocationForm
    public LocationForm findById(int id) {
        return locationHelper.entityToForm(locationMapper.findById(id));
    }

    // save idの有無で新規登録か更新か判断
    public void saveLocation(LocationForm locationForm) {
        if (locationForm == null) {
            return;
        }
        LocationEntity locationEntity = locationHelper.formToEntity(locationForm);
        if (locationForm.getId() == null) {
            locationMapper.insertToSave(locationEntity);
        } else {
            locationMapper.updateToSave(locationEntity);
        }
    }

    // 理論削除
    public void deleteLocation(int id) {
        locationMapper.deleteLocation(id);
    }

    // 検索 searchLocation → List<LocationEntity> → List<LocationForm>
    public List<LocationForm> searchLocation(SearchLocationForm searchLocationForm) {
        if (searchLocationForm == null) {
            return null;
        }
        return locationMapper.searchLocation(searchLocationForm).stream()
                .map(locationHelper::entityToForm)
                .collect(Collectors.toList());

    }

}
