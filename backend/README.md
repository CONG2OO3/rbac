# RBAC 后端

技术栈：Spring Boot 3、MyBatis-Plus、MySQL、Redis、Sa-Token。

## 前后端同包部署

前端页面由后端根路径 `/` 提供，接口仍使用 `/api` 前缀。Vue 的页面地址在刷新时会回退到 `index.html`，静态资源由 Spring Boot 直接提供。开发时仍可使用 Vite 的 `/api` 代理。

从仓库根目录执行以下命令，将当前前端代码构建并复制到 `backend/src/main/resources/static`，然后打包后端：

```powershell
./backend/deploy-frontend.ps1 -Build
mvn -f backend/pom.xml clean package
```

如果 `front/dist` 已经是最新构建结果，可以省略 `-Build`，仅执行复制。以后更新前端时，重新执行复制并重新打包 JAR；不要只替换正在运行的 JAR 内的文件。启动后访问 `http://服务器地址:8085/`，接口仍为 `http://服务器地址:8085/api/...`。如果更改了前端路由，请同步更新 `WebMvcConfig` 中的页面回退路径。

## 已实现接口

- `POST /api/auth/login` 登录
- `POST /api/auth/logout` 退出
- `GET /api/auth/info` 当前用户
- `/api/system/user` 用户 CRUD
- `/api/system/role` 角色 CRUD
- `/api/system/menu` 菜单 CRUD、菜单树
- `/api/system/dept` 部门 CRUD
- `/api/system/post` 岗位 CRUD
- `GET /api/system/file/list` 文件分页查询
- `POST /api/system/file/upload` 通过当前存储策略上传图片、附件或视频
- `GET /api/system/file/{id}/content` 文件预览或下载
- `DELETE /api/system/file/{ids}` 删除文件
- `GET /api/public/config` 公开读取系统名称、Logo 及登录/注册开关
- `/api/system/config/*` 管理员维护系统配置和 Logo
- `GET /api/auth/captcha` 获取一次性图形验证码
- `POST /api/auth/register` 在开放注册时创建普通用户

列表接口统一为 `GET /list?pageNum=1&pageSize=10&keyword=`，删除接口为 `DELETE /{ids}`。

## 环境配置

- `application.yml`：所有环境共用的端口、上传大小、MyBatis-Plus 和 Sa-Token 配置。
- `application-test.yml`：测试环境，默认启用；连接本地 MySQL/Redis，并自动执行 `schema.sql`、`data.sql`。
- `application-prod.yml`：生产环境；数据库、Redis 和对象存储信息从环境变量读取，不自动执行 SQL，也不开放 Swagger 文档。

存储方式在“系统配置 → 存储配置”中动态设置，支持 MinIO、阿里云 OSS 和星云图，保存后无需重启即可切换新上传文件的存储服务。三个策略始终可用，文件列表显示所有历史记录，读取时按各文件原有的存储类型选择策略，因此切换后应保留旧服务的配置。配置保存在已有的 `sys_config` 表中，密钥不会通过配置查询接口返回。首次使用前须在页面填写一种完整的存储配置。`starimg` 仅支持图片；上传沿用现有 `/api/system/file/upload`，无需新接口。星云图当前只提供上传接口，删除文件记录不会删除图床上的图片。

生产环境首次部署前，请先创建数据库，并按需执行 `src/main/resources/schema.sql` 和 `src/main/resources/data.sql`。不要把真实密码或密钥写入配置文件或提交到代码仓库。

已存在的数据库可单独执行 `db/system_config.sql`，创建系统配置表和菜单。验证码依赖 Redis，Logo 上传依赖当前启用的对象存储。默认关闭验证码和自主注册；开启验证码后，登录与注册请求必须提交 `captchaId` 和 `captchaCode`。

自主注册账号自动关联普通用户角色；`/system/**` 和 `/business/**` 管理接口仅允许超级管理员角色访问。

## 本地启动（test）

PowerShell：

```powershell
$env:DB_USER = "root"
$env:DB_PASSWORD = "你的 MySQL 密码"
$env:SPRING_PROFILES_ACTIVE = "test" # 可省略，默认就是 test
$env:REDIS_HOST = "localhost"
# 如 8080 被占用，可临时切换端口
# $env:SERVER_PORT = "8081"
mvn spring-boot:run
```

应用首次启动会自动创建 `rbac` 数据库中的系统表并写入初始化数据。默认登录账号：`admin / 123456`。

前端开发服务器通过 `/api` 代理到 `http://localhost:8080`。

## 生产启动（prod）

在部署平台中设置 `SPRING_PROFILES_ACTIVE=prod`，并提供 `DB_HOST`、`DB_USER`、`DB_PASSWORD`、`REDIS_HOST` 等环境变量。对象存储凭据不再写入 `application*.yml`；启动后由超级管理员在“系统配置 → 存储配置”中设置。可选设置 `DB_PORT`、`DB_NAME`、`REDIS_PORT`、`REDIS_PASSWORD`、`REDIS_DATABASE`、`DB_USE_SSL` 和 `SERVER_PORT`。

例如启动 JAR：

```powershell
java -jar target/rbac-backend-1.0.0.jar --spring.profiles.active=prod
```
