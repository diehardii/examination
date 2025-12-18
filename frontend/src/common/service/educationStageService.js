import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/',
    withCredentials: true,
});

export const fetchStages = async () => {
    const res = await api.get('/education-stages');
    return res.data;
};
