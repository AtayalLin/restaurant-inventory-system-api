package com.ataya.ris.domain;

import com.ataya.ris.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 銷售訂單：待出餐 → 準備中 → 已完成（扣庫存）／已退款 */
@Getter @Setter @Entity
public class SalesOrder extends BaseEntity {

    public enum Status { PENDING, PREPARING, COMPLETED, REFUNDED }
    public enum Channel { DINE_IN, TAKEOUT, DELIVERY }

    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "sales_order_item", joinColumns = @JoinColumn(name = "order_id"))
    private List<Item> items = new ArrayList<>();

    @Column(precision = 12, scale = 2) private BigDecimal subtotal;
    @Column(precision = 12, scale = 2) private BigDecimal taxAmount;
    @Column(precision = 12, scale = 2) private BigDecimal discount = BigDecimal.ZERO;
    @Column(precision = 12, scale = 2) private BigDecimal total;

    @Enumerated(EnumType.STRING)
    private Channel channel = Channel.DINE_IN;
    @Column(length = 200)
    private String note;
    /** 併發測試批號，正式訂單為 null */
    private String testBatchId;
    /** 所屬門市 */
    private String storeId;

    @Getter @Setter @Embeddable
    public static class Item {
        private String productId;
        private Integer quantity;
        @Column(precision = 12, scale = 2)
        private BigDecimal unitPrice;
    }
}
