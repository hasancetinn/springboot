package com.ecommerce.demo.controller;

import com.ecommerce.demo.dto.request.CategoryRequest;
import com.ecommerce.demo.dto.response.ApiResponse;
import com.ecommerce.demo.dto.response.PagedResponse;
import com.ecommerce.demo.exception.ApiException;
import com.ecommerce.demo.exception.NotFoundException;
import com.ecommerce.demo.model.Category;
import com.ecommerce.demo.service.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * CategoryController için saf Mockito unit testleri.
 *
 * Spring context yüklemez — controller metodları doğrudan çağrılır,
 * CategoryService mock'lanır.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CategoryController Unit Tests")
class CategoryControllerTest {

    @Mock
    private CategoryService categoryService;

    @InjectMocks
    private CategoryController categoryController;

    private Category electronics;
    private Category clothing;

    @BeforeEach
    void setUp() {
        electronics = Category.builder().id(1L).name("Electronics").build();
        clothing = Category.builder().id(2L).name("Clothing").build();
    }

    // ----------------------------------------------------------------
    // index() testleri
    // ----------------------------------------------------------------

    @Test
    @DisplayName("index() → 200 OK, sayfalı kategori listesi döner")
    void index_shouldReturn200WithPagedCategoryList() {
        PagedResponse<Category> page =
                PagedResponse.<Category>builder()
                        .content(List.of(electronics, clothing))
                        .page(0)
                        .size(10)
                        .totalElements(2)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .empty(false)
                        .build();
        when(categoryService.findPage(0, 10, null)).thenReturn(page);

        ResponseEntity<ApiResponse<PagedResponse<Category>>> response =
                categoryController.index(0, 10, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Categories fetched successfully.");
        assertThat(response.getBody().getData().getContent()).hasSize(2);
        assertThat(response.getBody().getData().getContent().get(0).getName()).isEqualTo("Electronics");
        assertThat(response.getBody().getData().getContent().get(1).getName()).isEqualTo("Clothing");
        assertThat(response.getBody().getData().getTotalElements()).isEqualTo(2);

        verify(categoryService, times(1)).findPage(0, 10, null);
    }

    @Test
    @DisplayName("index() → 200 OK, boş sayfa döner")
    void index_shouldReturn200WithEmptyPage() {
        PagedResponse<Category> page =
                PagedResponse.<Category>builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .first(true)
                        .last(true)
                        .empty(true)
                        .build();
        when(categoryService.findPage(0, 10, "x")).thenReturn(page);

        ResponseEntity<ApiResponse<PagedResponse<Category>>> response =
                categoryController.index(0, 10, "x");

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent()).isEmpty();
    }

    @Test
    @DisplayName("index() → service tam olarak bir kez çağrılmalı")
    void index_shouldCallServiceExactlyOnce() {
        PagedResponse<Category> page =
                PagedResponse.<Category>builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .first(true)
                        .last(true)
                        .empty(true)
                        .build();
        when(categoryService.findPage(0, 10, null)).thenReturn(page);

        categoryController.index(0, 10, null);

        verify(categoryService, times(1)).findPage(0, 10, null);
        verifyNoMoreInteractions(categoryService);
    }

    // ----------------------------------------------------------------
    // findById() testleri
    // ----------------------------------------------------------------

    @Test
    @DisplayName("findById() → 200 OK, kategori döner")
    void findById_shouldReturn200_whenCategoryExists() {
        when(categoryService.findById(1L)).thenReturn(electronics);

        ResponseEntity<ApiResponse<Category>> response = categoryController.findById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Category fetched successfully.");
        assertThat(response.getBody().getData().getId()).isEqualTo(1L);
        assertThat(response.getBody().getData().getName()).isEqualTo("Electronics");

        verify(categoryService).findById(1L);
    }

