package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class ProductCreateDTO {

    @NotBlank(message = "Product name is required")
    private String name;

    private String description;

    @NotNull(message = "Product price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private Integer stockQuantity;

    private String sku;

    private Boolean active = true;

    @NotNull(message = "Category ID is required")
    private Long categoryId;

    private List<ProductImageDTO> images = new ArrayList<>();

    private Map<String, String> attributes = new HashMap<>();
}