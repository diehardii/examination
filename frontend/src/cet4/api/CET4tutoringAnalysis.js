import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/cet4/tutoring-analysis',
  withCredentials: true,
  timeout: 20000,
});

export const triggerHomepageAnalysis = () => api.post('/homepage');
