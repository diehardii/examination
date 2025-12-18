import roleManApi from "@/common/api/roleMan.js";

// 用户角色相关服务
export const getAllUsersWithRoles = async () => {
    try {
        const response = await roleManApi.getAllUsersWithRoles();
        console.log(response.data);
        if (response.data.success) {
            return response.data.data;
        }
        throw new Error(response.data.message || '获取用户列表失败');
    } catch (error) {
        throw new Error('获取用户列表失败: ' + error.message);
    }
};

export const getAllRoles = async () => {
    try {
        const response = await roleManApi.getAllRoles();
        if (response.data.success) {
            return response.data.data;
        }
        throw new Error(response.data.message || '获取角色列表失败');
    } catch (error) {
        throw new Error('获取角色列表失败: ' + error.message);
    }
};

export const updateUserRole = async (phone, roleId) => {
    try {
        const response = await roleManApi.updateUserRole({
            phone,
            roleId
        });
        if (response.data.success) {
            return response.data;
        }
        throw new Error(response.data.message || '更新角色失败');
    } catch (error) {
        throw new Error('更新角色失败: ' + error.message);
    }
};