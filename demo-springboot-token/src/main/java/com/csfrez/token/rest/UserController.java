package com.csfrez.token.rest;

import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.util.SaResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author
 * @date 2025/6/20 11:51
 * @email
 */
@RestController
@RequestMapping("/user/")
public class UserController {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    // 测试登录，浏览器访问： http://localhost:8080/user/doLogin?username=zhang&password=123456
    @RequestMapping("doLogin")
    public String doLogin(String username, String password) {
        // 此处仅作模拟示例，真实项目需要从数据库中查询数据进行比对
        if ("zhang".equals(username) && "123456".equals(password)) {
            StpUtil.login(10001);
            return "登录成功";
        }
        return "登录失败";
    }

    // 查询登录状态，浏览器访问： http://localhost:8080/user/isLogin
    @RequestMapping("isLogin")
    public String isLogin() {
        return "当前会话是否登录：" + StpUtil.isLogin();
    }

    @RequestMapping("test")
    public SaResult test() {
        stringRedisTemplate.opsForValue().set("hello", "Hello World");
        return SaResult.ok(stringRedisTemplate.opsForValue().get("hello"));
    }

}
