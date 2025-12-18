/**
 * HS3 高考英语试卷生成 API
 */
import axios from 'axios'

const API_BASE = 'http://localhost:8080/api/hs3/paper-gen'

// 创建 axios 实例
const api = axios.create({
  baseURL: API_BASE,
  withCredentials: true,
  timeout: 10 * 60 * 1000 // 10分钟超时（Coze调用耗时较长）
})

export const HS3_PAPER_GEN_API = {
  // 生成试卷
  GENERATE: '/generate',
  // 单题生成
  GENERATE_SINGLE: '/coze/generate-single',
  // 存储segment到ChromaDB
  STORE_SEGMENT: '/store-segment',
  // 健康检查
  HEALTH: '/health'
}

export default {
  /**
   * 生成高考英语试卷（同步模式）
   * @param {Object} params - 生成参数
   * @param {string} params.subjectEn - 科目（高考）
   * @param {string} params.exam_paper_en_source - 来源（AIfromreal）
   */
  generatePaper(params) {
    return api.post(HS3_PAPER_GEN_API.GENERATE, params)
  },

  /**
   * 单题生成（使用Coze工作流）
   * @param {Object} params - 生成参数
   * @param {string} params.examTopic - 考试主题
   * @param {string} params.inputExamPaperSamp - 输入的样本试卷
   * @param {string} params.examPaperEnSource - 来源
   * @param {string} params.segmentIdSelf - segment ID
   */
  generateSingleQuestion(params) {
    return api.post(HS3_PAPER_GEN_API.GENERATE_SINGLE, params)
  },

  /**
   * 存储segment到ChromaDB
   * @param {Object} params - 存储参数
   */
  storeSegment(params) {
    return api.post(HS3_PAPER_GEN_API.STORE_SEGMENT, params)
  },

  /**
   * 健康检查
   */
  health() {
    return api.get(HS3_PAPER_GEN_API.HEALTH)
  }
}
