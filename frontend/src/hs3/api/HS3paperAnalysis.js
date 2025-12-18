/**
 * HS3 高考英语试卷解析 API
 */
import axios from 'axios'

const API_BASE = 'http://localhost:8080/api/hs3/paper-analysis'

// 创建 axios 实例
const api = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
  timeout: 10 * 60 * 1000 // 10分钟超时（Coze调用耗时较长）
})

export const HS3_API = {
  // 试卷解析相关
  PARSE_WORD: '/parse-word',
  PARSE_AND_ANALYZE: '/parse-and-analyze',
  PARSE_TEXT: '/parse-text',
  // Neo4j 展示数据（题目结构）
  GET_PAPER_DISPLAY: (examPaperId) => `/paper/${examPaperId}`,
  CLEAR_QUESTIONS: (examPaperId) => `/paper/${examPaperId}/questions`,
  GET_TOPICS: '/topics',
  HEALTH: '/health',
  
  // 试卷列表
  EXAM_PAPER_LIST: '/exam-papers'
}

export default {
  /**
   * 解析Word文件并输出到本地
   */
  parseWord(formData) {
    return api.post(HS3_API.PARSE_WORD, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  /**
   * 上传Word文件，调用Coze解析并分析试卷结构
   */
  parseAndAnalyze(formData) {
    return api.post(HS3_API.PARSE_AND_ANALYZE, formData, {
      headers: { 'Content-Type': 'multipart/form-data' }
    })
  },

  /**
   * 使用文本内容调用Coze解析
   */
  parseText(inputFile, examPaperId = '2024_English_I', topics = null) {
    return api.post(HS3_API.PARSE_TEXT, {
      inputFile,
      examPaperId,
      topics
    })
  },

  /**
   * 获取试卷展示数据（带层级结构）
   */
  getPaperDisplay(examPaperId) {
    return api.get(HS3_API.GET_PAPER_DISPLAY(examPaperId))
  },

  /**
   * 获取试卷列表
   */
  getExamPaperList() {
    return api.get(HS3_API.EXAM_PAPER_LIST)
  },

  /**
   * 清空试卷的题目数据
   */
  clearQuestions(examPaperId) {
    return api.delete(HS3_API.CLEAR_QUESTIONS(examPaperId))
  },

  /**
   * 获取所有topics（知识点）
   */
  getTopics() {
    return api.get(HS3_API.GET_TOPICS)
  },

  /**
   * 健康检查
   */
  health() {
    return api.get(HS3_API.HEALTH)
  }
}

