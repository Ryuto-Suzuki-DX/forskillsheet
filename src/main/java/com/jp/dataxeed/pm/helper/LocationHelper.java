package com.jp.dataxeed.pm.helper;

import org.springframework.stereotype.Component;

import com.jp.dataxeed.pm.entity.LocationEntity;
import com.jp.dataxeed.pm.form.location.LocationForm;

@Component
public class LocationHelper {

    public LocationForm entityToForm(LocationEntity locationEntity) {
        LocationForm locationForm = new LocationForm();
        locationForm.setId(locationEntity.getId());
        locationForm.setName(locationEntity.getName());
        locationForm.setDeleteFlag(locationEntity.getDeleteFlag());
        locationForm.setCreatedAt(locationEntity.getCreatedAt());
        locationForm.setUpdatedAt(locationEntity.getUpdatedAt());
        return locationForm;
    }

    public LocationEntity formToEntity(LocationForm locationForm) {
        LocationEntity locationEntity = new LocationEntity();
        locationEntity.setId(locationForm.getId());
        locationEntity.setName(locationForm.getName());
        locationEntity.setDeleteFlag(locationForm.getDeleteFlag());
        locationEntity.setCreatedAt(locationForm.getCreatedAt());
        locationEntity.setUpdatedAt(locationForm.getUpdatedAt());
        return locationEntity;
    }
}
