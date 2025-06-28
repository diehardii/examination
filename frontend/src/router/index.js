import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '@/views/HomeView.vue';
import AuthView from '@/views/AuthView.vue'; // 替换原来的 Login.vue
import { useAuthStore } from '@/stores/auth';
import PaperGen from '@/views/PaperGenView.vue';
import PaperMan from '@/views/PaperManView.vue';
import PaperTest from '@/views/PaperTestView.vue';
import PaperAna from '@/views/PaperAnaView.vue';
import QuestionAna from '@/views/QuestionAnaView.vue';
import UserMan from '@/views/UserManView.vue';
import SystemMan from "@/views/SystemManView.vue";
import WrongQuestionMan from "@/views/WrongQuestionManView.vue";


const routes = [
    {
        path: '/',
        redirect: '/auth/login', // 默认重定向到 Auth 页（动态登录/注册）
    },
    {
        path: '/home',
        name: 'home',
        component: HomeView,
        meta: { requiresAuth: true }, // 需要登录
    },
    //从5173返回到home
    {
        path: '/home/from-analysis',
        name: 'homeFromAnalysis',
        component: HomeView,
        meta: {
            requiresAuth: true,
            fromCrossSystem: true // 新增标记
        }
    },

    {
        path: '/auth',
        name: 'auth',
        component: AuthView,
        meta: { requiresAuth: false },
        children: [
            {
                path: 'login',
                name: 'login',
                component: () => import('@/components/Login.vue')
            },
            {
                path: 'register',
                name: 'register',
                component: () => import('@/components/Register.vue')
            }
        ]
    },
    {
        path: '/paper-gen',
        name: 'PaperGen',
        component: PaperGen
    },
    {
        path: '/paper-show/:id',
        name: 'PaperShow',
        component: () => import('@/components/PaperShow.vue'),
        props: true
    },
    {
        path: '/paper-man',
        name: 'PaperMan',
        component: PaperMan,
        meta: { requiresAuth: true }
    },
// 考试功能相关
    {
         path: '/paper-test',
        name: 'PaperTest',
        component: PaperTest,
        meta: { requiresAuth: true },
    },
    {
        path: '/question-answer/:paperId',
        name: 'QuestionAnswer',
        component: () => import('@/components/QuestionAnswer.vue'),
        props: true,

    },
    {
        path: '/test-result',
        name: 'TestResult',
        component: () => import('@/components/TestResult.vue'),
        props: (route) => ({ resultData: route.params.resultData }),

    },
    {
        path: '/paper-ana',
        name: 'PaperAna',
        component: PaperAna
    },
    {
        path: '/analysis/result/:subject',
        name: 'AnalysisResult',
        component: () => import('@/components/PaperAnaResult.vue'),
        props: true
    },
    {
        path: '/question-ana',
        name: 'QuestionAna',
        component: QuestionAna,
    },
    {
        path: '/questionAna-result',
        name: 'QuestionAnaResult',
        component: () => import('@/components/QuestionAnaResult.vue'),
        props: (route) => ({
            analysisData: route.query.analysisData,
            questionData: JSON.parse(route.query.questionData || '{}')

        })
    },
    {
        path: '/system-man',
        name: 'SystemMan',
        component: SystemMan,
    },

    {
        path: '/wrong-question-man',
        name: 'WrongQuestionMan',
        component: WrongQuestionMan,
        meta: { requiresAuth: true }
    },
    {
        path: '/user-man',
        name: 'UserMan',
        component: UserMan,
        meta: { requiresAuth: true }
    },

];

const router = createRouter({
    history: createWebHistory(import.meta.env.BASE_URL),
    routes,
});

router.beforeEach(async (to, from, next) => {
    const authStore = useAuthStore();

    // 如果目标路由需要认证
    if (to.meta.requiresAuth) {
        try {

            // 如果是来自跨系统的返回，优先使用返回的token验证
            if (to.meta.fromCrossSystem ) {

                    await authStore.fetchUser(); // 确保用户信息是最新的
                    next('/home');
                    return;

            }





            await authStore.fetchUser();
            if (authStore.isAuthenticated) {
                next(); // 已认证，放行
            } else {
                next('/auth'); // 未认证，重定向到 Auth 页
            }
        } catch (error) {
            next('/auth'); // 获取用户信息失败，重定向到 Auth 页
        }
    } else if (to.name === 'auth' && authStore.isAuthenticated) {
        next('/home'); // 已登录用户访问 Auth 页，重定向到主页
    } else {
        next(); // 其他情况直接放行
    }
});

export default router;