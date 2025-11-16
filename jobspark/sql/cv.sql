-- 简历主表
CREATE TABLE `cv` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '简历ID',
  `name` VARCHAR(100) NOT NULL COMMENT '姓名（必填）',
  `birth_date` DATE NULL COMMENT '出生日期（用于计算年龄，可选）',
  `title` VARCHAR(200) NULL COMMENT '期望岗位/头衔（可选）',
  `avatar_url` VARCHAR(500) NULL COMMENT '头像URL（可选）',
  `summary_markdown` TEXT NULL COMMENT '个人摘要（Markdown格式）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  `created_time` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  INDEX `idx_name` (`name`),
  INDEX `idx_is_deleted` (`is_deleted`) COMMENT '逻辑删除查询索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历基本信息表';

-- 联系方式表
CREATE TABLE `cv_contact` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `cv_id` BIGINT NOT NULL COMMENT '简历ID',
  `phone` VARCHAR(20) NULL COMMENT '手机号',
  `email` VARCHAR(100) NULL COMMENT '邮箱',
  `wechat` VARCHAR(100) NULL COMMENT '微信号（可选）',
  `location` VARCHAR(200) NULL COMMENT '所在地（可选）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`cv_id`) REFERENCES `cv`(`id`) ON DELETE CASCADE,
  INDEX `idx_cv_id` (`cv_id`) COMMENT '按简历ID查询联系方式',
  INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历联系方式表';

-- 社交链接表
CREATE TABLE `cv_social_link` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `cv_id` BIGINT NOT NULL COMMENT '简历ID',
  `label` VARCHAR(100) NOT NULL COMMENT '链接名称（如：GitHub/CSDN）',
  `url` VARCHAR(500) NOT NULL COMMENT '链接地址',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序（升序）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`cv_id`) REFERENCES `cv`(`id`) ON DELETE CASCADE,
  INDEX `idx_cv_id` (`cv_id`) COMMENT '按简历ID查询社交链接',
  INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历社交链接表';

-- 教育经历表
CREATE TABLE `cv_education` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `cv_id` BIGINT NOT NULL COMMENT '简历ID',
  `school` VARCHAR(200) NOT NULL COMMENT '学校名称',
  `major` VARCHAR(200) NOT NULL COMMENT '专业名称',
  `degree` VARCHAR(50) NULL COMMENT '学历（如：本科/硕士/博士）',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `end_date` DATE NULL COMMENT '结束日期（可为空表示在读）',
  `description_markdown` TEXT NULL COMMENT '描述（如：GPA/荣誉等，Markdown格式）',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序（升序）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`cv_id`) REFERENCES `cv`(`id`) ON DELETE CASCADE,
  INDEX `idx_cv_edu` (`cv_id`, `start_date` DESC) COMMENT '按简历ID+开始时间倒序查询（最新在前）',
  INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历教育经历表';

-- 工作/实习经历表
CREATE TABLE `cv_experience` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `cv_id` BIGINT NOT NULL COMMENT '简历ID',
  `type` VARCHAR(20) NOT NULL COMMENT '经历类型（全职/实习/兼职/ freelance）',
  `company` VARCHAR(200) NOT NULL COMMENT '公司名称',
  `industry` VARCHAR(100) NULL COMMENT '行业（如：互联网/金融）',
  `role` VARCHAR(200) NOT NULL COMMENT '职位名称',
  `start_date` DATE NOT NULL COMMENT '开始日期',
  `end_date` DATE NULL COMMENT '结束日期（可为空表示在职）',
  `description_markdown` TEXT NULL COMMENT '工作概述（Markdown格式）',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序（升序）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`cv_id`) REFERENCES `cv`(`id`) ON DELETE CASCADE,
  INDEX `idx_cv_exp` (`cv_id`, `start_date` DESC) COMMENT '按简历ID+开始时间倒序查询（最新在前）',
  INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历工作经历表';

-- 项目经验表
CREATE TABLE `cv_project` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `cv_id` BIGINT NOT NULL COMMENT '简历ID',
  `name` VARCHAR(200) NOT NULL COMMENT '项目名称',
  `start_date` DATE NULL COMMENT '项目开始日期',
  `end_date` DATE NULL COMMENT '项目结束日期',
  `role` VARCHAR(200) NULL COMMENT '角色/职责',
  `description_markdown` TEXT NULL COMMENT '项目描述（Markdown格式）',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序（升序）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`cv_id`) REFERENCES `cv`(`id`) ON DELETE CASCADE,
  INDEX `idx_cv_project` (`cv_id`, `start_date` DESC) COMMENT '按简历ID+开始时间倒序查询',
  INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历项目经验表';

