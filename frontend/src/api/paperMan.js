import axios from 'axios';
const api = axios.create({
    baseURL: 'http://localhost:8080/api/exam-paper/',
    withCredentials: true, // 允许跨域携带 Cookie
});
export default {
// 新增试卷管理相关API
getAvailablePapers(username) {
    return api.get(`/available`, { params: { username } });
},
getUserPapers(username) {
    return api.get(`/user`, { params: { username } });
},
saveUserPapers(username, examPaperIds) {
    return api.post(`/save`, examPaperIds, { params: { username } });
}
};