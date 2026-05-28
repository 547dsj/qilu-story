以下是修改后的完整项目 README，适用于根目录，反映当前项目的实际状态（前后端均已实现，包含新增功能）：

```markdown
# 歧路·互动小说平台

完整的互动小说网站项目，技术栈 Spring Boot + Vue 3 + MySQL。

## 项目介绍

「歧路·互动小说」是一个创新的在线互动小说平台：
1. 用户输入故事开头和标题
2. AI 自动生成多层分支剧情（最多 6 层，约 1 万字）
3. 读者通过选择选项推动故事发展
4. 每个分支最终走向不同的结局
5. 管理个人故事：查看列表、删除故事

## 项目结构

```
qilu-story/
├── qilu_story_init.sql              # 数据库初始化脚本
├── qilu-story-backend/              # Spring Boot 后端项目
│   ├── pom.xml
│   ├── README.md
│   ├── .gitignore
│   └── src/main/
│       ├── java/com/qilu/story/
│       └── resources/
└── qilu-story-frontend/             # Vue 3 前端项目
    ├── package.json
    ├── vite.config.js
    ├── index.html
    └── src/
        ├── api/                     # API 接口封装
        ├── assets/                  # 静态资源
        ├── router/                  # 路由配置
        ├── store/                   # Pinia 状态管理
        └── views/                   # 页面组件
```

## 快速开始

### 1. 初始化数据库
```bash
mysql -u root -p < qilu_story_init.sql
```

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
- **框架**: Spring Boot 2.7.14
- **ORM**: MyBatis-Plus 3.5.3
- **数据库**: MySQL 8.0.33
- **认证**: JWT (jjwt 0.11.5)
- **密码加密**: Spring Security BCrypt
- **AI 集成**: DeepSeek API
- **JSON**: FastJSON 2.0.25
- **异步支持**: `@Async` 注解（需在启动类启用）

### 前端
- **框架**: Vue 3
- **构建工具**: Vite 5
- **HTTP 客户端**: Axios
- **路由**: Vue Router 4
- **状态管理**: Pinia 2
- **UI 框架**: Element Plus

### 数据库
- **数据库**: MySQL 5.7+
- **字符集**: utf8mb4（支持 emoji）
- **引擎**: InnoDB（支持事务、外键）

## 核心功能

### 1. 用户认证
- ✅ 注册：BCrypt 加密密码
- ✅ 登录：生成 JWT token（7 天有效）
- ✅ 令牌验证：过滤器拦截需要认证的请求

### 2. 故事管理
- ✅ 创建故事：用户输入标题和开头，立即返回故事 ID
- ✅ 异步生成分支：后台调用 DeepSeek API 递归生成多层分支（默认深度 6 层）
- ✅ 生成状态检查：前端轮询 `/ready` 接口，完成后自动加载
- ✅ 我的故事列表：获取当前用户的所有故事
- ✅ 删除故事：级联删除所有关联节点

### 3. 读者交互
- ✅ 分支选择：每个节点两个选项
- ✅ 树形结构：递归生成，支持深层分支
- ✅ 结局标记：标识故事已完结

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
| DELETE | /api/stories/{storyId} | 删除故事 |
| GET | /api/stories/{storyId}/ready | 检查故事是否生成完成 |
| GET | /api/stories/{storyId}/start | 获取故事根节点 |
| GET | /api/stories/{storyId}/nodes/{nodeId}/next | 获取下一个节点 |

> 详细的请求/响应示例请参见后端模块内的 [README](qilu-story-backend/README.md)。

## 配置说明

### 数据库配置
修改 `qilu-story-backend/src/main/resources/application.yml`：
```yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/qilu_story?useSSL=false&serverTimezone=Asia/Shanghai
    username: root
    password: your_password   # 建议使用环境变量 ${DB_PASSWORD}
```

### DeepSeek API 配置
获取 API 密钥：https://platform.deepseek.com/api_keys

```yaml
deepseek:
  api-key: sk-your-api-key-here   # 建议使用环境变量 ${DEEPSEEK_API_KEY}
  api-url: https://api.deepseek.com/v1/chat/completions
  model: deepseek-chat
  max-tokens: 1000
  temperature: 0.7
```

### JWT 配置
```yaml
jwt:
  secret: your-secret-key-at-least-32-characters   # 建议使用环境变量
  expiration: 604800000  # 7 天（毫秒）
```

### 前端代理配置
前端 `vite.config.js` 已配置代理将 `/api` 请求转发到 `http://localhost:8081`，无需额外设置。

## 开发指南

### 添加新字段
1. 修改 `qilu_story_init.sql` 中的表结构
2. 更新对应的 Entity 类（使用 Lombok）
3. 如果需要更新关联的 DTO 或 Service 逻辑

### 添加新 API
1. 在 `controller` 包中创建或修改对应的 Controller
2. 在 `service` 包中实现业务逻辑
3. 定义请求/响应的 DTO（Data Transfer Object）

### 调整 AI 生成深度
在 `StoryService.java` 中的 `createStorySync` 方法内修改递归深度参数：
```java
generateBranchRecursively(storyId, rootNodeId, opening, 1, 6); // 6 表示最大深度
```

### 调试技巧
- 启用 MyBatis-Plus SQL 日志：`mybatis-plus.configuration.log-impl: org.apache.ibatis.logging.stdout.StdOutImpl`
- 使用浏览器开发者工具查看网络请求（前端代理日志）
- 检查后端日志中的 `DeepSeek API 响应` 内容
- 使用 Postman 或 curl 直接测试 API

## 常见问题

**Q: 为什么创建故事后前端一直显示“生成中”？**
A: AI 生成需要 30–60 秒，前端每 2 秒轮询 `/ready` 接口，完成后自动加载。请确保后端服务正常运行且 DeepSeek API 密钥有效。

**Q: 如何修改生成的故事字数？**
A: 修改 `DeepseekService.buildPrompt()` 方法中的 `content` 字数要求（例如 150-200 字），并调整递归深度（见上文）。

**Q: 如何支持多个语言？**
A: 可在 DTO 中添加语言字段，在 AI 提示词中指定语言，并返回相应语言的内容。

## 性能优化建议

- [ ] 使用 Redis 缓存热门故事节点
- [ ] 为 `story_node` 表添加索引（`story_id`, `parent_node_id`）
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

- 项目主页：https://github.com/your-org/qilu-story
- 问题反馈：https://github.com/your-org/qilu-story/issues

---

**最后更新**: 2026年5月28日
```

