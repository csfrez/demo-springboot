package com.csfrez.demospringboot.base;

import lombok.Data;

@Data
public class ResponseResult<T> {
    private int code;
    private String message;
    private T data;

    public static <T> ResponseResult<T> success(T data) {
        ResponseResult<T> responseResult = new ResponseResult<>();
        responseResult.setCode(ResultCode.SUCCESS.getCode());
        responseResult.setMessage(ResultCode.SUCCESS.getMessage());
        responseResult.setData(data);
        return responseResult;
    }

    public static <T> ResponseResult<T> success() {
        ResponseResult<T> responseResult = new ResponseResult<>();
        responseResult.setCode(ResultCode.SUCCESS.getCode());
        responseResult.setMessage(ResultCode.SUCCESS.getMessage());
        return responseResult;
    }

    public static <T> ResponseResult failed(String message) {
        ResponseResult<T> responseResult = new ResponseResult<>();
        responseResult.setCode(ResultCode.FAIL.getCode());
        responseResult.setMessage(message);
        return responseResult;
    }

    public static <T> ResponseResult failed(int code, String message) {
        ResponseResult<T> responseResult = new ResponseResult<>();
        responseResult.setCode(code);
        responseResult.setMessage(message);
        return responseResult;
    }

    public static boolean isSucceed(ResponseResult responseResult) {
        return responseResult.getCode() == ResultCode.SUCCESS.getCode();
    }
}