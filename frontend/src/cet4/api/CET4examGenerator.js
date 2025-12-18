import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:5000/api/cet4',
    withCredentials: false,
    timeout: 30 * 60 * 1000, // 30分钟，生成训练题目耗时较长
    headers: {
        'Content-Type': 'application/json'
    }
});

/**
 * CET4试卷生成API服务
 */
export default {
    /**
     * 生成试卷题目
     * @param {string} inputExamPaperSamp - 试卷样例JSON字符串
     * @param {string} examTopic - 考试主题
     * @param {string} model - 模型名称，默认为deepseek-reasoner
     * @param {string} examPaperEnSource - 试卷来源
     * @param {string} segmentIdSelf - 自定义segment_id
     * @returns {Promise} 生成的试卷数据
     */
    generateExam(inputExamPaperSamp, examTopic, model = 'deepseek-reasoner', examPaperEnSource = null, segmentIdSelf = null) {
        const requestData = {
            inputExamPaperSamp,
            examTopic,
            model
        };
        
        // 添加可选参数
        if (examPaperEnSource) {
            requestData.examPaperEnSource = examPaperEnSource;
        }
        if (segmentIdSelf) {
            requestData.segmentIdSelf = segmentIdSelf;
        }
        
        return api.post('/generate-exam', requestData).then(response => response.data).catch(error => {
            if (error.response) {
                // Preserve diagnostic details for upstream handlers
                const details = {
                    status: error.response.status,
                    data: error.response.data
                };
                throw Object.assign(error, { details });
            }
            throw error;
        });
    },

    /**
     * 健康检查
     * @returns {Promise}
     */
    healthCheck() {
        return api.get('/health').then(response => response.data).catch(error => {
            throw error;
        });
    }
};
