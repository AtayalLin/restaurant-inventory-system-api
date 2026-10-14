package com.ataya.ris.repository;

import com.ataya.ris.domain.PurchaseOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, String> {
}
