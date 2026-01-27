package com.csfrez.svr.lambda;

import java.io.Serializable;

/**
 * 支持序列化的双参数函数接口，Lambda要符合这个格式
 * @author
 * @date 2026/1/27 14:23
 * @email
 */
public interface SerialBiFunction<T, U, R> extends Serializable {
    // 方法格式：传入T（Service实例）和U（参数），返回R（结果）
    R apply(T t, U u);
}

