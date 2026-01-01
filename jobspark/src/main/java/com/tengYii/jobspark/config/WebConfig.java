package com.tengYii.jobspark.config;

import com.tengYii.jobspark.common.interceptor.AuthInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web MVC配置类
 * 用于注册拦截器、配置跨域等
 *
 * @author tengYii
 * @since 1.0.0
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private AuthInterceptor authInterceptor;

    /**
     * 注册拦截器
     *
     * @param registry 拦截器注册表
     */
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 注册用户认证拦截器
        registry.addInterceptor(authInterceptor)
                // 拦截所有请求
                .addPathPatterns("/**")
                // 排除不需要认证的接口
                .excludePathPatterns(
                        // 登录接口
                        "/api/v1/auth/login",
                        // 注册接口
                        "/api/v1/auth/register",
                        // swagger接口文档接口及静态资源
                        "/swagger/**",
                        "/swagger-ui/**",
                        "/v3/api-docs/**",
                        "/webjars/**"
                );
    }
}