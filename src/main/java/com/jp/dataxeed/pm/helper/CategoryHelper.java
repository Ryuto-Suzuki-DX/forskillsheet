package com.jp.dataxeed.pm.helper;

import org.springframework.stereotype.Component;

import com.jp.dataxeed.pm.entity.CategoryEntity;
import com.jp.dataxeed.pm.form.category.CategoryForm;

@Component
public class CategoryHelper {

    // Form → Entity
    public CategoryEntity formToEntity(CategoryForm categoryForm) {
        if (categoryForm == null) {
            return null;
        }

        CategoryEntity categoryEntity = new CategoryEntity();
        categoryEntity.setId(categoryForm.getId());
        categoryEntity.setName(categoryForm.getName());
        categoryEntity.setDeleteFlag(categoryForm.getDeleteFlag());
        categoryEntity.setCreatedAt(categoryForm.getCreatedAt());
        categoryEntity.setUpdatedAt(categoryForm.getUpdatedAt());

        return categoryEntity;
    }

    // Entity → Form
    public CategoryForm entityToForm(CategoryEntity categoryEntity) {
        if (categoryEntity == null) {
            return null;
        }

        CategoryForm categoryForm = new CategoryForm();
        categoryForm.setId(categoryEntity.getId());
        categoryForm.setName(categoryEntity.getName());
        categoryForm.setDeleteFlag(categoryEntity.getDeleteFlag());
        categoryForm.setCreatedAt(categoryEntity.getCreatedAt());
        categoryForm.setUpdatedAt(categoryEntity.getUpdatedAt());

        return categoryForm;

    }
}
