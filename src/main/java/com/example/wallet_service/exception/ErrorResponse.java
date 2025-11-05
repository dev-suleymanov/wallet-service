package com.example.wallet_service.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "success", "type", "subtype", "message" })
public record ErrorResponse(
        boolean success,
        String type,
        String subtype,
        String message
) {
    public ErrorResponse(ErrorType type, Enum<?> subtype, String message) {
        this(false, type.name(), subtype != null ? subtype.name() : null, message);
    }
}
