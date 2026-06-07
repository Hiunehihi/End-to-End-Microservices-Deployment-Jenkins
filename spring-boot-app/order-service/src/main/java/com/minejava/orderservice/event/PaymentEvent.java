package com.minejava.orderservice.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.minejava.orderservice.dto.OrderItemDto;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PaymentEvent {
    private String orderId;
    private String status; // SUCCESS, FAILED
    private List<OrderItemDto> items;
}
