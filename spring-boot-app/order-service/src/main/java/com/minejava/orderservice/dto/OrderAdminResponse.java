package com.minejava.orderservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderAdminResponse {
    private String orderNumber;
    private String status;
    private BigDecimal totalAmount;
    private Integer itemCount;
    private String customerName;
    private String customerEmail;
    private String userId;
    private Instant createdAt;
    private List<OrderItemDto> items;
}
