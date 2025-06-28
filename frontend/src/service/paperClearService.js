import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api',
    withCredentials: true, // 如果需要发送 cookies
    headers: {
        'Content-Type': 'application/json'
    }
});

export const fetchAllExamPapers = async () => {
    try {
        const response = await api.get('/paper-clear');
        return response.data;
    } catch (error) {
        console.error('Error fetching exam papers:', error);
        throw error;
    }
};

export const fetchUsersForExamPaper = async (examPaperId) => {
    try {
        const response = await api.get(`/paper-clear/${examPaperId}/users`);
        return response.data;
    } catch (error) {
        console.error('Error fetching users for exam paper:', error);
        throw error;
    }
};

export const deleteExamPaper = async (examPaperId) => {
    try {
        const response = await api.delete(`/paper-clear/${examPaperId}`);
        return response.data;
    } catch (error) {
        console.error('Error deleting exam paper:', error);
        throw error;
    }
};