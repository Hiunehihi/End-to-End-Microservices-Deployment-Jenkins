package com.minejava.paymentservice.controller;

import com.minejava.paymentservice.dto.PaymentRequest;
import com.minejava.paymentservice.event.PaymentEvent;
import com.minejava.paymentservice.service.PaymentService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.io.UnsupportedEncodingException;
import java.util.Map;

@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;
    private final KafkaTemplate<String, PaymentEvent> kafkaTemplate;

    @PostMapping("/create")
    public String createPayment(@RequestBody PaymentRequest paymentRequest, HttpServletRequest request) throws UnsupportedEncodingException {
        return paymentService.createPayment(paymentRequest, request);
    }

    @GetMapping("/vnpay-callback")
    public ResponseEntity<String> vnpayCallback(@RequestParam Map<String, String> queryParams) {
        if (!paymentService.isValidVnPayCallback(queryParams)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid VNPay signature.");
        }

        String vnp_ResponseCode = queryParams.get("vnp_ResponseCode");
        String orderId = queryParams.get("vnp_TxnRef");
        if (orderId == null || orderId.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing order number.");
        }

        if ("00".equals(vnp_ResponseCode)) {
            kafkaTemplate.send("payment-topic", new PaymentEvent(orderId, "SUCCESS", paymentService.getOrderItems(orderId)));
            return ResponseEntity.ok("Payment Success. You can close this window.");
        } else {
            kafkaTemplate.send("payment-topic", new PaymentEvent(orderId, "FAILED", paymentService.getOrderItems(orderId)));
            return ResponseEntity.ok("Payment Failed.");
        }
    }

    @PostMapping("/manual-confirm")
    public String manualConfirm(@RequestBody PaymentRequest paymentRequest) {
        if (paymentRequest.getOrderId() == null || paymentRequest.getOrderId().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing order number");
        }
        kafkaTemplate.send("payment-topic", new PaymentEvent(paymentRequest.getOrderId(), "SUCCESS", paymentService.resolveItems(paymentRequest)));
        return "SUCCESS";
    }
}
