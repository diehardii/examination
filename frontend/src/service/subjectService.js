
import paperGenApi from "@/api/paperGen.js";
import paperAnaApi from "@/api/paperAna.js";
import subjectApi from "@/api/subject.js";

// 科目相关服务
export const getUserSubjects = async () => {
    try {
        const response = await paperAnaApi.getUserSubjects();
        return response.data;
    } catch (error) {
        throw new Error('获取用户科目列表失败');
    }
};



export const getSubjects = async () => {
    try {
        const response = await paperGenApi.getSubjects();
        console.log(response.data)// 使用 auth.js 的封装;
        return response.data;
    } catch (error) {
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
