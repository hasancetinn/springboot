package com.ecommerce.demo.service;

import com.ecommerce.demo.config.RedisCacheConfig;
import com.ecommerce.demo.dto.request.ProductRequest;
import com.ecommerce.demo.dto.response.PagedResponse;
import com.ecommerce.demo.exception.NotFoundException;
import com.ecommerce.demo.model.Category;
import com.ecommerce.demo.model.Product;
import com.ecommerce.demo.repository.CategoryRepository;
import com.ecommerce.demo.repository.ProductRepository;
import com.ecommerce.demo.support.SearchNormalizer;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    @Transactional(readOnly = true)
    @Cacheable(
            cacheNames = RedisCacheConfig.CACHE_PRODUCTS_INDEX,
            key =
                    "#page + ':' + #size + ':' + T(com.ecommerce.demo.support.SearchNormalizer).normalize(#search)")
    public PagedResponse<Product> findPage(int page, int size, String search) {
        String term = SearchNormalizer.normalize(search);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Product> result = productRepository.searchByTerm(term, pageable);
        return PagedResponse.fromPage(result.map(this::snapshotProduct));
    }

    /** Plain snapshot for API/cache — no lazy proxies on relations. */
    private Product snapshotProduct(Product p) {
        Category c = p.getCategory();
        Category categorySnapshot =
                c == null
                        ? null
                        : Category.builder()
                                .id(c.getId())
                                .name(c.getName())
                                .createdAt(c.getCreatedAt())
                                .updatedAt(c.getUpdatedAt())
                                .deletedAt(c.getDeletedAt())
                                .products(null)
                                .build();
        return Product.builder()
                .id(p.getId())
                .name(p.getName())
                .description(p.getDescription())
                .image(p.getImage())
                .price(p.getPrice())
                .category(categorySnapshot)
                .createdAt(p.getCreatedAt())
                .updatedAt(p.getUpdatedAt())
                .deletedAt(p.getDeletedAt())
                .build();
    }

    public Product findById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product Not found ID " +id));
    }

    @Transactional
    @CacheEvict(cacheNames = RedisCacheConfig.CACHE_PRODUCTS_INDEX, allEntries = true)
    public Product save(ProductRequest request) {

        Optional<Category> existingCategory = categoryRepository.findByNameAny(request.getCategoryId());

        if (existingCategory.isPresent()) {
            Product newProduct = Product.builder()
                    .name(request.getName())
                    .description(request.getDescription())
                    .category(existingCategory.get())
                    .image(request.getImage())
                    .price(request.getPrice())
                    .build();

            return productRepository.save(newProduct);
        }else {
            throw new NotFoundException("Category does not exist");
        }

    }
}
