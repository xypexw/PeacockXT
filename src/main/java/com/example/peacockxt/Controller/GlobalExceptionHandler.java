package com.example.peacockxt.Controller;

import com.example.peacockxt.DTO.ErrorResponse;
import com.example.peacockxt.Models.CustomException.BusinessLogicException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(DataAccessException.class)
    public ResponseEntity<ErrorResponse> handleDatabaseException(DataAccessException e) {
        log.error("Database error occurred: {}", e.getMessage(), e);
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR).body(new ErrorResponse(
                        "INTERNAL_SERVER_ERROR",
                        "An unexpected database error occurred. Please try again later."
                ));
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessLogicException e) {
        log.warn("Business rule violation: {}", e.getMessage());
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST).body(new ErrorResponse("BUSINESS_RULE_VIOLATION", e.getMessage()
                ));
    }
}