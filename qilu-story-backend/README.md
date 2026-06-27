# 歧路·互动小说平台 - Spring Boot 后端

一个基于 Spring Boot + MyBatis-Plus + JWT 的互动小说平台后端，支持 AI 自动生成多层故事分支。

## 项目特点

✨ **核心功能**
- 用户认证：注册、登录、JWT 令牌管理
- 故事创建：用户可创建故事并设定开头
- AI 生成：集成 DeepSeek API，自动生成多层故事分支（最多 7 层，约 1 万字）
- 带重试机制：单次 API 调用失败自动重试 3 次（间隔 2 秒）
- 互动阅读：每个节点提供两个选项，支持树形故事结构（递归生成）
- 故事管理：获取当前用户的故事列表、按 ID 获取节点、删除故事
- 异步生成：创建故事后立即返回 ID，后台异步生成分支
- 同步生成：提供同步生成方法，适合测试或等待完成后再返回
- 事务管理：确保数据一致性
- 统一响应：ApiResponse 封装 `{ code, message, data }`

🔐 **安全特性**
- BCrypt 密码加密
- JWT 身份认证（7 天有效期）
- CORS 跨域配置（指定来源白名单）
- 全局异常处理（`GlobalExceptionHandler` + `BusinessException`）

## 技术栈

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

## 项目结构

```
qilu-story-backend/
├── pom.xml
├── .gitignore
├── README.md
└── src/
    └── main/
        ├── java/com/qilu/story/
        │   ├── QiluStoryApplication.java        # 启动类（含 @EnableAsync）
        │   ├── controller/
        │   │   ├── AuthController.java           # 认证接口
        │   │   └── StoryController.java          # 故事接口
        │   ├── service/
        │   │   ├── UserService.java              # 用户业务
        │   │   ├── StoryService.java             # 故事业务（核心递归生成）
        │   │   └── DeepseekService.java          # DeepSeek API 调用（含重试）
        │   ├── mapper/
        │   │   ├── UserMapper.java               # 用户数据访问
        │   │   ├── StoryMapper.java              # 故事数据访问
        │   │   └── StoryNodeMapper.java          # 故事节点数据访问
        │   ├── entity/
        │   │   ├── User.java                     # 用户实体
        │   │   ├── Story.java                    # 故事实体
        │   │   └── StoryNode.java                # 故事节点实体
        │   ├── dto/
        │   │   ├── RegisterRequest.java          # 注册请求
        │   │   ├── LoginRequest.java             # 登录请求
        │   │   ├── LoginResponse.java            # 登录响应
        │   │   ├── CreateStoryRequest.java       # 创建故事请求
        │   │   └── DeepseekResponse.java         # AI 响应
        │   ├── config/
        │   │   ├── WebConfig.java                # CORS 跨域配置
        │   │   └── FilterConfig.java             # 过滤器注册
        │   ├── filter/
        │   │   └── JwtAuthenticationFilter.java  # JWT 认证过滤器
        │   ├── exception/
        │   │   ├── BusinessException.java        # 业务异常
        │   │   └── GlobalExceptionHandler.java   # 全局异常处理
        │   └── utils/
        │       ├── ApiResponse.java              # 统一响应封装
        │       └── JwtUtil.java                  # JWT 工具类
        └── resources/
            ├── application.yml                   # 主配置
            └── application.yml.example           # 配置模板
```

## API 文档

### 认证相关

#### 1. 用户注册
```
POST /api/auth/register
Content-Type: application/json

{
  "username": "user123",
  "password": "password123",
  "nickname": "小说迷"
}

响应:
{
  "code": 200,
  "message": "注册成功",
  "data": 1
}
```

#### 2. 用户登录
```
POST /api/auth/login
Content-Type: application/json

{
  "username": "user123",
  "password": "password123"
}

响应:
{
  "code": 200,
  "message": "登录成功",
  "data": {
    "token": "eyJhbGciOiJIUzUxMiJ9...",
    "userInfo": {
      "id": 1,
      "username": "user123",
      "nickname": "小说迷"
    }
  }
}
```

### 故事相关（需要 JWT 认证）

#### 3. 创建故事（异步生成分支）
```
POST /api/stories
Authorization: Bearer <token>
Content-Type: application/json

{
  "title": "勇者的冒险",
  "opening": "你醒来时发现自己在一个陌生的森林里..."
}

响应:
{
  "code": 200,
  "message": "故事创建成功",
  "data": 1   // 故事 ID
}
```
> 注意：此接口立即返回故事 ID，后台异步生成分支（最大深度 7 层）。前端应通过 `/ready` 接口轮询检查生成状态。API 调用失败会自动重试 3 次，每次间隔 2 秒。

