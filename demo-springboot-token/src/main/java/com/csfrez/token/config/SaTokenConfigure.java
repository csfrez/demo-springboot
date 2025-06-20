package com.csfrez.token.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * @author
 * @date 2025/6/20 15:05
 * @email
 */
@Configuration
public class SaTokenConfigure implements WebMvcConfigurer {
    // 注册 Sa-Token 拦截器，打开注解式鉴权功能
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册 Sa-Token 拦截器，打开注解式鉴权功能
//        registry.addInterceptor(new SaInterceptor()).addPathPatterns("/**");
        // 注册 Sa-Token 拦截器，校验规则为 StpUtil.checkLogin() 登录校验。
//        registry.addInterceptor(new SaInterceptor(handle -> StpUtil.checkLogin()))
//                .addPathPatterns("/**")
//                .excludePathPatterns("/user/doLogin");

        // 注册 Sa-Token 拦截器，定义详细认证规则
        registry.addInterceptor(new SaInterceptor(handler -> {
            // 指定一条 match 规则
            SaRouter
                    .match("/**")    // 拦截的 path 列表，可以写多个
                    .notMatch("/user/doLogin")        // 排除掉的 path 列表，可以写多个
                    .check(r -> StpUtil.checkLogin());        // 要执行的校验动作，可以写完整的 lambda 表达式

            // 根据路由划分模块，不同模块不同鉴权
//            SaRouter.match("/user/**", r -> StpUtil.checkPermission("user"));
            SaRouter.match("/admin/**", r -> StpUtil.checkPermission("admin"));
            SaRouter.match("/goods/**", r -> StpUtil.checkPermission("goods"));
            SaRouter.match("/orders/**", r -> StpUtil.checkPermission("orders"));
            SaRouter.match("/notice/**", r -> StpUtil.checkPermission("notice"));
            SaRouter.match("/comment/**", r -> StpUtil.checkPermission("comment"));
        })).addPathPatterns("/**");
    }
}
