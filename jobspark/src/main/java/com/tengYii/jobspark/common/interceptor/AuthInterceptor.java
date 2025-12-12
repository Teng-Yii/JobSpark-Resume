package com.tengYii.jobspark.common.interceptor;

import com.tengYii.jobspark.common.utils.login.JwtTokenUtil;
import com.tengYii.jobspark.common.utils.login.UserContext;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * 用户认证拦截器
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String token = request.getHeader("Authorization");

        if (StringUtils.isEmpty(token) || !token.startsWith("Bearer ")) {
            response.setStatus(401);
            return false;
        }

        try {
            String actualToken = token.substring(7);
            Long userId = jwtTokenUtil.getUserIdFromToken(actualToken);

            // 将userId存储到ThreadLocal
            UserContext.setCurrentUserId(userId);
            return true;

        } catch (Exception e) {
            response.setStatus(401);
            return false;
        }
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 清除ThreadLocal，防止内存泄漏
        UserContext.clear();
    }
}