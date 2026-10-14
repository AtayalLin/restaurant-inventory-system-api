package com.ataya.ris.common;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

/**
 * 所有資料表共用欄位：字串 id 與建立時間
 * id 用字串而非自動遞增數字，是為了和前端（原本的 json-server）資料格式一致，前端不用改。
 */
@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity {

    @Id
    @Column(length = 40)
    private String id;

    private Instant createdAt;

    /** 新增時若前端沒給 id／建立時間，由後端補上 */
    @PrePersist
    void fillDefaults() {
        if (id == null || id.isBlank()) id = newId();
        if (createdAt == null) createdAt = Instant.now();
    }

    public static String newId() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
