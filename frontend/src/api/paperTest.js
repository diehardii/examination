import axios from 'axios';
const api = axios.create({
    baseURL: 'http://localhost:8080/api/exam-paper/',
    withCredentials: true, // 允许跨域携带 Cookie
});
export default {
// 新增试卷管理相关API
    getPaperQuestions(paperId) {
        return api.get(`/question-answer/${paperId}`);
    },
    submitAnswers(data) {
        return api.post(`/submit-answer`, data);
    },

};




