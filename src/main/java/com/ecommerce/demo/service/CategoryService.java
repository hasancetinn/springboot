package com.ecommerce.demo.service;

import com.ecommerce.demo.config.RedisCacheConfig;
import com.ecommerce.demo.dto.request.CategoryRequest;
import com.ecommerce.demo.dto.response.PagedResponse;
import com.ecommerce.demo.exception.ApiException;
import com.ecommerce.demo.exception.NotFoundException;
import com.ecommerce.demo.model.Category;
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
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    @Cacheable(cacheNames = RedisCacheConfig.CACHE_CATEGORIES_INDEX, key = "#page + ':' + #size + ':' + T(com.ecommerce.demo.support.SearchNormalizer).normalize(#search)")
    public PagedResponse<Category> findPage(int page, int size, String search) {
        String term = SearchNormalizer.normalize(search);
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "id"));
        Page<Category> result =
            term.isEmpty()
                ? categoryRepository.findAll(pageable)
                : categoryRepository.searchByName(term, pageable);
        return PagedResponse.fromPage(result.map(this::snapshotCategory));
    }


    private Category snapshotCategory(Category c) {
        return Category.builder()
            .id(c.getId())
            .name(c.getName())
            .createdAt(c.getCreatedAt())
            .updatedAt(c.getUpdatedAt())
            .deletedAt(c.getDeletedAt())
            .products(null)
            .build();
    }

    public Category findById(Long id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("Category not found with ID: " + id));
    }

    @Transactional
    @CacheEvict(
        cacheNames = {
            RedisCacheConfig.CACHE_CATEGORIES_INDEX,
            RedisCacheConfig.CACHE_PRODUCTS_INDEX
        },
        allEntries = true)
    public Category save(CategoryRequest request) {
        // Check finding by name including deleted ones
        Optional<Category> existingCategory = categoryRepository.findByNameAny(request.getName());

        if (existingCategory.isPresent()) {
            Category category = existingCategory.get();
            if (category.getDeletedAt() == null) {
                throw new ApiException("Category name already exists", HttpStatus.CONFLICT);
            } else {
                category.setDeletedAt(null);
                return categoryRepository.save(category);
            }
        }

        Category newCategory = Category.builder()
            .name(request.getName())
            .build();

        return categoryRepository.save(newCategory);
    }

    @Transactional
    @CacheEvict(
        cacheNames = {
            RedisCacheConfig.CACHE_CATEGORIES_INDEX,
            RedisCacheConfig.CACHE_PRODUCTS_INDEX
        },
        allEntries = true)
    public Category update(Long id, CategoryRequest request) {
        Category category = findById(id);

        if (!category.getName().equals(request.getName())) {
            Optional<Category> conflicting = categoryRepository.findByNameAny(request.getName());
            if (conflicting.isPresent()) {
                throw new ApiException("Category name already exists (potentially as deleted)", HttpStatus.CONFLICT);
            }
        }

        category.setName(request.getName());
        return categoryRepository.save(category);
    }

    @Transactional
    @CacheEvict(
        cacheNames = {
            RedisCacheConfig.CACHE_CATEGORIES_INDEX,
            RedisCacheConfig.CACHE_PRODUCTS_INDEX
        },
        allEntries = true)
    public void delete(Long id) {
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Category not found with ID: " + id);
        }

        if (productRepository.existsByCategoryId(id)) {
            throw new ApiException("Cannot delete category with associated products", HttpStatus.CONFLICT);
        }

        categoryRepository.deleteById(id);
    }
}
