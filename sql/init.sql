-- 元启AI Agents平台 数据库初始化脚本

-- 创建数据库
CREATE DATABASE IF NOT EXISTS yuanqi CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE yuanqi;

-- 用户表
CREATE TABLE IF NOT EXISTS sys_user (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    username VARCHAR(50) NOT NULL COMMENT '用户名',
    email VARCHAR(100) NOT NULL COMMENT '邮箱',
    password VARCHAR(100) NOT NULL COMMENT '加密密码',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '头像URL',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色：admin-管理员 user-普通用户',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '状态：0-禁用 1-正常',
    api_key VARCHAR(255)NOT NULL COMMENT '智谱AI API Key（加密存储）',
    chat_model VARCHAR(50) DEFAULT 'glm-4-flash-250414' COMMENT '文字聊天模型',
    chat_vision_model VARCHAR(50) DEFAULT 'glm-4v-flash' COMMENT '图文聊天模型',
    image_model VARCHAR(50) DEFAULT 'cogview-3-flash' COMMENT '生图模型',
    video_model VARCHAR(50) DEFAULT 'cogvideox-flash' COMMENT '生视频模型',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除 1-已删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_email (email),
    INDEX idx_username (username),
    INDEX idx_role (role),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 会话表
CREATE TABLE IF NOT EXISTS chat_session (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话唯一标识UUID',
    title VARCHAR(200) DEFAULT NULL COMMENT '会话标题（首条消息自动生成）',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_session_id (session_id),
    INDEX idx_user_id (user_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='会话表';

-- 对话消息表
CREATE TABLE IF NOT EXISTS chat_message (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    session_id VARCHAR(64) NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色：user-用户 assistant-助手 system-系统',
    content TEXT NOT NULL COMMENT '消息内容',
    model_used VARCHAR(50) DEFAULT NULL COMMENT '实际使用的模型（根据chat_type自动选择）',
    tools_used JSON DEFAULT NULL COMMENT '使用的工具列表（JSON数组，空数组表示未使用）',
    tool_results JSON DEFAULT NULL COMMENT '工具返回结果（JSON格式，包含每个工具的返回）',
    input_tokens INT DEFAULT 0 COMMENT '输入Token数',
    output_tokens INT DEFAULT 0 COMMENT '输出Token数',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (id),
    INDEX idx_session_id (session_id),
    INDEX idx_role (role),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息表';

-- 知识库表
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    name VARCHAR(100) NOT NULL COMMENT '知识库名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '知识库描述',
    collection_name VARCHAR(100) NOT NULL COMMENT 'Chroma集合名称',
    doc_count INT DEFAULT 0 COMMENT '文档数量',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_collection_name (collection_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- 智能体表
CREATE TABLE IF NOT EXISTS agent (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT DEFAULT NULL COMMENT '用户ID（NULL表示系统预设）',
    name VARCHAR(100) NOT NULL COMMENT '智能体名称',
    description VARCHAR(500) DEFAULT NULL COMMENT '智能体描述',
    avatar VARCHAR(255) DEFAULT NULL COMMENT '智能体头像',
    system_prompt TEXT NOT NULL COMMENT '系统提示词',
    welcome_message VARCHAR(500) DEFAULT NULL COMMENT '欢迎语',
    model_type VARCHAR(20) DEFAULT 'chat' COMMENT '模型类型',
    is_public TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开：0-私有 1-公开',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_is_public (is_public)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='智能体表';

-- 用量统计表
CREATE TABLE IF NOT EXISTS usage_stats (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    stats_date DATE NOT NULL COMMENT '统计日期',
    model_type VARCHAR(20) NOT NULL COMMENT '模型类型',
    call_count INT DEFAULT 0 COMMENT '调用次数',
    input_tokens INT DEFAULT 0 COMMENT '输入Token数',
    output_tokens INT DEFAULT 0 COMMENT '输出Token数',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_user_date_type (user_id, stats_date, model_type),
    INDEX idx_user_id (user_id),
    INDEX idx_stats_date (stats_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用量统计表';

-- AI生成内容记录表（图片/视频）
CREATE TABLE IF NOT EXISTS ai_generated_content (
    id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    type VARCHAR(20) NOT NULL COMMENT '类型：image-图片 video-视频',
    prompt VARCHAR(1000) NOT NULL COMMENT '提示词',
    result_url VARCHAR(500) DEFAULT NULL COMMENT '生成结果URL（图片/视频）',
    cover_url VARCHAR(500) DEFAULT NULL COMMENT '封面图URL（视频专用）',
    task_id VARCHAR(100) DEFAULT NULL COMMENT '异步任务ID（视频专用）',
    status TINYINT NOT NULL DEFAULT 0 COMMENT '状态：0-处理中 1-成功 2-失败',
    error_msg VARCHAR(500) DEFAULT NULL COMMENT '错误信息（失败时）',
    model_used VARCHAR(50) DEFAULT NULL COMMENT '使用的模型',
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '逻辑删除',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    INDEX idx_user_id (user_id),
    INDEX idx_type (type),
    INDEX idx_status (status),
    INDEX idx_task_id (task_id),
    INDEX idx_create_time (create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='AI生成内容记录表';
