package com.ecommerce.productservice.service;


import com.ecommerce.productservice.dto.ProductCreateDTO;
import com.ecommerce.productservice.dto.ProductDTO;
import com.ecommerce.productservice.dto.ProductUpdateDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;

public interface ProductService {

    ProductDTO getProductById(Long id);

    Page<ProductDTO> getAllProducts(Pageable pageable);

    Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable);

    Page<ProductDTO> searchProducts(String query, Pageable pageable);

    Page<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    Page<ProductDTO> getInStockProducts(Pageable pageable);

    ProductDTO createProduct(ProductCreateDTO productDTO);

    ProductDTO updateProduct(Long id, ProductUpdateDTO productDTO);

    void deleteProduct(Long id);

    ProductDTO updateStock(Long id, Integer quantity);

    ProductDTO getProductBySku(String sku);
}

