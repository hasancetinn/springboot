package com.ecommerce.demo.controller;

import com.ecommerce.demo.dto.request.ProductRequest;
import com.ecommerce.demo.dto.response.ApiResponse;
import com.ecommerce.demo.dto.response.PagedResponse;
import com.ecommerce.demo.exception.NotFoundException;
import com.ecommerce.demo.model.Category;
import com.ecommerce.demo.model.Product;
import com.ecommerce.demo.service.ProductService;
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
import static org.mockito.Mockito.*;

/**
 * ProductController için saf Mockito unit testleri.
 *
 * Spring context yüklemez — controller metodları doğrudan çağrılır,
 * ProductService mock'lanır.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductController Unit Tests")
class ProductControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private ProductController productController;

    private Category electronics;
    private Product laptop;
    private Product phone;

    @BeforeEach
    void setUp() {
        electronics = Category.builder().id(1L).name("Electronics").build();

        laptop = Product.builder()
                .id(1L)
                .name("Laptop")
                .description("Gaming laptop")
                .price(1500.0)
                .image("laptop.jpg")
                .category(electronics)
                .build();

        phone = Product.builder()
                .id(2L)
                .name("Phone")
                .description("Smartphone")
                .price(800.0)
                .image("phone.jpg")
                .category(electronics)
                .build();
    }

    // ----------------------------------------------------------------
    // index() testleri
    // ----------------------------------------------------------------

    @Test
    @DisplayName("index() → 200 OK, sayfalı ürün listesi döner")
    void index_shouldReturn200WithPagedProductList() {
        PagedResponse<Product> page =
                PagedResponse.<Product>builder()
                        .content(List.of(laptop, phone))
                        .page(0)
                        .size(10)
                        .totalElements(2)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .empty(false)
                        .build();
        when(productService.findPage(0, 10, null)).thenReturn(page);

        ResponseEntity<ApiResponse<PagedResponse<Product>>> response =
                productController.index(0, 10, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Products fetched successfully");
        assertThat(response.getBody().getData().getContent()).hasSize(2);
        assertThat(response.getBody().getData().getContent().get(0).getName()).isEqualTo("Laptop");
        assertThat(response.getBody().getData().getContent().get(1).getName()).isEqualTo("Phone");

        verify(productService, times(1)).findPage(0, 10, null);
    }

    @Test
    @DisplayName("index() → 200 OK, boş sayfa döner")
    void index_shouldReturn200WithEmptyPage() {
        PagedResponse<Product> page =
                PagedResponse.<Product>builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .first(true)
                        .last(true)
                        .empty(true)
                        .build();
        when(productService.findPage(0, 10, null)).thenReturn(page);

        ResponseEntity<ApiResponse<PagedResponse<Product>>> response =
                productController.index(0, 10, null);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getData().getContent()).isEmpty();
    }

    @Test
    @DisplayName("index() → service tam olarak bir kez çağrılmalı")
    void index_shouldCallServiceExactlyOnce() {
        PagedResponse<Product> page =
                PagedResponse.<Product>builder()
                        .content(List.of())
                        .page(0)
                        .size(10)
                        .totalElements(0)
                        .totalPages(0)
                        .first(true)
                        .last(true)
                        .empty(true)
                        .build();
        when(productService.findPage(0, 10, null)).thenReturn(page);

        productController.index(0, 10, null);

        verify(productService, times(1)).findPage(0, 10, null);
        verifyNoMoreInteractions(productService);
    }

    @Test
    @DisplayName("index() → fiyatlar doğru döner")
    void index_shouldReturnCorrectPrices() {
        PagedResponse<Product> page =
                PagedResponse.<Product>builder()
                        .content(List.of(laptop, phone))
                        .page(0)
                        .size(10)
                        .totalElements(2)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .empty(false)
                        .build();
        when(productService.findPage(0, 10, null)).thenReturn(page);

        ResponseEntity<ApiResponse<PagedResponse<Product>>> response =
                productController.index(0, 10, null);

        assertThat(response.getBody().getData().getContent().get(0).getPrice()).isEqualTo(1500.0);
        assertThat(response.getBody().getData().getContent().get(1).getPrice()).isEqualTo(800.0);
    }

    @Test
    @DisplayName("index() → kategori bilgisi beraber döner")
    void index_shouldReturnProductsWithCategory() {
        PagedResponse<Product> page =
                PagedResponse.<Product>builder()
                        .content(List.of(laptop))
                        .page(0)
                        .size(10)
                        .totalElements(1)
                        .totalPages(1)
                        .first(true)
                        .last(true)
                        .empty(false)
                        .build();
        when(productService.findPage(0, 10, null)).thenReturn(page);

        ResponseEntity<ApiResponse<PagedResponse<Product>>> response =
                productController.index(0, 10, null);

        assertThat(response.getBody().getData().getContent().get(0).getCategory().getName())
                .isEqualTo("Electronics");
    }

    // ----------------------------------------------------------------
    // getById() testleri
    // ----------------------------------------------------------------

    @Test
    @DisplayName("getById() → 200 OK, ürün döner")
    void getById_shouldReturn200_whenProductExists() {
        when(productService.findById(1L)).thenReturn(laptop);

        ResponseEntity<ApiResponse<Product>> response = productController.getById(1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).isEqualTo("Product fetch successfully");
        assertThat(response.getBody().getData().getId()).isEqualTo(1L);
        assertThat(response.getBody().getData().getName()).isEqualTo("Laptop");
        assertThat(response.getBody().getData().getPrice()).isEqualTo(1500.0);

        verify(productService).findById(1L);
    }

    @Test
    @DisplayName("getById() → NotFoundException fırlatmalı (ürün yok)")
    void getById_shouldThrowNotFoundException_whenProductNotFound() {
        when(productService.findById(99L))
                .thenThrow(new NotFoundException("Product Not found ID 99"));

        assertThatThrownBy(() -> productController.getById(99L))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Product Not found ID 99");

        verify(productService).findById(99L);
    }

    @Test
    @DisplayName("getById() → doğru ID service'e iletilmeli")
    void getById_shouldPassCorrectIdToService() {
        Long productId = 42L;
        when(productService.findById(productId)).thenReturn(laptop);

        productController.getById(productId);

        verify(productService).findById(productId);
    }

    @Test
    @DisplayName("getById() → dönen ürün detay bilgileri eksiksiz olmalı")
    void getById_shouldReturnCompleteProductDetails() {
        when(productService.findById(1L)).thenReturn(laptop);

        ResponseEntity<ApiResponse<Product>> response = productController.getById(1L);

        Product data = response.getBody().getData();
        assertThat(data.getId()).isEqualTo(1L);
        assertThat(data.getName()).isEqualTo("Laptop");
        assertThat(data.getDescription()).isEqualTo("Gaming laptop");
        assertThat(data.getPrice()).isEqualTo(1500.0);
        assertThat(data.getImage()).isEqualTo("laptop.jpg");
        assertThat(data.getCategory().getName()).isEqualTo("Electronics");
    }

    // ----------------------------------------------------------------
    // store() testleri
    // ----------------------------------------------------------------

    @Test
    @DisplayName("store() → 201 Created, yeni ürün döner")
    void store_shouldReturn201_whenRequestIsValid() {
        ProductRequest request = new ProductRequest(
                1L, "Laptop", "Gaming laptop", "laptop.jpg", 1500.0
        );

        when(productService.save(any(ProductRequest.class))).thenReturn(laptop);

        ResponseEntity<ApiResponse<Product>> response = productController.store(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getMessage()).isEqualTo("Product created successfully");
        assertThat(response.getBody().getData().getName()).isEqualTo("Laptop");
        assertThat(response.getBody().getData().getPrice()).isEqualTo(1500.0);

        verify(productService, times(1)).save(any(ProductRequest.class));
    }

    @Test
    @DisplayName("store() → NotFoundException fırlatmalı (kategori bulunamadı)")
    void store_shouldThrowNotFoundException_whenCategoryNotFound() {
        ProductRequest request = new ProductRequest(
                999L, "Laptop", "Description", null, 1500.0
        );

        when(productService.save(any(ProductRequest.class)))
                .thenThrow(new NotFoundException("Category does not exist"));

        assertThatThrownBy(() -> productController.store(request))
                .isInstanceOf(NotFoundException.class)
                .hasMessage("Category does not exist");
    }

    @Test
    @DisplayName("store() → request service'e doğru iletilmeli")
    void store_shouldPassRequestToService() {
        ProductRequest request = new ProductRequest(
                1L, "Laptop", "Description", "img.jpg", 999.99
        );

        when(productService.save(request)).thenReturn(laptop);

        productController.store(request);

        verify(productService).save(request);
    }

    @Test
    @DisplayName("store() → kategori bilgisiyle birlikte ürün dönmeli")
    void store_shouldReturnProductWithCategory() {
        ProductRequest request = new ProductRequest(
                1L, "Laptop", "Description", null, 1500.0
        );

        when(productService.save(any(ProductRequest.class))).thenReturn(laptop);

        ResponseEntity<ApiResponse<Product>> response = productController.store(request);

        assertThat(response.getBody().getData().getCategory()).isNotNull();
        assertThat(response.getBody().getData().getCategory().getName()).isEqualTo("Electronics");
    }

    @Test
    @DisplayName("store() → null image ile ürün oluşturulabilmeli")
    void store_shouldCreateProductWithNullImage() {
        Product productWithoutImage = Product.builder()
                .id(3L)
                .name("Tablet")
                .price(500.0)
                .category(electronics)
                .build();

        ProductRequest request = new ProductRequest(
                1L, "Tablet", "A tablet", null, 500.0
        );

        when(productService.save(any(ProductRequest.class))).thenReturn(productWithoutImage);

        ResponseEntity<ApiResponse<Product>> response = productController.store(request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody().getData().getImage()).isNull();
    }
}