#### 4. 获取当前用户的所有故事
```
GET /api/stories/my-stories
Authorization: Bearer <token>

响应:
{
  "code": 200,
  "message": "获取成功",
  "data": [
    {
      "id": 1,
      "title": "勇者的冒险",
      "opening": "你醒来时发现自己在一个陌生的森林里...",
      "authorId": 1,
      "createTime": "2025-05-18T10:00:00"
    }
  ]
}
```

#### 5. 删除故事
```
DELETE /api/stories/{storyId}
Authorization: Bearer <token>

响应:
{
  "code": 200,
  "message": "删除成功",
  "data": null
}
```
> 删除故事前会验证是否为作者本人；删除后数据库通过 `ON DELETE CASCADE` 级联删除所有关联节点。

#### 6. 检查故事是否已生成完成
```
GET /api/stories/{storyId}/ready
Authorization: Bearer <token>

响应:
{
  "code": 200,
  "message": "获取成功",
  "data": true    // true-已完成，false-生成中
}
```

#### 7. 获取故事根节点
```
GET /api/stories/{storyId}/start
Authorization: Bearer <token>

响应:
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "id": 1,
    "storyId": 1,
    "parentNodeId": null,
    "content": "你醒来时发现自己在一个陌生的森林里...",
    "optionALabel": "向旁边的山洞探去",
    "optionBLabel": "沿着小溪向前走",
    "optionANextId": 2,
    "optionBNextId": 3,
    "isEnding": 0,
    "createTime": "2025-05-18T10:00:00"
  }
}
```

#### 8. 获取指定节点（按节点 ID）
```
GET /api/stories/nodes/{nodeId}
Authorization: Bearer <token>

响应:
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "id": 2,
    "storyId": 1,
    "parentNodeId": 1,
    "content": "你走进了黑暗的山洞，突然看到一个闪闪发光的宝箱...",
    "optionALabel": "打开宝箱",
    "optionBLabel": "小心逃离",
    "optionANextId": 4,
    "optionBNextId": 5,
    "isEnding": 0,
    "createTime": "2025-05-18T10:00:00"
  }
}
```

#### 9. 获取下一个节点（根据选择）
```
GET /api/stories/{storyId}/nodes/{nodeId}/next?choice=A
Authorization: Bearer <token>

响应:
{
  "code": 200,
  "message": "获取成功",
  "data": {
    "id": 2,
    "storyId": 1,
    "parentNodeId": 1,
    "content": "你走进了黑暗的山洞，突然看到一个闪闪发光的宝箱...",
    "optionALabel": "打开宝箱",
    "optionBLabel": "小心逃离",
    "optionANextId": 4,
    "optionBNextId": 5,
    "isEnding": 0,
    "createTime": "2025-05-18T10:00:00"
  }
}
```

## 环境配置

### 敏感信息保护（重要）
**不要将真实密钥提交到公开仓库！请使用环境变量。**

修改 `application.yml`，使用 `${ENV_VAR}` 占位符：

```yaml
spring:
  application:
    name: qilu-story-backend
    
  datasource:
    url: jdbc:mysql://localhost:3306/qilu_story?useUnicode=true&characterEncoding=utf-8&useSSL=false&serverTimezone=Asia/Shanghai
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD}
    driver-class-name: com.mysql.cj.jdbc.Driver

server:
  port: 8081

mybatis-plus:
  configuration:
    log-impl: org.apache.ibatis.logging.stdout.StdOutImpl

deepseek:
  api-key: ${DEEPSEEK_API_KEY}          # 必须设置环境变量
  api-url: https://api.deepseek.com/v1/chat/completions
  model: deepseek-chat
  max-tokens: 1000
  temperature: 0.7

jwt:
  secret: ${JWT_SECRET:your-secret-key-at-least-32-characters}
  expiration: 604800000                  # 7 天（毫秒）

logging:
  level:
    com.qilu.story: DEBUG                # 开发调试日志
```

### 设置环境变量示例
- **Windows (CMD)**: `set DEEPSEEK_API_KEY=sk-xxxxx`
- **Windows (PowerShell)**: `$env:DEEPSEEK_API_KEY="sk-xxxxx"`
- **Linux/macOS**: `export DEEPSEEK_API_KEY=sk-xxxxx`

## 安装与运行

### 前置条件
- JDK 17+
- Maven 3.6+
- MySQL 5.7+ / 8.0+

### 步骤

1. **初始化数据库**
```bash
mysql -u root -p < qilu_story_init.sql
```

2. **配置环境变量**
   设置 `DEEPSEEK_API_KEY`、`DB_PASSWORD` 等。

3. **编译项目**
```bash
mvn clean compile
```

4. **运行项目**
```bash
mvn spring-boot:run
```

或者打包运行：
```bash
mvn clean package
java -jar target/qilu-story-backend-1.0.0.jar
```

5. **验证运行**
   访问：`http://localhost:8081/api/auth/login`（应返回未授权，表示服务正常）

## 数据库表结构

