import paperAnaApi from "@/api/paperAna.js";
import questionAnaApi from "@/api/questionAna.js";



/**
 * 获取用户考试记录
 * @param {number} userId - 用户ID
 * @returns {Promise<Array>} 用户考试记录列表
 */
export const getUserTestRecords = async (userId) => {
    try {
        const response = await questionAnaApi.getUserTestRecords(userId);
        return response.data;
    } catch (error) {
        throw new Error('获取用户考试记录失败');
    }
};

export const getExamRecords = async (subject) => {
    try {
        const response = await paperAnaApi.getExamRecords(subject);
        return response.data;
    } catch (error) {
        throw new Error('获取考试记录失败');
    }
};

export const analyzeSubject = async (subject) => {
    try {
        const response = await paperAnaApi.analyzeSubject(subject);
        return response.data;
    } catch (error) {
        throw new Error('科目分析失败');
    }
};