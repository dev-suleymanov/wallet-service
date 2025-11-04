package com.example.wallet_service.exception;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

@JsonPropertyOrder({ "success", "type", "subtype", "message" })
public class ErrorResponse {
    private final boolean success = false;
    private final String message;
    private final String type;
    private final String subtype;

    public ErrorResponse(ErrorType type, Enum<?> subtype, String message) {
        this.type = type.name();
        this.subtype = subtype != null ? subtype.name() : null;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getType() {
        return type;
    }

    public String getSubtype() {
        return subtype;
    }
}
