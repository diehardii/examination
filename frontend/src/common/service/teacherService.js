import axios from 'axios';

const api = axios.create({
  baseURL: 'http://localhost:8080/api/',
  withCredentials: true,
});

export const fetchTeachers = async () => {
  const res = await api.get('/teachers');
  return res.data;
};

export const fetchTeacherClassesByStage = async (teacherId, stageId) => {
  const res = await api.get(`/teachers/${teacherId}/stages/${stageId}/classes`);
  return res.data;
};

export const assignTeacherClassesByStage = async (teacherId, stageId, classIds) => {
  await api.post(`/teachers/${teacherId}/stages/${stageId}/classes`, { classIds });
};

export const fetchTeacherAllAssignments = async (teacherId) => {
  const res = await api.get(`/teachers/${teacherId}/assignments`);
  return res.data;
};
