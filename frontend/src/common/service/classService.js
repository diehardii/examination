import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/',
    withCredentials: true,
});

export const fetchClasses = async () => {
    const res = await api.get('/classes');
    return res.data;
};

export const fetchClassesByStage = async (stageId) => {
    const res = await api.get(`/classes/by-stage/${stageId}`);
    return res.data;
};

export const createClass = async (payload) => {
    const res = await api.post('/classes', payload);
    return res.data;
};

export const updateClass = async (payload) => {
    await api.put('/classes', payload);
};

export const deleteClass = async (classId) => {
    await api.delete(`/classes/${classId}`);
};
