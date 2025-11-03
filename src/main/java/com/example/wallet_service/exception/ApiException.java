package com.example.wallet_service.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final ErrorCode code;
    private final String message;

    public ApiException(HttpStatus status, ErrorCode code, String message) {
        super(message);
        this.status = status;
        this.code = code;
        this.message = message;
    }

    public static ApiException invalidRequest() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.INVALID_REQUEST,
                "Invalid request data"
        );
    }

    public static ApiException internalError() {
        return new ApiException(
                HttpStatus.INTERNAL_SERVER_ERROR,
                ErrorCode.INTERNAL_ERROR,
                "Internal server error"
        );
    }
}

enum ErrorCode {
    WALLET_NOT_FOUND,
    INSUFFICIENT_FUNDS,
    INVALID_REQUEST,
    INTERNAL_ERROR
}