### user 表
| 字段 | 类型 | 说明 |
|-----|-----|------|
| id | BIGINT | 用户 ID（主键自增） |
| username | VARCHAR(50) | 用户名（唯一，索引 `idx_username`） |
| password | VARCHAR(255) | BCrypt 加密密码 |
| nickname | VARCHAR(50) | 昵称 |
| create_time | DATETIME | 创建时间（默认 CURRENT_TIMESTAMP） |

### story 表
| 字段 | 类型 | 说明 |
|-----|-----|------|
| id | BIGINT | 故事 ID（主键自增） |
| title | VARCHAR(100) | 故事标题 |
| opening | TEXT | 故事开头 |
| author_id | BIGINT | 作者 ID（外键 → user.id，`ON DELETE CASCADE`） |
| create_time | DATETIME | 创建时间（默认 CURRENT_TIMESTAMP） |

索引：`idx_author_id`、`idx_create_time`

### story_node 表
| 字段 | 类型 | 说明 |
|-----|-----|------|
| id | BIGINT | 节点 ID（主键自增） |
| story_id | BIGINT | 所属故事 ID（外键 → story.id，`ON DELETE CASCADE`） |
| parent_node_id | BIGINT | 父节点 ID（自引用 → story_node.id，`ON DELETE CASCADE`） |
| content | TEXT | 节点内容（150-200 字） |
| option_a_label | VARCHAR(50) | 选项 A 标签（2-6 字） |
| option_b_label | VARCHAR(50) | 选项 B 标签（2-6 字） |
| option_a_next_id | BIGINT | 选项 A 下一节点（自引用 → story_node.id，`ON DELETE SET NULL`） |
| option_b_next_id | BIGINT | 选项 B 下一节点（自引用 → story_node.id，`ON DELETE SET NAT`） |
| is_ending | TINYINT | 是否结局（0-否，1-是） |
| create_time | DATETIME | 创建时间（默认 CURRENT_TIMESTAMP） |

索引：`idx_story_id`、`idx_parent_node_id`、`idx_option_a_next_id`、`idx_option_b_next_id`、`idx_is_ending`、`idx_create_time`

## AI 生成策略

### DeepSeek 服务 (`DeepseekService.java`)

- **重试机制**：API 调用失败自动重试 3 次，每次间隔 2 秒
- **解析容错**：支持直接 JSON 解析和从 Markdown 代码块中提取 JSON 两种模式
- **默认值填充**：AI 返回空值时自动填充默认选项文本

### 提示词结构
```
当前剧情：{previousContent}
→ 返回值：{optionA: {label, content(150-200字), isEnding}, optionB: ...}
```

### 生成深度
| 方法 | 最大深度 | 用途 |
|-----|-----|------|
| `generateBranchesAsync` | 7 层 | 生产环境（异步主流程） |
| `generateBranchesSync` | 6 层 | 测试/同步场景 |

## 常见问题

### Q: 如何修改 JWT 有效期？
A: 修改 `application.yml` 中的 `jwt.expiration` 值（毫秒）。7 天 = 604800000。

### Q: 如何集成自己的 AI 接口？
A: 修改 `DeepseekService.java`，替换 API 调用、提示词构建和响应解析逻辑。

### Q: 如何添加更多的故事分支选项（如 optionC）？
A: 修改 `StoryNode` 实体、数据库表结构，以及 `DeepseekResponse` DTO 和相关递归逻辑。

### Q: 创建故事后多久能生成完成？
A: 取决于配置的递归深度和 AI 响应速度。默认深度 7 层，每层约 3-5 秒，总计约 30-60 秒。前端可通过 `/ready` 接口轮询。API 调用带 3 次重试保证稳定性。

### Q: 如何开启/关闭 SQL 日志？
A: 修改 `application.yml` 中的 `mybatis-plus.configuration.log-impl`：
- 开启：`org.apache.ibatis.logging.stdout.StdOutImpl`
- 关闭：注释或删除该配置行

## 注意事项

- 确保 DeepSeek API 密钥正确且账户余额充足。
- 生产环境务必使用环境变量传递敏感信息，不要硬编码在 `application.yml` 中。
- MySQL 字符集必须设置为 `utf8mb4`（使用 `utf8mb4_unicode_ci` 排序规则）以支持中文和 emoji。
- 递归生成分支会多次调用 AI API（7 层满二叉树约调用 127 次），注意额度消耗。
- 删除故事会触发级联删除，请谨慎操作。
- 开发环境可使用 `spring-boot-devtools` 热重载，生产环境应排除该依赖。

## 后续扩展建议

- [ ] 添加故事搜索、筛选功能
- [ ] 用户评分、评论功能
- [ ] 故事发布、审核流程
- [ ] 阅读历史记录
- [ ] 数据库查询优化（分页）
- [ ] 单元测试覆盖
- [ ] API 速率限制（防刷）
- [ ] 文件上传（故事封面图）
- [ ] Docker 容器化部署

## License

MIT

## 作者

歧路·互动小说平台开发团队