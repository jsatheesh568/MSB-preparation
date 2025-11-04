package com.example.orderservice.service;

import com.example.orderservice.client.ProductClient;
import com.example.orderservice.domain.OrderEntity;
import com.example.orderservice.dto.OrderDTO;
import com.example.orderservice.dto.OrderItemDTO;
import com.example.orderservice.dto.PlaceOrderRequest;
import com.example.orderservice.repository.OrderRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductClient productClient;
    private final ObjectMapper objectMapper = new ObjectMapper(); // for items JSON

    private static final String PRODUCT_CB = "productServiceCB";

    /**
     * Place order: validate product stock via product-service and reduce stock.
     * Circuit breaker around calls to product service with fallback.
     */
    @Transactional
    @CircuitBreaker(name = PRODUCT_CB, fallbackMethod = "placeOrderFallback")
    public OrderDTO placeOrder(PlaceOrderRequest req) throws Exception {
        // 1) Validate and compute total
        double total = 0.0;

        // For each item, check product price and stock
        for (OrderItemDTO item : req.getItems()) {
            Map<String, Object> product = productClient.getProductById(item.getProductId());
            if (product == null || product.isEmpty()) {
                throw new IllegalArgumentException("Product not found: " + item.getProductId());
            }
            // read price and stock carefully (product-service returns "price" & "stock")
            double price = ((Number) product.getOrDefault("price", 0)).doubleValue();
            int stock = ((Number) product.getOrDefault("stock", 0)).intValue();
            if (stock < item.getQuantity()) {
                throw new IllegalStateException("Insufficient stock for product " + item.getProductId());
            }
            total += price * item.getQuantity();
            item.setPrice(price); // snapshot price
        }

        // 2) Persist order with status PLACED (we'll mark as PLACED and then reduce stock)
        OrderEntity order = OrderEntity.builder()
                .userId(req.getUserId())
                .totalAmount(total)
                .status("PLACED")
                .createdAt(Instant.now())
                .itemsJson(writeItemsJson(req.getItems()))
                .build();

        OrderEntity saved = orderRepository.save(order);

        // 3) Reduce stock by calling product-service for each item (could be async / event-driven)
        for (OrderItemDTO item : req.getItems()) {
            Map<String, String> resp = productClient.reduceStock(item.getProductId(), item.getQuantity());
            // basic check
            if (resp == null || !"OK".equalsIgnoreCase(resp.getOrDefault("status","OK"))) {
                // If reduce fails, we could throw and let transaction rollback
                throw new IllegalStateException("Failed to reduce stock for product " + item.getProductId());
            }
        }

        return toDto(saved);
    }

    // fallback for circuit breaker
    public OrderDTO placeOrderFallback(PlaceOrderRequest req, Throwable t) {
        // Simple fallback: save an order with FAILED status and return it
        OrderEntity failed = OrderEntity.builder()
                .userId(req.getUserId())
                .totalAmount(0.0)
                .status("FAILED")
                .createdAt(Instant.now())
                .itemsJson(writeItemsJson(req.getItems()))
                .build();
        OrderEntity saved = orderRepository.save(failed);
        return toDto(saved);
    }

    private String writeItemsJson(List<OrderItemDTO> items) {
        try {
            return objectMapper.writeValueAsString(items);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }

    public Optional<OrderDTO> findById(Long id) {
        return orderRepository.findById(id).map(this::toDto);
    }

    public List<OrderDTO> findAll() {
        return orderRepository.findAll()
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    private OrderDTO toDto(OrderEntity e) {
        List<OrderItemDTO> items;
        try {
            items = Arrays.asList(objectMapper.readValue(e.getItemsJson(), OrderItemDTO[].class));
        } catch (Exception ex) {
            items = Collections.emptyList();
        }
        return OrderDTO.builder()
                .id(e.getId())
                .userId(e.getUserId())
                .items(items)
                .totalAmount(e.getTotalAmount())
                .status(e.getStatus())
                .build();
    }
}
