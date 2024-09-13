package com.csfrez.demospringboot.chain.dto;


import cn.hutool.json.JSONUtil;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class Result {

    private Boolean success;

    private String code;

    public static Result success(){
        return new Result(true, "200");
    }

    public static Result failure(String code) {
        return new Result(false, code);
    }

    @Override
    public String toString() {
        return JSONUtil.toJsonStr(this);
    }

    public boolean isSuccess() {
        return this.success;
    }
}
