-- ============================================
-- 教师表重新设计方案 - MySQL版本
-- 创建日期：2025-12-10
-- 说明：删除teachers表，直接通过teaching_assignments管理老师-学段-班级关系
-- 数据库：MySQL 5.7+
-- ============================================

-- 1. 删除旧的teachers表（已删除）
-- teachers表已被删除，不再需要独立的教师表

-- 2. teaching_assignments表（已修改，直接管理教师-学段-班级关系）
-- 新的teaching_assignments表结构：
CREATE TABLE IF NOT EXISTS `teaching_assignments` (
    `assignment_id` INT NOT NULL AUTO_INCREMENT COMMENT '分配ID',
    `teacher_id` BIGINT NOT NULL COMMENT '教师ID，关联users表',
    `stage_id` INT NOT NULL COMMENT '学段ID，教师所属学段',
    `class_id` INT NOT NULL COMMENT '班级ID',
    `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    PRIMARY KEY (`assignment_id`),
    UNIQUE KEY `uk_teacher_class` (`teacher_id`, `class_id`),
    KEY `idx_teacher_id` (`teacher_id`),
    KEY `idx_stage_id` (`stage_id`),
    KEY `idx_class_id` (`class_id`),
    CONSTRAINT `fk_teaching_assignments_teacher` FOREIGN KEY (`teacher_id`) REFERENCES `users`(`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_teaching_assignments_stage` FOREIGN KEY (`stage_id`) REFERENCES `education_stages`(`stage_id`) ON DELETE CASCADE,
    CONSTRAINT `fk_teaching_assignments_class` FOREIGN KEY (`class_id`) REFERENCES `classes`(`class_id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='教师班级分配表（包含学段信息）';

-- 3. 创建触发器：验证老师学段和班级学段匹配
DELIMITER $$

DROP TRIGGER IF EXISTS `check_stage_match_before_insert`$$
CREATE TRIGGER `check_stage_match_before_insert`
BEFORE INSERT ON `teaching_assignments`
FOR EACH ROW
BEGIN
    DECLARE class_stage_id INT;
    
    -- 获取班级所属的学段
    SELECT c.stage_id INTO class_stage_id
    FROM classes c
    INNER JOIN grades g ON c.grade_id = g.grade_id
    WHERE c.class_id = NEW.class_id;
    
    -- 验证班级学段是否与分配的学段匹配
    IF class_stage_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = '班级不存在或学段信息不完整';
    END IF;
    
    IF class_stage_id != NEW.stage_id THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = '教师学段与班级学段不匹配';
    END IF;
END$$

DROP TRIGGER IF EXISTS `check_stage_match_before_update`$$
CREATE TRIGGER `check_stage_match_before_update`
BEFORE UPDATE ON `teaching_assignments`
FOR EACH ROW
BEGIN
    DECLARE class_stage_id INT;
    
    -- 获取班级所属的学段
    SELECT c.stage_id INTO class_stage_id
    FROM classes c
    INNER JOIN grades g ON c.grade_id = g.grade_id
    WHERE c.class_id = NEW.class_id;
    
    -- 验证班级学段是否与分配的学段匹配
    IF class_stage_id IS NULL THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = '班级不存在或学段信息不完整';
    END IF;
    
    IF class_stage_id != NEW.stage_id THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = '教师学段与班级学段不匹配';
    END IF;
END$$

DELIMITER ;

-- ============================================
-- 数据迁移示例（如果有旧数据需要迁移）
-- ============================================

-- 步骤1：从旧备份表中获取所有教师ID，插入到新的teachers表
-- INSERT INTO teachers (teacher_id)
-- SELECT DISTINCT teacher_id FROM teachers_backup_20251210;

-- 步骤2：将旧表中的学段关系迁移到teacher_stages表
-- INSERT INTO teacher_stages (teacher_id, stage_id, is_primary)
-- SELECT teacher_id, stage_id, 1 
-- FROM teachers_backup_20251210 
-- WHERE stage_id IS NOT NULL
-- ON DUPLICATE KEY UPDATE is_primary=1;

-- 步骤3：如果teaching_assignments表的外键约束有问题，需要重建
-- ALTER TABLE teaching_assignments DROP FOREIGN KEY fk_teaching_assignments_teacher;
-- ALTER TABLE teaching_assignments ADD CONSTRAINT fk_teaching_assignments_teacher 
--     FOREIGN KEY (teacher_id) REFERENCES teachers(teacher_id) ON DELETE CASCADE;

-- ============================================
-- 索引优化建议
-- ============================================
-- teacher_stages表已经包含了必要的索引：
-- 1. 主键索引（id）
-- 2. 唯一索引（teacher_id, stage_id）- 防止重复关联
-- 3. teacher_id索引 - 优化按教师查询
-- 4. stage_id索引 - 优化按学段查询

-- ============================================
-- 使用示例
-- ============================================

-- 示例1：为教师添加学段
-- INSERT INTO teacher_stages (teacher_id, stage_id, is_primary) VALUES (1001, 1, 1);
-- INSERT INTO teacher_stages (teacher_id, stage_id, is_primary) VALUES (1001, 2, 0);

-- 示例2：查询教师的所有学段
-- SELECT ts.teacher_id, ts.stage_id, es.display_name as stage_name, ts.is_primary
-- FROM teacher_stages ts
-- LEFT JOIN education_stages es ON ts.stage_id = es.stage_id
-- WHERE ts.teacher_id = 1001;

-- 示例3：查询某学段的所有教师
-- SELECT ts.teacher_id, u.real_name, u.username
-- FROM teacher_stages ts
-- LEFT JOIN users u ON ts.teacher_id = u.id
-- WHERE ts.stage_id = 1;

-- 示例4：删除教师的某个学段关联
-- DELETE FROM teacher_stages WHERE teacher_id = 1001 AND stage_id = 2;

-- 示例5：更新主学段
-- UPDATE teacher_stages SET is_primary = 0 WHERE teacher_id = 1001;
-- UPDATE teacher_stages SET is_primary = 1 WHERE teacher_id = 1001 AND stage_id = 3;
