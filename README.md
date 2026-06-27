# 歧路·互动小说平台

完整的互动小说网站项目，技术栈 Spring Boot + Vue 3 + MySQL。

## 项目介绍

「歧路·互动小说」是一个创新的在线互动小说平台：
1. 用户输入故事开头和标题
2. AI 自动生成多层分支剧情（最多 7 层，约 1 万字）
3. 读者通过选择选项推动故事发展
4. 每个分支最终走向不同的结局
5. 管理个人故事：查看列表、删除故事

## 项目结构

```
qilu-story/
├── qilu_story_init.sql          # 数据库初始化脚本
├── README.md
├── qilu-story-backend/          # Spring Boot 后端
│   ├── pom.xml
│   ├── src/
│   │   └── main/
│   │       ├── java/com/qilu/story/
│   │       │   ├── QiluStoryApplication.java
│   │       │   ├── controller/
│   │       │   │   ├── AuthController.java
│   │       │   │   └── StoryController.java
│   │       │   ├── service/
│   │       │   │   ├── UserService.java
│   │       │   │   ├── StoryService.java
│   │       │   │   └── DeepseekService.java
│   │       │   ├── mapper/
│   │       │   │   ├── UserMapper.java
│   │       │   │   ├── StoryMapper.java
│   │       │   │   └── StoryNodeMapper.java
│   │       │   ├── entity/
│   │       │   │   ├── User.java
│   │       │   │   ├── Story.java
│   │       │   │   └── StoryNode.java
│   │       │   ├── dto/
│   │       │   │   ├── RegisterRequest.java
│   │       │   │   ├── LoginRequest.java
│   │       │   │   ├── LoginResponse.java
│   │       │   │   ├── CreateStoryRequest.java
│   │       │   │   └── DeepseekResponse.java
│   │       │   ├── config/
│   │       │   │   ├── WebConfig.java
│   │       │   │   └── FilterConfig.java
│   │       │   ├── filter/
│   │       │   │   └── JwtAuthenticationFilter.java
│   │       │   ├── exception/
│   │       │   │   ├── BusinessException.java
│   │       │   │   └── GlobalExceptionHandler.java
│   │       │   └── utils/
│   │       │       ├── ApiResponse.java
│   │       │       └── JwtUtil.java
│   │       └── resources/
│   │           ├── application.yml
│   │           └── application.yml.example
│   └── .gitignore
└── qilu-story-frontend/         # Vue 3 前端
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── api/
        │   └── index.js
        ├── assets/
        │   └── styles.css
        ├── components/
        │   └── BookCover.vue
        ├── composables/
        │   └── useBook.js
        ├── router/
        │   └── index.js
        ├── store/
        │   └── user.js
        └── views/
            ├── BookView.vue
            ├── HomeView.vue
            ├── LoginView.vue
            ├── RegisterView.vue
            └── StoryView.vue
```

## 快速开始

### 1. 初始化数据库
```bash
mysql -u root -p < qilu_story_init.sql
```

脚本将创建 `qilu_story` 数据库（如不存在），并建立 `user`、`story`、`story_node` 三张表及完整索引和外键约束。

### 2. 配置后端
进入 `qilu-story-backend` 目录，修改 `src/main/resources/application.yml` 中的数据库和 API 配置（**建议使用环境变量，避免硬编码密钥**）。

### 3. 运行后端
```bash
cd qilu-story-backend
mvn spring-boot:run
```

后端将在 http://localhost:8081 运行。

### 4. 运行前端
```bash
cd qilu-story-frontend
npm install
npm run dev
```

前端将在 http://localhost:5173 运行，并代理 API 请求到后端。

## 技术栈详情

