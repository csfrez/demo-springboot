package com.csfrez.demospringboot.dto;

import cn.sticki.spel.validator.constrain.SpelAssert;
import cn.sticki.spel.validator.constrain.SpelNotNull;
import cn.sticki.spel.validator.javax.SpelValid;
import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Data
/**
 *
 * https://spel-validator.sticki.cn/guide/getting-started.html
 */
@SpelValid // 添加启动注解
public class User {

    @NotBlank(message = "姓名不能为空")
    @SpelAssert(assertTrue = "T(cn.hutool.core.util.StrUtil).length(#this.name) <= 5", message = "姓名长度不能超过5")
    private String name;

    @NotNull(message = "年龄不能为空")
    @SpelAssert(assertTrue = "#this.age < 18", message = "年龄不能大于17")
    private Integer age;

    @NotBlank(message = "性别不能为空")
    private String sex;

    @NotNull
    private Boolean switchAudio;

    /**
     * 此处开启了注解校验
     * 当 switchAudio 字段为 true 时，校验 audioContent，audioContent 不能为null
     */
    @SpelNotNull(condition = "#this.switchAudio == true", message = "语音内容不能为空")
    private Object audioContent;

}
