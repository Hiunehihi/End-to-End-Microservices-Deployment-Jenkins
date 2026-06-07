package com.minejava.inventoryservice.service;

import com.minejava.inventoryservice.dto.OrderItemDto;
import com.minejava.inventoryservice.event.InventoryEvent;
import com.minejava.inventoryservice.event.PaymentEvent;
import com.minejava.inventoryservice.repository.InventoryRepository;
import com.minejava.inventoryservice.repository.ProcessedInventoryEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private InventoryRepository inventoryRepository;

    @Mock
    private ProcessedInventoryEventRepository processedEventRepository;

    @Mock
    private KafkaTemplate<String, InventoryEvent> kafkaTemplate;

    @InjectMocks
    private InventoryService inventoryService;

    @Test
    void successfulPaymentDecrementsStockAndPublishesSuccess() {
        PaymentEvent event = new PaymentEvent("order-1", "SUCCESS",
                List.of(new OrderItemDto("SKU-1", 2, 10.0)));
        when(processedEventRepository.existsById("order-1")).thenReturn(false);
        when(inventoryRepository.decrementStockIfAvailable("SKU-1", 2)).thenReturn(1);

        inventoryService.handlePaymentEvent(event);

        verify(inventoryRepository).decrementStockIfAvailable("SKU-1", 2);
        verify(processedEventRepository).save(argThat(saved -> "order-1".equals(saved.getOrderId())
                && "SUCCESS".equals(saved.getStatus())));
        verify(kafkaTemplate).send(eq("inventory-topic"),
                argThat(message -> "order-1".equals(message.getOrderId())
                        && "SUCCESS".equals(message.getStatus())));
    }

    @Test
    void duplicatePaymentDoesNotDecrementStockAgain() {
        when(processedEventRepository.existsById("order-1")).thenReturn(true);

        inventoryService.handlePaymentEvent(new PaymentEvent("order-1", "SUCCESS",
                List.of(new OrderItemDto("SKU-1", 1, 10.0))));

        verify(inventoryRepository, never()).decrementStockIfAvailable("SKU-1", 1);
        verify(kafkaTemplate, never()).send(eq("inventory-topic"), org.mockito.ArgumentMatchers.any());
    }

    @Test
    void insufficientStockPublishesFailureAndRejectsPayment() {
        PaymentEvent event = new PaymentEvent("order-2", "SUCCESS",
                List.of(new OrderItemDto("SKU-2", 3, 15.0)));
        when(processedEventRepository.existsById("order-2")).thenReturn(false);
        when(inventoryRepository.decrementStockIfAvailable("SKU-2", 3)).thenReturn(0);

        assertThrows(IllegalStateException.class, () -> inventoryService.handlePaymentEvent(event));

        verify(kafkaTemplate).send(eq("inventory-topic"),
                argThat(message -> "FAILED".equals(message.getStatus())));
        verify(processedEventRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }
}
