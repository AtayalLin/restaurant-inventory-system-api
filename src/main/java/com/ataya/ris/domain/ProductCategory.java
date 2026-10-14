package com.ataya.ris.domain;

import com.ataya.ris.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 商品分類（主食、小菜、飲料…） */
@Getter @Setter @Entity
public class ProductCategory extends BaseEntity {
    private String name;
}
