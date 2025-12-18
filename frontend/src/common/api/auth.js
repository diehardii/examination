import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api',
    withCredentials: true, // 允许跨域携带 Cookie
});

export default {
    login(credentials) {
        return api.post('/auth/login', credentials);
    },
    register(userInfo) {
        return api.post('/auth/register', {
            username: userInfo.username,
            password: userInfo.password,
            phone: userInfo.phone
        }, {
            validateStatus: function (status) {
                // 处理 400 等状态码，不抛出错误
                return status < 500;
            }
        });
    },
    getCurrentUser() {
        return api.get('/user/me');
    }


};

