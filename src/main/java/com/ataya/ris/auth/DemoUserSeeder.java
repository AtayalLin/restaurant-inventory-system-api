package com.ataya.ris.auth;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * 建立三個示範帳號（與前端示範身分相同），方便面試官直接登入體驗不同權限
 * 密碼由 DEMO_PASSWORD 環境變數設定，預設 demo1234；正式環境應關閉 app.seed.enabled
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "app.seed.enabled", havingValue = "true", matchIfMissing = true)
class DemoUserSeeder implements CommandLineRunner {

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final String demoPassword;

    DemoUserSeeder(AppUserRepository users, PasswordEncoder passwordEncoder,
                   @Value("${app.demo-password:demo1234}") String demoPassword) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.demoPassword = demoPassword;
    }

    @Override
    public void run(String... args) {
        if (users.count() > 0) return;
        create("u-owner", "owner", "林老闆", AppUser.Role.OWNER, null);
        create("u-manager", "manager", "王店長", AppUser.Role.MANAGER, "st1");
        create("u-staff", "staff", "小陳", AppUser.Role.STAFF, "st1");
        log.info("已建立示範帳號：owner／manager／staff");
    }

    private void create(String id, String username, String name, AppUser.Role role, String storeId) {
        AppUser u = new AppUser();
        u.setId(id);
        u.setUsername(username);
        u.setName(name);
        u.setRole(role);
        u.setStoreId(storeId);
        u.setPasswordHash(passwordEncoder.encode(demoPassword));
        users.save(u);
    }
}
