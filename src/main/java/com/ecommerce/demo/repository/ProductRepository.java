package com.ecommerce.demo.repository;

import com.ecommerce.demo.model.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @EntityGraph(attributePaths = "category")
    @Query(
            "SELECT p FROM Product p WHERE LOWER(p.name) LIKE LOWER(CONCAT('%', :q, '%')) OR "
                    + "LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Product> searchByTerm(@Param("q") String q, Pageable pageable);
}
