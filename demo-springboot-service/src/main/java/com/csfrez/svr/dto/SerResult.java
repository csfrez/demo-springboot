package com.csfrez.svr.dto;

import lombok.Data;

/**
 * 服务调用的统一返回结果，前端拿到就知道是成功还是失败
 *
 * @author
 * @date 2026/1/27 11:52
 * @email
 */
@Data
public class SerResult<T> {
    private int code;       // 200=成功，500=失败，前端一看就懂
    private String msg;     // 提示信息，比如“操作成功”“查不到用户”
    private T data;         // 成功时返回的数据，比如用户信息

    // 成功的时候调用这个方法，把数据传进去
    public static <T> SerResult<T> success(T data) {
        SerResult<T> result = new SerResult<>();
        result.setCode(200);
        result.setMsg("操作成功");
        result.setData(data);
        return result;
    }

    // 失败的时候调用这个方法，传错误信息
    public static <T> SerResult<T> fail(String msg) {
        SerResult<T> result = new SerResult<>();
        result.setCode(500);
        result.setMsg(msg);
        result.setData(null);
        return result;
    }
}
