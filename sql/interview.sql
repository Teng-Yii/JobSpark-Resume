-- 1. 面试会话表 (主表)
DROP TABLE IF EXISTS interview_session;
CREATE TABLE interview_session (
    id BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '会话ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    cv_id BIGINT NOT NULL COMMENT '简历ID',
    target_company VARCHAR(200) NULL COMMENT '目标公司',
    target_position VARCHAR(200) DEFAULT 'Java后端开发' COMMENT '目标岗位',
    difficulty VARCHAR(20) DEFAULT 'MEDIUM' COMMENT '难度: EASY/MEDIUM/HARD',
    status VARCHAR(20) DEFAULT 'PLANNING' COMMENT '状态: PLANNING/IN_PROGRESS/PAUSED/COMPLETED',
    current_stage INT DEFAULT 0 COMMENT '当前处于第几个阶段',
    start_time DATETIME NULL COMMENT '开始时间',
    end_time DATETIME NULL COMMENT '结束时间',
    delete_flag TINYINT(1) DEFAULT 0,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_user_cv (user_id, cv_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试会话主表';

-- 2. 面试计划表 (存储 Plan)
DROP TABLE IF EXISTS interview_plan;
CREATE TABLE interview_plan (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL COMMENT '会话ID',
    stage_order INT NOT NULL COMMENT '阶段顺序 (1, 2, 3...)',
    stage_name VARCHAR(100) NOT NULL COMMENT '阶段名称 (如: Java基础)',
    focus_area TEXT NULL COMMENT '重点考察领域 (JSON数组)',
    duration_minutes INT DEFAULT 10 COMMENT '预计时长',
    status VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/ACTIVE/COMPLETED/SKIPPED',
    delete_flag TINYINT(1) DEFAULT 0,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_order (session_id, stage_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试计划表 (Planner输出)';

-- 3. 面试问答记录表 (存储 Execute 过程)
DROP TABLE IF EXISTS interview_qa;
CREATE TABLE interview_qa (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    plan_id BIGINT NOT NULL COMMENT '属于哪个计划阶段',
    question_order INT NOT NULL COMMENT '问题序号',
    question TEXT NOT NULL COMMENT '面试官问题',
    answer TEXT NULL COMMENT '候选人回答',
    is_follow_up TINYINT(1) DEFAULT 0 COMMENT '是否为追问',
    parent_qa_id BIGINT NULL COMMENT '追问的父问题ID',
    delete_flag TINYINT(1) DEFAULT 0,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_time DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session_qa (session_id, question_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试问答流水表';

-- 4. 面试评估表 (存储 Reflection 结果)
DROP TABLE IF EXISTS interview_evaluation;
CREATE TABLE interview_evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    qa_id BIGINT NOT NULL COMMENT '关联的具体问题',
    score INT NULL COMMENT '单项得分 (0-10)',
    evaluation_dimension VARCHAR(50) NULL COMMENT '评估维度 (准确性/深度/逻辑性)',
    feedback TEXT NULL COMMENT '具体评语/建议',
    weakness_tag VARCHAR(200) NULL COMMENT '发现的薄弱点标签',
    delete_flag TINYINT(1) DEFAULT 0,
    created_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_score (session_id, score)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='面试评估细节表';