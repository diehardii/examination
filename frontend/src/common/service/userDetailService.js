// 文件位置: frontend/src/service/userDetailService.js
import axios from 'axios';
const api = axios.create({
    baseURL: 'http://localhost:8080/api/',
    withCredentials: true, // 允许跨域携带 Cookie
});
export const getUserDetail = (userId) => {
    return api.get('/user-detail', { params: { userId } });
};

export const saveUserDetail = (userDetail) => {
    return api.post('/user-detail', userDetail);
};

export const getAllUsers = async () => {
    try {
        const response = await api.get('/user-detail/all');
        return response.data;
    } catch (error) {
        console.error('获取用户列表失败:', error);
        return [];
    }
};

// 获取学段列表
export const getEducationStages = () => {
    return api.get('/education-stages');
};

// 获取年级列表（可选按学段过滤）
export const getGrades = (stageId) => {
    if (stageId) {
        return api.get('/grades', { params: { stageId } });
    }
    return api.get('/grades');
};

// 获取班级列表
export const getClasses = () => {
    return api.get('/classes');
};

// 根据学段获取班级
export const getClassesByStage = (stageId) => {
    return api.get(`/classes/by-stage/${stageId}`);
};