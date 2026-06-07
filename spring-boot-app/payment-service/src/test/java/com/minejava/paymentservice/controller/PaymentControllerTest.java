package com.minejava.paymentservice.controller;

import com.minejava.paymentservice.dto.OrderItemDto;
import com.minejava.paymentservice.event.PaymentEvent;
import com.minejava.paymentservice.service.PaymentService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentControllerTest {

    @Mock
    private PaymentService paymentService;

    @Mock
    private KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @InjectMocks
    private PaymentController paymentController;

    @Test
    void successfulVnPayCallbackPublishesSuccessfulPayment() {
        Map<String, String> params = Map.of("vnp_TxnRef", "order-1", "vnp_ResponseCode", "00");
        List<OrderItemDto> items = List.of(new OrderItemDto("SKU-1", 1, 50.0));
        when(paymentService.isValidVnPayCallback(params)).thenReturn(true);
        when(paymentService.getOrderItems("order-1")).thenReturn(items);

        ResponseEntity<String> response = paymentController.vnpayCallback(params);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(kafkaTemplate).send(eq("payment-topic"),
                argThat(event -> "SUCCESS".equals(event.getStatus()) && event.getItems().equals(items)));
    }

    @Test
    void invalidVnPaySignatureDoesNotPublishPayment() {
        Map<String, String> params = Map.of("vnp_TxnRef", "order-2", "vnp_ResponseCode", "00");
        when(paymentService.isValidVnPayCallback(params)).thenReturn(false);

        ResponseEntity<String> response = paymentController.vnpayCallback(params);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        verifyNoInteractions(kafkaTemplate);
    }
}
