import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/subject',
    withCredentials: true, // 允许跨域携带 Cookie
});

export default {


    // 添加科目
    addSubject(subjectName) {
        return api.post('/', subjectName);
    },

    // 更新科目
    updateSubject(oldName, newSubject) {
        console.log(newSubject);
        console.log(oldName);
        return api.put(`/${oldName}`, newSubject);
    },

    // 删除科目
    deleteSubject(subjectName) {
        return api.delete(`/${subjectName}`);
    }
}