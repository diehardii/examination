import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/role-management/',
    withCredentials: true, // 允许跨域携带 Cookie
});

export default {
    // 获取所有用户及其角色
    getAllUsersWithRoles() {
        return api.get('/users');
    },

    // 获取所有角色
    getAllRoles() {
        return api.get('/roles');
    },

    // 更新用户角色
    updateUserRole(payload) {
        return api.post('/update-role', payload);
    }
};