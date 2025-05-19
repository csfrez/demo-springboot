package com.csfrez.demospringboot.base;

import lombok.Getter;

@Getter
public enum ResultCode {

    SUCCESS(200, "成功"),
    FAIL(900, "失败"),
    PARAM_ERROR(400, "参数错误"),
    UNAUTHORIZED(401, "未认证"),
    NOT_FOUND(404, "未找到"),
    INTERNAL_SERVER_ERROR(500, "服务器内部错误");

    ResultCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    private int code;

    private String message;
}
