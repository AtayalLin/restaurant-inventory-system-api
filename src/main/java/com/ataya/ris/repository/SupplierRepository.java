package com.ataya.ris.repository;

import com.ataya.ris.domain.Supplier;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, String> {
}
