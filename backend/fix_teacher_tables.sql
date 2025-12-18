-- Teachers table
CREATE TABLE IF NOT EXISTS teachers (
    teacher_id BIGINT PRIMARY KEY,
    stage_id INT NOT NULL,
    CONSTRAINT fk_teachers_user FOREIGN KEY (teacher_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_teachers_stage FOREIGN KEY (stage_id) REFERENCES education_stages(stage_id)
);

-- Teacher-class link table (existing schema uses teaching_assignments)
CREATE TABLE IF NOT EXISTS teaching_assignments (
    assignment_id INT PRIMARY KEY AUTO_INCREMENT,
    teacher_id BIGINT NOT NULL,
    class_id INT NOT NULL,
    CONSTRAINT fk_teaching_assignments_teacher FOREIGN KEY (teacher_id) REFERENCES teachers(teacher_id) ON DELETE CASCADE,
    CONSTRAINT fk_teaching_assignments_class FOREIGN KEY (class_id) REFERENCES classes(class_id) ON DELETE CASCADE
);
