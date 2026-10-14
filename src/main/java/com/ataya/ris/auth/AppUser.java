package com.ataya.ris.auth;

import com.ataya.ris.common.BaseEntity;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** 系統使用者；密碼只存 BCrypt 雜湊，永遠不回傳給前端 */
@Getter @Setter @Entity
@Table(name = "app_user") // user 是部分資料庫的保留字，改用 app_user
public class AppUser extends BaseEntity {

    public enum Role { OWNER, MANAGER, STAFF }

    @Column(unique = true, nullable = false, length = 50)
    private String username;

    @JsonIgnore
    private String passwordHash;

    private String name;

    @Enumerated(EnumType.STRING)
    private Role role;

    /** 所屬門市；總部為 null */
    private String storeId;
}
