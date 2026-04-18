# 元启 AI Agents 平台

![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0.0-blue)
![Vue](https://img.shields.io/badge/Vue-3-42b883)
![License](https://img.shields.io/badge/License-MIT-yellow)

> **元，始也。启，开也。**

**元启**是一套全能型多模态 AI Agents 聚合平台。支持全景多模态大模型、RAG架构智能问答、可视化应用生成、丰富 MCP 工具调用、图/视频生成及全方位的 API 计费统计。

本脚本主要用户在API中转、QQ自动回复功能上 QQ交流群:883640898

本地使用master分支 服务器使用servers分支

---

## ✨ 功能特性

### 🤖 智能对话
- 流式输出，打字机效果
- 支持多轮对话与上下文记忆
- 支持图片对话（多模态）
- 会话管理与历史记录

### 📚 知识库 RAG
- 上传文档（PDF、Word、TXT、Markdown 等）
- 自动向量化存储
- 语义检索与智能问答
- 支持引用来源

### 🎭 智能体
- 自定义智能体配置
- 系统提示词设置
- 打造专属 AI 助手

### 🔌 API 中转
- 生成 API Key 调用 AI 能力
- 支持 QQ 机器人等第三方接入
- 用量统计与管理

### 🛠️ MCP 工具
- 工具管理与扩展
- 动态启用/禁用
- 增强 AI 能力
- **联网搜索**：实时新闻、热点事件检索
- **天气查询**：实时天气信息
- **一言工具**：随机励志语录

### 🎨 内容生成
- AI 生图（CogView）
- AI 生视频（CogVideoX）

---

## 🚀 技术栈

### 后端
| 技术 | 说明 |
|------|------|
| Spring Boot 3.5 | 核心框架 |
| Spring AI 1.0.0 | AI 开发框架 |
| MySQL 8.0 | 数据库 |
| MyBatis-Plus 3.5.15 | ORM 框架 |
| Caffeine | 本地缓存 |
| Sa-Token 1.45.0 | 认证授权 |
| 智谱 AI | AI 服务提供商 |

### 前端
| 技术 | 说明 |
|------|------|
| Vue 3 | 前端框架 |
| Element Plus | UI 组件库 |
| Pinia | 状态管理 |
| Vite | 构建工具 |

---

## 📦 项目结构

```
YuanQi/
├── YuanQi/                    # 后端服务
│   ├── src/main/java/
│   │   ├── controller/        # 控制器
│   │   ├── service/           # 服务层
│   │   ├── mapper/            # 数据访问
│   │   └── pojo/              # 实体类
│   └── sql/init.sql           # 数据库脚本
│
└── YuanQi-View/               # 前端应用
    ├── src/
    │   ├── api/               # 接口封装
    │   ├── views/             # 页面组件
    │   └── stores/            # 状态管理
    └── vite.config.js
```

---

## 🎯 核心亮点

- **流式对话** - 基于 SSE 实现实时流式输出，体验丝滑
- **RAG 知识库** - 完整的文档解析、向量化、检索问答流程
- **API 中转** - 为第三方应用提供标准化 AI 接口
- **安全设计** - 敏感数据加密存储，JWT 认证
- **精美界面** - 粒子动画、艺术字动效、响应式布局

---

## 📄 开源协议

本项目基于 [MIT](LICENSE) 协议开源，完全免费使用。

---

> 练手即问道，小成亦星辰。
