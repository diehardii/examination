-- 初始化subjects_en表
CREATE TABLE IF NOT EXISTS subjects_en (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    subject_en_name VARCHAR(100) NOT NULL UNIQUE,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 插入示例数据
INSERT INTO subjects_en (subject_en_name, description) VALUES
('Mathematics', '数学课程'),
('Physics', '物理课程'),
('Chemistry', '化学课程'),
('Biology', '生物课程'),
('Computer Science', '计算机科学课程'),
('English', '英语课程'),
('History', '历史课程'),
('Geography', '地理课程');