package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.CategoryDTO;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.exception.BadRequestException;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    @Cacheable(value = "categories", key = "#id")
    public CategoryDTO getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        return mapToDTO(category, true);
    }

    @Override
    @Cacheable(value = "categories", key = "'all'")
    public List<CategoryDTO> getAllCategories() {
        return categoryRepository.findAll().stream()
                .map(category -> mapToDTO(category, false))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "categories", key = "'topLevel'")
    public List<CategoryDTO> getTopLevelCategories() {
        return categoryRepository.findByParentIsNull().stream()
                .map(category -> mapToDTO(category, true))
                .collect(Collectors.toList());
    }

    @Override
    @Cacheable(value = "categories", key = "'sub_' + #parentId")
    public List<CategoryDTO> getSubcategories(Long parentId) {
        if (!categoryRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Category", "id", parentId);
        }

        return categoryRepository.findByParentId(parentId).stream()
                .map(category -> mapToDTO(category, false))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public CategoryDTO createCategory(CategoryDTO categoryDTO) {
        // Check if category with same name already exists
        if (categoryRepository.findByName(categoryDTO.getName()).isPresent()) {
            throw new BadRequestException("Category with name '" + categoryDTO.getName() + "' already exists");
        }

        Category category = new Category();
        category.setName(categoryDTO.getName());
        category.setDescription(categoryDTO.getDescription());

        // Set parent category if provided
        if (categoryDTO.getParentId() != null) {
            Category parent = categoryRepository.findById(categoryDTO.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", categoryDTO.getParentId()));
            category.setParent(parent);
        }

        Category savedCategory = categoryRepository.save(category);
        return mapToDTO(savedCategory, false);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public CategoryDTO updateCategory(Long id, CategoryDTO categoryDTO) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Check if name is being changed and if new name already exists
        if (categoryDTO.getName() != null && !categoryDTO.getName().equals(category.getName())) {
            categoryRepository.findByName(categoryDTO.getName()).ifPresent(existingCategory -> {
                throw new BadRequestException("Category with name '" + categoryDTO.getName() + "' already exists");
            });
            category.setName(categoryDTO.getName());
        }

        // Update description if provided
        if (categoryDTO.getDescription() != null) {
            category.setDescription(categoryDTO.getDescription());
        }

        // Update parent if provided
        if (categoryDTO.getParentId() != null) {
            // Cannot set parent to itself
            if (categoryDTO.getParentId().equals(id)) {
                throw new BadRequestException("Category cannot be its own parent");
            }

            Category parent = categoryRepository.findById(categoryDTO.getParentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Parent Category", "id", categoryDTO.getParentId()));
            category.setParent(parent);
        } else if (categoryDTO.getParentId() == null && categoryDTO.getParentId() != category.getParent().getId()) {
            // Remove parent if null is explicitly provided
            category.setParent(null);
        }

        Category updatedCategory = categoryRepository.save(category);
        return mapToDTO(updatedCategory, false);
    }

    @Override
    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Check if category has products
        if (categoryRepository.hasProducts(id)) {
            throw new BadRequestException("Cannot delete category with products");
        }

        // Check if category has subcategories
        if (!category.getSubcategories().isEmpty()) {
            throw new BadRequestException("Cannot delete category with subcategories");
        }

        categoryRepository.delete(category);
    }

    @Override
    public long countProductsInCategory(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }

        return categoryRepository.countProductsByCategoryId(categoryId);
    }

    // Helper method to map Category entity to CategoryDTO
    private CategoryDTO mapToDTO(Category category, boolean includeSubcategories) {
        CategoryDTO categoryDTO = new CategoryDTO();
        categoryDTO.setId(category.getId());
        categoryDTO.setName(category.getName());
        categoryDTO.setDescription(category.getDescription());

        if (category.getParent() != null) {
            categoryDTO.setParentId(category.getParent().getId());
        }

        // Recursively map subcategories if requested
        if (includeSubcategories && category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            categoryDTO.setSubcategories(
                    category.getSubcategories().stream()
                            .map(subcategory -> mapToDTO(subcategory, false))
                            .collect(Collectors.toList())
            );
        }

        return categoryDTO;
    }
}
