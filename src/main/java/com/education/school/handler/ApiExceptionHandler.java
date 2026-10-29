package com.education.school.handler;

import com.education.school.dto.ApiError;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ApiExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiError> handleIllegalState(IllegalStateException exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError(exception.getMessage()));
    }

    // Нарушение ограничений БД (дубликат, связь с другими данными) — это конфликт данных, а не сбой сервера
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiError> handleDataIntegrity(DataIntegrityViolationException exception) {
        log.warn("Нарушение ограничения БД: {}", exception.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiError("Такая запись уже существует или связана с другими данными"));
    }

    // Битый JSON или значение не того типа (буквы вместо числа) — ошибка запроса
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class,
                       NumberFormatException.class, ClassCastException.class})
    public ResponseEntity<ApiError> handleBadRequest(Exception exception) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ApiError("Некорректные данные в запросе"));
    }
}
