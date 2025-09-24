package com.jp.dataxeed.pm.mapper;

import java.util.List;

import com.jp.dataxeed.pm.entity.CategoryEntity;
import com.jp.dataxeed.pm.form.category.SearchCategoryForm;

public interface CategoryMapper {

    // findAll
    List<CategoryEntity> findAll();

    // searchCategoryForm → List<CategoryEntity>
    List<CategoryEntity> searchCategory(SearchCategoryForm searchCategoryForm);

    // categoryId → categoryEntity
    CategoryEntity findById(int categoryId);

    // categoryEntity → insert
    void insertToSave(CategoryEntity categoryEntity);

    // categoryEntity → update
    void updateToSave(CategoryEntity categoryEntity);

    // deleteById
    void deleteCategory(int id);
}
