package com.ataya.ris.domain;

import com.ataya.ris.common.BaseEntity;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/** 菜單商品；recipe 是配方（每份用到哪些食材、多少量），出餐時依此扣庫存 */
@Getter @Setter @Entity
public class Product extends BaseEntity {

    public enum TaxType { TAX_5, TAX_10, TAX_FREE }

    private String name;
    private String categoryId;
    @Column(precision = 12, scale = 2)
    private BigDecimal price;
    @Enumerated(EnumType.STRING)
    private TaxType taxType = TaxType.TAX_5;
    @Column(length = 500)
    private String description;
    private String imageUrl;
    private Boolean isActive = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "product_recipe", joinColumns = @JoinColumn(name = "product_id"))
    private List<RecipeItem> recipe = new ArrayList<>();

    @Getter @Setter @Embeddable
    public static class RecipeItem {
        private String ingredientId;
        @Column(precision = 14, scale = 3)
        private BigDecimal quantity;
    }
}
