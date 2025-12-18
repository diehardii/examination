// src/api/knowledgeManager.js
import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8081/api/knowledge-storage', // knowledgeManager的端口
    withCredentials: true // 允许跨域携带 Cookie
});

export default {
    /**
     * 精准查询指定知识点下的所有内容
     * @param {string} knowledgePointName - 知识点名称
     * @returns {Promise} API响应
     */
    queryKnowledgePointContent(knowledgePointName) {
        return api.post('/query-knowledge-point-content', {
            knowledgePointName: knowledgePointName
        });
    },

    /**
    queryKnowledgeContent(queryText, nResults = 5) {
        return api.post('/query-knowledge-content', {
            query: queryText,
            nResults: nResults
        });
    },

    /**
     * 测试ChromaDB连接
     * @returns {Promise} API响应
     */
    testConnection() {
        return api.get('/connection');
    },

    /**
     * 获取集合信息
     * @returns {Promise} API响应
     */
    getCollectionInfo() {
        return api.get('/info');
    }
};
