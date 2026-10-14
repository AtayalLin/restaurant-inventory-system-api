package com.ataya.ris.auth;

import com.ataya.ris.common.BusinessException;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.web.bind.annotation.*;

import java.time.Duration;
import java.time.Instant;

/**
 * 登入與目前使用者
 * - POST /api/auth/login  帳密正確回傳 JWT（有效 8 小時，約一個班次）
 * - GET  /api/auth/me     依 token 回傳目前使用者
 */
@RestController
@RequestMapping("/api/auth")
class AuthController {

    private static final Duration TOKEN_TTL = Duration.ofHours(8);

    private final AppUserRepository users;
    private final PasswordEncoder passwordEncoder;
    private final JwtEncoder jwtEncoder;

    AuthController(AppUserRepository users, PasswordEncoder passwordEncoder, JwtEncoder jwtEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
        this.jwtEncoder = jwtEncoder;
    }

    record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    record UserView(String id, String name, String role, String storeId) {
        static UserView of(AppUser u) { return new UserView(u.getId(), u.getName(), u.getRole().name(), u.getStoreId()); }
    }
    record LoginResponse(String token, UserView user) {}

    @PostMapping("/login")
    LoginResponse login(@Valid @RequestBody LoginRequest req) {
        // 帳號不存在與密碼錯誤回同一個訊息，避免被用來猜測哪些帳號存在
        AppUser user = users.findByUsername(req.username())
                .filter(u -> passwordEncoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "BAD_CREDENTIALS", "帳號或密碼錯誤"));

        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .subject(user.getId())
                .issuedAt(now)
                .expiresAt(now.plus(TOKEN_TTL))
                .claim("role", user.getRole().name())
                .claim("name", user.getName())
                .claim("storeId", user.getStoreId() == null ? "" : user.getStoreId())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new LoginResponse(token, UserView.of(user));
    }

    @GetMapping("/me")
    UserView me(@org.springframework.security.core.annotation.AuthenticationPrincipal Jwt jwt) {
        return users.findById(jwt.getSubject()).map(UserView::of)
                .orElseThrow(() -> new BusinessException(HttpStatus.UNAUTHORIZED, "USER_NOT_FOUND", "使用者不存在，請重新登入"));
    }
}
