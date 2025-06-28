// 文件位置: frontend/src/service/paperGenFromWrongService.js
import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/',
    withCredentials: true, // 允许跨域携带 Cookie
});


export const generateExamPaperFromWrong = async (data) => {
    try {
        const response = await api.post('/exam-paper/generate-from-wrong', data);
        console.log('生成试卷API响应数据:', response.data); // 确认数据结构

        // 确保处理可能的null/undefined
        return {
            id: response.data?.id || null,
            examPaperName: response.data?.examPaperName || '',
            examPaperContent: response.data?.examPaperContent || '',
            examPaperDifficulty: response.data?.examPaperDifficulty || '自定义',
            examPaperSubject: response.data?.examPaperSubject || '',
            questions: response.data?.questions || []
        };
    } catch (error) {
        console.error('生成试卷失败:', error);
        throw error; // 保持与您示例中一致的错误处理方式
    }
};