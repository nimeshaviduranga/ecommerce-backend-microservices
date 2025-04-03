package com.ecommerce.productservice.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductImageDTO {

    private Long id;

    @NotBlank(message = "Image URL is required")
    private String url;

    private String alt;

    private Boolean isPrimary = false;
}
