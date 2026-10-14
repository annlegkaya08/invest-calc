package com.example.investcalc.api.exception;

import com.example.investcalc.api.dto.response.ErrorResponse;
import com.example.investcalc.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

//любые исключения контроллеров в {@link ErrorResponse}
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex, HttpServletRequest req) {
        ApiErrorCode code = ex.getErrorCode();
        if (code.getHttpStatus() >= 500) {
            log.error("Business error {} at {}", code, req.getRequestURI(), ex);
        } else {
            log.warn("Business error {} at {}: {}", code, req.getRequestURI(), ex.getMessage());
        }
        return build(code.getHttpStatus(), code.name(), ex.getMessage(), req, ex.getDetails());
    }

    //Ошибки @Valid на теле запроса
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException ex,
                                                              HttpServletRequest req) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .toList();
        return build(400, ApiErrorCode.VALIDATION_ERROR.name(), "Ошибка валидации запроса", req, details);
    }

    //Ошибки валидации параметров метода (@RequestParam, @PathVariable с ограничениями)
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraint(ConstraintViolationException ex, HttpServletRequest req) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .toList();
        return build(400, ApiErrorCode.VALIDATION_ERROR.name(), "Ошибка валидации запроса", req, details);
    }

    //Битый JSON, неверные типы полей, пустое тело
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException ex,
                                                          HttpServletRequest req) {
        log.warn("Unreadable request at {}: {}", req.getRequestURI(), ex.getMessage());
        return build(400, ApiErrorCode.MALFORMED_REQUEST.name(),
                "Тело запроса не удалось прочитать: проверьте формат JSON и типы полей", req, List.of());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex,
                                                            HttpServletRequest req) {
        return build(400, ApiErrorCode.MALFORMED_REQUEST.name(),
                "Неверное значение параметра '" + ex.getName() + "'", req, List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAny(Exception ex, HttpServletRequest req) {
        if (ex instanceof org.springframework.web.ErrorResponse springError) {
            int status = springError.getStatusCode().value();
            if (status < 500) {
                log.warn("HTTP {} at {}: {}", status, req.getRequestURI(), ex.getMessage());
                String detail = springError.getBody().getDetail();
                String message = detail != null ? detail : "Ошибка запроса";
                return build(status, "HTTP_" + status, message, req, List.of());
            }
        }
        log.error("Unhandled exception at {}", req.getRequestURI(), ex);
        return build(500, ApiErrorCode.INTERNAL_ERROR.name(), "Внутренняя ошибка сервера", req, List.of());
    }

    private ResponseEntity<ErrorResponse> build(int status, String code, String message,
                                                HttpServletRequest req, List<String> details) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.of(status, code, message, req.getRequestURI(), details));
    }
}
