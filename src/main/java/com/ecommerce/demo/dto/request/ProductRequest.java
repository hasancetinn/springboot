package com.ecommerce.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    @NotBlank(message = "Category Id cannot be null")
    private String categoryId;

    @NotBlank(message = "Product name cannot be blank")
    @Size(min = 2, max = 50, message = "Category name must be between 2 and 50 characters")
    private String name;

    @Size(min = 2, max = 255, message = "Description must be between 2 and 255 characters")
    private String description;

    private String image;
    private Double price;
}
