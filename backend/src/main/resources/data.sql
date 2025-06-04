-- 初始化标签数据
INSERT INTO tags (name, color) VALUES 
('文档', '#4299E1'),
('笔记', '#48BB78'),
('重要', '#F56565'),
('工作', '#ED8936'),
('学习', '#9F7AEA'),
('个人', '#667EEA')
ON DUPLICATE KEY UPDATE name=name; 