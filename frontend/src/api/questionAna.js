// src/api/questionAnalysis.js
import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/question-analysis/',
    withCredentials: true, // 允许跨域携带 Cookie
});

export default {
    // 获取用户考试记录
    getUserTestRecords(userId) {
        return api.get(`user-test-records/${userId}`);
    },

    // 获取带答案的题目列表
    getQuestionsWithAnswers(testRecordId, userId) {
        return api.get(`questions-with-answers/${testRecordId}/${userId}`);
    },

    // 分析题目
    analyzeQuestion(testRecordId, questionNumber, userId) {
        return api.post(`analyze`, null, {
            params: {
                userTestRecordId: testRecordId,
                questionNumber: questionNumber,
                userId: userId
            }
        });
    }
};