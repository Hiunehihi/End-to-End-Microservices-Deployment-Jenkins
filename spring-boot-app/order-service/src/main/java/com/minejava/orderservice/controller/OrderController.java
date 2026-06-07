package com.minejava.orderservice.controller;

import com.minejava.orderservice.dto.OrderAdminResponse;
import com.minejava.orderservice.dto.OrderPaymentItemsResponse;
import com.minejava.orderservice.dto.OrderRequest;
import com.minejava.orderservice.dto.OrderStatsResponse;
import com.minejava.orderservice.service.OrderService;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;

import java.util.List;

@RestController
@RequestMapping("/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    public record StatusUpdateRequest(String status) {}

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public String placeOrder(@RequestBody OrderRequest orderRequest,
                             @RequestHeader(value = "X-User-Id", required = false) String userId,
                             @RequestHeader(value = "X-User-Username", required = false) String username,
                             @RequestHeader(value = "X-User-Email", required = false) String email) {
        return orderService.placeOrder(orderRequest, userId, username, email);
    }

    @GetMapping("/me")
    @ResponseStatus(HttpStatus.OK)
    public List<OrderAdminResponse> getMyOrders(@RequestHeader("X-User-Id") String userId) {
        return orderService.getOrdersForUser(userId);
    }

    @GetMapping("/admin/all")
    @ResponseStatus(HttpStatus.OK)
    public List<OrderAdminResponse> getAllOrdersForAdmin() {
        return orderService.getAllOrdersForAdmin();
    }

    @PatchMapping("/admin/{orderNumber}/status")
    @ResponseStatus(HttpStatus.OK)
    public OrderAdminResponse updateStatus(@PathVariable String orderNumber,
                                           @RequestBody StatusUpdateRequest body) {
        return orderService.updateStatus(orderNumber, body.status());
    }

    @GetMapping("/admin/stats")
    @ResponseStatus(HttpStatus.OK)
    public OrderStatsResponse stats() {
        return orderService.getStats();
    }

    @GetMapping("/internal/{orderNumber}/payment-items")
    @ResponseStatus(HttpStatus.OK)
    public OrderPaymentItemsResponse getPaymentItems(@PathVariable String orderNumber) {
        return orderService.getPaymentItems(orderNumber);
    }
}
