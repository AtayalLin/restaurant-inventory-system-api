package com.ataya.ris.service;

import com.ataya.ris.common.BusinessException;
import com.ataya.ris.domain.*;
import com.ataya.ris.domain.StockLog.ChangeType;
import com.ataya.ris.domain.StockLog.ReferenceType;
import com.ataya.ris.repository.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 庫存相關的業務流程：出餐扣庫存、採購收貨加庫存
 *
 * 為什麼放在後端：前端版本要依序呼叫十幾次 API（讀配方、逐一扣庫存、寫紀錄），
 * 中途斷線就會「扣了一半」。這裡用 @Transactional 包成一筆交易：全部成功才寫入，
 * 任何一步失敗（庫存不足、被同時修改）就整筆還原。
 */
@Service
public class InventoryService {

    private final SalesOrderRepository salesOrders;
    private final PurchaseOrderRepository purchaseOrders;
    private final ProductRepository products;
    private final IngredientRepository ingredients;
    private final StockLogRepository stockLogs;

    // 建構子注入：依賴在建立物件時就確定，欄位可以宣告 final，也方便測試時替換
    public InventoryService(SalesOrderRepository salesOrders, PurchaseOrderRepository purchaseOrders, ProductRepository products,
                            IngredientRepository ingredients, StockLogRepository stockLogs) {
        this.salesOrders = salesOrders;
        this.purchaseOrders = purchaseOrders;
        this.products = products;
        this.ingredients = ingredients;
        this.stockLogs = stockLogs;
    }

    /** 出餐完成：依配方扣減食材庫存，訂單改為已完成 */
    @Transactional
    public SalesOrder completeSalesOrder(String orderId) {
        SalesOrder order = salesOrders.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("銷售訂單", orderId));
        if (order.getStatus() != SalesOrder.Status.PREPARING) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STATUS",
                    "只有「準備中」的訂單可以出餐，目前狀態：" + order.getStatus());
        }

        // 1. 依配方計算每種食材的總用量（同食材出現在多個商品時合併）
        Map<String, BigDecimal> usage = new LinkedHashMap<>();
        for (SalesOrder.Item item : order.getItems()) {
            Product product = products.findById(item.getProductId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "PRODUCT_NOT_FOUND",
                            "訂單中的商品不存在：" + item.getProductId()));
            for (Product.RecipeItem r : product.getRecipe()) {
                usage.merge(r.getIngredientId(), r.getQuantity().multiply(BigDecimal.valueOf(item.getQuantity())), BigDecimal::add);
            }
        }

        // 2. 先檢查全部食材，一次列出所有不足的項目，再開始扣
        List<Ingredient> targets = ingredients.findAllById(usage.keySet());
        String insufficient = targets.stream()
                .filter(i -> i.getCurrentStock().compareTo(usage.get(i.getId())) < 0)
                .map(Ingredient::getName)
                .collect(Collectors.joining("、"));
        if (!insufficient.isEmpty()) {
            throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INSUFFICIENT_STOCK", "庫存不足，無法完成訂單：" + insufficient);
        }

        // 3. 逐一扣庫存（樂觀鎖）並寫異動紀錄
        for (Ingredient ing : targets) {
            applyStockChange(ing, usage.get(ing.getId()).negate(), ChangeType.SALE_DEDUCT,
                    ReferenceType.SALES_ORDER, order.getId(), order.getTestBatchId());
        }

        order.setStatus(SalesOrder.Status.COMPLETED);
        return salesOrders.save(order);
    }

    /** 採購收貨：增加食材庫存，採購單改為已收貨 */
    @Transactional
    public PurchaseOrder receivePurchaseOrder(String orderId) {
        PurchaseOrder order = purchaseOrders.findById(orderId)
                .orElseThrow(() -> BusinessException.notFound("採購單", orderId));
        if (order.getStatus() != PurchaseOrder.Status.ORDERED) {
            throw new BusinessException(HttpStatus.CONFLICT, "INVALID_STATUS",
                    "只有「已下單」的採購單可以收貨，目前狀態：" + order.getStatus());
        }
        for (PurchaseOrder.Item item : order.getItems()) {
            Ingredient ing = ingredients.findById(item.getIngredientId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "INGREDIENT_NOT_FOUND",
                            "採購單中的食材不存在：" + item.getIngredientId()));
            applyStockChange(ing, item.getQuantity(), ChangeType.PURCHASE_RECEIVE,
                    ReferenceType.PURCHASE_ORDER, order.getId(), null);
        }
        order.setStatus(PurchaseOrder.Status.RECEIVED);
        order.setReceivedDate(Instant.now());
        return purchaseOrders.save(order);
    }

    /** 以樂觀鎖更新單一食材庫存；版本號不符代表被同時修改，丟出 409 讓整筆交易還原 */
    private void applyStockChange(Ingredient ing, BigDecimal change, ChangeType type,
                                  ReferenceType refType, String refId, String testBatchId) {
        BigDecimal newStock = ing.getCurrentStock().add(change);
        int updated = ingredients.updateStockIfVersion(ing.getId(), newStock, ing.getVersion());
        if (updated == 0) {
            throw new BusinessException(HttpStatus.CONFLICT, "STOCK_CONFLICT",
                    "「" + ing.getName() + "」的庫存剛被其他人修改，請重新整理後再試");
        }
        StockLog log = new StockLog();
        log.setIngredientId(ing.getId());
        log.setChangeType(type);
        log.setQuantityChange(change);
        log.setResultingStock(newStock);
        log.setReferenceType(refType);
        log.setReferenceId(refId);
        log.setTestBatchId(testBatchId);
        stockLogs.save(log);
    }
}
