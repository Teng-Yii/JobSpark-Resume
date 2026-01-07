package com.tengYii.jobspark.common.interceptor;

import com.tengYii.jobspark.common.utils.login.JwtTokenUtil;
import com.tengYii.jobspark.common.utils.login.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Objects;

/**
 * 用户认证拦截器
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    /**
     * Bearer Token 前缀
     */
    private static final String BEARER_PREFIX = "Bearer ";

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * 请求预处理
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @return true表示继续处理
     * @throws Exception 异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. 获取token信息
        String authHeader = request.getHeader("Authorization");

        // 2. 校验token合法性
        if (StringUtils.isEmpty(authHeader) || !StringUtils.startsWith(authHeader, BEARER_PREFIX)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        try {
            // 3. 获取token，去掉前缀
            String actualToken = StringUtils.substringAfter(authHeader, BEARER_PREFIX);

            // 4. 判空校验（防止某些极端情况截取为空串）
            if (StringUtils.isEmpty(actualToken)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            Boolean isValid = jwtTokenUtil.validateToken(actualToken);
            if (Objects.isNull(isValid) || !isValid) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            // 解析token获取用户ID
            Long userId = jwtTokenUtil.getUserIdFromToken(actualToken);
            if (Objects.isNull(userId)) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                return false;
            }

            UserContext.setCurrentUserId(userId);
            return true;

        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }
    }

    /**
     * 请求完成后的清理工作
     * 清除ThreadLocal中的用户信息，防止内存泄漏
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @param ex       处理过程中的异常（如果有）
     * @throws Exception 清理过程中的异常
     */
    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}