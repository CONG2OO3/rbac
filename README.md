<h1 align="center">RBAC</h1>

<p align="center">基于 Spring Boot 3 和 Vue 3 的后台管理系统</p>

<p align="center">
  <img alt="Java 17" src="https://img.shields.io/badge/Java-17-ED8B00?style=flat-square">
  <img alt="Spring Boot 3.3" src="https://img.shields.io/badge/Spring%20Boot-3.3-6DB33F?style=flat-square">
  <img alt="Vue 3" src="https://img.shields.io/badge/Vue-3-42B883?style=flat-square">
  <img alt="MySQL" src="https://img.shields.io/badge/MySQL-4479A1?style=flat-square">
  <img alt="Redis" src="https://img.shields.io/badge/Redis-DC382D?style=flat-square">
</p>

<p align="center">
  <a href="#功能">功能</a> ·
  <a href="#快速开始">快速开始</a> ·
  <a href="#打包部署">打包部署</a> ·
  <a href="#项目文档">项目文档</a>
</p>

<p align="center">
  <img src="./原型图.png" alt="RBAC 管理后台首页设计原型" width="920">
  <br>
  <sub>管理后台首页设计原型</sub>
</p>

## 功能

- **账号与权限** · 管理用户、角色、菜单、部门和岗位。Sa-Token 负责登录态，管理接口校验超级管理员角色。
- **系统设置** · 自定义系统名称与 Logo，按需启用图形验证码和自主注册。自主注册账号自动获得普通用户角色。
- **文件管理** · 上传、预览和下载文件。支持 MinIO、阿里云 OSS 和星云图（仅图片），可在后台切换新文件的存储方式。
- **业务管理** · 内置产品和订单模块，可沿着 Vue 页面、REST API 和 MyBatis-Plus 代码查看完整的业务实现。
- **日志与监控** · 查看登录日志、操作日志、在线用户、服务指标和访问趋势。

## 快速开始

准备 JDK 17、Maven、Node.js 22.12 或更新版本，以及本地 MySQL 和 Redis。以下命令使用 PowerShell。

```powershell
git clone https://github.com/CONG2OO3/rbac.git
cd rbac
```

### 1. 启动后端

```powershell
$env:SPRING_PROFILES_ACTIVE = 'test'
$env:DB_USER = 'root'
$env:DB_PASSWORD = '填写本地 MySQL 密码'
mvn -f backend/pom.xml spring-boot:run
```

后端默认监听 `8085`，连接本机 MySQL（`3306`）和 Redis（`6379`）。`test` 环境使用 `rbac` 数据库并自动执行建表和初始化脚本。如果 MySQL 账号没有创建数据库的权限，先创建 `rbac` 数据库并授权。Redis 需要密码时，在启动前设置 `REDIS_PASSWORD`。

### 2. 启动前端

打开另一个终端，从仓库根目录运行以下命令。

```powershell
cd front
npm ci
npm run dev
```

- **管理后台** · 打开 [localhost:5173](http://localhost:5173)，使用本地初始化账号 `admin / 123456` 登录。
- **接口文档** · [Swagger UI](http://localhost:8085/swagger-ui.html)。

开发时，Vite 会将 `/api` 请求代理到 `http://localhost:8085`。试用文件上传前，先在「系统配置 → 存储配置」中配置一个存储服务。

## 打包部署

在仓库根目录执行以下命令。脚本会构建 Vue 前端并复制到 Spring Boot 静态资源目录，随后 Maven 将前端和 API 一起打包进 JAR。

```powershell
npm --prefix front ci
./backend/deploy-frontend.ps1 -Build
mvn -f backend/pom.xml clean package
```

生产部署时，先创建目标数据库并按需执行 [建表脚本](./backend/src/main/resources/schema.sql) 和 [初始化数据脚本](./backend/src/main/resources/data.sql)。通过部署环境设置 `DB_HOST`、`DB_USER`、`DB_PASSWORD`、`REDIS_HOST` 和 `REDIS_PASSWORD`，并用 `DB_NAME` 指定目标库。若导入了示例管理员账号，上线前请修改初始密码。

```powershell
java -jar backend/target/rbac-backend-1.0.0.jar --spring.profiles.active=prod
```

启动后，Spring Boot 在 `/` 提供前端页面，在 `/api` 提供接口。

## 项目结构

```text
rbac/
├── backend/   Spring Boot API、环境配置和初始化 SQL
├── front/     Vue 页面、路由和 API 客户端
├── db/        各模块的增量 SQL
├── docs/      项目学习教程和接口调用链
└── doc/       从环境搭建到部署的开发笔记
```

## 项目文档

- [项目学习与实现教程](./docs/项目学习与实现教程.md)
- [接口端到端调用链](./docs/接口端到端调用链.md)
- [后端接口、环境配置与存储策略](./backend/README.md)