### 后端
| 组件 | 版本 | 说明 |
|-----|-----|------|
| Spring Boot | 2.7.14 | Web 框架 |
| MyBatis-Plus | 3.5.3 | ORM 框架 |
| MySQL Connector | 8.0.33 | 数据库驱动 |
| JWT (jjwt) | 0.11.5 | 身份认证 |
| Spring Security Crypto | - | BCrypt 密码加密 |
| FastJSON | 2.0.25 | JSON 处理 |
| Lombok | - | 代码生成 + SLF4J 日志 |
| Devtools | - | 热重载（开发环境） |

### 前端
| 组件 | 版本 | 说明 |
|-----|-----|------|
| Vue | ^3.4.0 | 渐进式框架 |
| Vite | ^5.4.1 | 构建工具 |
| Axios | ^1.5.1 | HTTP 客户端 |
| Vue Router | ^4.2.5 | 路由管理 |
| Pinia | ^2.3.0 | 状态管理 |
| Element Plus | ^2.4.13 | UI 组件库 |

### 数据库
- **数据库**: MySQL 5.7+ / 8.0+
- **字符集**: `utf8mb4`，排序规则 `utf8mb4_unicode_ci`
- **引擎**: InnoDB（支持事务和行级锁）
- **外键约束**: 级联删除（`ON DELETE CASCADE`）

## 核心功能

### 1. 用户认证
- ✅ 注册：BCrypt 加密密码存储
- ✅ 登录：生成 JWT token（7 天有效期）
- ✅ 令牌验证：`JwtAuthenticationFilter` 拦截需要认证的请求

### 2. 故事管理
- ✅ 创建故事：用户输入标题和开头，立即返回故事 ID
- ✅ 异步生成分支：后台调用 DeepSeek API 递归生成多层分支（默认最大深度 7 层）
- ✅ 重试机制：单次 API 调用失败自动重试 3 次（间隔 2 秒）
- ✅ 生成状态检查：前端轮询 `/ready` 接口，完成后自动加载
- ✅ 我的故事列表：获取当前用户的所有故事（按时间倒序）
- ✅ 删除故事：级联删除所有关联节点（数据库层 `ON DELETE CASCADE`）

### 3. 读者交互
- ✅ 分支选择：每个节点两个选项（A / B）
- ✅ 树形结构：递归生成，支持深层分支
- ✅ 结局标记：`is_ending` 字段标识故事完结
- ✅ 直接节点跳转：支持通过节点 ID 直接获取任意节点

## API 端点

所有接口以 `/api` 为前缀，返回格式统一为 `{ code, message, data }`。需要认证的接口需在请求头携带 `Authorization: Bearer <token>`。

### 认证类
| 方法 | 端点 | 说明 |
|-----|------|------|
| POST | /api/auth/register | 用户注册 |
| POST | /api/auth/login | 用户登录 |

### 故事类（需认证）
| 方法 | 端点 | 说明 |
|-----|------|------|
| POST | /api/stories | 创建故事（异步生成） |
| GET | /api/stories/my-stories | 获取当前用户的故事列表 |
| GET | /api/stories/{storyId}/start | 获取故事根节点 |
| GET | /api/stories/{storyId}/ready | 检查故事是否生成完成 |
| GET | /api/stories/nodes/{nodeId} | 通过 ID 获取指定节点 |
| GET | /api/stories/{storyId}/nodes/{nodeId}/next?choice=A | 获取下一个节点 |
| DELETE | /api/stories/{storyId} | 删除故事（需为作者本人） |

> 详细的请求/响应示例请参见后端模块内的 [README](qilu-story-backend/README.md)。

## 配置说明

### 数据库配置
修改 `qilu-story-backend/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qilu_story?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: ${DB_PASSWORD}       # 建议使用环境变量
    driver-class-name: com.mysql.cj.jdbc.Driver
```

### DeepSeek API 配置
获取 API 密钥：https://platform.deepseek.com/api_keys

```yaml
deepseek:
  api-key: ${DEEPSEEK_API_KEY}     # 建议使用环境变量
  api-url: https://api.deepseek.com/v1/chat/completions
  model: deepseek-chat
  max-tokens: 1000
  temperature: 0.7
```

