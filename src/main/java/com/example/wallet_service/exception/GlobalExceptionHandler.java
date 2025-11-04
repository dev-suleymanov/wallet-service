package com.example.wallet_service.exception;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingPathVariableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.Optional;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new ErrorResponse(ex.getType(), ex.getSubtype(), ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        FieldError first = ex.getBindingResult().getFieldErrors().stream().findFirst().orElse(null);
        String msg = first != null ? first.getDefaultMessage() : "Validation failed";
        var subtype = (first != null && "must not be null".equals(first.getDefaultMessage()))
                ? ErrorType.Validation.MISSING_FIELD
                : ErrorType.Validation.INVALID_FIELD;

        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorType.VALIDATION, subtype, msg));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleNotReadable(HttpMessageNotReadableException ex) {
        String msg = "Invalid JSON";
        var subtype = ErrorType.Validation.INVALID_JSON;

        Throwable cause = ex.getCause();
        if (cause instanceof InvalidFormatException ife) {
            String field = firstFieldName(ife).orElse(null);
            Class<?> targetType = ife.getTargetType();
            if (field != null && targetType != null) {
                switch (field) {
                    case "operationType" -> { msg = "operationType must be DEPOSIT or WITHDRAW"; subtype = ErrorType.Validation.INVALID_FIELD; }
                    case "walletId"      -> { msg = "walletId must be a valid UUID";          subtype = ErrorType.Validation.INVALID_FIELD; }
                    case "amount"        -> { msg = "amount must be a number";                subtype = ErrorType.Validation.INVALID_FIELD; }
                    default              -> { msg = field + " has invalid format";            subtype = ErrorType.Validation.INVALID_FIELD; }
                }
            }
        }
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(ErrorType.VALIDATION, subtype, msg));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnknown(Exception ex) {
        return ResponseEntity.internalServerError()
                .body(new ErrorResponse(ErrorType.SYSTEM, ErrorType.System.INTERNAL_ERROR, "Unexpected server error"));
    }

    private Optional<String> firstFieldName(JsonMappingException jme) {
        if (jme.getPath() == null || jme.getPath().isEmpty()) return Optional.empty();
        JsonMappingException.Reference ref = jme.getPath().get(0);
        return Optional.ofNullable(ref.getFieldName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        if ("walletId".equals(ex.getName())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(
                            ErrorType.VALIDATION,
                            ErrorType.Validation.INVALID_FIELD,
                            "walletId must be a valid UUID"
                    ));
        }
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        ErrorType.VALIDATION,
                        ErrorType.Validation.INVALID_FIELD,
                        ex.getName() + " has invalid format"
                ));
    }

    @ExceptionHandler(MissingPathVariableException.class)
    public ResponseEntity<ErrorResponse> handleMissingPathVar(MissingPathVariableException ex) {
        if ("walletId".equals(ex.getVariableName())) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponse(
                            ErrorType.VALIDATION,
                            ErrorType.Validation.MISSING_FIELD,
                            "walletId must not be null"
                    ));
        }
        return ResponseEntity.badRequest()
                .body(new ErrorResponse(
                        ErrorType.VALIDATION,
                        ErrorType.Validation.MISSING_FIELD,
                        ex.getVariableName() + " is required"
                ));
    }
}
