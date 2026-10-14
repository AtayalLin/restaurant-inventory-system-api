package com.ataya.ris;

import com.ataya.ris.repository.ProductRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** 通用 CRUD 行為要和 json-server 一致，前端才能直接換成這個後端 */
@SpringBootTest
@AutoConfigureMockMvc
class CrudApiTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired ProductRepository products;

    @BeforeEach
    void clean() {
        products.deleteAll();
    }

    private String createProduct() throws Exception {
        String body = """
                {"name":"滷肉飯","categoryId":"cat001","price":45,"taxType":"TAX_5","isActive":true,
                 "recipe":[{"ingredientId":"i003","quantity":60}]}""";
        String json = mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())       // 沒給 id 時自動產生
                .andExpect(jsonPath("$.createdAt").isNotEmpty())
                .andReturn().getResponse().getContentAsString();
        JsonNode node = mapper.readTree(json);
        return node.get("id").asText();
    }

    @Test
    void 新增後可以查詢單筆與列表() throws Exception {
        String id = createProduct();
        mvc.perform(get("/api/products/" + id))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("滷肉飯"))
                .andExpect(jsonPath("$.isActive").value(true))
                .andExpect(jsonPath("$.recipe[0].ingredientId").value("i003"));
        mvc.perform(get("/api/products")).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void PATCH只更新有傳的欄位() throws Exception {
        String id = createProduct();
        mvc.perform(patch("/api/products/" + id).contentType(MediaType.APPLICATION_JSON).content("{\"price\":50}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(50))
                .andExpect(jsonPath("$.name").value("滷肉飯"))          // 沒傳的欄位不變
                .andExpect(jsonPath("$.recipe", hasSize(1)));
    }

    @Test
    void 刪除後查詢回404() throws Exception {
        String id = createProduct();
        mvc.perform(delete("/api/products/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.id").value(id));
        mvc.perform(get("/api/products/" + id))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.title").value("NOT_FOUND"));
    }

    @Test
    void 指定已存在的id新增回409() throws Exception {
        String id = createProduct();
        mvc.perform(post("/api/products").contentType(MediaType.APPLICATION_JSON).content("{\"id\":\"" + id + "\",\"name\":\"重複\"}"))
                .andExpect(status().isConflict());
    }
}