### JWT 配置
```yaml
jwt:
  secret: ${JWT_SECRET}            # 至少 32 字符，建议使用环境变量
  expiration: 604800000            # 7 天（毫秒）
```

### CORS 配置
后端 `WebConfig.java` 已配置以下来源的跨域访问：
- `http://localhost:8080`、`http://localhost:5173`
- `http://127.0.0.1:8080`、`http://127.0.0.1:5173`

### 前端代理配置
前端 `vite.config.js` 已配置代理将 `/api` 请求转发到 `http://localhost:8081`，无需额外设置。

## 开发指南

### 添加新字段
1. 修改 `qilu_story_init.sql` 中的表结构
2. 更新对应的 Entity 类（使用 Lombok `@Data` / `@Builder`）
3. 如需更新关联的 DTO 或 Service 逻辑

### 添加新 API
1. 在 `controller` 包中创建或修改对应的 Controller
2. 在 `service` 包中实现业务逻辑
3. 定义请求/响应的 DTO
4. 使用 `ApiResponse.success()` / `ApiResponse.error()` 统一返回格式

### 调整 AI 生成深度
在 `StoryService.java` 中：
- `generateBranchesAsync`（主流程）默认最大深度 **7 层**
- `generateBranchesSync`（同步测试）默认最大深度 **6 层**

```java
// StoryService.java: generateBranchesAsync
generateBranchRecursively(storyId, rootNodeId, opening, 1, 7);
```

### 调整故事每节字数
修改 `DeepseekService.java` 中 `buildPrompt()` 方法的提示词：
```
content 150-200字
```

### 调试技巧
- MyBatis-Plus SQL 日志已启用：`StdOutImpl`
- 启用后端日志：在 `application.yml` 设置 `logging.level.com.qilu.story: DEBUG`
- 使用浏览器开发者工具查看网络请求（前端代理日志）
- 检查后端日志中的 `DeepSeek API 响应` 内容
- 使用 Postman 或 curl 直接测试 API

## 常见问题

**Q: 为什么创建故事后前端一直显示"生成中"？**
A: AI 生成需要 30-60 秒，前端每 2 秒轮询 `/ready` 接口，完成后自动加载。请确保后端服务正常运行且 DeepSeek API 密钥有效。API 调用失败会自动重试 3 次。

**Q: 如何修改生成的故事字数？**
A: 修改 `DeepseekService.buildPrompt()` 方法中提示词的 `content` 字数要求（默认 150-200 字），并调整递归深度（见上文）。

**Q: 如何支持多个语言？**
A: 可在 DTO 中添加语言字段，在 AI 提示词中指定语言，并返回相应语言的内容。

## 性能优化建议

- [ ] 使用 Redis 缓存热门故事节点
- [ ] 数据库索引已包含：`idx_username`、`idx_author_id`、`idx_create_time`、`idx_story_id`、`idx_parent_node_id`、`idx_option_a_next_id`、`idx_option_b_next_id`、`idx_is_ending`
- [ ] 前端实现虚拟滚动优化长列表
- [ ] 将 AI 生成任务放入消息队列（如 RabbitMQ）进一步解耦

## 安全建议

- 🔐 **不要将真实密钥提交到公开仓库**：使用环境变量替换 `application.yml` 中的明文
- 🔐 生产环境启用 HTTPS
- 🔐 添加请求速率限制（如每分钟最多创建 5 个故事）
- 🔐 对用户输入进行 XSS 过滤
- 🔐 定期更新依赖版本

## 许可证

MIT

## 贡献

欢迎 PR 和 Issue！

## 联系方式

- 项目主页：https://github.com/547dsj/qilu-story
- 问题反馈：https://github.com/547dsj/qilu-story/issues

---

**最后更新**: 2026年6月27日