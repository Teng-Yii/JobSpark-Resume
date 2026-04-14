-- Agent可观测性系统数据库表设计

-- 1. Agent执行轨迹表（主表）
DROP TABLE IF EXISTS agent_execution_trace;
CREATE TABLE agent_execution_trace (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    trace_id VARCHAR(100) NOT NULL COMMENT '唯一追踪ID，格式: sessionId_memoryId_agentId_timestamp',
    session_id VARCHAR(64) NULL COMMENT '面试会话ID',
    memory_id VARCHAR(100) NULL COMMENT 'Memory ID，格式: userId_resumeId',
    
    -- Agent信息
    agent_name VARCHAR(100) NOT NULL COMMENT 'Agent名称',
    agent_id VARCHAR(100) NOT NULL COMMENT 'Agent实例ID',
    parent_agent_id VARCHAR(100) NULL COMMENT '父Agent ID（用于嵌套调用）',
    
    -- 执行状态
    status VARCHAR(20) NOT NULL DEFAULT 'RUNNING' COMMENT '执行状态: RUNNING/SUCCESS/FAILED',
    start_time DATETIME NOT NULL COMMENT '开始时间',
    end_time DATETIME NULL COMMENT '结束时间',
    duration_ms BIGINT NULL COMMENT '执行耗时(毫秒)',
    
    -- 输入输出摘要（限制长度避免表过大）
    input_summary TEXT NULL COMMENT '输入参数摘要(JSON格式)',
    output_summary TEXT NULL COMMENT '输出结果摘要',
    
    -- 错误信息
    error_message TEXT NULL COMMENT '错误信息',
    error_stack_trace TEXT NULL COMMENT '错误堆栈',
    
    -- 审计字段
    delete_flag TINYINT(1) DEFAULT 0 COMMENT '删除标记',
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    
    UNIQUE INDEX uk_trace_id (trace_id),
    INDEX idx_session_id (session_id),
    INDEX idx_memory_id (memory_id),
    INDEX idx_agent_name (agent_name),
    INDEX idx_status (status),
    INDEX idx_start_time (start_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent执行轨迹表';

-- 2. Agent工具调用记录表
DROP TABLE IF EXISTS agent_tool_invocation;
CREATE TABLE agent_tool_invocation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
    trace_id VARCHAR(100) NOT NULL COMMENT '关联的Agent执行轨迹ID',
    invocation_order INT NOT NULL DEFAULT 0 COMMENT '调用顺序',
    
    -- 工具信息
    tool_name VARCHAR(100) NOT NULL COMMENT '工具名称',
    tool_input TEXT NULL COMMENT '工具输入参数(JSON格式)',
    tool_output TEXT NULL COMMENT '工具输出结果',
    
    -- 执行状态
    success TINYINT(1) DEFAULT 1 COMMENT '是否执行成功',
    execution_time_ms BIGINT NULL COMMENT '执行耗时(毫秒)',
    
    -- 审计字段
    delete_flag TINYINT(1) DEFAULT 0,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_trace_id (trace_id),
    INDEX idx_tool_name (tool_name),
    FOREIGN KEY (trace_id) REFERENCES agent_execution_trace(trace_id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent工具调用记录表';

-- 3. Agent会话聚合统计表（可选，用于快速查询统计）
DROP TABLE IF EXISTS agent_session_stats;
CREATE TABLE agent_session_stats (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    memory_id VARCHAR(100) NOT NULL COMMENT 'Memory ID',
    
    -- 统计指标
    total_agent_calls INT DEFAULT 0 COMMENT 'Agent调用总次数',
    total_tool_calls INT DEFAULT 0 COMMENT '工具调用总次数',
    total_duration_ms BIGINT DEFAULT 0 COMMENT '总耗时(毫秒)',
    success_count INT DEFAULT 0 COMMENT '成功次数',
    failed_count INT DEFAULT 0 COMMENT '失败次数',
    
    -- 时间信息
    first_call_time DATETIME NULL COMMENT '首次调用时间',
    last_call_time DATETIME NULL COMMENT '最后调用时间',
    
    delete_flag TINYINT(1) DEFAULT 0,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    UNIQUE INDEX uk_session_id (session_id),
    INDEX idx_memory_id (memory_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent会话聚合统计表';
