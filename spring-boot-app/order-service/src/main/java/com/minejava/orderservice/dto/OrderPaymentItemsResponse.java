package com.minejava.orderservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class OrderPaymentItemsResponse {
    private String orderNumber;
    private String status;
    private List<OrderItemDto> items;
}
