import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/',
    withCredentials: true,
});

export const fetchGrades = async (stageId) => {
    const res = await api.get('/grades', { params: { stageId } });
    return res.data;
};
