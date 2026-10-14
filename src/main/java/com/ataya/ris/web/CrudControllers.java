package com.ataya.ris.web;

import com.ataya.ris.common.CrudController;
import com.ataya.ris.domain.*;
import com.ataya.ris.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
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
    PurchaseOrderController(PurchaseOrderRepository r, ObjectMapper m) { super(r, m, "採購單"); }
}

@RestController @RequestMapping("/api/salesOrders")
class SalesOrderController extends CrudController<SalesOrder> {
    SalesOrderController(SalesOrderRepository r, ObjectMapper m) { super(r, m, "銷售訂單"); }
}

@RestController @RequestMapping("/api/stockLogs")
class StockLogController extends CrudController<StockLog> {
    StockLogController(StockLogRepository r, ObjectMapper m) { super(r, m, "庫存紀錄"); }
}
