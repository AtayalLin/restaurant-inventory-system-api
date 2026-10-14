# 🍜 智慧餐飲進銷存管理系統 — 後端 API

[![CI](https://github.com/AtayalLin/restaurant-inventory-system-api/actions/workflows/ci.yml/badge.svg)](https://github.com/AtayalLin/restaurant-inventory-system-api/actions/workflows/ci.yml)

[前端 React 專案](https://github.com/AtayalLin/restaurant-inventory-system-React) 的 REST API，取代開發期使用的 json-server。

| 分類 | 技術 |
|---|---|
| 框架 | Spring Boot 3.5、Java 21 |
| 資料庫 | MySQL 8（測試使用 H2 記憶體資料庫） |
| 資料存取 | Spring Data JPA／Hibernate |
| 測試 | JUnit 5、MockMvc、AssertJ |
| 部署 | Docker、Docker Compose、GitHub Actions CI |

---

## 設計重點

- **與前端零改動對接**：網址與行為對齊 json-server（`GET/POST/PUT/PATCH/DELETE /api/{資源}`、字串 id、PATCH 只更新有傳的欄位、找不到回 404），前端只要改一行環境變數就能從 json-server 換過來。通用邏輯集中在 `CrudController`，每個資源只要宣告網址。
- **出餐扣庫存放在後端交易中**：`POST /api/salesOrders/{id}/complete` 用 `@Transactional` 包成一筆交易。依配方計算用量 → 先檢查所有食材 → 逐一扣減並寫異動紀錄；任何一步失敗就整筆還原，不會「扣了一半」。
- **樂觀鎖防止超賣**：扣庫存使用條件更新 `UPDATE … WHERE id = ? AND version = ?`。兩台收銀機同時出餐時，只有一邊會成功，另一邊得到 `409 STOCK_CONFLICT`，由資料庫保證一致性。
- **標準錯誤格式**：使用 Problem Details（RFC 9457），例如 `{"status":422,"title":"INSUFFICIENT_STOCK","detail":"庫存不足，無法完成訂單：麵條"}`。
- **密碼不進版控**：連線資訊全部由環境變數提供，`.env` 已列入 `.gitignore`。

## API 一覽

| 資源 | 網址 |
|---|---|
| 門市 | `/api/stores` |
| 商品分類 | `/api/productCategories` |
| 菜單商品（含配方） | `/api/products` |
| 食材 | `/api/ingredients` |
| 供應商 | `/api/suppliers` |
| 採購單 | `/api/purchaseOrders` |
| 銷售訂單 | `/api/salesOrders` |
| 庫存異動紀錄 | `/api/stockLogs` |

| 業務動作 | 說明 |
|---|---|
| `POST /api/salesOrders/{id}/complete` | 出餐完成：依配方扣庫存（僅限「準備中」）|
| `POST /api/purchaseOrders/{id}/receive` | 採購收貨：增加庫存（僅限「已下單」）|

---

## 啟動方式

### 方式一：Docker Compose（最簡單，不需要安裝 Java 與 MySQL）

```bash
cp .env.example .env        # 填入自訂的 DB_PASSWORD
docker compose up --build   # 啟動 MySQL（port 3307）＋ API（port 8080）
```

### 方式二：IDE＋本機 MySQL（開發用）

1. 在 MySQL Workbench 確認本機 MySQL 已啟動（127.0.0.1:3306）。資料庫 `ris` 會自動建立。
2. 設定環境變數 `DB_PASSWORD` 為你的 MySQL root 密碼。
   - Eclipse：Run Configurations → Environment；IntelliJ：Run/Debug Configurations → Environment variables
3. 執行 `RisApplication`。第一次啟動會自動匯入 `seed/db.json` 範例資料（與前端同一份）。

> Eclipse 使用者：本專案使用 Lombok，請先執行一次 `java -jar lombok.jar` 把 Lombok 裝進 Eclipse，否則 IDE 會顯示找不到 getter／setter（Maven 建置不受影響）。

### 連接前端

前端專案的 `.env` 加入：

```bash
API_PROXY_TARGET=http://localhost:8080/api
```

重新執行 `npm run dev`，前端的 `/api/*` 請求就會轉到這個後端。

## 測試

```bash
mvn verify
```

測試使用 H2 記憶體資料庫，不需要 MySQL。涵蓋：

- CRUD 行為與 json-server 一致（自動產生 id、PATCH 部分更新、404、重複 id 回 409）
- 出餐依配方扣庫存、寫入異動紀錄、版本號遞增
- 任一食材不足時整筆不扣（交易還原）
- 非「準備中」訂單不能出餐
- 樂觀鎖：版本號不符時不更新
- 採購收貨增加庫存
- API 錯誤回應格式（422 INSUFFICIENT_STOCK）

## 後續規劃

- JWT 登入與角色權限（總部／店長／員工），取代前端的示範身分
- Flyway 管理資料表版本（目前開發階段使用 `ddl-auto: update`）
- 前端「出餐完成」改呼叫後端 `/complete` 端點，取代前端逐筆呼叫
- Testcontainers：CI 改用真的 MySQL 跑測試
