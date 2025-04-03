package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductUpdateDTO {

    private String name;

    private String description;

    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private Integer stockQuantity;

    private String sku;

    private Boolean active;

    private Long categoryId;

    private List<ProductImageDTO> images = new ArrayList<>();

    private Map<String, String> attributes = new HashMap<>();
}