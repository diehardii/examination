
import paperGenApi from "@/api/paperGen.js"; // 导入封装好的菜单API

export const generateExamPaper = async (data) => {
    try {
        const response = await paperGenApi.genPaper(data);
        console.log(response);
        return response.data;
    } catch (error) {
        throw new Error(error.response?.data?.error || '生成试卷失败');
    }
};

export const getExamPaper = async (id) => {
    try {
        const response = await paperGenApi.getPaper(id);
        return response.data;
    } catch (error) {
        throw new Error('获取试卷详情失败');
    }
};