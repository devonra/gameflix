package com.gameflix.userservice.web;

import com.gameflix.userservice.dto.Dtos.ErrorResponse;
import com.gameflix.userservice.service.AuthService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> invalidInput(MethodArgumentNotValidException ex) {
        Map<String, String> details = new LinkedHashMap<>();
        // Only field names and messages: rejected values (e.g. passwords) are never echoed back
        for (FieldError fe : ex.getBindingResult().getFieldErrors()) {
            details.putIfAbsent(fe.getField(), fe.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Bad Request", "Invalid input", details));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorResponse> unreadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorResponse("Bad Request", "Malformed request body", null));
    }

    @ExceptionHandler(AuthService.EmailAlreadyRegisteredException.class)
    ResponseEntity<ErrorResponse> duplicateEmail() {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Conflict", "Email already registered", null));
    }

    @ExceptionHandler(AuthService.InvalidCredentialsException.class)
    ResponseEntity<ErrorResponse> badCredentials() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new ErrorResponse("Unauthorized", "Invalid email or password", null));
    }
}
