package com.example.wallet_service.exception;

public class ErrorResponse {
    private final boolean success = false;
    private final String message;
    private final ErrorType type;
    private final Enum<?> subtype;

    public ErrorResponse(ErrorType type, Enum<?> subtype, String message) {
        this.type = type;
        this.subtype = subtype;
        this.message = message;
    }
}