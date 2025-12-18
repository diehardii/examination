import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/cet4/wrong-questions',
    withCredentials: true, // 允许跨域携带 Cookie
});

/**
 * 错题答疑API服务
 */
export default {
    /**
     * 获取错题列表（支持分页）
     * @param {string} sortBy - 排序依据: time(时间) 或 percent(正确率)
     * @param {string} sortOrder - 排序顺序: ASC(正序) 或 DESC(倒序)
     * @param {number} page - 页码（从1开始）
     * @param {number} pageSize - 每页大小
     * @returns {Promise} 错题列表数据（包含分页信息）
     */
    getWrongQuestionsList(sortBy = 'time', sortOrder = 'DESC', page = 1, pageSize = 10) {
        return api.get('/list', {
            params: {
                sortBy,
                sortOrder,
                page,
                pageSize
            }
        }).then(response => {
            return response.data;
        }).catch(error => {
            throw error;
        });
    },

    /**
     * 获取错题详情
     * @param {number} testEnId - 考试ID
     * @param {string} segmentId - 段落ID
     * @returns {Promise} 错题详情数据
     */
    getWrongQuestionDetail(testEnId, segmentId) {
        return api.get('/detail', {
            params: {
                testEnId,
                segmentId
            }
        }).then(response => {
            return response.data;
        }).catch(error => {
            throw error;
        });
    }
};
