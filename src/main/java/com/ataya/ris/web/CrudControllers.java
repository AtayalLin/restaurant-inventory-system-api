package com.ataya.ris.web;

import com.ataya.ris.common.BusinessException;
import com.ataya.ris.common.CrudController;
import com.ataya.ris.domain.*;
import com.ataya.ris.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// 各資源的 CRUD 端點；網址與前端 json-server 時期相同（/api/products …）
// 行為都繼承自 CrudController，這裡只負責「哪個網址對應哪張資料表」

@RestController @RequestMapping("/api/stores")
class StoreController extends CrudController<Store> {
    StoreController(StoreRepository r, ObjectMapper m) { super(r, m, "門市"); }
}

@RestController @RequestMapping("/api/productCategories")
class ProductCategoryController extends CrudController<ProductCategory> {
    ProductCategoryController(ProductCategoryRepository r, ObjectMapper m) { super(r, m, "商品分類"); }
}

@RestController @RequestMapping("/api/products")
class ProductController extends CrudController<Product> {
    ProductController(ProductRepository r, ObjectMapper m) { super(r, m, "商品"); }
}

@RestController @RequestMapping("/api/ingredients")
class IngredientController extends CrudController<Ingredient> {
    IngredientController(IngredientRepository r, ObjectMapper m) { super(r, m, "食材"); }
}

@RestController @RequestMapping("/api/suppliers")
class SupplierController extends CrudController<Supplier> {
    SupplierController(SupplierRepository r, ObjectMapper m) { super(r, m, "供應商"); }
}

@RestController @RequestMapping("/api/purchaseOrders")
class PurchaseOrderController extends CrudController<PurchaseOrder> {
    private final IngredientRepository ingredients;

    PurchaseOrderController(PurchaseOrderRepository r, ObjectMapper m, IngredientRepository ingredients) {
        super(r, m, "採購單");
        this.ingredients = ingredients;
    }

    /** 叫貨規則：每一項都不能低於該食材的最低訂購量（前端也會檢查，這裡防止繞過前端直接呼叫 API） */
    @Override
    protected void validate(PurchaseOrder order) {
        for (PurchaseOrder.Item item : order.getItems()) {
            ingredients.findById(item.getIngredientId()).ifPresent(ing -> {
                if (ing.getMinOrderQty() != null && item.getQuantity() != null
                        && item.getQuantity().compareTo(ing.getMinOrderQty()) < 0) {
                    throw new BusinessException(HttpStatus.UNPROCESSABLE_ENTITY, "BELOW_MIN_ORDER",
                            "「" + ing.getName() + "」低於最低訂購量 " + ing.getMinOrderQty().stripTrailingZeros().toPlainString() + " " + ing.getUnit());
                }
            });
        }
    }
}

@RestController @RequestMapping("/api/salesOrders")
class SalesOrderController extends CrudController<SalesOrder> {
    SalesOrderController(SalesOrderRepository r, ObjectMapper m) { super(r, m, "銷售訂單"); }
}

@RestController @RequestMapping("/api/stockLogs")
class StockLogController extends CrudController<StockLog> {
    StockLogController(StockLogRepository r, ObjectMapper m) { super(r, m, "庫存紀錄"); }
}
