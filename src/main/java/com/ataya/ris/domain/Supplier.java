package com.ataya.ris.domain;

import com.ataya.ris.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/** 供應商 */
@Getter @Setter @Entity
public class Supplier extends BaseEntity {
    private String name;
    private String contact;
    private String phone;
    private String paymentTerm;

    /** 叫貨日（0=週日 … 6=週六）；空清單代表每天都可以叫貨 */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "supplier_order_day", joinColumns = @JoinColumn(name = "supplier_id"))
    @Column(name = "weekday")
    private List<Integer> orderDays = new ArrayList<>();
}
