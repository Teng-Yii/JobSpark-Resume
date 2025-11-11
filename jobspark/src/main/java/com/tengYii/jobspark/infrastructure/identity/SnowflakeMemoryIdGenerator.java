package com.tengYii.jobspark.infrastructure.identity;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.tengYii.jobspark.domain.identity.MemoryId;
import com.tengYii.jobspark.domain.identity.MemoryIdGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

/**
 * 雪花算法实现生成memoryId
 */
@Component
public class SnowflakeMemoryIdGenerator implements MemoryIdGenerator {

    /**
     * 生成符合领域规则的会话记忆ID
     *
     * @param userIdentifier 用户标识（登录用户=userId，匿名用户=deviceId，无需前缀，由实现类统一处理）
     * @param sessionId      会话唯一标识，若不存在通过雪花算法生成
     * @return 完整的、合法的 MemoryId 值对象（无需调用方再校验）
     */
    @Override
    public MemoryId generate(String userIdentifier, String sessionId) {
        if(StringUtils.isBlank(sessionId)){
            sessionId = String.valueOf(IdWorker.getId());
        }
        // 1. 按领域规则拼接格式（userIdentifier 无需前缀，由实现类统一添加）
        String userType = userIdentifier.startsWith("user_") ? "user" : "anon"; // 假设 userId 前缀为 user_，deviceId 无前缀（可按实际业务调整）
        String idValue = String.format("conv:%s:%s:session:%s", userType, userIdentifier, sessionId);

        // 2. 直接创建 MemoryId，触发领域规则校验（不合法会抛出异常）
        return new MemoryId(idValue);
    }
}
