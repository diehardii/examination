import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8080/api/auth',
    withCredentials: true, // 允许跨域携带 Cookie
});



export const changePassword = async (passwordData) => {
    const response = await api.post('/change-password', passwordData);
    return response.data;
}

// 可以根据需要添加其他认证相关的方法
// export const logout = async () => {
//     const response = await api.post('/logout');
//     return response.data;
// }