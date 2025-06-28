import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/exam-paper/',
    withCredentials: true, // 允许跨域携带 Cookie
});

export default {
    getSubjects() {
        return api.get(`/subjects`);
    },
    genPaper(data) {
        return api.post(`/generate`, data);
    },
    getPaper(id) {
        return api.get(`/${id}`);
    }

};