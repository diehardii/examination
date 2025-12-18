/**
 * HS3 高考英语试卷生成服务
 */
import HS3PaperGenApi from '@/hs3/api/HS3paperGen'

/**
 * 生成高考英语试卷
 * @param {string} subjectEn - 科目（高考）
 * @param {string} examPaperEnSource - 来源（AIfromreal）
 * @returns {Promise<Object>} 生成结果
 */
export async function generateHS3Paper(subjectEn = '高考', examPaperEnSource = 'AIfromreal') {
  const response = await HS3PaperGenApi.generatePaper({
    subjectEn,
    exam_paper_en_source: examPaperEnSource
  })
  return response.data
}

/**
 * 单题生成
 * @param {string} examTopic - 考试主题
 * @param {string} inputExamPaperSamp - 输入的样本试卷JSON
 * @param {string} examPaperEnSource - 来源
 * @param {string} segmentIdSelf - segment ID
 * @returns {Promise<Object>} 生成结果
 */
export async function generateSingleQuestion(examTopic, inputExamPaperSamp, examPaperEnSource, segmentIdSelf) {
  const response = await HS3PaperGenApi.generateSingleQuestion({
    examTopic,
    inputExamPaperSamp,
    examPaperEnSource,
    segmentIdSelf
  })
  return response.data
}

/**
 * 存储segment到ChromaDB
 * @param {Object} params - 存储参数
 * @returns {Promise<Object>} 存储结果
 */
export async function storeSegmentToChroma(params) {
  const response = await HS3PaperGenApi.storeSegment(params)
  return response.data
}

/**
 * 健康检查
 * @returns {Promise<Object>} 健康状态
 */
export async function healthCheck() {
  const response = await HS3PaperGenApi.health()
  return response.data
}
