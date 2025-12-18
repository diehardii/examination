import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/',
    withCredentials: true,
});

// CET4学情分析服务API (Python后端)
const cet4AnalyticsApi = axios.create({
    baseURL: 'http://localhost:5000/api/cet4/analytics/',
    withCredentials: false,
});

// 获取教师所有学段的班级分配
export const fetchTeacherAllAssignments = async (teacherId) => {
    const res = await api.get(`/teachers/${teacherId}/assignments`);
    return res.data;
};

// 获取教师某学段的班级列表
export const fetchTeacherClassesByStage = async (teacherId, stageId) => {
    const res = await api.get(`/teachers/${teacherId}/stages/${stageId}/classes`);
    return res.data;
};

// 获取班级的学生列表
export const fetchClassStudents = async (classId) => {
    const res = await api.get(`/student-class/classes/${classId}/students`);
    return res.data;
};

// 获取班级详情
export const fetchClassById = async (classId) => {
    const res = await api.get(`/classes/${classId}`);
    return res.data;
};

// 根据学段获取班级列表
export const fetchClassesByStage = async (stageId) => {
    const res = await api.get(`/classes/by-stage/${stageId}`);
    return res.data;
};

// ==================== CET4 学情分析API (Python后端) ====================

// === 学生个人分析 ===

// 1. 获取学生成绩变化趋势
export const fetchCET4StudentScoreTrend = async (userId) => {
    const res = await cet4AnalyticsApi.get(`student/score-trend/${userId}`);
    return res.data;
};

// 2. 获取学生各题型得分率分析
export const fetchCET4StudentSegmentAnalysis = async (userId) => {
    const res = await cet4AnalyticsApi.get(`student/segment-analysis/${userId}`);
    return res.data;
};

// 3. 获取学生大类题型分析
export const fetchCET4StudentSectionAnalysis = async (userId) => {
    const res = await cet4AnalyticsApi.get(`student/section-analysis/${userId}`);
    return res.data;
};

// 4. 获取学生薄弱点分析
export const fetchCET4StudentWeakPoints = async (userId) => {
    const res = await cet4AnalyticsApi.get(`student/weak-points/${userId}`);
    return res.data;
};

// 5. 获取学生进步情况对比
export const fetchCET4StudentProgress = async (userId) => {
    const res = await cet4AnalyticsApi.get(`student/progress/${userId}`);
    return res.data;
};

// 13. 获取学生考试历史记录
export const fetchCET4StudentExamHistory = async (userId) => {
    const res = await cet4AnalyticsApi.get(`student/exam-history/${userId}`);
    return res.data;
};

// 14. 获取听力与阅读能力对比
export const fetchCET4ListeningReadingAnalysis = async (userId) => {
    const res = await cet4AnalyticsApi.get(`student/listening-reading/${userId}`);
    return res.data;
};

// 16. 获取时间段对比分析
export const fetchCET4TimeComparison = async (userId, days = 30) => {
    const res = await cet4AnalyticsApi.get(`student/time-comparison/${userId}?days=${days}`);
    return res.data;
};

// 11. 获取学生与班级平均对比
export const fetchCET4StudentVsClass = async (userId, classId) => {
    const res = await cet4AnalyticsApi.get(`student/vs-class/${userId}/${classId}`);
    return res.data;
};

// === 班级分析 ===

// 6. 获取班级整体情况概览
export const fetchCET4ClassOverview = async (classId) => {
    const res = await cet4AnalyticsApi.get(`class/overview/${classId}`);
    return res.data;
};

// 7. 获取班级成绩分布
export const fetchCET4ClassScoreDistribution = async (classId) => {
    const res = await cet4AnalyticsApi.get(`class/score-distribution/${classId}`);
    return res.data;
};

// 8. 获取班级各题型平均得分率
export const fetchCET4ClassSegmentComparison = async (classId) => {
    const res = await cet4AnalyticsApi.get(`class/segment-comparison/${classId}`);
    return res.data;
};

// 9. 获取班级学生排名
export const fetchCET4ClassStudentRanking = async (classId, limit = 20) => {
    const res = await cet4AnalyticsApi.get(`class/student-ranking/${classId}?limit=${limit}`);
    return res.data;
};

// 10. 获取班级成绩趋势
export const fetchCET4ClassTrend = async (classId) => {
    const res = await cet4AnalyticsApi.get(`class/trend/${classId}`);
    return res.data;
};

// 15. 获取班级薄弱点分析
export const fetchCET4ClassWeakPoints = async (classId) => {
    const res = await cet4AnalyticsApi.get(`class/weak-points/${classId}`);
    return res.data;
};

// === 试卷分析 ===

// 12. 获取试卷分析
export const fetchCET4ExamPaperAnalysis = async (examPaperId) => {
    const res = await cet4AnalyticsApi.get(`exam-paper/${examPaperId}`);
    return res.data;
};

// 健康检查
export const checkCET4AnalyticsHealth = async () => {
    const res = await cet4AnalyticsApi.get('health');
    return res.data;
};
