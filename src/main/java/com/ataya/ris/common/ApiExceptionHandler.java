package com.ataya.ris.common;

import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 統一錯誤格式：使用 RFC 9457 Problem Details（Spring 內建 ProblemDetail）
 * 回應範例：{ "status": 422, "title": "INSUFFICIENT_STOCK", "detail": "庫存不足，無法完成訂單：麵條" }
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    ProblemDetail handleBusiness(BusinessException ex) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(ex.getStatus(), ex.getMessage());
        problem.setTitle(ex.getCode());
        return problem;
    }
}
