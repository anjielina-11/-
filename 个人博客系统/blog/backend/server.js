const express = require('express');
const cors = require('cors');
const db = require('./db');

const app = express();
const PORT = 3000;

app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

app.get('/api/articles', async (req, res) => {
  try {
    const [rows] = await db.query('SELECT * FROM articles ORDER BY create_time DESC');
    res.json({ code: 200, data: rows });
  } catch (err) {
    res.status(500).json({ code: 500, message: '获取文章列表失败', error: err.message });
  }
});

app.get('/api/articles/:id', async (req, res) => {
  try {
    const [rows] = await db.query('SELECT * FROM articles WHERE id = ?', [req.params.id]);
    if (rows.length === 0) {
      return res.status(404).json({ code: 404, message: '文章不存在' });
    }
    res.json({ code: 200, data: rows[0] });
  } catch (err) {
    res.status(500).json({ code: 500, message: '获取文章详情失败', error: err.message });
  }
});

app.post('/api/articles', async (req, res) => {
  try {
    const { title, content, category } = req.body;
    if (!title || !content) {
      return res.status(400).json({ code: 400, message: '标题和内容不能为空' });
    }
    const [result] = await db.query(
      'INSERT INTO articles (title, content, category) VALUES (?, ?, ?)',
      [title, content, category || '未分类']
    );
    res.json({ code: 200, message: '发布成功', data: { id: result.insertId } });
  } catch (err) {
    res.status(500).json({ code: 500, message: '发布文章失败', error: err.message });
  }
});

app.delete('/api/articles/:id', async (req, res) => {
  try {
    const [result] = await db.query('DELETE FROM articles WHERE id = ?', [req.params.id]);
    if (result.affectedRows === 0) {
      return res.status(404).json({ code: 404, message: '文章不存在' });
    }
    res.json({ code: 200, message: '删除成功' });
  } catch (err) {
    res.status(500).json({ code: 500, message: '删除文章失败', error: err.message });
  }
});

app.listen(PORT, () => {
  console.log(`博客后端服务已启动: http://localhost:${PORT}`);
});
