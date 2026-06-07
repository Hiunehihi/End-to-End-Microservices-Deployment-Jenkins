package com.minejava.inventoryservice.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import com.minejava.inventoryservice.dto.InventoryResponse;
import com.minejava.inventoryservice.dto.OrderItemDto;
import com.minejava.inventoryservice.event.InventoryEvent;
import com.minejava.inventoryservice.event.PaymentEvent;
import com.minejava.inventoryservice.model.Inventory;
import com.minejava.inventoryservice.model.ProcessedInventoryEvent;
import com.minejava.inventoryservice.repository.InventoryRepository;
import com.minejava.inventoryservice.repository.ProcessedInventoryEventRepository;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryService {

    private final InventoryRepository inventoryRepository;
    private final ProcessedInventoryEventRepository processedInventoryEventRepository;
    private final KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    @Transactional(readOnly = true)
    public List<InventoryResponse> isInStock(List<String> skuCode) {
        List<String> lowerSkuCodes = skuCode.stream().map(String::toLowerCase).toList();
        
        return inventoryRepository.findAll().stream()
            .filter(inv -> lowerSkuCodes.contains(inv.getSkuCode().toLowerCase()))
            .map(inventory -> 
                InventoryResponse.builder()
                .skuCode(inventory.getSkuCode())
                .isInStock(inventory.getQuantity() > 0)
                .quantity(inventory.getQuantity())
                .build()
            ).toList();
    }

    @Transactional
    public void initInventory(String skuCode, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuCodeIgnoreCase(skuCode)
                .orElseGet(Inventory::new);
        inventory.setSkuCode(skuCode);
        inventory.setQuantity(quantity == null ? 0 : quantity);
        inventoryRepository.save(inventory);
        log.info("Inventory synced for {}: quantity {}", skuCode, inventory.getQuantity());
    }

    @Transactional(readOnly = true)
    public List<InventoryResponse> listAll() {
        return inventoryRepository.findAll().stream()
                .map(inv -> InventoryResponse.builder()
                        .skuCode(inv.getSkuCode())
                        .quantity(inv.getQuantity())
                        .isInStock(inv.getQuantity() != null && inv.getQuantity() > 0)
                        .build())
                .toList();
    }

    @Transactional
    public InventoryResponse updateQuantity(String skuCode, Integer quantity) {
        Inventory inventory = inventoryRepository.findBySkuCodeIgnoreCase(skuCode)
                .orElseGet(Inventory::new);
        inventory.setSkuCode(skuCode);
        inventory.setQuantity(quantity == null ? 0 : quantity);
        Inventory saved = inventoryRepository.save(inventory);
        return InventoryResponse.builder()
                .skuCode(saved.getSkuCode())
                .quantity(saved.getQuantity())
                .isInStock(saved.getQuantity() != null && saved.getQuantity() > 0)
                .build();
    }

    @KafkaListener(topics = "payment-topic", groupId = "inventory-group")
    @Transactional
    public void handlePaymentEvent(PaymentEvent paymentEvent) {
        if (!"SUCCESS".equals(paymentEvent.getStatus())) {
            return;
        }

        String orderId = paymentEvent.getOrderId();
        if (orderId == null || orderId.isBlank()) {
            log.warn("Ignoring payment success without orderId");
            return;
        }

        if (processedInventoryEventRepository.existsById(orderId)) {
            log.info("Inventory for order {} was already processed. Skipping duplicate event.", orderId);
            return;
        }

        if (paymentEvent.getItems() == null || paymentEvent.getItems().isEmpty()) {
            kafkaTemplate.send("inventory-topic", new InventoryEvent(orderId, "FAILED"));
            throw new IllegalStateException("Payment event does not contain order items for " + orderId);
        }

        log.info("Payment success for order {}. Decrementing stock atomically.", orderId);
        for (OrderItemDto item : paymentEvent.getItems()) {
            String skuCode = item.getSkuCode();
            Integer quantity = item.getQuantity();

            if (skuCode == null || skuCode.isBlank() || quantity == null || quantity <= 0) {
                kafkaTemplate.send("inventory-topic", new InventoryEvent(orderId, "FAILED"));
                throw new IllegalStateException("Invalid inventory item in order " + orderId);
            }

            int updatedRows = inventoryRepository.decrementStockIfAvailable(skuCode, quantity);
            if (updatedRows != 1) {
                kafkaTemplate.send("inventory-topic", new InventoryEvent(orderId, "FAILED"));
                throw new IllegalStateException("Not enough inventory for SKU " + skuCode + " in order " + orderId);
            }
            log.info("DECREMENT SUCCESS for {}: removed={}", skuCode, quantity);
        }

        processedInventoryEventRepository.save(new ProcessedInventoryEvent(orderId, "SUCCESS", LocalDateTime.now()));
        kafkaTemplate.send("inventory-topic", new InventoryEvent(orderId, "SUCCESS"));
    }
}
