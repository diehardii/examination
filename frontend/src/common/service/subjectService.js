import axios from 'axios';
import paperGenApi from "@/cet4/api/CET4paperGen.js";
import subjectApi from "@/common/api/subject.js";
const api = axios.create({
    baseURL: 'http://localhost:8080/api/',
    withCredentials: true
});

// 英文科目相关服务（仅支持 subjects_en 表）
export const getSubjectsEn = async () => {
    try {
        const response = await paperGenApi.getSubjectsEn();
        console.log('[getSubjectsEn] subjects_en 返回数据:', response.data);
        return response.data;
    } catch (error) {
        console.error('[getSubjectsEn] 调用 subjects_en/list 失败', error);
        throw new Error('获取科目列表失败');
    }
};

// 添加科目
export const addSubject = async (subjectName) => {
    try {
        const response = await subjectApi.addSubject(subjectName);
        return response.data;
    } catch (error) {
        throw new Error(error.response?.data || '添加科目失败');
    }
};

// 更新科目
export const updateSubject = async (oldName, newName) => {
    try {
        const response = await subjectApi.updateSubject(oldName, { subjectName: newName });
        return response.data;
    } catch (error) {
        throw new Error(error.response?.data || '更新科目失败');
    }
};

// 删除科目
export const deleteSubject = async (subjectName) => {
    try {
        const response = await subjectApi.deleteSubject(subjectName);
        return response.data;
    } catch (error) {
        throw new Error(error.response?.data || '删除科目失败');
    }
};

export const getSubjectsForTeacher = async (teacherId) => {
    try {
        const response = await api.get(`/subject/teacher/${teacherId}`);
        return response.data;
    } catch (error) {
        throw new Error('获取教师科目列表失败');
    }
};

export const getSubjectByName = async (subjectName) => {
    try {
        const response = await api.get(`/subject/name/${subjectName}`);
        return response.data;
    } catch (error) {
        throw new Error('根据科目名称获取科目信息失败');
    }
};

