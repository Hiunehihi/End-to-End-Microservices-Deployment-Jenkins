package com.minejava.paymentservice.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderPaymentItemsResponse {
    private String orderNumber;
    private String status;
    private List<OrderItemDto> items;
}
