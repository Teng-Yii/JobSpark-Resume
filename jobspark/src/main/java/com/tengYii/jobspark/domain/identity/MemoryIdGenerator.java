package com.tengYii.jobspark.domain.identity;

/**
 * MemoryId生成器接口
 */
public interface MemoryIdGenerator {

    /**
     * 生成符合领域规则的会话记忆ID
     *
     * @param userIdentifier 用户标识（登录用户=userId，匿名用户=deviceId，无需前缀，由实现类统一处理）
     * @param sessionId      会话唯一标识（基础设施层生成的唯一ID，如雪花ID、UUID）
     * @return 完整的、合法的 MemoryId 值对象（无需调用方再校验）
     */
    MemoryId generate(String userIdentifier, String sessionId);

    /**
     * 多租户扩展方法（同样返回 MemoryId，保持接口一致性）
     *
     * @param userIdentifier 用户唯一标识
     * @param sessionId      会话Id
     * @param tenantId       租户Id
     * @return 生成的MemoryId
     */
    default MemoryId generateWithTenant(String userIdentifier, String sessionId, String tenantId) {
        // 先调用核心生成方法得到基础 MemoryId，再拼接租户标识（复用已有逻辑）
        MemoryId baseId = generate(userIdentifier, sessionId);
        String tenantIdValue = String.format("%s:tenant:%s", baseId.value(), tenantId);

        // 依赖 MemoryId 构造函数校验新格式
        return new MemoryId(tenantIdValue);
    }
}