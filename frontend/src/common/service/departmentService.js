// 文件位置: frontend/src/service/departmentService.js
import axios from 'axios';
const api = axios.create({
    baseURL: 'http://localhost:8080/api/',
    withCredentials: true, // 允许跨域携带 Cookie
});
/**
 * 获取所有院系列表
 * @returns {Promise<Array>} 院系数组
 */

/**
 * 获取所有院系列表
 * @returns {Promise<Array>} 院系数组
 */
export const getDepartments = async () => {
    try {
        const response = await api.get('/departments'); // 直接解构data
        return response.data.map(dept => ({
            departmentId: dept.departmentId,
            departmentName: dept.departmentName,
            universityId: dept.universityId || null, // 确保有默认值
            university: null // 强制设为null（如需关联数据需额外接口）
        }));
    } catch (error) {
        console.error('获取院系列表失败:', error);
        return []; // 返回空数组作为fallback
    }
};
/**
 * 根据大学ID获取所属院系
 * @param {number} universityId 大学ID
 * @returns {Promise<Array>} 院系数组
 */
/**
 * 根据大学ID获取院系
 * @param {number} universityId 大学ID
 * @returns {Promise<Array>} 院系数组
 */
export const getDepartmentsByUniversity = async (universityId) => {
    try {
        const response = await api.get(`/departments/by-university/${universityId}`);
        console.log('API响应数据department:', response.data); // 确认数据结构

        return response.data.map(dept => ({
            departmentId: dept.departmentId,
            departmentName: dept.departmentName,
            universityId: dept.universityId
            // 不返回university对象避免循环引用
        }));
    } catch (error) {
        console.error(`获取大学${universityId}的院系列表失败:`, error);
        return [];
    }
};

/**
 * 根据大学ID获取院系 (别名方法)
 * @param {number} universityId 大学ID
 * @returns {Promise<Array>} 院系数组
 */
export const getDepartmentsByUniversityId = async (universityId) => {
    return getDepartmentsByUniversity(universityId);
};

/**
 * 创建新院系
 * @param {Object} department 院系数据 { universityId, departmentName }
 * @returns {Promise<Object>} 创建后的院系数据（含ID）
 */
export const createDepartment = (department) => {
    console.log("create dep"+department.departmentName);
    console.log("create dep"+department.departmentId);
    console.log("create dep"+department.universityId);



    return api.post('/departments', department)
        .then(response => {
            if (response.status === 200) {
                return response.data;
            }
            throw new Error('新增院系信息失败');
        });
};


/**
 * 更新院系信息
 * @param {number} id 院系ID
 * @param {Object} department 更新的院系数据 { departmentName, universityId }
 * @returns {Promise<Object>} 更新后的院系数据
 */
export const updateDepartment = (id, department) => {
    return api.put(`/departments/${id}`, department)
        .then(response => {
            if (response.status === 200) {
                return response.data;
            }
            throw new Error('更新院系信息失败');
        });
};

/**
 * 删除院系
 * @param {number} id 院系ID
 * @returns {Promise<Object>} 删除操作结果
 */
export const deleteDepartment = (id) => {
    return api.delete(`/departments/${id}`)
        .then(response => {
            if (response.status === 200) {
                return response.data;
            }
            throw new Error('删除院系失败');
        });
};

// 可选：其他操作
export const departmentService = {
    getDepartments,
    getDepartmentsByUniversity,
    createDepartment,
    updateDepartment,
    deleteDepartment

};