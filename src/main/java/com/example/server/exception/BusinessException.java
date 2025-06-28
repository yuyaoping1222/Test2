package com.example.server.exception;

import com.example.server.model.ExecutionCode;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final int code;

    public BusinessException(int code, String message) {
        super(message);
        this.code = code;
    }

    public BusinessException(ExecutionCode executionCode) {
        super(executionCode.getMessage());

        this.code = executionCode.getCode();
    }
}