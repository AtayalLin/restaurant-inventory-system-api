package com.ataya.ris.domain;

import com.ataya.ris.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 門市 */
@Getter @Setter @Entity
public class Store extends BaseEntity {
    private String name;
    private String address;
}
