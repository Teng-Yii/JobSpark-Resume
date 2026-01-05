package com.tengYii.jobspark.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Spring Security 配置类
 * 用于配置安全拦截规则，解决引入 spring-boot-starter-security 导致的 Swagger 等接口访问 401 问题。
 *
 * @author TengYii
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * 配置 SecurityFilterChain 过滤器链
     *
     * @param http HttpSecurity 对象
     * @return SecurityFilterChain
     * @throws Exception 异常
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                // 禁用 CSRF，因为通常 API 服务不需要
                .csrf(AbstractHttpConfigurer::disable)
                // 禁用默认的表单登录，以免弹出浏览器原生登录框
                .formLogin(AbstractHttpConfigurer::disable)
                // 禁用 HTTP Basic，以免弹出浏览器原生登录框
                .httpBasic(AbstractHttpConfigurer::disable)
                // 权限控制配置
                .authorizeHttpRequests(auth -> auth
                        // Swagger 相关路径放行
                        .requestMatchers("/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs/**", "/webjars/**", "/doc.html").permitAll()
                        // Auth 接口放行
                        .requestMatchers("/api/v1/auth/login", "/api/v1/auth/register",
                                "/api/v1/auth/sendForgetPasswordCode", "/api/v1/auth/forgetPassword").permitAll()
                        // 其他所有请求也放行，交由业务层的 AuthInterceptor 处理
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}