package com.tengYii.jobspark.common.utils.login;

/**
 * 用户上下文工具类
 */
public class UserContext {

    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 设置当前用户ID
     */
    public static void setCurrentUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前用户ID
     */
    public static Long getCurrentUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 清除当前用户信息
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
    }
}
