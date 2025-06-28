// src/api/menu.js
import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api',
    withCredentials: true // 允许跨域携带 Cookie
});

export default {
    getMenuItems() {
        return api.get('/menu');
    }
};