const API_BASE = 'http://localhost:3000/api';

function getId() {
  return new URLSearchParams(location.search).get('id');
}

function escapeHtml(str) {
  const div = document.createElement('div');
  div.textContent = str;
  return div.innerHTML;
}

function formatTime(dt) {
  if (!dt) return '';
  const d = new Date(dt);
  const months = ['JAN','FEB','MAR','APR','MAY','JUN','JUL','AUG','SEP','OCT','NOV','DEC'];
  return `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()} at ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`;
}

async function loadArticle() {
  const id = getId();
  const header = document.getElementById('detailHeader');
  const content = document.getElementById('detailContent');
  if (!id) {
    header.innerHTML = '<div class="empty-state"><p>缺少文章ID</p></div>';
    return;
  }
  try {
    const res = await fetch(`${API_BASE}/articles/${id}`);
    const json = await res.json();
    if (json.code !== 200) {
      header.innerHTML = '<div class="empty-state"><p>文章不存在</p></div>';
      return;
    }
    const a = json.data;
    document.title = `${a.title} - MyBlog`;
    header.innerHTML = `
      <a href="index.html" class="back-link">&larr; Back to Articles</a>
      <span class="detail-category">${escapeHtml(a.category || '未分类')}</span>
      <h1 class="detail-title">${escapeHtml(a.title)}</h1>
      <div class="detail-byline">
        <span>MyBlog Editorial</span>
        <span class="sep"></span>
        <span>${formatTime(a.create_time)}</span>
      </div>
    `;
    content.textContent = a.content;
  } catch (err) {
    header.innerHTML = '<div class="empty-state"><p>加载失败</p><span>请确认后端服务已启动</span></div>';
  }
}

loadArticle();
