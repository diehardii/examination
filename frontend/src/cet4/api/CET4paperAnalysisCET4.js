import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/cet4/paper-analysis',
  withCredentials: true,
  // 解析耗时很长，放宽超时为 1 小时
  timeout: 3600000,
});

const apiConcurrent = axios.create({
  baseURL: 'http://localhost:8080/api/cet4/paper-analysis-concurrent',
  withCredentials: true,
  timeout: 3600000,
});

export default {
  parsePdfConcurrent(formData, params = {}) {
    return apiConcurrent.post('/upload', formData, {
      params,
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      timeout: 3600000,
    });
  },
  parsePlainConcurrent(formData) {
    return apiConcurrent.post('/plain', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      timeout: 3600000,
    });
  },
  parseImageConcurrent(formData) {
    return apiConcurrent.post('/image', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      timeout: 3600000,
    });
  },
  submitTask(formData, params = {}) {
    return api.post('/tasks', formData, {
      params,
      headers: {
        'Content-Type': 'multipart/form-data',
      },
      timeout: 3600000,
    });
  },
  listTasks(limit = 20) {
    return api.get('/tasks', { params: { limit } });
  },
  getTask(taskId) {
    return api.get(`/tasks/${taskId}`);
  },
  getTaskResult(taskId) {
    return api.get(`/tasks/${taskId}/result`);
  },
  deleteTask(taskId) {
    return api.delete(`/tasks/${taskId}`);
  },
  querySectionA(params) {
    return api.get('/section-a/chroma', { params });
  },
  storeSectionA(payload) {
    return api.post('/section-a/chroma', payload);
  },
  storeSectionB(payload) {
    return api.post('/section-b/chroma', payload);
  },
  storeSectionC(payload) {
    return api.post('/section-c/chroma', payload);
  },
  storeWriting(payload) {
    return api.post('/writing/chroma', payload);
  },
  storeTranslation(payload) {
    return api.post('/translation/chroma', payload);
  },
  storePart2AB(payload) {
    return api.post('/part2/ab/chroma', payload);
  },
  storePart2C(payload) {
    return api.post('/part2/c/chroma', payload);
  },
  queryExamPaperUnits(params) {
    return api.get('/exam-paper/units', { params });
  },
};
