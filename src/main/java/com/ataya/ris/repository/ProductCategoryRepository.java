package com.ataya.ris.repository;

import com.ataya.ris.domain.ProductCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductCategoryRepository extends JpaRepository<ProductCategory, String> {
}
