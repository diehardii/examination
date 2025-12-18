import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/api/student-class';

const studentClassService = {
    // 获取所有学生及其班级信息
    async getAllStudents() {
        try {
            const response = await axios.get(`${API_BASE_URL}/students`);
            return response.data;
        } catch (error) {
            console.error('Error fetching students:', error);
            throw error;
        }
    },

    // 获取未分配班级的学生
    async getUnassignedStudents() {
        try {
            const response = await axios.get(`${API_BASE_URL}/students/unassigned`);
            return response.data;
        } catch (error) {
            console.error('Error fetching unassigned students:', error);
            throw error;
        }
    },

    // 获取学生的班级信息
    async getStudentClassInfo(studentId) {
        try {
            const response = await axios.get(`${API_BASE_URL}/students/${studentId}`);
            return response.data;
        } catch (error) {
            console.error('Error fetching student class info:', error);
            throw error;
        }
    },

    // 为学生分配班级
    async assignClassToStudent(studentId, classId) {
        try {
            const response = await axios.post(
                `${API_BASE_URL}/students/${studentId}/assign`,
                { classId }
            );
            return response.data;
        } catch (error) {
            console.error('Error assigning class to student:', error);
            throw error;
        }
    },

    // 获取所有班级及其学生统计
    async getAllClassesWithStudentCount() {
        try {
            const response = await axios.get(`${API_BASE_URL}/classes`);
            return response.data;
        } catch (error) {
            console.error('Error fetching classes with student count:', error);
            throw error;
        }
    },

    // 获取班级的学生列表
    async getClassStudents(classId) {
        try {
            const response = await axios.get(`${API_BASE_URL}/classes/${classId}/students`);
            return response.data;
        } catch (error) {
            console.error('Error fetching class students:', error);
            throw error;
        }
    },

    // 批量为学生分配班级
    async batchAssignStudentsToClass(classId, studentIds) {
        try {
            const response = await axios.post(
                `${API_BASE_URL}/classes/${classId}/assign-students`,
                { studentIds }
            );
            return response.data;
        } catch (error) {
            console.error('Error batch assigning students to class:', error);
            throw error;
        }
    },

    // 移除学生的班级分配
    async removeStudentFromClass(studentId) {
        try {
            const response = await axios.delete(`${API_BASE_URL}/students/${studentId}/remove`);
            return response.data;
        } catch (error) {
            console.error('Error removing student from class:', error);
            throw error;
        }
    }
};

export default studentClassService;
