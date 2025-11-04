package com.example.orderservice.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.Map;

@FeignClient(name = "product-service", url = "${product.service.url:http://localhost:9002}")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    Map<String, Object> getProductById(@PathVariable("id") Long id);
    // Note: returns generic map to avoid sharing DTO jar; in full repo you would use shared DTOs.

    @PostMapping("/api/products/{id}/reduce")
    Map<String, String> reduceStock(@PathVariable("id") Long id, @RequestParam int qty);
}