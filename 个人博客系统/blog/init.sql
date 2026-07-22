CREATE DATABASE IF NOT EXISTS my_blog;

USE my_blog;

CREATE TABLE articles (
  id INT PRIMARY KEY AUTO_INCREMENT,
  title VARCHAR(255) NOT NULL,
  content TEXT NOT NULL,
  category VARCHAR(100),
  create_time DATETIME DEFAULT NOW()
);

INSERT INTO articles (title, content, category) VALUES ('欢迎来到我的前后端博客', '这是一套完整的前后端分离博客系统，纯HTML+CSS+JS+Node.js+MySQL实现，界面美观、功能完整！', '日常');
