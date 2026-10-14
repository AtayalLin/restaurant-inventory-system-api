package com.ataya.ris.common;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 業務規則錯誤：帶有 HTTP 狀態碼與錯誤代碼，由 ApiExceptionHandler 轉成標準錯誤回應
 * 例：庫存不足 → 422 INSUFFICIENT_STOCK；同時被修改 → 409 STOCK_CONFLICT
 */
@Getter
public class BusinessException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public BusinessException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public static BusinessException notFound(String resource, String id) {
        return new BusinessException(HttpStatus.NOT_FOUND, "NOT_FOUND", "找不到" + resource + "：" + id);
    }
}
