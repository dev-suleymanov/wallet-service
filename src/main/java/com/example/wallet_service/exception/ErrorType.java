package com.example.wallet_service.exception;

import com.fasterxml.jackson.annotation.JsonValue;

public enum ErrorType {

    VALIDATION(Validation.class),
    BUSINESS(Business.class),
    SYSTEM(System.class);

    private final Class<? extends Enum<?>> subtype;

    ErrorType(Class<? extends Enum<?>> subtype) {
        this.subtype = subtype;
    }

    public Class<? extends Enum<?>> getSubtype() {
        return subtype;
    }

    public enum Validation {
        INVALID_JSON,
        INVALID_FIELD,
        MISSING_FIELD
    }

    public enum Business {
        WALLET_NOT_FOUND,
        INSUFFICIENT_FUNDS,
        CONCURRENCY_ERROR
    }

    public enum System {
        INTERNAL_ERROR
    }

    @JsonValue
    public String toJson() {
        return name();
    }
}
