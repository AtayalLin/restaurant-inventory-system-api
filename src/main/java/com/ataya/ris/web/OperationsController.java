package com.ataya.ris.web;

import com.ataya.ris.domain.PurchaseOrder;
import com.ataya.ris.domain.SalesOrder;
import com.ataya.ris.service.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 業務動作端點（不是單純 CRUD）：一次呼叫完成整個流程，由後端保證交易一致
 * - POST /api/salesOrders/{id}/complete     出餐完成並扣庫存
 * - POST /api/purchaseOrders/{id}/receive   採購收貨並加庫存
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
class OperationsController {

    private final InventoryService inventoryService;

    @PostMapping("/salesOrders/{id}/complete")
    SalesOrder completeSalesOrder(@PathVariable String id) {
        return inventoryService.completeSalesOrder(id);
    }

    @PostMapping("/purchaseOrders/{id}/receive")
    PurchaseOrder receivePurchaseOrder(@PathVariable String id) {
        return inventoryService.receivePurchaseOrder(id);
    }
}
