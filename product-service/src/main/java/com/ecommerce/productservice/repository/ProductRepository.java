package com.ecommerce.productservice.repository;

import com.ecommerce.productservice.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Long> {

    // Find products by category ID
    Page<Product> findByCategoryId(Long categoryId, Pageable pageable);

    // Find active products
    Page<Product> findByActiveTrue(Pageable pageable);

    // Find active products by category
    Page<Product> findByActiveTrueAndCategoryId(Long categoryId, Pageable pageable);

    // Search products by name or description
    @Query("SELECT p FROM Product p WHERE " +
            "(LOWER(p.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
            "LOWER(p.description) LIKE LOWER(CONCAT('%', :searchTerm, '%'))) " +
            "AND p.active = true")
    Page<Product> searchProducts(@Param("searchTerm") String searchTerm, Pageable pageable);

    // Find products by price range
    Page<Product> findByPriceBetweenAndActiveTrue(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // Find products by stock availability
    Page<Product> findByStockQuantityGreaterThanAndActiveTrue(Integer minStock, Pageable pageable);

    // Find products by SKU
    Product findBySku(String sku);
}
