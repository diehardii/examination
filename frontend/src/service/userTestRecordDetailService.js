import axios from 'axios'

const api = axios.create({
    baseURL: 'http://localhost:8080/api/exam-paper',
    withCredentials: true, // 允许跨域携带 Cookie
});



export const getUserTestRecordsByExamPaperId = async (userId, examPaperId) => {
    const response = await api.get(`/test-records/${userId}/${examPaperId}`)
    return response.data
}

export const getTestRecordDetails = async (testId) => {
    const response = await api.get(`/test-record-details/${testId}`)
    return response.data
}