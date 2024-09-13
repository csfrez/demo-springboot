package com.csfrez.demospringboot.web;


import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.csfrez.demospringboot.chain.dto.User;
import com.csfrez.demospringboot.exception.CustomException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@Slf4j
public class WebController {

    @GetMapping("/hello")
    public String hello() {
        return "Hello Spring, Time is " + System.currentTimeMillis();
    }

    @GetMapping("/test")
    public String test() throws Exception {
        int random = RandomUtil.randomInt(5);
        log.info("random: {}", random);

        if(random == 4){
            throw new CustomException(501, "自定义异常");
        }

        if(random == 3){
            throw new Exception("抛出异常");
        }

        if(random == 2) {
            throw new RuntimeException("运行时异常");
        }

        String s = null;
        if(random == 1) {
            int length = s.length();
        }

        if(random == 0) {
            int result = 1 / 0;
        }
        return "Success, random is " + random;
    }

    @GetMapping("/user")
    public String getUser(@RequestParam String name, @RequestParam Integer age) {
        log.info("name: {}, age: {}", name, age);
        if (name == null || name.trim().isEmpty()) {
            throw new IllegalArgumentException("姓名为空");
        }
        if (age == null) {
            throw new IllegalArgumentException("年龄为空");
        }
        if (age < 0 || age > 200) {
            throw new RuntimeException("年龄不合法");
        }
        return "User parameters are valid";
    }

    @GetMapping("/user2")
    public String getUser(@Valid User user) {
        log.info("user: {}", JSONUtil.toJsonStr(user));
        return "User parameters are valid " + System.currentTimeMillis();
    }

    @PostMapping("/user3")
    public String createUser(@Valid @RequestBody User user) {
        return "User parameters are valid: age=" + user.getAge();
    }
}
