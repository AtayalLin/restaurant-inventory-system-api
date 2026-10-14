package com.ataya.ris;

import com.ataya.ris.common.BusinessException;
import com.ataya.ris.domain.*;
import com.ataya.ris.repository.*;
import com.ataya.ris.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 出餐扣庫存、採購收貨、樂觀鎖：庫存正確性是這個系統最核心的規則 */
@SpringBootTest
@AutoConfigureMockMvc
@WithMockUser(roles = "OWNER") // 權限規則另由 AuthApiTest 驗證，這裡以總部身分測試業務邏輯
class InventoryServiceTest {

    @Autowired InventoryService service;
    @Autowired IngredientRepository ingredients;
    @Autowired ProductRepository products;
    @Autowired SalesOrderRepository salesOrders;
    @Autowired PurchaseOrderRepository purchaseOrders;
    @Autowired StockLogRepository stockLogs;
    @Autowired TransactionTemplate tx;
    @Autowired MockMvc mvc;

    @BeforeEach
    void setUp() {
        stockLogs.deleteAll();
        salesOrders.deleteAll();
        purchaseOrders.deleteAll();
        products.deleteAll();
        ingredients.deleteAll();
        // 牛肉麵配方：牛腱 200g＋麵條 150g；庫存：牛腱 1000g、麵條 500g
        ingredients.save(ingredient("beef", "牛腱肉", "1000"));
        ingredients.save(ingredient("noodle", "麵條", "500"));
        Product p = new Product();
        p.setId("p1");
        p.setName("牛肉麵");
        p.setPrice(new BigDecimal("180"));
        p.setRecipe(new ArrayList<>(List.of(recipe("beef", "200"), recipe("noodle", "150"))));
        products.save(p);
    }

    @Test
    void 出餐依配方扣庫存並寫入異動紀錄() {
        salesOrders.save(order("o1", SalesOrder.Status.PREPARING, 2));

        SalesOrder done = service.completeSalesOrder("o1");

        assertThat(done.getStatus()).isEqualTo(SalesOrder.Status.COMPLETED);
        assertThat(stock("beef")).isEqualByComparingTo("600");
        assertThat(stock("noodle")).isEqualByComparingTo("200");
        assertThat(ingredients.findById("beef").orElseThrow().getVersion()).isEqualTo(1); // 版本號 +1
        assertThat(stockLogs.findAll()).hasSize(2)
                .allMatch(l -> l.getChangeType() == StockLog.ChangeType.SALE_DEDUCT && l.getQuantityChange().signum() < 0);
    }

    @Test
    void 任一食材不足時整筆不扣() {
        salesOrders.save(order("o1", SalesOrder.Status.PREPARING, 4)); // 麵條需要 600g，只有 500g

        assertThatThrownBy(() -> service.completeSalesOrder("o1"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("麵條");
        assertThat(stock("beef")).isEqualByComparingTo("1000"); // 牛腱足夠也不能先扣
        assertThat(salesOrders.findById("o1").orElseThrow().getStatus()).isEqualTo(SalesOrder.Status.PREPARING);
        assertThat(stockLogs.count()).isZero();
    }

    @Test
    void 非準備中的訂單不能出餐() {
        salesOrders.save(order("o1", SalesOrder.Status.PENDING, 1));
        assertThatThrownBy(() -> service.completeSalesOrder("o1"))
                .isInstanceOf(BusinessException.class)
                .extracting("code").isEqualTo("INVALID_STATUS");
    }

    @Test
    void 樂觀鎖版本不符時不更新() {
        // 模擬「讀取後被別人改過」：用舊版本號 99 更新，應該更新 0 筆
        Integer updated = tx.execute(s -> ingredients.updateStockIfVersion("beef", new BigDecimal("1"), 99));
        assertThat(updated).isZero();
        assertThat(stock("beef")).isEqualByComparingTo("1000");
    }

    @Test
    void 採購收貨增加庫存() {
        PurchaseOrder po = new PurchaseOrder();
        po.setId("po1");
        po.setStatus(PurchaseOrder.Status.ORDERED);
        PurchaseOrder.Item item = new PurchaseOrder.Item();
        item.setIngredientId("noodle");
        item.setQuantity(new BigDecimal("2000"));
        item.setUnitPrice(new BigDecimal("0.04"));
        po.setItems(new ArrayList<>(List.of(item)));
        purchaseOrders.save(po);

        PurchaseOrder received = service.receivePurchaseOrder("po1");

        assertThat(received.getStatus()).isEqualTo(PurchaseOrder.Status.RECEIVED);
        assertThat(received.getReceivedDate()).isNotNull();
        assertThat(stock("noodle")).isEqualByComparingTo("2500");
    }

    @Test
    void API庫存不足回422與錯誤代碼() throws Exception {
        salesOrders.save(order("o1", SalesOrder.Status.PREPARING, 4));
        mvc.perform(post("/api/salesOrders/o1/complete"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.title").value("INSUFFICIENT_STOCK"));
    }

    // ── 測試資料輔助方法 ──────────────────────────────

    private BigDecimal stock(String id) {
        return ingredients.findById(id).orElseThrow().getCurrentStock();
    }

    private static Ingredient ingredient(String id, String name, String stock) {
        Ingredient i = new Ingredient();
        i.setId(id);
        i.setName(name);
        i.setUnit("g");
        i.setCurrentStock(new BigDecimal(stock));
        return i;
    }

    private static Product.RecipeItem recipe(String ingredientId, String qty) {
        Product.RecipeItem r = new Product.RecipeItem();
        r.setIngredientId(ingredientId);
        r.setQuantity(new BigDecimal(qty));
        return r;
    }

    private static SalesOrder order(String id, SalesOrder.Status status, int qty) {
        SalesOrder o = new SalesOrder();
        o.setId(id);
        o.setStatus(status);
        SalesOrder.Item item = new SalesOrder.Item();
        item.setProductId("p1");
        item.setQuantity(qty);
        item.setUnitPrice(new BigDecimal("180"));
        o.setItems(new ArrayList<>(List.of(item)));
        return o;
    }
}
