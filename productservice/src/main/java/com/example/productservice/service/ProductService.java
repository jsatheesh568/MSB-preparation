package com.example.productservice.service;

import com.example.productservice.domain.Product;
import com.example.productservice.dto.ProductDTO;
import com.example.productservice.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository repo;

    public ProductDTO toDto(Product p) {
        return ProductDTO.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .price(p.getPrice())
                .stock(p.getStock())
                .build();
    }

    public Product create(Product p) {
        return repo.save(p);
    }

    public Optional<ProductDTO> findByIdDto(Long id) {
        return repo.findById(id).map(this::toDto);
    }

    public Optional<Product> findById(Long id) {
        return repo.findById(id);
    }

    public Page<ProductDTO> list(int page, int size, String sortBy) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy));
        return repo.findAll(pageable).map(this::toDto);
    }

    public List<ProductDTO> searchByName(String q) {
        return repo.findByNameContainingIgnoreCase(q)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    public Page<ProductDTO> filterByPrice(double min, double max, int page, int size) {
        return repo.findByPriceBetween(min, max, PageRequest.of(page, size)).map(this::toDto);
    }

    // reduce stock (used by order client)
    public boolean reduceStock(Long productId, int qty) {
        return repo.findById(productId)
                .map(p -> {
                    if (p.getStock() < qty) return false;
                    p.setStock(p.getStock() - qty);
                    repo.save(p);
                    return true;
                }).orElse(false);
    }
}
