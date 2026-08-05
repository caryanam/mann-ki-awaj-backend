package com.mka.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        Map<String, String> validationErrors = new LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(error -> validationErrors.put(error.getField(), error.getDefaultMessage()));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Validation Failed");
        response.put("timestamp", LocalDateTime.now());
        response.put("errors", validationErrors);

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(ValidationException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("status", HttpStatus.NOT_FOUND.value());
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("status", HttpStatus.CONFLICT.value());
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedException(UnauthorizedException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("status", HttpStatus.UNAUTHORIZED.value());
        response.put("message", "Invalid email or password");
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.UNAUTHORIZED);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", "Malformed JSON request structure");
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(com.mka.translation.exception.TranslationRequestException.class)
    public ResponseEntity<Map<String, Object>> handleTranslationRequestException(com.mka.translation.exception.TranslationRequestException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(com.mka.translation.exception.TranslationServiceUnavailableException.class)
    public ResponseEntity<Map<String, Object>> handleTranslationServiceUnavailableException(com.mka.translation.exception.TranslationServiceUnavailableException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("status", HttpStatus.SERVICE_UNAVAILABLE.value());
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(com.mka.translation.exception.TranslationFailedException.class)
    public ResponseEntity<Map<String, Object>> handleTranslationFailedException(com.mka.translation.exception.TranslationFailedException ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("status", HttpStatus.BAD_GATEWAY.value());
        response.put("message", ex.getMessage());
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.BAD_GATEWAY);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("success", false);
        response.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        response.put("message", ex.getMessage() != null ? ex.getMessage() : "Internal server error");
        response.put("timestamp", LocalDateTime.now());
        return new ResponseEntity<>(response, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
