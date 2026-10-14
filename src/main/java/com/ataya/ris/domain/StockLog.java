package com.ataya.ris.domain;

import com.ataya.ris.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/** 庫存異動紀錄：每次出餐扣庫存、採購收貨都留一筆，可追查庫存變化 */
@Getter @Setter @Entity
public class StockLog extends BaseEntity {

    public enum ChangeType { SALE_DEDUCT, PURCHASE_RECEIVE }
    public enum ReferenceType { SALES_ORDER, PURCHASE_ORDER }

    private String ingredientId;
    @Enumerated(EnumType.STRING)
    private ChangeType changeType;
    /** 正數＝增加，負數＝減少 */
    @Column(precision = 14, scale = 3)
    private BigDecimal quantityChange;
    @Column(precision = 14, scale = 3)
    private BigDecimal resultingStock;
    @Enumerated(EnumType.STRING)
    private ReferenceType referenceType;
    private String referenceId;
    private String testBatchId;
}
