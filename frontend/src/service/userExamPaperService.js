import paperManApi from "@/api/paperMan.js";


export const getAvailableExamPapers = async (username) => {
    try {
        const response = await paperManApi.getAvailablePapers(username);
        return response.data;
    } catch (error) {
        throw new Error(error.response?.data?.error || '获取可用试卷失败');
    }
};

export const getUserExamPapers = async (username) => {
    try {
        const response = await paperManApi.getUserPapers(username);
        return response.data;
    } catch (error) {
        throw new Error(error.response?.data?.error || '获取用户试卷失败');
    }
};

export const saveUserExamPapers = async (username, examPaperIds) => {
    try {
        await paperManApi.saveUserPapers(username, examPaperIds);
    } catch (error) {
        throw new Error(error.response?.data?.error || '保存用户试卷失败');
    }
};