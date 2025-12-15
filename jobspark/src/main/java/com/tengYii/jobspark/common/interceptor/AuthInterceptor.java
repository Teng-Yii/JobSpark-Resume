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

    @Autowired
    private JwtTokenUtil jwtTokenUtil;

    /**
     * 请求预处理，验证JWT token并设置用户上下文
     *
     * @param request  HTTP请求
     * @param response HTTP响应
     * @param handler  处理器
     * @return true表示继续处理请求，false表示拦截请求
     * @throws Exception 处理异常
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 获取Authorization头
        String token = request.getHeader("Authorization");

        // 检查token格式
        if (StringUtils.isEmpty(token) || !StringUtils.startsWith(token, "Bearer ")) {
            response.setStatus(401);
            return false;
        }

        try {
            // 提取实际的token（去掉"Bearer "前缀）
            String actualToken = token.substring(7);

            // 先验证token是否有效
            Boolean isValid = jwtTokenUtil.validateToken(actualToken);
            if (Objects.isNull(isValid) || !isValid) {
                response.setStatus(401);
                return false;
            }

            // token有效后再获取用户ID
            Long userId = jwtTokenUtil.getUserIdFromToken(actualToken);
            if (Objects.isNull(userId)) {
                response.setStatus(401);
                return false;
            }

            // 将userId存储到ThreadLocal
            UserContext.setCurrentUserId(userId);
            return true;

        } catch (Exception e) {
            // token解析或验证过程中出现任何异常都认为是无效token
            response.setStatus(401);
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