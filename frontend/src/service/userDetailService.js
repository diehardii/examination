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