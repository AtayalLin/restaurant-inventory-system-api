package com.ataya.ris.domain;

import com.ataya.ris.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 供應商 */
@Getter @Setter @Entity
public class Supplier extends BaseEntity {
    private String name;
    private String contact;
    private String phone;
    private String paymentTerm;
}
