package com.ataya.ris.repository;

import com.ataya.ris.domain.SalesOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SalesOrderRepository extends JpaRepository<SalesOrder, String> {
}
