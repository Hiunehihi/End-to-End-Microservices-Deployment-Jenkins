package com.minejava.orderservice.service;

import java.util.Arrays;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import com.minejava.orderservice.dto.InventoryResponse;
import com.minejava.orderservice.dto.OrderAdminResponse;
import com.minejava.orderservice.dto.OrderItemDto;
import com.minejava.orderservice.dto.OrderLineItemsDto;
import com.minejava.orderservice.dto.OrderPaymentItemsResponse;
import com.minejava.orderservice.dto.OrderRequest;
import com.minejava.orderservice.dto.OrderStatsResponse;
import com.minejava.orderservice.event.OrderPlacedEvent;
import com.minejava.orderservice.event.PaymentEvent;
import com.minejava.orderservice.model.Order;
import com.minejava.orderservice.model.OrderLineItems;
import com.minejava.orderservice.repository.OrderRepository;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ResponseStatusException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {

    private static final List<String> ALLOWED_STATUSES = List.of(
            "PENDING", "COMPLETED", "FAILED", "SHIPPED", "DELIVERED", "CANCELLED");

    private final OrderRepository orderRepository;
    private final WebClient.Builder webClientBuilder;
    private final ObservationRegistry observationRegistry;
    private final ApplicationEventPublisher applicationEventPublisher;

    public String placeOrder(OrderRequest orderRequest, String userId, String username, String email) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setStatus("PENDING");
        order.setUserId(userId);
        order.setCustomerName(username);
        order.setCustomerEmail(email);

        List<OrderLineItems> orderLineItems = orderRequest.getOrderLineItemsDtoList()
                    .stream()
                    .map(this::mapToDto)
                    .toList();
        order.setOrderLineItemsList(orderLineItems);

        List<String> skuCodes = order.getOrderLineItemsList().stream()
                                .map(OrderLineItems::getSkuCode)
                                .toList();

        Observation inventoryServiceObservation = Observation.createNotStarted("inventory-service-lookup",
                this.observationRegistry);

        return inventoryServiceObservation.observe(() -> {
            InventoryResponse[] inventoryResponseArray;
            try {
                inventoryResponseArray = webClientBuilder.build().get()
                        .uri("http://inventory-service:8080/api/inventory",
                                uriBuilder -> uriBuilder.queryParam("skuCode", skuCodes).build())
                        .retrieve()
                        .bodyToMono(InventoryResponse[].class)
                        .block();
            } catch (WebClientResponseException e) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Inventory service rejected the request");
            } catch (Exception e) {
                throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "Inventory service is unavailable");
            }

            Map<String, Integer> inventoryMap = Arrays.stream(inventoryResponseArray != null ? inventoryResponseArray : new InventoryResponse[0])
                    .collect(Collectors.toMap(
                            inventory -> inventory.getSkuCode().toUpperCase(),
                            InventoryResponse::getQuantity,
                            (left, right) -> left));

            boolean allProductsInStock = order.getOrderLineItemsList().stream().allMatch(item -> {
                Integer available = inventoryMap.getOrDefault(item.getSkuCode().toUpperCase(), 0);
                return available >= item.getQuantity();
            });

            log.info("Stock verification for {}: {}", skuCodes, allProductsInStock);

            if (allProductsInStock && inventoryResponseArray.length > 0) {
                orderRepository.save(order);
                applicationEventPublisher.publishEvent(new OrderPlacedEvent(this, order.getOrderNumber()));
                return order.getOrderNumber();
            } else {
                throw new ResponseStatusException(HttpStatus.CONFLICT, "Product currently not in stock");
            }
        });
    }

    @KafkaListener(topics = "payment-topic", groupId = "order-group")
    public void handlePaymentEvent(PaymentEvent paymentEvent) {
        log.info("Received payment event for order: {}", paymentEvent.getOrderId());
        orderRepository.findByOrderNumber(paymentEvent.getOrderId()).ifPresent(order -> {
            if ("SUCCESS".equals(paymentEvent.getStatus())) {
                order.setStatus("COMPLETED");
            } else {
                order.setStatus("FAILED");
            }
            orderRepository.save(order);
            log.info("Updated order {} status to {}", order.getOrderNumber(), order.getStatus());
        });
    }

    private OrderLineItems mapToDto(OrderLineItemsDto orderLineItemsDto){
        OrderLineItems orderLineItems = new OrderLineItems();
        orderLineItems.setPrice(orderLineItemsDto.getPrice());
        orderLineItems.setQuantity(orderLineItemsDto.getQuantity());
        orderLineItems.setSkuCode(orderLineItemsDto.getSkuCode());
        return orderLineItems;
    }

    @Transactional(readOnly = true)
    public List<OrderAdminResponse> getAllOrdersForAdmin() {
        return orderRepository.findAll().stream()
                .sorted(Comparator.comparing(Order::getId).reversed())
                .map(this::mapToAdminResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderAdminResponse> getOrdersForUser(String userId) {
        return orderRepository.findByUserIdOrderByIdDesc(userId).stream()
                .map(this::mapToAdminResponse)
                .toList();
    }

    public OrderAdminResponse updateStatus(String orderNumber, String status) {
        String upper = status == null ? "" : status.toUpperCase();
        if (!ALLOWED_STATUSES.contains(upper)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Status must be one of " + ALLOWED_STATUSES);
        }
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
        order.setStatus(upper);
        return mapToAdminResponse(orderRepository.save(order));
    }

    @Transactional(readOnly = true)
    public OrderStatsResponse getStats() {
        List<Order> all = orderRepository.findAll();

        Map<String, Long> byStatus = all.stream()
                .collect(Collectors.groupingBy(o -> o.getStatus() == null ? "UNKNOWN" : o.getStatus(),
                        Collectors.counting()));

        BigDecimal revenue = all.stream()
                .filter(o -> "COMPLETED".equals(o.getStatus()) || "DELIVERED".equals(o.getStatus()) || "SHIPPED".equals(o.getStatus()))
                .flatMap(o -> o.getOrderLineItemsList().stream())
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, long[]> qtyMap = new HashMap<>();
        Map<String, BigDecimal> revMap = new HashMap<>();
        for (Order o : all) {
            for (OrderLineItems it : o.getOrderLineItemsList()) {
                qtyMap.computeIfAbsent(it.getSkuCode(), k -> new long[]{0})[0] += it.getQuantity();
                revMap.merge(it.getSkuCode(),
                        it.getPrice().multiply(BigDecimal.valueOf(it.getQuantity())),
                        BigDecimal::add);
            }
        }
        List<OrderStatsResponse.TopProduct> top = qtyMap.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue()[0], a.getValue()[0]))
                .limit(5)
                .map(e -> OrderStatsResponse.TopProduct.builder()
                        .skuCode(e.getKey())
                        .totalQuantity(e.getValue()[0])
                        .totalRevenue(revMap.getOrDefault(e.getKey(), BigDecimal.ZERO))
                        .build())
                .toList();

        return OrderStatsResponse.builder()
                .totalOrders(all.size())
                .totalRevenue(revenue)
                .ordersByStatus(byStatus)
                .topProducts(top)
                .build();
    }

    @Transactional(readOnly = true)
    public OrderPaymentItemsResponse getPaymentItems(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));

        return OrderPaymentItemsResponse.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .items(order.getOrderLineItemsList().stream().map(this::mapToOrderItemDto).toList())
                .build();
    }

    private OrderAdminResponse mapToAdminResponse(Order order) {
        List<OrderItemDto> items = order.getOrderLineItemsList().stream().map(this::mapToOrderItemDto).toList();
        BigDecimal total = order.getOrderLineItemsList().stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int itemCount = order.getOrderLineItemsList().stream()
                .mapToInt(OrderLineItems::getQuantity)
                .sum();

        return OrderAdminResponse.builder()
                .orderNumber(order.getOrderNumber())
                .status(order.getStatus())
                .totalAmount(total)
                .itemCount(itemCount)
                .userId(order.getUserId())
                .customerName(order.getCustomerName() != null ? order.getCustomerName() : "Guest")
                .customerEmail(order.getCustomerEmail() != null ? order.getCustomerEmail() : "")
                .createdAt(order.getCreatedAt())
                .items(items)
                .build();
    }

    private OrderItemDto mapToOrderItemDto(OrderLineItems item) {
        return new OrderItemDto(item.getSkuCode(), item.getQuantity(), item.getPrice().doubleValue());
    }
}
