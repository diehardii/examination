import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:5000/api/cet4',
    withCredentials: false,
    headers: {
        'Content-Type': 'application/json'
    }
});

/**
 * CET4 AI辅导老师API服务
 */
export default {
    /**
     * 向AI辅导老师提问
     * @param {number} userId - 用户ID
     * @param {string} segmentId - 题目片段ID
     * @param {string} questionType - 题型
     * @param {string} document - 题目document JSON字符串
     * @param {Array} userAnswers - 用户答案列表
     * @param {string} question - 学生问题
     * @returns {Promise} AI回答
     */
    async askQuestion(userId, segmentId, questionType, document, userAnswers, question) {
        try {
            const response = await api.post('/tutoring/ask', {
                user_id: userId,
                segment_id: segmentId,
                question_type: questionType,
                document: document,
                user_answers: userAnswers,
                question: question
            });
            return response.data;
        } catch (error) {
            throw error;
        }
    },

    /**
     * 向AI辅导老师提问 (流式SSE)
     * @param {number} userId - 用户ID
     * @param {string} segmentId - 题目片段ID
     * @param {string} questionType - 题型
     * @param {string} document - 题目document JSON字符串
     * @param {Array} userAnswers - 用户答案列表
     * @param {string} question - 学生问题
     * @param {Function} onChunk - 接收数据块的回调函数
     * @param {Function} onComplete - 完成时的回调函数
     * @param {Function} onError - 错误时的回调函数
     */
    askQuestionStream(userId, segmentId, questionType, document, userAnswers, question, onChunk, onComplete, onError) {
        const url = `http://localhost:5000/api/cet4/tutoring/ask-stream`;
        const requestBody = JSON.stringify({
            user_id: userId,
            segment_id: segmentId,
            question_type: questionType,
            document: document,
            user_answers: userAnswers,
            question: question
        });

        // 使用fetch进行SSE请求
        fetch(url, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: requestBody
        })
        .then(response => {
            if (!response.ok) {
                throw new Error(`HTTP error! status: ${response.status}`);
            }

            const reader = response.body.getReader();
            const decoder = new TextDecoder();
            let buffer = '';

            const processStream = () => {
                reader.read().then(({ done, value }) => {
                    if (done) {
                        return;
                    }

                    // 解码数据
                    buffer += decoder.decode(value, { stream: true });
                    
                    // 处理SSE数据格式 "data: {...}\n\n"
                    const lines = buffer.split('\n\n');
                    buffer = lines.pop(); // 保留不完整的行

                    lines.forEach(line => {
                        if (line.startsWith('data: ')) {
                            try {
                                const jsonData = JSON.parse(line.substring(6));
                                if (jsonData.type === 'chunk') {
                                    // 接收到数据块
                                    if (onChunk) onChunk(jsonData.content);
                                } else if (jsonData.type === 'done') {
                                    // 接收完成
                                    if (onComplete) onComplete(jsonData);
                                } else if (jsonData.type === 'error') {
                                    // 接收到错误
                                    if (onError) onError(new Error(jsonData.message));
                                }
                            } catch (e) {
                                if (onError) onError(e);
                            }
                        }
                    });

                    // 继续读取
                    processStream();
                }).catch(error => {
                    if (onError) onError(error);
                });
            };

            processStream();
        })
        .catch(error => {
            if (onError) onError(error);
        });
    },

    /**
     * 结束辅导会话
     * @param {number} userId - 用户ID
     * @param {string} segmentId - 题目片段ID
     * @returns {Promise}
     */
    async endSession(userId, segmentId) {
        console.log('[API] endSession 调用开始');
        console.log('[API] userId:', userId);
        console.log('[API] segmentId:', segmentId);
        try {
            const response = await api.post('/tutoring/end-session', {
                user_id: userId,
                segment_id: segmentId
            });
            console.log('[API] endSession 响应:', response.data);
            return response.data;
        } catch (error) {
            console.error('[API] endSession 失败:', error);
            throw error;
        }
    }
};
