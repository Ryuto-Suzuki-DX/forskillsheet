package com.jp.dataxeed.pm.helper;

import org.springframework.stereotype.Component;

import com.jp.dataxeed.pm.entity.UserEntity;
import com.jp.dataxeed.pm.form.user.UserForm;

@Component
public class UserHelper {

    // UserEntity → UserForm
    public UserForm EntityToForm(UserEntity userEntity) {
        if (userEntity == null) {
            return null;
        }
        UserForm userForm = new UserForm();
        userForm.setId(userEntity.getId());
        userForm.setUsername(userEntity.getUsername());
        userForm.setName(userEntity.getName());
        userForm.setPassword(userEntity.getPassword());
        userForm.setRole(userEntity.getRole());
        userForm.setCreatedAt(userEntity.getCreatedAt());
        userForm.setUpdatedAt(userEntity.getUpdatedAt());

        return userForm;
    }

    // UserForm → UserEntity
    public UserEntity formToEntity(UserForm userForm) {
        if (userForm == null) {
            return null;
        }
        UserEntity userEntity = new UserEntity();
        userEntity.setId(userForm.getId());
        userEntity.setUsername(userForm.getUsername());
        userEntity.setName(userForm.getName());
        userEntity.setPassword(userForm.getPassword());
        userEntity.setRole(userForm.getRole());
        userEntity.setCreatedAt(userForm.getCreatedAt());
        userEntity.setUpdatedAt(userForm.getUpdatedAt());

        return userEntity;
    }
}
