const API_BASE = 'http://localhost:3000/api';

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function formatTime(dt) {
  if (!dt) return '';
  const d = new Date(dt);
  const months = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];
  return `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
}

function showToast(message, type = 'success') {
  const toast = document.createElement('div');
  toast.className = `toast toast-${type}`;
  toast.textContent = message;
  document.body.appendChild(toast);
  setTimeout(() => toast.remove(), 3000);
}

async function loadArticles() {
  try {
    const res = await fetch(`${API_BASE}/articles`);
    const json = await res.json();
    const list = document.getElementById('manageList');
    if (json.code !== 200 || !json.data.length) {
      list.innerHTML = '<div class="empty-state"><p>暂无文章</p><span>在左侧发布第一篇吧</span></div>';
      return;
    }
    list.innerHTML = json.data.map(article => `
      <div class="manage-item">
        <div class="manage-item-info">
          <div class="manage-item-title">${escapeHtml(article.title)}</div>
          <div class="manage-item-meta"><span class="tag">${escapeHtml(article.category || '未分类')}</span>${formatTime(article.create_time)}</div>
        </div>
        <button class="btn btn-danger" onclick="deleteArticle(${article.id})">Delete</button>
      </div>
    `).join('');
  } catch (err) {
    document.getElementById('manageList').innerHTML =
      '<div class="empty-state"><p>加载失败</p><span>请确认后端服务已启动</span></div>';
  }
}

async function deleteArticle(id) {
  if (!confirm('确定要删除这篇文章吗？')) return;
  try {
    const res = await fetch(`${API_BASE}/articles/${id}`, { method: 'DELETE' });
    const json = await res.json();
    if (json.code === 200) {
      showToast('删除成功');
      loadArticles();
    } else {
      showToast(json.message || '删除失败', 'error');
    }
  } catch (err) {
    showToast('删除失败', 'error');
  }
}

document.getElementById('articleForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  const title = document.getElementById('title').value.trim();
  const category = document.getElementById('category').value.trim();
  const content = document.getElementById('content').value.trim();
  if (!title || !content) {
    showToast('标题和内容不能为空', 'error');
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/articles`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ title, category, content })
    });
    const json = await res.json();
    if (json.code === 200) {
      showToast('发布成功');
      document.getElementById('articleForm').reset();
      loadArticles();
    } else {
      showToast(json.message || '发布失败', 'error');
    }
  } catch (err) {
    showToast('发布失败', 'error');
  }
});

loadArticles();
