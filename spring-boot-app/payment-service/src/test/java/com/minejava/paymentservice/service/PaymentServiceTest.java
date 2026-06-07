package com.minejava.paymentservice.service;

import com.minejava.paymentservice.config.VNPayConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private VNPayConfig vnPayConfig;

    @Mock
    private WebClient.Builder webClientBuilder;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    void callbackWithoutSecureHashIsRejected() {
        assertFalse(paymentService.isValidVnPayCallback(Map.of("vnp_TxnRef", "order-1")));
    }

    @Test
    void callbackWithMatchingCanonicalSignatureIsAccepted() {
        when(vnPayConfig.getHashSecret()).thenReturn("secret");
        when(vnPayConfig.hmacSHA512("secret", "vnp_Amount=10000&vnp_TxnRef=order-1"))
                .thenReturn("expected-hash");

        boolean valid = paymentService.isValidVnPayCallback(Map.of(
                "vnp_SecureHash", "expected-hash",
                "vnp_SecureHashType", "HmacSHA512",
                "vnp_TxnRef", "order-1",
                "vnp_Amount", "10000"));

        assertTrue(valid);
    }
}
