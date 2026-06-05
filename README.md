# NReader

一个私人小说阅读器应用，包含 Android 客户端和 Python 后端服务。

## 项目结构

```
NReader/
├── NReader_Android/     # Android 客户端应用
├── NReader_py/         # Python 后端服务
└── README.md
```

## 功能特性

- 📚 **书架管理** - 管理本地书籍，支持进度保存
- 📖 **在线书库** - 连接后端服务获取在线小说资源
- 📖 **阅读器** - 支持多种阅读设置（字体、背景、亮度等）
- 📡 **WiFi传书** - 通过局域网传输小说文件
- 📁 **本地导入** - 从设备本地存储导入小说
- 🔐 **用户认证** - 支持登录远程服务器

## 快速开始

### 后端服务 (NReader_py)

#### 环境要求
- Python 3.8+
- Flask 3.0.0+

#### 安装依赖
```bash
cd NReader_py
pip install -r requirements.txt
```

#### 启动服务
```bash
python app.py
```

服务将在 `http://localhost:5000` 启动

#### 默认账号
- 用户名: `admin`
- 密码: `123456`

### Android 客户端 (NReader_Android)

#### 环境要求
- Android Studio
- JDK 11+
- Android SDK (minSdk 31, targetSdk 35)

#### 构建运行
1. 使用 Android Studio 打开 `NReader_Android` 目录
2. 等待 Gradle 同步完成
3. 连接 Android 设备或启动模拟器
4. 点击 Run 按钮安装运行

## 技术栈

### Android 客户端
- **语言**: Kotlin
- **UI框架**: Jetpack Compose
- **架构**: MVVM
- **网络**: OkHttp + Gson
- **存储**: SharedPreferences

### 后端服务
- **框架**: Flask 3.0.0
- **数据库**: SQLite
- **认证**: Token + Session

## API 接口

### 登录
```
POST /api/login
Body: { "username": "admin", "password": "123456" }
```

### 获取小说列表
```
GET /api/novels?page=1&page_size=10&search=关键词
Header: Authorization: {token}
```

### 获取小说内容
```
GET /api/novel/{novel_id}
Header: Authorization: {token}
```

### 健康检查
```
GET /api/health
```

## 配置说明

### 后端配置
编辑 `NReader_py/app.py` 中的 `NOVELS_DIR` 变量设置小说文件目录：
```python
NOVELS_DIR = Path('D:/temp')  # 修改为你的小说目录
```

### Android 客户端配置
在应用的「我的」页面点击「网络配置」设置后端服务器地址和登录凭据。

## 项目截图

| 书架 | 阅读器 | 设置 |
|:---:|:---:|:---:|
| 书架管理界面 | 小说阅读界面 | 阅读设置界面 |

## 许可证

MIT License