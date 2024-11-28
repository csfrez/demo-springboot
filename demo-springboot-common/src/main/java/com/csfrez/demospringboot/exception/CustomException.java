package com.csfrez.demospringboot.exception;

public class CustomException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;

    public CustomException(int code, String message) {
        super(message);
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
