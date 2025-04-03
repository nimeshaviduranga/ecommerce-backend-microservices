package com.ecommerce.productservice.repository;


import com.ecommerce.productservice.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    Optional<Category> findByName(String name);

    // Find top-level categories (no parent)
    List<Category> findByParentIsNull();

    // Find direct subcategories of a parent category
    List<Category> findByParentId(Long parentId);

    // Check if a category has products
    @Query("SELECT CASE WHEN COUNT(p) > 0 THEN true ELSE false END FROM Product p WHERE p.category.id = :categoryId")
    boolean hasProducts(Long categoryId);

    // Count products in a category
    @Query("SELECT COUNT(p) FROM Product p WHERE p.category.id = :categoryId")
    long countProductsByCategoryId(Long categoryId);
}
