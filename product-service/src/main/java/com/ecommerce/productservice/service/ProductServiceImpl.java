package com.ecommerce.productservice.service;

import com.ecommerce.productservice.dto.ProductCreateDTO;
import com.ecommerce.productservice.dto.ProductDTO;
import com.ecommerce.productservice.dto.ProductImageDTO;
import com.ecommerce.productservice.dto.ProductUpdateDTO;
import com.ecommerce.productservice.entity.Category;
import com.ecommerce.productservice.entity.Product;
import com.ecommerce.productservice.entity.ProductAttribute;
import com.ecommerce.productservice.entity.ProductImage;
import com.ecommerce.productservice.exception.ResourceNotFoundException;
import com.ecommerce.productservice.repository.CategoryRepository;
import com.ecommerce.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.modelmapper.ModelMapper;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ModelMapper modelMapper;

    @Override
    @Cacheable(value = "products", key = "#id")
    public ProductDTO getProductById(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        return mapToDTO(product);
    }

    @Override
    @Cacheable(value = "products", key = "'all_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductDTO> getAllProducts(Pageable pageable) {
        return productRepository.findByActiveTrue(pageable)
                .map(this::mapToDTO);
    }

    @Override
    @Cacheable(value = "products", key = "'category_' + #categoryId + '_' + #pageable.pageNumber + '_' + #pageable.pageSize")
    public Page<ProductDTO> getProductsByCategory(Long categoryId, Pageable pageable) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }

        return productRepository.findByActiveTrueAndCategoryId(categoryId, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public Page<ProductDTO> searchProducts(String query, Pageable pageable) {
        return productRepository.searchProducts(query, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public Page<ProductDTO> getProductsByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        return productRepository.findByPriceBetweenAndActiveTrue(minPrice, maxPrice, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public Page<ProductDTO> getInStockProducts(Pageable pageable) {
        return productRepository.findByStockQuantityGreaterThanAndActiveTrue(0, pageable)
                .map(this::mapToDTO);
    }

    @Override
    public ProductDTO getProductBySku(String sku) {
        Product product = productRepository.findBySku(sku);
        if (product == null) {
            throw new ResourceNotFoundException("Product", "sku", sku);
        }
        return mapToDTO(product);
    }

    @Override
    @Transactional
    public ProductDTO createProduct(ProductCreateDTO productDTO) {
        Product product = new Product();

        // Set basic properties
        product.setName(productDTO.getName());
        product.setDescription(productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setStockQuantity(productDTO.getStockQuantity());
        product.setSku(productDTO.getSku());
        product.setActive(productDTO.getActive() != null ? productDTO.getActive() : true);

        // Set category
        Category category = categoryRepository.findById(productDTO.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", productDTO.getCategoryId()));
        product.setCategory(category);

        // Set images
        if (productDTO.getImages() != null && !productDTO.getImages().isEmpty()) {
            productDTO.getImages().forEach(imageDTO -> {
                ProductImage image = new ProductImage();
                image.setUrl(imageDTO.getUrl());
                image.setAlt(imageDTO.getAlt());
                image.setPrimary(imageDTO.getIsPrimary() != null ? imageDTO.getIsPrimary() : false);
                image.setProduct(product);
                product.getImages().add(image);
            });
        }

        // Set attributes
        if (productDTO.getAttributes() != null && !productDTO.getAttributes().isEmpty()) {
            productDTO.getAttributes().forEach((key, value) -> {
                ProductAttribute attribute = new ProductAttribute();
                attribute.setName(key);
                attribute.setValue(value);
                attribute.setProduct(product);
                product.getAttributes().add(attribute);
            });
        }

        // Save and return
        Product savedProduct = productRepository.save(product);
        return mapToDTO(savedProduct);
    }

    @Override
    @Transactional
    @CachePut(value = "products", key = "#id")
    public ProductDTO updateProduct(Long id, ProductUpdateDTO productDTO) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        // Update basic properties if provided
        if (productDTO.getName() != null) {
            product.setName(productDTO.getName());
        }

        if (productDTO.getDescription() != null) {
            product.setDescription(productDTO.getDescription());
        }

        if (productDTO.getPrice() != null) {
            product.setPrice(productDTO.getPrice());
        }

        if (productDTO.getStockQuantity() != null) {
            product.setStockQuantity(productDTO.getStockQuantity());
        }

        if (productDTO.getSku() != null) {
            product.setSku(productDTO.getSku());
        }

        if (productDTO.getActive() != null) {
            product.setActive(productDTO.getActive());
        }

        // Update category if provided
        if (productDTO.getCategoryId() != null) {
            Category category = categoryRepository.findById(productDTO.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", productDTO.getCategoryId()));
            product.setCategory(category);
        }

        // Update images if provided
        if (productDTO.getImages() != null) {
            // Clear existing images
            product.getImages().clear();

            // Add new images
            productDTO.getImages().forEach(imageDTO -> {
                ProductImage image = new ProductImage();
                image.setUrl(imageDTO.getUrl());
                image.setAlt(imageDTO.getAlt());
                image.setPrimary(imageDTO.getIsPrimary() != null ? imageDTO.getIsPrimary() : false);
                image.setProduct(product);
                product.getImages().add(image);
            });
        }

        // Update attributes if provided
        if (productDTO.getAttributes() != null) {
            // Clear existing attributes
            product.getAttributes().clear();

            // Add new attributes
            productDTO.getAttributes().forEach((key, value) -> {
                ProductAttribute attribute = new ProductAttribute();
                attribute.setName(key);
                attribute.setValue(value);
                attribute.setProduct(product);
                product.getAttributes().add(attribute);
            });
        }

        // Save and return
        Product updatedProduct = productRepository.save(product);
        return mapToDTO(updatedProduct);
    }

    @Override
    @Transactional
    @CacheEvict(value = "products", key = "#id")
    public void deleteProduct(Long id) {
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product", "id", id);
        }

        productRepository.deleteById(id);
    }

    @Override
    @Transactional
    @CachePut(value = "products", key = "#id")
    public ProductDTO updateStock(Long id, Integer quantity) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        product.setStockQuantity(quantity);
        Product updatedProduct = productRepository.save(product);

        return mapToDTO(updatedProduct);
    }

    // Helper method to map Product entity to ProductDTO
    private ProductDTO mapToDTO(Product product) {
        ProductDTO productDTO = modelMapper.map(product, ProductDTO.class);

        // Set category info
        if (product.getCategory() != null) {
            productDTO.setCategoryId(product.getCategory().getId());
            productDTO.setCategoryName(product.getCategory().getName());
        }

        // Map images
        if (product.getImages() != null && !product.getImages().isEmpty()) {
            productDTO.setImages(product.getImages().stream()
                    .map(image -> {
                        ProductImageDTO imageDTO = new ProductImageDTO();
                        imageDTO.setId(image.getId());
                        imageDTO.setUrl(image.getUrl());
                        imageDTO.setAlt(image.getAlt());
                        imageDTO.setIsPrimary(image.isPrimary());
                        return imageDTO;
                    })
                    .collect(Collectors.toList()));
        }

        // Map attributes
        if (product.getAttributes() != null && !product.getAttributes().isEmpty()) {
            Map<String, String> attributes = product.getAttributes().stream()
                    .collect(Collectors.toMap(
                            ProductAttribute::getName,
                            ProductAttribute::getValue
                    ));
            productDTO.setAttributes(attributes);
        }

        return productDTO;
    }
}
