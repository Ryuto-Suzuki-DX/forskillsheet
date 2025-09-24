package com.jp.dataxeed.pm.service;

import java.util.stream.Collectors;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.jp.dataxeed.pm.entity.CategoryEntity;
import com.jp.dataxeed.pm.form.category.CategoryForm;
import com.jp.dataxeed.pm.form.category.SearchCategoryForm;
import com.jp.dataxeed.pm.helper.CategoryHelper;
import com.jp.dataxeed.pm.mapper.CategoryMapper;

@Service
public class CategoryService {

    private final CategoryMapper categoryMapper;
    private final CategoryHelper categoryHelper;

    @Autowired
    public CategoryService(CategoryMapper categoryMapper, CategoryHelper categoryHelper) {
        this.categoryMapper = categoryMapper;
        this.categoryHelper = categoryHelper;
    }

    // findAll
    public List<CategoryForm> findAll() {
        return categoryMapper.findAll().stream()
                .map(categoryHelper::entityToForm)
                .collect(Collectors.toList());
    }

    // searchCategoryForm → List<CategoryForm>
    public List<CategoryForm> searchCategory(SearchCategoryForm searchCategoryForm) {
        if (searchCategoryForm == null) {
            return List.of();
        }
        return categoryMapper.searchCategory(searchCategoryForm).stream()
                .map(categoryHelper::entityToForm)
                .collect(Collectors.toList());
    }

    // categoryId → CategoryEntity → CategoryForm
    public CategoryForm findById(int categoryId) {
        CategoryEntity categoryEntity = categoryMapper.findById(categoryId);
        if (categoryEntity == null) {
            return null;
        }
        return categoryHelper.entityToForm(categoryEntity);
    }

    // save
    public void saveCategory(CategoryForm categoryForm) {
        CategoryEntity categoryEntity = categoryHelper.formToEntity(categoryForm);
        if (categoryEntity != null) {
            if (categoryEntity.getId() == null) {
                categoryMapper.insertToSave(categoryEntity);
            } else {
                categoryMapper.updateToSave(categoryEntity);
            }
        } else {
            System.out.println("categoryEntityがNull");
        }
    }

    // delete
    public void deleteCategory(int id) {
        categoryMapper.deleteCategory(id);
    }

}
