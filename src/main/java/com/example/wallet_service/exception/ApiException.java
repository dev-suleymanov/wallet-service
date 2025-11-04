package com.example.wallet_service.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final ErrorType type;
    private final Enum<?> subtype;

    public ApiException(HttpStatus status, ErrorType type, Enum<?> subtype, String message) {
        super(message);
        this.status = status;
        this.type = type;
        this.subtype = subtype;
    }

    public HttpStatus getStatus() { return status; }
    public ErrorType getType() { return type; }
    public Enum<?> getSubtype() { return subtype; }

    public static ApiException validation(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorType.VALIDATION,
                ErrorType.Validation.INVALID_FIELD,
                message
        );
    }
    public static ApiException invalidJson(String message) {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorType.VALIDATION,
                ErrorType.Validation.INVALID_JSON,
                message
        );
    }
    public static ApiException walletNotFound(String walletId) {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorType.BUSINESS,
                ErrorType.Business.WALLET_NOT_FOUND,
                "Wallet with id " + walletId + " not found"
        );
    }

    public static ApiException insufficientFunds() {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorType.BUSINESS,
                ErrorType.Business.INSUFFICIENT_FUNDS,
                "Insufficient funds"
        );
    }

    public static ApiException concurrency() {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorType.BUSINESS,
                ErrorType.Business.CONCURRENCY_ERROR,
                "Concurrent modification, please retry"
        );
    }

    public static ApiException internal(String message) {
        return new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorType.SYSTEM,
                ErrorType.System.INTERNAL_ERROR,
                message
        );
    }
}
