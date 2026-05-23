from flask import Flask, jsonify, request, send_file
from flask_cors import CORS
import os
import sys
from pathlib import Path
import sqlite3

app = Flask(__name__)
CORS(app)

NOVELS_DIR = Path('D:/temp')
DB_PATH = 'novels.db'

def init_db():
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    cursor.execute('DROP TABLE IF EXISTS novels')
    
    cursor.execute('''
        CREATE TABLE novels (
            id INTEGER PRIMARY KEY AUTOINCREMENT,
            title TEXT NOT NULL,
            author TEXT DEFAULT '本地文件',
            file_path TEXT NOT NULL,
            cover TEXT DEFAULT ''
        )
    ''')
    
    if NOVELS_DIR.exists():
        for file in NOVELS_DIR.glob('*.txt'):
            file_path_str = str(file).replace("\\", "/")
            cursor.execute('''
                INSERT INTO novels (title, file_path)
                VALUES (?, ?)
            ''', (file.stem, file_path_str))
    
    conn.commit()
    conn.close()

def get_novels_from_db(page=1, page_size=10, search=''):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    offset = (page - 1) * page_size
    
    if search:
        cursor.execute('''
            SELECT id, title, author, file_path, cover 
            FROM novels 
            WHERE title LIKE ? 
            ORDER BY id 
            LIMIT ? OFFSET ?
        ''', (f'%{search}%', page_size, offset))
    else:
        cursor.execute('''
            SELECT id, title, author, file_path, cover 
            FROM novels 
            ORDER BY id 
            LIMIT ? OFFSET ?
        ''', (page_size, offset))
    
    novels = cursor.fetchall()
    
    cursor.execute('SELECT COUNT(*) FROM novels WHERE title LIKE ?', (f'%{search}%',))
    total = cursor.fetchone()[0]
    
    conn.close()
    
    return [{
        'id': str(novel[0]),
        'title': novel[1],
        'author': novel[2],
        'cover': novel[4],
        'isInShelf': False,
        'filePath': f'file:///{novel[3]}'
    } for novel in novels], total

@app.route('/api/novels', methods=['GET'])
def get_novels_list():
    page = int(request.args.get('page', 1))
    page_size = int(request.args.get('page_size', 10))
    search = request.args.get('search', '')
    
    novels, total = get_novels_from_db(page, page_size, search)
    
    return jsonify({
        'novels': novels,
        'total': total,
        'page': page,
        'page_size': page_size
    })

@app.route('/api/novel/<int:novel_id>', methods=['GET'])
def get_novel_content(novel_id):
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    
    cursor.execute('SELECT title, file_path FROM novels WHERE id = ?', (novel_id,))
    result = cursor.fetchone()
    
    conn.close()
    
    if not result:
        return jsonify({'error': 'Novel not found'}), 404
    
    title, file_path = result
    file_path = Path(file_path)
    
    if not file_path.exists():
        return jsonify({'error': 'File not found'}), 404
    
    try:
        content = read_file_with_encoding(file_path)
        
        return jsonify({
            'id': str(novel_id),
            'title': title,
            'content': content
        })
    except Exception as e:
        return jsonify({'error': str(e)}), 500

def read_file_with_encoding(file_path):
    encodings = ['utf-8', 'gbk', 'gb2312', 'gb18030', 'cp1252']
    
    for encoding in encodings:
        try:
            with open(file_path, 'r', encoding=encoding) as f:
                return f.read()
        except UnicodeDecodeError:
            continue
    
    raise ValueError(f"Unable to decode file with any of the following encodings: {encodings}")

@app.route('/api/health', methods=['GET'])
def health_check():
    return jsonify({'status': 'ok', 'message': 'NReader Backend is running'})

if __name__ == '__main__':
    print('NReader Backend Server')
    print(f'Novels directory: {NOVELS_DIR}')
    
    init_db()
    print('Database initialized')
    
    conn = sqlite3.connect(DB_PATH)
    cursor = conn.cursor()
    cursor.execute('SELECT COUNT(*) FROM novels')
    count = cursor.fetchone()[0]
    conn.close()
    
    print(f'Found {count} novels')
    print('Server starting on http://localhost:5000')
    app.run(host='0.0.0.0', port=5000, debug=True)