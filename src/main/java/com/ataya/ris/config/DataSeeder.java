package com.ataya.ris.config;

import com.ataya.ris.domain.*;
import com.ataya.ris.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.List;

/**
 * 第一次啟動（資料庫是空的）時，匯入 seed/db.json 範例資料
 * db.json 就是前端 json-server 用的同一份檔案，前後端資料一致
 * 測試時以 app.seed.enabled=false 關閉
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
class DataSeeder implements CommandLineRunner {

    private final ObjectMapper mapper;
    private final StoreRepository stores;
    private final ProductCategoryRepository categories;
    private final ProductRepository products;
    private final IngredientRepository ingredients;
    private final SupplierRepository suppliers;
    private final PurchaseOrderRepository purchaseOrders;
    private final SalesOrderRepository salesOrders;

    // 建構子注入：依賴在建立物件時就確定，欄位可以宣告 final，也方便測試時替換
    DataSeeder(ObjectMapper mapper, StoreRepository stores, ProductCategoryRepository categories, ProductRepository products,
               IngredientRepository ingredients, SupplierRepository suppliers,
               PurchaseOrderRepository purchaseOrders, SalesOrderRepository salesOrders) {
        this.mapper = mapper;
        this.stores = stores;
        this.categories = categories;
        this.products = products;
        this.ingredients = ingredients;
        this.suppliers = suppliers;
        this.purchaseOrders = purchaseOrders;
        this.salesOrders = salesOrders;
    }

    @Override
    public void run(String... args) throws Exception {
        if (products.count() > 0) return; // 已有資料就不覆蓋

        try (InputStream in = new ClassPathResource("seed/db.json").getInputStream()) {
            JsonNode root = mapper.readTree(in);
            seed(root, "stores", Store.class, stores);
            seed(root, "productCategories", ProductCategory.class, categories);
            seed(root, "suppliers", Supplier.class, suppliers);
            seed(root, "ingredients", Ingredient.class, ingredients);
            seed(root, "products", Product.class, products);
            seed(root, "purchaseOrders", PurchaseOrder.class, purchaseOrders);
            seed(root, "salesOrders", SalesOrder.class, salesOrders);
        }
        log.info("已匯入範例資料（seed/db.json）");
    }

    private <T> void seed(JsonNode root, String key, Class<T> type, JpaRepository<T, String> repo) {
        JsonNode node = root.get(key);
        if (node == null || !node.isArray()) return;
        List<T> rows = mapper.convertValue(node, mapper.getTypeFactory().constructCollectionType(List.class, type));
        repo.saveAll(rows);
    }
}
