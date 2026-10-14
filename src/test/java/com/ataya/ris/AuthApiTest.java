package com.ataya.ris;

import com.ataya.ris.auth.AppUser;
import com.ataya.ris.auth.AppUserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** 登入與角色權限：前端只是隱藏按鈕，真正擋下請求的是後端，所以一定要測 */
@SpringBootTest
@AutoConfigureMockMvc
class AuthApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository users;
    @Autowired PasswordEncoder encoder;

    @BeforeEach
    void setUp() {
        users.deleteAll();
        user("owner", AppUser.Role.OWNER, null);
        user("manager", AppUser.Role.MANAGER, "st1");
        user("staff", AppUser.Role.STAFF, "st1");
    }

    @Test
    void 帳密正確回傳token與使用者資料_不含密碼() throws Exception {
        mvc.perform(login("owner", "pw1234"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isNotEmpty())
                .andExpect(jsonPath("$.user.role").value("OWNER"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void 密碼錯誤回401() throws Exception {
        mvc.perform(login("owner", "wrong"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.title").value("BAD_CREDENTIALS"));
    }

    @Test
    void 沒有token不能存取API() throws Exception {
        mvc.perform(get("/api/products")).andExpect(status().isUnauthorized());
    }

    @Test
    void 有token可以讀取_也能查詢目前使用者() throws Exception {
        String token = tokenOf("staff");
        mvc.perform(get("/api/products").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.name").value("staff"))
                .andExpect(jsonPath("$.storeId").value("st1"));
    }

    @Test
    void 員工不能修改菜單_也不能看採購單() throws Exception {
        String token = tokenOf("staff");
        mvc.perform(post("/api/products").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"name\":\"新商品\"}"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/purchaseOrders").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void 店長可以看採購單但不能建立() throws Exception {
        String token = tokenOf("manager");
        mvc.perform(get("/api/purchaseOrders").header("Authorization", "Bearer " + token)).andExpect(status().isOk());
        mvc.perform(post("/api/purchaseOrders").header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 竄改過的token無效() throws Exception {
        String token = tokenOf("staff");
        mvc.perform(get("/api/products").header("Authorization", "Bearer " + token + "x"))
                .andExpect(status().isUnauthorized());
    }

    // ── 輔助方法 ──────────────────────────────

    private void user(String username, AppUser.Role role, String storeId) {
        AppUser u = new AppUser();
        u.setId("u-" + username);
        u.setUsername(username);
        u.setName(username);
        u.setRole(role);
        u.setStoreId(storeId);
        u.setPasswordHash(encoder.encode("pw1234"));
        users.save(u);
    }

    private org.springframework.test.web.servlet.RequestBuilder login(String username, String password) {
        return post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"" + username + "\",\"password\":\"" + password + "\"}");
    }

    private String tokenOf(String username) throws Exception {
        String json = mvc.perform(login(username, "pw1234")).andReturn().getResponse().getContentAsString();
        return mapper.readTree(json).get("token").asText();
    }
}
