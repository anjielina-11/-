const API_BASE = 'http://localhost:3000/api';

async function loadArticles() {
  try {
    const res = await fetch(`${API_BASE}/articles`);
    const json = await res.json();
    const list = document.getElementById('articleList');
    if (json.code !== 200 || !json.data.length) {
      list.innerHTML = '<div class="empty-state" style="grid-column:span 12"><p>暂无文章</p><span>去管理后台发布第一篇吧</span></div>';
      return;
    }
    list.innerHTML = json.data.map((article, i) => {
      const style = i === 0 ? 'featured' : (i < 3 ? 'standard' : 'compact');
      return `
      <article class="article-card ${style}" onclick="location.href='detail.html?id=${article.id}'">
        <div class="card-body">
          <span class="card-category">${escapeHtml(article.category || '未分类')}</span>
          <h2 class="card-title">${escapeHtml(article.title)}</h2>
          <p class="card-excerpt">${escapeHtml(article.content)}</p>
          <div class="card-meta">
            <span class="card-date">${formatTime(article.create_time)}</span>
            <a href="detail.html?id=${article.id}" class="card-read-more" onclick="event.stopPropagation()">Read More &rarr;</a>
          </div>
        </div>
      </article>`;
    }).join('');
  } catch (err) {
    document.getElementById('articleList').innerHTML =
      '<div class="empty-state" style="grid-column:span 12"><p>加载失败</p><span>请确认后端服务已启动</span></div>';
  }
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
  return `${months[d.getMonth()]} ${d.getDate()}, ${d.getFullYear()}`;
}

loadArticles();
