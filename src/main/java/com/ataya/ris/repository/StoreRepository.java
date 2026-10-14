package com.ataya.ris.repository;

import com.ataya.ris.domain.Store;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StoreRepository extends JpaRepository<Store, String> {
}
