# NReader Web 管理系统使用说明

## 启动服务

运行以下命令启动 Flask 服务器：

```bash
python app.py
```

或者使用提供的启动脚本：

```bash
# Windows
start.bat

# Linux/Mac
sh start.sh
```

服务器将在 http://localhost:5000 启动。

## 访问管理系统

在浏览器中访问：http://localhost:5000

## 登录

默认账号：
- 用户名：`admin`
- 密码：`123456`

## 功能说明

### 1. 小说列表页面
- 显示数据库中所有小说的列表
- 显示小说的 ID、书名、作者和文件路径

### 2. 用户管理页面
- 查看所有用户列表
- 添加新用户
- 编辑现有用户（可修改用户名和密码）
- 删除用户（注意：admin 用户不能被删除）

## API 接口

除了 Web 管理界面，还提供以下 API 接口：

### 登录接口
- **URL**: `POST /api/login`
- **参数**: `{ "username": "admin", "password": "123456" }`
- **返回**: 
  ```json
  {
    "code": 0,
    "message": "登录成功",
    "token": "token字符串",
    "expiry": "2023-06-04 12:00:00"
  }
  ```

### 获取小说列表
- **URL**: `GET /api/novels`
- **Header**: `Authorization: {token}`
- **参数**: `page`, `page_size`, `search`（可选）

### 获取小说内容
- **URL**: `GET /api/novel/{novel_id}`
- **Header**: `Authorization: {token}`

## 技术说明

- 使用 Flask 作为 Web 框架
- 使用 SQLite 数据库
- 密码使用 MD5 加密存储
- Session 管理用户登录状态
- Token 有效期为 1 天
