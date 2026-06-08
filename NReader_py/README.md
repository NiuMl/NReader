# NReader 后端服务

局域网小说阅读器后端服务，使用 Python + FastAPI 开发。

## 功能特性

- 自动扫描书籍目录（支持 .txt 和 .epub 格式）
- 书籍元数据提取（书名、作者、章节、总字数等）
- RESTful API 接口
- WiFi 传书功能
- 跨域支持

## 安装依赖

```bash
pip install -r requirements.txt
```

## 配置

编辑 `config.yaml` 文件：

```yaml
# 书籍扫描目录
books_dir: D:/temp

# 服务监听端口
port: 8000

# 数据库路径
db_path: nreader.db

# 支持的文件格式
supported_formats:
  - .txt
  - .epub

# 上传文件保存目录
upload_dir: uploaded
```

## 运行

```bash
python main.py
```

服务启动后会自动扫描配置的书籍目录。

## API 文档

启动服务后访问 `http://localhost:8000/docs` 查看完整的 API 文档。

### 主要接口

- `GET /api/books` - 获取书籍列表（支持分页、排序）
- `GET /api/books/{id}` - 获取书籍详情
- `GET /api/books/search?q=关键词` - 搜索书籍
- `GET /api/chapters/{book_id}/{chapter_index}` - 获取章节内容
- `POST /api/scan` - 手动触发扫描
- `POST /api/upload` - 上传书籍文件
- `GET /upload` - WiFi 传书网页界面

## WiFi 传书

1. 确保手机和电脑在同一局域网
2. 在浏览器访问 `http://电脑IP:8000/upload`
3. 选择 .txt 或 .epub 文件上传
4. 上传的书籍会自动添加到书库
