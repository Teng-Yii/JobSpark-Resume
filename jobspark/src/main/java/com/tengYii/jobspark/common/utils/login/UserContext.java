package com.tengYii.jobspark.common.utils.login;

import java.util.Objects;

/**
 * 用户上下文工具类
 *
 * 使用ThreadLocal存储当前线程的用户信息，确保线程安全
 * 主要用于在请求处理过程中传递用户上下文信息
 *
 * @author tengYii
 * @since 1.0.0
 */
public class UserContext {

    /**
     * 存储用户ID的ThreadLocal变量
     */
    private static final ThreadLocal<Long> USER_ID_HOLDER = new ThreadLocal<>();

    /**
     * 存储用户名的ThreadLocal变量
     */
    private static final ThreadLocal<String> USERNAME_HOLDER = new ThreadLocal<>();

    /**
     * 私有构造函数，防止实例化
     */
    private UserContext() {
        throw new UnsupportedOperationException("Utility class cannot be instantiated");
    }

    /**
     * 设置当前用户ID
     *
     * @param userId 用户ID
     */
    public static void setCurrentUserId(Long userId) {
        USER_ID_HOLDER.set(userId);
    }

    /**
     * 获取当前用户ID
     *
     * @return 当前用户ID，如果未设置则返回null
     */
    public static Long getCurrentUserId() {
        return USER_ID_HOLDER.get();
    }

    /**
     * 设置当前用户名
     *
     * @param username 用户名
     */
    public static void setCurrentUsername(String username) {
        USERNAME_HOLDER.set(username);
    }

    /**
     * 获取当前用户名
     *
     * @return 当前用户名，如果未设置则返回null
     */
    public static String getCurrentUsername() {
        return USERNAME_HOLDER.get();
    }

    /**
     * 设置当前用户信息
     *
     * @param userId   用户ID
     * @param username 用户名
     */
    public static void setCurrentUser(Long userId, String username) {
        setCurrentUserId(userId);
        setCurrentUsername(username);
    }

    /**
     * 检查当前是否有用户登录
     *
     * @return true表示有用户登录，false表示无用户登录
     */
    public static boolean hasCurrentUser() {
        return Objects.nonNull(getCurrentUserId());
    }

    /**
     * 获取当前用户ID，如果未登录则抛出异常
     *
     * @return 当前用户ID
     * @throws IllegalStateException 如果当前无用户登录
     */
    public static Long requireCurrentUserId() {
        Long userId = getCurrentUserId();
        if (Objects.isNull(userId)) {
            throw new IllegalStateException("当前无用户登录");
        }
        return userId;
    }

    /**
     * 获取当前用户名，如果未登录则抛出异常
     *
     * @return 当前用户名
     * @throws IllegalStateException 如果当前无用户登录
     */
    public static String requireCurrentUsername() {
        String username = getCurrentUsername();
        if (Objects.isNull(username)) {
            throw new IllegalStateException("当前无用户登录");
        }
        return username;
    }

    /**
     * 清除当前用户信息
     *
     * 注意：这个方法必须在请求结束时调用，防止ThreadLocal内存泄漏
     */
    public static void clear() {
        USER_ID_HOLDER.remove();
        USERNAME_HOLDER.remove();
    }
}
