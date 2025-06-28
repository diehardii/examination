import { defineStore } from 'pinia';
import { ref } from 'vue';
import { useRouter } from 'vue-router';
import authApi from '@/api/auth'; // 导入封装好的 API
import { SignJWT, jwtVerify } from 'jose'


export const useAuthStore = defineStore('auth', () => {
    const user = ref(null);
    const isAuthenticated = ref(false);
    const router = useRouter();


    // 生成跨系统token
    const generateCrossSystemToken = async () => {
        if (!isAuthenticated.value) return null

        const secret = new TextEncoder().encode('key-for-examination-cross-system')
        return await new SignJWT({ userId: user.value.id, timestamp: Date.now() })
            .setProtectedHeader({ alg: 'HS256' })
            .setExpirationTime('5m')
            .sign(secret)
    }

// 验证跨系统token
    const verifyCrossSystemToken = async (token) => {
        try {
            const secret = new TextEncoder().encode('key-for-examination-cross-system')
            const { payload } = await jwtVerify(token, secret)
            return payload
        } catch (error) {
            console.error('Token验证失败:', error)
            return null
        }
    }
    const fetchUser = async () => {
        try {
            const response = await authApi.getCurrentUser(); // 使用 auth.js 的封装
            user.value = response.data;
            isAuthenticated.value = true;
            return response.data;
        } catch (error) {
            user.value = null;
            isAuthenticated.value = false;
            throw error;
        }
    };

    const login = async (credentials) => {
        try {
            const response = await authApi.login(credentials); // 使用 auth.js 的封装
            user.value = response.data;
            isAuthenticated.value = true;
            // 存入localStorage
            localStorage.setItem('user', JSON.stringify(user.value));
            return response.data;
        } catch (error) {
            throw error;
        }
    };

    const logout = async () => {
        try {
            user.value = null;
            isAuthenticated.value = false;
            router.push('/login');
        } catch (error) {
            throw error;
        }
    };

    const register = async (userInfo) => {
        try {
            const response = await authApi.register(userInfo);
            return response.data;
        } catch (error) {
            // 将后端错误信息抛出，供组件捕获
            throw new Error(error.response?.data?.message || error.message || '注册失败');
        }
    };


    // Private method
    const clearAuth = () => {
        user.value = null;
        isAuthenticated.value = false;
        localStorage.removeItem('user');
    };

    return {
        user,
        isAuthenticated,
        fetchUser,
        login,
        register,
        logout,
        generateCrossSystemToken,
        verifyCrossSystemToken
   };
});