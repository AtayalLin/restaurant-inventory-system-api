package com.ataya.ris.auth;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

/**
 * 安全設定：無狀態 JWT 驗證＋依角色限制寫入
 *
 * 權限規則與前端 src/lib/permissions.ts 一致（前端只負責隱藏按鈕，真正擋下請求的是這裡）：
 * - 菜單商品寫入：總部
 * - 食材寫入：總部、店長
 * - 採購單查看：總部、店長；建立／修改／收貨：總部
 * - 其餘 API：登入即可
 */
@Slf4j
@Configuration
public class SecurityConfig {

    static final String OWNER = "OWNER", MANAGER = "MANAGER";

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // 用 Authorization 標頭帶 JWT、不用 Cookie，所以沒有 CSRF 風險，可以關閉
            .csrf(csrf -> csrf.disable())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.POST, "/api/auth/login").permitAll()
                .requestMatchers("/error").permitAll() // 讓 400 等錯誤回應不被誤判成 401
                .requestMatchers(HttpMethod.GET, "/api/purchaseOrders/**").hasAnyRole(OWNER, MANAGER)
                .requestMatchers("/api/purchaseOrders/**").hasRole(OWNER)
                .requestMatchers(HttpMethod.GET, "/api/products/**").authenticated()
                .requestMatchers("/api/products/**").hasRole(OWNER)
                .requestMatchers(HttpMethod.GET, "/api/ingredients/**").authenticated()
                .requestMatchers("/api/ingredients/**").hasAnyRole(OWNER, MANAGER)
                .anyRequest().authenticated())
            .oauth2ResourceServer(o -> o.jwt(jwt -> jwt.jwtAuthenticationConverter(roleConverter())));
        return http.build();
    }

    /** JWT 的 role 欄位（例如 OWNER）轉成 Spring 的 ROLE_OWNER 權限 */
    private JwtAuthenticationConverter roleConverter() {
        JwtGrantedAuthoritiesConverter authorities = new JwtGrantedAuthoritiesConverter();
        authorities.setAuthoritiesClaimName("role");
        authorities.setAuthorityPrefix("ROLE_");
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(authorities);
        return converter;
    }

    /**
     * HS256 簽章金鑰：從環境變數 JWT_SECRET 讀取（至少 32 字元）
     * 沒設定時啟動時隨機產生（重啟後舊 token 失效），避免把金鑰寫死在程式碼或設定檔
     */
    @Bean
    SecretKey jwtKey(@Value("${app.jwt.secret:}") String secret) {
        byte[] bytes;
        if (secret.length() >= 32) {
            bytes = secret.getBytes(StandardCharsets.UTF_8);
        } else {
            bytes = new byte[32];
            new SecureRandom().nextBytes(bytes);
            log.warn("未設定 JWT_SECRET（或少於 32 字元），已使用隨機金鑰；重新啟動後需重新登入");
        }
        return new SecretKeySpec(bytes, "HmacSHA256");
    }

    @Bean
    JwtEncoder jwtEncoder(SecretKey key) {
        return new NimbusJwtEncoder(new ImmutableSecret<>(key));
    }

    @Bean
    JwtDecoder jwtDecoder(SecretKey key) {
        return NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
