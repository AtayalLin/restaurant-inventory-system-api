package com.ataya.ris.repository;

import com.ataya.ris.domain.Ingredient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;

public interface IngredientRepository extends JpaRepository<Ingredient, String> {

    /**
     * 樂觀鎖條件更新：只有版本號和讀取時一樣才更新庫存，並把版本號 +1
     * 回傳更新筆數；0 代表這段期間被別人改過（例如兩台收銀機同時出餐），呼叫端要中止交易
     * 用一句 SQL 完成「比對＋更新」，資料庫層級保證不會兩邊都成功
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Ingredient i set i.currentStock = :stock, i.version = i.version + 1 where i.id = :id and i.version = :version")
    int updateStockIfVersion(@Param("id") String id, @Param("stock") BigDecimal stock, @Param("version") int version);
}
