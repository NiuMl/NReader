# NReader Python Backend

小说阅读器后端服务，用于读取本地小说文件并提供API接口。

## 功能特性

- 读取本地小说文件（支持 .txt 格式）
- 提供小说列表 API
- 提供小说内容 API
- 支持跨域访问（CORS）

## 安装依赖

```bash
pip install -r requirements.txt
```

## 运行服务

```bash
python app.py
```

服务将在 http://localhost:5000 启动

## API 接口

### 1. 获取小说列表

```
GET /api/novels
```

返回所有可用的小说列表。

**响应示例：**
```json
[
  {
    "id": "local_傲世丹神",
    "title": "傲世丹神",
    "author": "本地文件",
    "cover": "",
    "isInShelf": false,
    "filePath": "file:///D:/temp/傲世丹神.txt"
  }
]
```

### 2. 获取小说内容

```
GET /api/novel/<novel_id>
```

根据小说ID获取小说内容。

**参数：**
- `novel_id`: 小说ID（如 `local_傲世丹神`）

**响应示例：**
```json
{
  "id": "local_傲世丹神",
  "title": "傲世丹神",
  "content": "小说内容..."
}
```

### 3. 健康检查

```
GET /api/health
```

检查服务是否正常运行。

**响应示例：**
```json
{
  "status": "ok",
  "message": "NReader Backend is running"
}
```

## 配置

默认小说目录：`D:/temp`

如需修改，编辑 `app.py` 中的 `NOVELS_DIR` 变量。

## 前端集成

在前端代码中，将小说列表的获取方式从本地文件改为调用后端API：

```typescript
// 替换原有的 library.ts 数据
async function fetchNovels() {
  const response = await fetch('http://localhost:5000/api/novels')
  const novels = await response.json()
  return novels
}

// 获取小说内容
async function fetchNovelContent(novelId: string) {
  const response = await fetch(`http://localhost:5000/api/novel/${novelId}`)
  const data = await response.json()
  return data.content
}
```

## 注意事项

- 确保 `D:/temp` 目录存在且包含 .txt 格式的小说文件
- 服务默认监听所有网络接口（0.0.0.0），可在局域网内访问
- 开发模式下启用 debug，生产环境请关闭