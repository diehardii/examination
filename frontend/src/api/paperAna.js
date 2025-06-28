import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/exam-paper/',
    withCredentials: true, // 允许跨域携带 Cookie
});

export default {
    // 获取用户所有科目
    getUserSubjects() {
        return api.get('/user-subjects');
    },

    // 获取指定科目的考试记录
    getExamRecords(subject) {
        return api.get('/records', {
            params: { subject }
        });
    },

    // 分析指定科目
    analyzeSubject(subject) {
        return api.post('/analyze', null, {
            params: { subject }
        });
    },

};