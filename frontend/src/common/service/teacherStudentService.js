import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/',
    withCredentials: true, // 允许跨域携带 Cookie
});

// 获取教师的学生列表
export const getTeacherStudents = async (teacherId) => {
    try {
        const response = await api.get(`/teacher-student/teacher/${teacherId}/students`);
        return response.data;
    } catch (error) {
        console.error('获取教师学生列表失败:', error);
        throw error;
    }
};

// 保存教师学生关系
export const saveTeacherStudentRelation = async (teacherId, studentIds) => {
    try {
        const response = await api.post('/teacher-student/save', {
            teacherId: teacherId,
            studentIds: studentIds
        });
        return response.data;
    } catch (error) {
        console.error('保存教师学生关系失败:', error);
        throw error;
    }
}; 