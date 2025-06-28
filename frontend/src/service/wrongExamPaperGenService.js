import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8080/api/wrong-exam-paper',
    withCredentials: true, // 允许跨域携带 Cookie
});


export const getWrongQuestions = async (subjectId, userId) => {
    const response = await api.get(`/${subjectId}/${userId}`)
    return response.data
}

export const generateWrongExamPaper = async (data) => {
    const response = await api.post(`/generate-wrong-exam-paper`, data)
    return response.data
}