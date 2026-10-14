package com.ataya.ris.domain;

import com.ataya.ris.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** 採購單：待確認 → 已下單 → 已收貨（加庫存）／已取消 */
@Getter @Setter @Entity
public class PurchaseOrder extends BaseEntity {

    public enum Status { PENDING, ORDERED, RECEIVED, CANCELLED }

    private String supplierId;
    @Enumerated(EnumType.STRING)
    private Status status = Status.PENDING;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "purchase_order_item", joinColumns = @JoinColumn(name = "order_id"))
    private List<Item> items = new ArrayList<>();

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;
    private LocalDate expectedDate;
    private Instant receivedDate;
    @Column(length = 200)
    private String note;

    @Getter @Setter @Embeddable
    public static class Item {
        private String ingredientId;
        @Column(precision = 14, scale = 3)
        private BigDecimal quantity;
        @Column(precision = 14, scale = 4)
        private BigDecimal unitPrice;
    }
}
