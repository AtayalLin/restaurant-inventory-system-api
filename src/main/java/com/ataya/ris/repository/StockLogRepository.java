package com.ataya.ris.repository;

import com.ataya.ris.domain.StockLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StockLogRepository extends JpaRepository<StockLog, String> {
}
