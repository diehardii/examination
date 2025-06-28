import paperTestApi from "@/api/paperTest.js";
import questionAnaApi from "@/api/questionAna.js";


export const getPaperQuestions = async (paperId) => {
    try {
        const response = await paperTestApi.getPaperQuestions(paperId);
        return response.data;
    } catch (error) {
        throw new Error( '获取试卷对应试题失败');
    }
};

export const submitAnswers = async (data) => {
    try {
        const response = await paperTestApi.submitAnswers(data);
        return response.data;
    } catch (error) {
        throw new Error( '获取答题结果出错');
    }
};
/**
 * 获取带答案的题目列表
 * @param {number} testRecordId - 考试记录ID
 * @param {number} userId - 用户ID
 * @returns {Promise<Array>} 题目列表(包含答案)
 */
export const getQuestionsWithAnswers = async (testRecordId, userId) => {
    try {
        const response = await questionAnaApi.getQuestionsWithAnswers(testRecordId, userId);
        return response.data;
    } catch (error) {
        throw new Error('获取题目详情失败');
    }
};

/**
 * 分析题目
 * @param {number} testRecordId - 考试记录ID
 * @param {number} questionNumber - 题目编号
 * @param {number} userId - 用户ID
 * @returns {Promise<Object>} 分析结果
 */
export const analyzeQuestion = async (testRecordId, questionNumber, userId) => {
    try {
        const response = await questionAnaApi.analyzeQuestion(testRecordId, questionNumber, userId);
        return response.data;
    } catch (error) {
        throw new Error('题目分析失败');
    }
};


/**
 * 格式化日期时间
 * @param {string} dateTime - 日期时间字符串
 * @returns {string} 格式化后的日期时间
 */
export const formatDateTime = (dateTime) => {
    if (!dateTime) return '';
    const date = new Date(dateTime);
    return date.toLocaleString();
};

