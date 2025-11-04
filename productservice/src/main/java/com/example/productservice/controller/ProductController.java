package com.example.productservice.controller;

import com.example.productservice.domain.Product;
import com.example.productservice.dto.CreateProductRequest;
import com.example.productservice.dto.ProductDTO;
import com.example.productservice.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @PostMapping
    public ResponseEntity<ProductDTO> create(@Valid @RequestBody CreateProductRequest req) {
        Product p = Product.builder()
                .name(req.getName())
                .description(req.getDescription())
                .price(req.getPrice())
                .stock(req.getStock())
                .build();
        Product saved = productService.create(p);
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.toDto(saved));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductDTO> getById(@PathVariable Long id) {
        return productService.findByIdDto(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    public Page<ProductDTO> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy
    ) {
        return productService.list(page, size, sortBy);
    }

    @GetMapping("/search")
    public List<ProductDTO> search(@RequestParam String q) {
        return productService.searchByName(q);
    }

    @GetMapping("/filter/price")
    public Page<ProductDTO> filterByPrice(
            @RequestParam(defaultValue = "0") double min,
            @RequestParam(defaultValue = "999999") double max,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        return productService.filterByPrice(min, max, page, size);
    }

    // simple endpoint to reduce stock (used by order-service)
    @PostMapping("/{id}/reduce")
    public ResponseEntity<?> reduceStock(@PathVariable Long id, @RequestParam int qty) {
        boolean ok = productService.reduceStock(id, qty);
        if (!ok) return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Insufficient stock or product missing");
        return ResponseEntity.ok(Map.of("status", "OK"));
    }
}
