package com.tengYii.jobspark.domain.identity;

import com.tengYii.jobspark.common.utils.memoryid.MemoryIdGenerator;

import java.util.Optional;

/**
 * MemoryId 值对象（保持校验逻辑，简化静态方法）
 *
 * @param value 不可变对象，用户会话记忆的唯一标识
 */
public record MemoryId(String value) {

    /**
     * 领域规则校验：确保ID格式符合 conv:(user|anon):xxx:session:xxx[:tenant:xxx]
     *
     * @param value 不可变值
     */
    public MemoryId {
        if (value == null) {
            throw new IllegalArgumentException("memoryId不能为空");
        }
        // 正则优化：支持可选的租户后缀，示例：
        // conv:anon:device456:session:xyz789
        // conv:user:12345:session:abc123:tenant:company1
        String regex = "^conv:(user|anon):[^:]+:session:[^:]+(:tenant:[^:]+)?$";
        if (!value.matches(regex)) {
            throw new IllegalArgumentException("无效的memoryId格式：" + value + "，需符合 conv:(user|anon):用户标识:session:会话标识[:tenant:租户标识]");
        }
    }

    /**
     * 静态方法可保留（作为便捷创建入口，内部仍依赖生成器）
     *
     * @param generator      memoryId生成器
     * @param userIdentifier 用户Id唯一标识
     * @param sessionId      会话Id
     * @return 合法的 MemoryId 值对象
     */
    public static MemoryId of(MemoryIdGenerator generator, String userIdentifier, String sessionId) {
        // 生成器直接返回合法的 MemoryId
        return generator.generate(userIdentifier, sessionId);
    }

    /**
     * 新增便捷方法（领域层内可直接解析属性，无需外部处理字符串）
     *
     * @return 用户类型（user/anon）
     */
    public String getUserType() {
        return value.split(":")[1]; // 提取 user/anon
    }

    /**
     * 新增便捷方法（领域层内可直接解析属性，无需外部处理字符串）
     *
     * @return userId/deviceId
     */
    public String getUserIdentifier() {
        return value.split(":")[2];
    }

    /**
     * 提取 sessionId
     *
     * @return sessionId
     */
    public String getSessionId() {
        return value.split(":")[4];
    }

    /**
     * 提取租户标识（可选）
     *
     * @return tenant
     */
    public Optional<String> getTenantId() {
        String[] parts = value.split(":");
        return parts.length >= 7 ? Optional.of(parts[6]) : Optional.empty();
    }
}
