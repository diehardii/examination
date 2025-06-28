// 文件位置: frontend/src/service/universityService.js
import axios from 'axios';
const api = axios.create({
    baseURL: 'http://localhost:8080/api/',
    withCredentials: true, // 允许跨域携带 Cookie
});
/**
 * 获取所有大学列表
 * @returns {Promise<Array>} 大学数组
 */
// src/services/universityService.js
export const getUniversities = async () => {
    try {
        const response = await api.get('/universities');
        console.log('API响应数据:', response.data); // 确认数据结构

        // 确保处理可能的null/undefined
        return response.data?.map(univ => ({
            universityId: univ.universityId,
            universityName: univ.universityName,
            departments: univ.departments || null // 显式设置为null
        })) || [];
    } catch (error) {
        console.error('获取大学列表失败:', error);
        return [];
    }
};
/**
 * 根据ID获取大学详情
 * @param {number} id 大学ID
 * @returns {Promise<Object>} 大学对象
 */
export const getUniversityById = (id) => {
    return api.get(`/universities/${id}`)
        .then(response => {
            if (response.status === 200) {
                return response.data;
            }
            throw new Error('获取大学详情失败');
        });
};

/**
 * 创建新大学
 * @param {Object} university 大学数据 { universityName }
 * @returns {Promise<Object>} 创建后的大学数据（含ID）
 */
export const createUniversity = (university) => {
    return api.post('/universities', university)
        .then(response => response.data);
};


/**
 * 更新大学信息
 * @param {number} id 大学ID
 * @param {Object} university 更新的大学数据 { universityName }
 * @returns {Promise<Object>} 更新后的大学数据
 */
export const updateUniversity = (id, university) => {
    console.log("begin update"+university);
    console.log("id"+id);
    return api.put(`/universities/${id}`, university)
        .then(response => {
            if (response.status === 200) {
                return response.data;
            }
            throw new Error('更新大学信息失败');
        });
};

/**
 * 删除大学
 * @param {number} id 大学ID
 * @returns {Promise<Object>} 删除操作结果
 */
export const deleteUniversity = (id) => {
    return api.delete(`/universities/${id}`)
        .then(response => {
            if (response.status === 200) {
                return response.data;
            }
            throw new Error('删除大学失败');
        });
};
// 可选：其他操作
export const universityService = {
    getUniversities,
    getUniversityById,
    createUniversity,
    updateUniversity,
    deleteUniversity
};