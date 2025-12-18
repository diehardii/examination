-- 检查当前数据库中 user_test_record_en 表的结构
DESCRIBE user_test_record_en;

-- 查看当前的分数数据
SELECT test_en_id, test_en_score, correct_number, test_en_time 
FROM user_test_record_en 
ORDER BY test_en_id DESC 
LIMIT 10;

-- 如果字段类型不是 DOUBLE，修改字段类型
-- ALTER TABLE user_test_record_en MODIFY COLUMN test_en_score DOUBLE NOT NULL;

-- 查看最新的测试记录（ID=75）
SELECT * FROM user_test_record_en WHERE test_en_id = 75;

-- 查看对应的segment分数
SELECT * FROM user_test_record_segment_en WHERE test_en_id = 75;
