package com.ecommerce.demo.repository;

import com.ecommerce.demo.model.Category;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {
    Optional<Category> findByName(String name);
    Boolean existsByName(String name);

    @Query(value = "SELECT * FROM categories WHERE name = :name LIMIT 1", nativeQuery = true)
    Optional<Category> findByNameAny(@Param("name") String name);

    @Query(
            "SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :q, '%'))")
    Page<Category> searchByName(@Param("q") String q, Pageable pageable);
}