    @Test
    @DisplayName("findById() → NotFoundException fırlatmalı (kategori yok)")
    void findById_shouldThrowNotFoundException_whenCategoryNotFound() {
        when(categoryService.findById(99L))
                .thenThrow(new NotFoundException("Category not found with ID: 99"));

        assertThatThrownBy(() -> categoryController.findById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Category not found with ID: 99");
    }

    @Test
    @DisplayName("findById() → doğru ID service'e iletilmeli")
    void findById_shouldPassCorrectIdToService() {
        Long requestedId = 42L;
        when(categoryService.findById(requestedId)).thenReturn(electronics);

        categoryController.findById(requestedId);

        verify(categoryService).findById(requestedId);
    }

    // ----------------------------------------------------------------
    // store() testleri
    // ----------------------------------------------------------------

    @Test
    @DisplayName("store() → 201 Created, yeni kategori döner")
    void store_shouldReturn201_whenRequestIsValid() {
        CategoryRequest request = new CategoryRequest("Electronics");
        when(categoryService.save(any(CategoryRequest.class))).thenReturn(electronics);

        ResponseEntity<ApiResponse<Category>> response = categoryController.store(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getStatus()).isEqualTo(201);
        assertThat(response.getBody().getMessage()).isEqualTo("Category created successfully");
        assertThat(response.getBody().getData().getId()).isEqualTo(1L);
        assertThat(response.getBody().getData().getName()).isEqualTo("Electronics");

        verify(categoryService, times(1)).save(any(CategoryRequest.class));
    }

    @Test
    @DisplayName("store() → ApiException fırlatmalı (kategori adı zaten var)")
    void store_shouldThrowApiException_whenCategoryNameAlreadyExists() {
        CategoryRequest request = new CategoryRequest("Electronics");

        when(categoryService.save(any(CategoryRequest.class)))
                .thenThrow(new ApiException("Category name already exists", HttpStatus.CONFLICT));

        assertThatThrownBy(() -> categoryController.store(request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Category name already exists");
    }

    @Test
    @DisplayName("store() → silinen kategori adı ile yeni oluşturma (soft-delete restore)")
    void store_shouldAllowCreatingWithPreviouslyDeletedCategoryName() {
        CategoryRequest request = new CategoryRequest("Electronics");
        Category restoredCategory = Category.builder().id(1L).name("Electronics").build();

        when(categoryService.save(request)).thenReturn(restoredCategory);

        ResponseEntity<ApiResponse<Category>> response = categoryController.store(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().getName()).isEqualTo("Electronics");
    }

    // ----------------------------------------------------------------
    // update() testleri
    // ----------------------------------------------------------------

    @Test
    @DisplayName("update() → 200 OK, güncellenmiş kategori döner")
    void update_shouldReturn200_whenRequestIsValid() {
        CategoryRequest request = new CategoryRequest("Updated Electronics");
        Category updatedCategory = Category.builder().id(1L).name("Updated Electronics").build();

        when(categoryService.update(eq(1L), any(CategoryRequest.class))).thenReturn(updatedCategory);

        ResponseEntity<ApiResponse<Category>> response = categoryController.update(1L, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Category updated successfully");
        assertThat(response.getBody().getData().getName()).isEqualTo("Updated Electronics");

        verify(categoryService).update(eq(1L), any(CategoryRequest.class));
    }

    @Test
    @DisplayName("update() → NotFoundException fırlatmalı (kategori yok)")
    void update_shouldThrowNotFoundException_whenCategoryNotFound() {
        CategoryRequest request = new CategoryRequest("Updated Name");

        when(categoryService.update(eq(99L), any(CategoryRequest.class)))
                .thenThrow(new NotFoundException("Category not found with ID: 99"));

        assertThatThrownBy(() -> categoryController.update(99L, request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Category not found with ID: 99");
    }

    @Test
    @DisplayName("update() → ApiException fırlatmalı (yeni isim çakışıyor)")
    void update_shouldThrowApiException_whenNewNameConflicts() {
        CategoryRequest request = new CategoryRequest("Taken Name");

        when(categoryService.update(eq(1L), any(CategoryRequest.class)))
                .thenThrow(new ApiException("Category name already exists (potentially as deleted)", HttpStatus.CONFLICT));

        assertThatThrownBy(() -> categoryController.update(1L, request))
                .isInstanceOf(ApiException.class)
                .hasMessage("Category name already exists (potentially as deleted)");
    }

    @Test
    @DisplayName("update() → doğru ID ve request service'e iletilmeli")
    void update_shouldPassCorrectIdAndRequestToService() {
        Long categoryId = 5L;
        CategoryRequest request = new CategoryRequest("New Name");
        Category updated = Category.builder().id(5L).name("New Name").build();

        when(categoryService.update(eq(categoryId), eq(request))).thenReturn(updated);

        categoryController.update(categoryId, request);

        verify(categoryService).update(categoryId, request);
    }

    // ----------------------------------------------------------------
    // delete() testleri
    // ----------------------------------------------------------------

    @Test
    @DisplayName("delete() → 200 OK, data null döner")
    void delete_shouldReturn200_whenCategoryExists() {
        doNothing().when(categoryService).delete(1L);

        ResponseEntity<ApiResponse<Void>> response = categoryController.delete(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Category deleted successfully");
        assertThat(response.getBody().getData()).isNull();

        verify(categoryService, times(1)).delete(1L);
    }

    @Test
    @DisplayName("delete() → NotFoundException fırlatmalı (kategori yok)")
    void delete_shouldThrowNotFoundException_whenCategoryNotFound() {
        doThrow(new NotFoundException("Category not found with ID: 99"))
                .when(categoryService).delete(99L);

        assertThatThrownBy(() -> categoryController.delete(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Category not found with ID: 99");
    }

    @Test
    @DisplayName("delete() → doğru ID service'e iletilmeli")
    void delete_shouldPassCorrectIdToService() {
        Long categoryId = 7L;
        doNothing().when(categoryService).delete(categoryId);

        categoryController.delete(categoryId);

        verify(categoryService).delete(categoryId);
    }

    @Test
    @DisplayName("delete() → ApiException fırlatmalı (kategoriye bağlı ürünler var)")
    void delete_shouldThrowApiException_whenCategoryHasProducts() {
        doThrow(new ApiException("Cannot delete category with associated products", HttpStatus.CONFLICT))
                .when(categoryService).delete(1L);

        assertThatThrownBy(() -> categoryController.delete(1L))
                .isInstanceOf(ApiException.class)
                .hasMessage("Cannot delete category with associated products");
    }
}
