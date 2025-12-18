import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/exam-paper/',
    withCredentials: true, // 允许跨域携带 Cookie
});

export default {
    // 获取英文科目列表（subjects_en 表）
    getSubjectsEn() {
        // 直接访问后端英文科目端点（不走 exam-paper 前缀）
        return axios.get('http://localhost:8080/api/subjects-en/list', { withCredentials: true });
    },
    genPaper(data) {
        return api.post(`/generate`, data);
    },
    getPaper(id) {
        return api.get(`/${id}`);
    },
    getPaperGenInfo(paperIds) {
        return api.post(`/gen-info`, { paperIds });
    },
    queryExamPapers(params) {
        return api.post(`/query`, params);
    },
    exportExamPapers(params) {
        return api.post(`/export`, params, { responseType: 'blob' });
    }

};