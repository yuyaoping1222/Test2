package com.example.server.model;

import lombok.Getter;
import lombok.Setter;

@Getter
public enum ExecutionCode {
    INVALID_PARAMETER(4001, "Invalid parameter"),
    NOT_FOUND(4004, "Resource not found"),
    UNAUTHORIZED(4003, "Unauthorized operation"),
    BUSINESS_ERROR(4000, "Business error"),
    SYSTEM_ERROR(5000, "System error"),


    SUCCESS(200, "Success"),;


    private final int code;
    private String message;

    ExecutionCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public ExecutionCode withProperty(String property) {
        this.message = this.message + " - " + property;
        return this;
    }
}