-- 统一亮点表
CREATE TABLE `cv_highlight` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `type` TINYINT NOT NULL COMMENT '亮点类型：1-工作经历亮点 2-项目经历亮点',
  `related_id` BIGINT NOT NULL COMMENT '关联ID：根据type对应工作经历ID(cv_experience.id)或项目ID(cv_project.id)',
  `highlight_markdown` TEXT NOT NULL COMMENT '亮点内容（职责/业绩/贡献，Markdown格式）',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序（升序）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  -- 索引：按类型+关联ID查询（核心查询场景）
  INDEX `idx_type_related` (`type`, `related_id`) COMMENT '按类型+关联ID查询亮点（如：type=1+related_id=工作经历ID）',
  INDEX `idx_is_deleted` (`is_deleted`) COMMENT '逻辑删除过滤索引'
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='统一亮点表（工作经历/项目经历的亮点）';

-- 技能表
CREATE TABLE `cv_skill` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `cv_id` BIGINT NOT NULL COMMENT '简历ID',
  `category` VARCHAR(50) NULL COMMENT '技能分类（如：技术/语言/软技能）',
  `name` VARCHAR(100) NOT NULL COMMENT '技能名称',
  `level` VARCHAR(50) NULL COMMENT '熟练度（熟练/良好/了解/精通）',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序（升序）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`cv_id`) REFERENCES `cv`(`id`) ON DELETE CASCADE,
  INDEX `idx_cv_skill` (`cv_id`, `category`) COMMENT '按简历ID+分类查询技能',
  INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历技能表';

-- 证书/获奖表
CREATE TABLE `cv_certificate` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `cv_id` BIGINT NOT NULL COMMENT '简历ID',
  `name` VARCHAR(200) NOT NULL COMMENT '证书/奖项名称',
  `issuer` VARCHAR(200) NULL COMMENT '颁发机构',
  `date` DATE NULL COMMENT '获得日期',
  `description_markdown` TEXT NULL COMMENT '证书描述（如：等级/分数，Markdown格式）',
  `sort_order` INT DEFAULT 0 COMMENT '排序顺序（升序）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`cv_id`) REFERENCES `cv`(`id`) ON DELETE CASCADE,
  INDEX `idx_cv_cert` (`cv_id`, `date` DESC) COMMENT '按简历ID+获得时间倒序查询',
  INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历证书获奖表';

-- 格式元数据表
CREATE TABLE `cv_format_meta` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `cv_id` BIGINT NOT NULL COMMENT '简历ID',
  `theme` VARCHAR(50) DEFAULT 'default' COMMENT '简历主题（如：简约/商务）',
  `alignment` VARCHAR(20) DEFAULT 'left' COMMENT '对齐方式：left/center/right',
  `line_spacing` DOUBLE DEFAULT 1.4 COMMENT '行间距倍数',
  `font_family` VARCHAR(200) DEFAULT '"Noto Sans SC", "PingFang SC", "Microsoft YaHei", "SimSun", sans-serif' COMMENT '字体栈',
  `date_pattern` VARCHAR(20) DEFAULT 'yyyy.MM' COMMENT '日期格式',
  `hyperlink_style` VARCHAR(20) DEFAULT 'underline' COMMENT '超链接样式：underline/none',
  `show_avatar` BOOLEAN DEFAULT FALSE COMMENT '是否显示头像',
  `show_social` BOOLEAN DEFAULT TRUE COMMENT '是否显示社交链接',
  `two_column_layout` BOOLEAN DEFAULT FALSE COMMENT '是否双栏布局',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`cv_id`) REFERENCES `cv`(`id`) ON DELETE CASCADE,
  INDEX `idx_cv_id` (`cv_id`) COMMENT '按简历ID查询格式配置',
  INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历格式元数据表';

-- 国际化配置表
CREATE TABLE `cv_locale_config` (
  `id` BIGINT AUTO_INCREMENT PRIMARY KEY COMMENT '主键ID',
  `format_meta_id` BIGINT NOT NULL COMMENT '格式元数据ID',
  `locale` VARCHAR(10) DEFAULT 'zh-CN' COMMENT '语言标识（如：zh-CN, en-US, ja-JP）',
  `date_pattern` VARCHAR(20) DEFAULT 'yyyy.MM' COMMENT '本地化日期格式',
  `section_labels` JSON NULL COMMENT '区块名称本地化（如：{"education":"教育经历","experience":"工作经历"}）',
  `is_deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
  `created_time` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_time` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  FOREIGN KEY (`format_meta_id`) REFERENCES `cv_format_meta`(`id`) ON DELETE CASCADE,
  UNIQUE INDEX `uk_format_locale` (`format_meta_id`, `locale`) COMMENT '唯一约束：同一格式元数据下语言标识不重复',
  INDEX `idx_is_deleted` (`is_deleted`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历国际化配置表';