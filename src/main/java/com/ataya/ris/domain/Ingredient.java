package com.ataya.ris.domain;

import com.ataya.ris.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * 食材（中央廚房統一庫存）
 * version 是樂觀鎖版本號：每次異動庫存 +1，扣庫存時用條件更新確認沒有被別人同時修改。
 * 不使用 JPA @Version，是因為前端也會讀寫這個欄位（沿用 json-server 時期的做法）。
 */
@Getter @Setter @Entity
public class Ingredient extends BaseEntity {
    private String name;
    private String unit;
    private String category;
    private String supplierId;
    @Column(precision = 14, scale = 3)
    private BigDecimal currentStock = BigDecimal.ZERO;
    @Column(precision = 14, scale = 3)
    private BigDecimal safetyStock = BigDecimal.ZERO;
    /** 保存天數；null 代表不會過期 */
    private Integer expiryDays;
    @Column(precision = 14, scale = 4)
    private BigDecimal costPerUnit = BigDecimal.ZERO;
    private Integer version = 0;
}
