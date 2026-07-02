# PaperPilot AI Backend Skeleton

这个目录提供了一个对齐 `技术架构设计.md` 的 Spring Boot 骨架，用来承接前端 MVP 后续的真实数据流。

## 当前包含

- `pom.xml`：Spring Boot 3.4 风格依赖
- `src/main/java/...`：模型配置、论文导入两个最小接口
- `src/main/resources/application.yml`：本地开发配置模板

## 第一阶段建议接入顺序

1. 先把 `POST /api/model-config` 和 `POST /api/papers/import` 跑通
2. 再接 `AI Gateway`，统一转发到 OpenAI Compatible 端点
3. 然后补 MySQL 持久化、Redis 缓存、MinIO 文件管理

## 说明

当前服务用内存存储演示数据，目的是先把接口结构和 DTO/VO 分层搭起来，避免前端原型无法继续演进。
