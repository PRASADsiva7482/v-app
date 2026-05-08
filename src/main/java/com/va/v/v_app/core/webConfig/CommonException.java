package com.va.v.v_app.core.webConfig;

public class CommonException extends RuntimeException {

    private static final long serialVersionUID = 1L;
    private String errorCode;

    public CommonException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
