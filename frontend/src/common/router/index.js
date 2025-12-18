import { createRouter, createWebHistory } from 'vue-router';
import HomeView from '@/common/views/HomeView.vue';
import AuthView from '@/common/views/AuthView.vue'; // 替换原来的 Login.vue
import { useAuthStore } from '@/common/stores/auth';
import PaperGen from '@/cet4/views/CET4PaperGenView.vue';
import PaperTest from '@/cet4/views/CET4PaperTestView.vue';
import UserMan from '@/common/views/UserManView.vue';
import SystemMan from "@/common/views/SystemManView.vue";


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
                component: () => import('@/common/components/Login.vue')
            },
            {
                path: 'register',
                name: 'register',
                component: () => import('@/common/components/Register.vue')
            }
        ]
    },
    {
        path: '/paper-gen',
        name: 'PaperGen',
        component: PaperGen
    },
    {
        path: '/paper-test',
        name: 'PaperTest',
        component: PaperTest,
        meta: { requiresAuth: true },
    },
    {
        path: '/system-man',
        name: 'SystemMan',
        component: SystemMan,
    },
    {
        path: '/learning-analysis',
        name: 'LearningAnalysis',
        component: () => import('@/common/views/LearningAnalysisView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/user-man',
        name: 'UserMan',
        component: UserMan,
        meta: { requiresAuth: true }
    },
    {
        path: '/knowledge-management',
        name: 'KnowledgeManagement',
        component: () => import('@/cet4/views/CET4KnowledgeManagementView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/paper-analysis-cet4',
        name: 'PaperAnalysisCET4',
        component: () => import('@/cet4/views/CET4PaperAnalysisView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/paper-display',
        name: 'PaperDisplay',
        component: () => import('@/cet4/views/CET4PaperDisplayView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/paper-gen-result',
        name: 'PaperGenResult',
        component: () => import('@/cet4/views/CET4PaperGenResultView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/question-answer-en/:examPaperEnId',
        name: 'QuestionAnswerEn',
        component: () => import('@/cet4/components/CET4QuestionAnswerEn.vue'),
        props: true,
        meta: { requiresAuth: true }
    },
    {
        path: '/test-result-en/:id',
        name: 'TestResultEn',
        component: () => import('@/cet4/components/CET4TestResultEn.vue'),
        props: true,
        meta: { requiresAuth: true }
    },
    {
        path: '/test-result-en-local',
        name: 'TestResultEnLocal',
        component: () => import('@/cet4/components/CET4TestResultEnLocal.vue'),
        meta: { requiresAuth: true }
    },
    // 新增强化训练路由（学生/老师）
    {
        path: '/intensive-train',
        name: 'CET4IntensiveTrain',
        component: () => import('@/cet4/components/CET4IntensiveTrain.vue'),
        meta: { requiresAuth: true }
    },
    // 强化训练预览路由（不需要examPaperEnId，从sessionStorage读取数据）
    {
        path: '/intensive-train-preview',
        name: 'CET4IntensiveTrainPreview',
        component: () => import('@/cet4/components/CET4IntensiveTrainPreview.vue'),
        meta: { requiresAuth: true }
    },
    // 错题答疑路由
    {
        path: '/wrong-questions',
        name: 'WrongQuestions',
        component: () => import('@/cet4/views/CET4WrongQuestionsView.vue'),
        meta: { requiresAuth: true }
    },
    // 错题详情路由
    {
        path: '/wrong-question-detail/:testEnId/:segmentId',
        name: 'WrongQuestionDetail',
        component: () => import('@/cet4/components/CET4WrongQuestionDetail.vue'),
        props: true,
        meta: { requiresAuth: true }
    },
    // 智能问答路由
    {
        path: '/smart-qa',
        name: 'SmartQA',
        component: () => import('@/cet4/views/CET4SmartQAView.vue'),
        meta: { requiresAuth: true }
    },
    // HS3 高考英语路由
    {
        path: '/paper-analysis-hs3',
        name: 'PaperAnalysisHS3',
        component: () => import('@/hs3/views/HS3PaperAnalysisView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/paper-test-hs3',
        name: 'PaperTestHS3',
        component: () => import('@/hs3/views/HS3PaperTestView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/question-answer-hs3/:examPaperId',
        name: 'QuestionAnswerHS3',
        component: () => import('@/hs3/components/HS3QuestionAnswer.vue'),
        props: true,
        meta: { requiresAuth: true }
    },
    {
        path: '/hs3/test-result/:id',
        name: 'HS3TestResultEn',
        component: () => import('@/hs3/components/HS3TestResultEn.vue'),
        props: true,
        meta: { requiresAuth: true }
    },
    // HS3 高考英语试卷生成路由
    {
        path: '/paper-gen-hs3',
        name: 'HS3PaperGen',
        component: () => import('@/hs3/views/HS3PaperGenView.vue'),
        meta: { requiresAuth: true }
    },
    {
        path: '/paper-gen-result-hs3',
        name: 'HS3PaperGenResult',
        component: () => import('@/hs3/views/HS3PaperGenResultView.vue'),
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
                next('/auth/login'); // 未认证跳转到具体登录页
            }
        } catch (error) {
            next('/auth/login'); // 获取用户信息失败，重定向登录页
        }
    } else if (to.name === 'auth' && authStore.isAuthenticated) {
        next('/home'); // 已登录用户访问 Auth 页，重定向到主页
    } else {
        next(); // 其他情况直接放行
    }
});

export default router;