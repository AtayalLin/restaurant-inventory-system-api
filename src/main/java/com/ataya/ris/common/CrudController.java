package com.ataya.ris.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

/**
 * 通用 CRUD：行為對齊前端原本使用的 json-server，前端 hooks 不用修改
 * - GET    /{資源}        列表
 * - GET    /{資源}/{id}   單筆（找不到回 404）
 * - POST   /{資源}        新增（沒給 id 自動產生，回 201）
 * - PUT    /{資源}/{id}   整筆取代
 * - PATCH  /{資源}/{id}   只更新有傳的欄位
 * - DELETE /{資源}/{id}   刪除並回傳被刪的資料
 * 各資源的 Controller 只要繼承這個類別、指定網址即可。
 */
public abstract class CrudController<T extends BaseEntity> {

    private final JpaRepository<T, String> repository;
    private final ObjectMapper mapper;
    private final String resourceName;

    protected CrudController(JpaRepository<T, String> repository, ObjectMapper mapper, String resourceName) {
        this.repository = repository;
        this.mapper = mapper;
        this.resourceName = resourceName;
    }

    @GetMapping
    public List<T> list() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public T get(@PathVariable String id) {
        return find(id);
    }

    @PostMapping
    public ResponseEntity<T> create(@RequestBody T body) {
        if (body.getId() == null || body.getId().isBlank()) {
            body.setId(BaseEntity.newId());
        } else if (repository.existsById(body.getId())) {
            throw new BusinessException(HttpStatus.CONFLICT, "DUPLICATE_ID", resourceName + " id 已存在：" + body.getId());
        }
        validate(body);
        return ResponseEntity.status(HttpStatus.CREATED).body(repository.save(body));
    }

    @PutMapping("/{id}")
    public T replace(@PathVariable String id, @RequestBody T body) {
        T existing = find(id);
        body.setId(id);
        if (body.getCreatedAt() == null) body.setCreatedAt(existing.getCreatedAt()); // 保留原建立時間
        validate(body);
        return repository.save(body);
    }

    @PatchMapping("/{id}")
    public T patch(@PathVariable String id, @RequestBody JsonNode changes) throws IOException {
        T entity = find(id);
        // readerForUpdating：把請求中有的欄位覆蓋到現有資料上，沒傳的欄位保持不變
        mapper.readerForUpdating(entity).readValue(changes);
        entity.setId(id); // 不允許透過 PATCH 改 id
        validate(entity);
        return repository.save(entity);
    }

    @DeleteMapping("/{id}")
    public T delete(@PathVariable String id) {
        T entity = find(id);
        repository.delete(entity);
        return entity;
    }

    /** 儲存前的業務規則檢查；子類別覆寫，違反時丟出 BusinessException */
    protected void validate(T entity) {
    }

    private T find(String id) {
        return repository.findById(id).orElseThrow(() -> BusinessException.notFound(resourceName, id));
    }
}
