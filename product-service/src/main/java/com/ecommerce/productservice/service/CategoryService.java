package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.CategoryDTO;

import java.util.List;

public interface CategoryService {

    CategoryDTO getCategoryById(Long id);

    List<CategoryDTO> getAllCategories();

    List<CategoryDTO> getTopLevelCategories();

    List<CategoryDTO> getSubcategories(Long parentId);

    CategoryDTO createCategory(CategoryDTO categoryDTO);

    CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO);

    void deleteCategory(Long id);

    long countProductsInCategory(Long categoryId);
}
