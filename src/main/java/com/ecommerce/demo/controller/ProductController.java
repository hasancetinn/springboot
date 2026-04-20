package com.ecommerce.demo.controller;

import com.ecommerce.demo.dto.request.ProductRequest;
import com.ecommerce.demo.dto.response.ApiResponse;
import com.ecommerce.demo.dto.response.PagedResponse;
import com.ecommerce.demo.model.Product;
import com.ecommerce.demo.service.ProductService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
@Validated
public class ProductController {

    private final ProductService productService;

    @GetMapping(path = {"", "/"})
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<PagedResponse<Product>>> index(
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @RequestParam(required = false) String search) {
        return ResponseEntity.ok(
                new ApiResponse<>(
                        200,
                        "Products fetched successfully",
                        productService.findPage(page, size, search)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Product>> getById(@PathVariable Long id) {
        return ResponseEntity.ok
                (new ApiResponse<>(200, "Product fetch successfully", productService.findById(id)));
    }

    @PostMapping("/store")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Product>> store(@Valid @RequestBody ProductRequest request) {
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(new ApiResponse<>(200, "Product created successfully", productService.save(request)));
    }


}
