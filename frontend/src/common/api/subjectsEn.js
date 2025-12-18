import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8080/api/',
    withCredentials: true, // 允许跨域携带 Cookie
});

export const subjectsEnApi = {
    // 获取所有科目列表
    getAllSubjects: async () => {
        const response = await api.get('/subjects-en/list')
        return response.data
    },

    // 根据ID获取科目
    getSubjectById: async (id) => {
        const response = await api.get(`/subjects-en/${id}`)
        return response.data
    },

    // 根据名称获取科目
    getSubjectByName: async (name) => {
        const response = await api.get(`/subjects-en/name/${name}`)
        return response.data
    }
}