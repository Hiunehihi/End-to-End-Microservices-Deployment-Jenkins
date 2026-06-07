package com.minejava.orderservice.service;

import com.minejava.orderservice.dto.OrderAdminResponse;
import com.minejava.orderservice.dto.OrderStatsResponse;
import com.minejava.orderservice.event.PaymentEvent;
import com.minejava.orderservice.model.Order;
import com.minejava.orderservice.model.OrderLineItems;
import com.minejava.orderservice.repository.OrderRepository;
import io.micrometer.observation.ObservationRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private WebClient.Builder webClientBuilder;

    @Mock
    private ObservationRegistry observationRegistry;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private OrderService orderService;

    @Test
    void successfulPaymentMarksOrderCompleted() {
        Order order = order("order-1", "PENDING", "SKU-1", 1, "20.00");
        when(orderRepository.findByOrderNumber("order-1")).thenReturn(Optional.of(order));

        orderService.handlePaymentEvent(new PaymentEvent("order-1", "SUCCESS", List.of()));

        assertEquals("COMPLETED", order.getStatus());
        verify(orderRepository).save(order);
    }

    @Test
    void invalidAdminStatusIsRejected() {
        assertThrows(ResponseStatusException.class, () -> orderService.updateStatus("order-1", "unknown"));
    }

    @Test
    void statsOnlyCountCompletedRevenueButRankAllSoldItems() {
        Order completed = order("order-1", "COMPLETED", "SKU-1", 2, "10.00");
        Order pending = order("order-2", "PENDING", "SKU-2", 3, "4.00");
        when(orderRepository.findAll()).thenReturn(List.of(completed, pending));

        OrderStatsResponse stats = orderService.getStats();

        assertEquals(2, stats.getTotalOrders());
        assertEquals(new BigDecimal("20.00"), stats.getTotalRevenue());
        assertEquals(1L, stats.getOrdersByStatus().get("COMPLETED"));
        assertEquals("SKU-2", stats.getTopProducts().get(0).getSkuCode());
    }

    @Test
    void updateStatusReturnsOrderSummaryForAllowedStatus() {
        Order order = order("order-3", "PENDING", "SKU-3", 2, "7.00");
        when(orderRepository.findByOrderNumber("order-3")).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenReturn(order);

        OrderAdminResponse response = orderService.updateStatus("order-3", "shipped");

        assertEquals("SHIPPED", response.getStatus());
        assertEquals(new BigDecimal("14.00"), response.getTotalAmount());
    }

    private Order order(String number, String status, String sku, int quantity, String price) {
        OrderLineItems item = new OrderLineItems(null, sku, new BigDecimal(price), quantity);
        Order order = new Order();
        order.setOrderNumber(number);
        order.setStatus(status);
        order.setOrderLineItemsList(List.of(item));
        return order;
    }
}
