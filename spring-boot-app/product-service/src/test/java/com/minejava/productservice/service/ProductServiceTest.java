package com.minejava.productservice.service;

import com.minejava.productservice.dto.ProductRequest;
import com.minejava.productservice.dto.ProductResponse;
import com.minejava.productservice.model.Product;
import com.minejava.productservice.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private ProductService productService;

    @Test
    void updateProductNormalizesSkuAndBuildsDefaultImage() {
        Product existing = Product.builder()
                .id("product-1")
                .name("Old name")
                .description("Old description")
                .price(new BigDecimal("10.00"))
                .build();
        ProductRequest request = ProductRequest.builder()
                .skuCode(" sku new ")
                .name("New name")
                .price(new BigDecimal("12.50"))
                .build();
        when(productRepository.findById("product-1")).thenReturn(Optional.of(existing));

        ProductResponse response = productService.updateProduct("product-1", request);

        assertEquals("SKU NEW", response.getSkuCode());
        assertEquals("New name", response.getName());
        assertEquals("/assets/products/sku-new.svg", response.getImageUrl());
        verify(productRepository).save(existing);
    }
}
