package com.minejava.productservice.service;

import java.util.Arrays;
import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.minejava.productservice.dto.InventoryResponse;
import com.minejava.productservice.dto.ProductRequest;
import com.minejava.productservice.dto.ProductResponse;
import com.minejava.productservice.model.Product;
import com.minejava.productservice.repository.ProductRepository;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final WebClient.Builder webClientBuilder;
    private static final List<DemoProduct> DEMO_PRODUCTS = List.of(
            new DemoProduct("IPHONE-15-PRO-256-TITANIUM", "iPhone 15 Pro 256GB Titanium",
                    "Chip A17 Pro, màn hình Super Retina XDR 6.1 inch, camera Pro 48MP.",
                    new BigDecimal("28990000"), "/assets/products/iphone-15-pro.svg", 12),
            new DemoProduct("SAMSUNG-S24-ULTRA-256-BLACK", "Samsung Galaxy S24 Ultra 256GB",
                    "Galaxy AI, màn hình Dynamic AMOLED 2X 6.8 inch, bút S Pen, camera 200MP.",
                    new BigDecimal("26990000"), "/assets/products/samsung-s24-ultra.svg", 10),
            new DemoProduct("MACBOOK-AIR-M3-13-512-MIDNIGHT", "MacBook Air M3 13 inch 512GB",
                    "Chip Apple M3, RAM 16GB, SSD 512GB, màn hình Liquid Retina, pin cả ngày.",
                    new BigDecimal("32990000"), "/assets/products/macbook-air-m3.svg", 8),
            new DemoProduct("AIRPODS-PRO-2-USB-C", "AirPods Pro 2 USB-C",
                    "Chống ồn chủ động, Adaptive Audio, hộp sạc USB-C, âm thanh không gian.",
                    new BigDecimal("5490000"), "/assets/products/airpods-pro-2.svg", 25),
            new DemoProduct("IPAD-AIR-M2-11-128-WIFI", "iPad Air M2 11 inch 128GB Wi-Fi",
                    "Chip M2, màn hình Liquid Retina 11 inch, hỗ trợ Apple Pencil Pro.",
                    new BigDecimal("16990000"), "/assets/products/ipad-air-m2.svg", 14),
            new DemoProduct("SONY-WH1000XM5-BLACK", "Sony WH-1000XM5 Black",
                    "Tai nghe chống ồn cao cấp, âm thanh Hi-Res, pin đến 30 giờ.",
                    new BigDecimal("7990000"), "/assets/products/sony-wh1000xm5.svg", 18)
    );

    public void createProduct(ProductRequest productRequest) {
        String skuCode = normalizeSku(productRequest.getSkuCode(), productRequest.getName());
        Product product = Product.builder()
                .skuCode(skuCode)
                .name(productRequest.getName())
                .description(productRequest.getDescription())
                .price(productRequest.getPrice())
                .imageUrl(defaultImage(productRequest.getImageUrl(), skuCode))
                .build();

        productRepository.save(product);
        log.info("Product {} is saved", product.getId());

        try {
            int initialQuantity = productRequest.getQuantity() != null ? productRequest.getQuantity() : 0;
            webClientBuilder.build().post()
                    .uri("http://inventory-service:8080/api/inventory/init")
                    .bodyValue(Map.of("skuCode", product.getSkuCode(), "quantity", initialQuantity))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
            log.info("Inventory initialized for {} with quantity {}", product.getSkuCode(), initialQuantity);
        } catch (Exception e) {
            log.error("Failed to initialize inventory: {}", e.getMessage());
        }
    }

    public void deleteProduct(String id) {
        productRepository.deleteById(id);
        log.info("Product {} is deleted", id);
    }

    public ProductResponse updateProduct(String id, ProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));

        if (request.getName() != null) product.setName(request.getName());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getImageUrl() != null) product.setImageUrl(request.getImageUrl());
        if (request.getSkuCode() != null && !request.getSkuCode().isBlank()) {
            product.setSkuCode(normalizeSku(request.getSkuCode(), product.getName()));
        }
        productRepository.save(product);
        log.info("Product {} is updated", id);

        if (request.getQuantity() != null) {
            syncInventory(product.getSkuCode(), request.getQuantity());
        }
        return mapToProductResponse(product, request.getQuantity() == null ? 0 : request.getQuantity());
    }

    public List<ProductResponse> getAllProducts() {
        ensureDemoCatalog();
        List<Product> products = productRepository.findAll();
        if (products.isEmpty()) return List.of();

        List<String> skuCodes = products.stream()
                .map(product -> normalizeSku(product.getSkuCode(), product.getName()))
                .toList();

        try {
            InventoryResponse[] inventoryResponses = webClientBuilder.build().get()
                    .uri("http://inventory-service:8080/api/inventory",
                            uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                    .retrieve()
                    .bodyToMono(InventoryResponse[].class)
                    .block();

            Map<String, Integer> inventoryMap = Arrays.stream(inventoryResponses != null ? inventoryResponses : new InventoryResponse[0])
                    .collect(Collectors.toMap(
                        i -> i.getSkuCode().toLowerCase(), 
                        InventoryResponse::getQuantity,
                        (v1, v2) -> v1 // Nếu trùng thì lấy cái đầu
                    ));

            return products.stream()
                    .map(product -> {
                        String skuCode = normalizeSku(product.getSkuCode(), product.getName());
                        return mapToProductResponse(product, inventoryMap.getOrDefault(skuCode.toLowerCase(), 0));
                    })
                    .toList();
        } catch (Exception e) {
            log.error("Failed to fetch inventory: {}", e.getMessage());
            return products.stream().map(product -> mapToProductResponse(product, 0)).toList();
        }
    }

    private ProductResponse mapToProductResponse(Product product, Integer quantity) {
        String skuCode = normalizeSku(product.getSkuCode(), product.getName());
        return ProductResponse.builder()
                .id(product.getId())
                .skuCode(skuCode)
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .imageUrl(defaultImage(product.getImageUrl(), skuCode))
                .quantity(quantity)
                .build();
    }

    private void ensureDemoCatalog() {
        List<Product> products = productRepository.findAll();
        Map<String, Product> byName = products.stream()
                .collect(Collectors.toMap(Product::getName, product -> product, (left, right) -> left, LinkedHashMap::new));

        for (DemoProduct demo : DEMO_PRODUCTS) {
            Product product = byName.get(demo.name());
            boolean newProduct = product == null;
            if (product == null) {
                product = Product.builder()
                        .skuCode(demo.skuCode())
                        .name(demo.name())
                        .description(demo.description())
                        .price(demo.price())
                        .imageUrl(demo.imageUrl())
                        .build();
            } else {
                product.setSkuCode(demo.skuCode());
                product.setDescription(demo.description());
                product.setPrice(demo.price());
                product.setImageUrl(demo.imageUrl());
            }
            productRepository.save(product);
            if (newProduct) {
                syncInventory(demo.skuCode(), demo.quantity());
            }
        }

        products.stream()
                .filter(product -> product.getSkuCode() == null || product.getSkuCode().isBlank())
                .forEach(product -> {
                    product.setSkuCode(normalizeSku(product.getSkuCode(), product.getName()));
                    product.setImageUrl(defaultImage(product.getImageUrl(), product.getSkuCode()));
                    productRepository.save(product);
                });
    }

    private void syncInventory(String skuCode, Integer quantity) {
        try {
            webClientBuilder.build().post()
                    .uri("http://inventory-service:8080/api/inventory/init")
                    .bodyValue(Map.of("skuCode", skuCode, "quantity", quantity))
                    .retrieve()
                    .toBodilessEntity()
                    .block();
        } catch (Exception e) {
            log.warn("Inventory sync deferred for {}: {}", skuCode, e.getMessage());
        }
    }

    private String normalizeSku(String skuCode, String name) {
        if (skuCode != null && !skuCode.isBlank()) {
            return skuCode.trim().toUpperCase();
        }
        String source = name == null || name.isBlank() ? "PRODUCT" : name;
        String normalized = Normalizer.normalize(source, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]+", "-")
                .replaceAll("^-|-$", "")
                .toUpperCase();
        return normalized.isBlank() ? "PRODUCT" : normalized;
    }

    private String defaultImage(String imageUrl, String skuCode) {
        if (imageUrl != null && !imageUrl.isBlank()) {
            return imageUrl;
        }
        return "/assets/products/" + skuCode.toLowerCase().replaceAll("[^a-z0-9]+", "-") + ".svg";
    }

    private record DemoProduct(String skuCode, String name, String description, BigDecimal price, String imageUrl, Integer quantity) {}
}
