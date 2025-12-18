/**
 * HS3 高考英语试卷解析服务
 */
import HS3Api from '@/hs3/api/HS3paperAnalysis'

/**
 * 解析 Word 文件并输出到指定目录
 * @param {File} file - Word 文件
 * @returns {Promise<Object>} 解析结果
 */
export async function parseWordFile(file) {
  const formData = new FormData()
  formData.append('file', file)
  
  const response = await HS3Api.parseWord(formData)
  return response.data
}

/**
 * 上传Word文件，调用Coze解析并分析试卷结构（按Part分段解析）
 * 后端会自动使用Neo4j中的HS3模板结构（根节点id='HS3'）
 * @param {File} file - Word 文件
 * @param {Array<string>} topics - 知识点数组（可选）
 * @returns {Promise<Object>} 解析结果
 */
export async function parseAndAnalyzePaper(file, topics = []) {
  const formData = new FormData()
  formData.append('file', file)
  if (topics && topics.length > 0) {
    formData.append('topics', JSON.stringify(topics))
  }
  
  const response = await HS3Api.parseAndAnalyze(formData)
  return response.data
}

/**
 * 获取试卷展示数据（带层级结构）
 * @param {string} examPaperId - 试卷ID
 * @returns {Promise<Object>} 试卷层级结构数据
 */
export async function getPaperDisplay(examPaperId) {
  const response = await HS3Api.getPaperDisplay(examPaperId)
  return response.data
}

/**
 * 获取所有知识点（topics）
 * @returns {Promise<Array>} 知识点列表
 */
export async function getTopics() {
  const response = await HS3Api.getTopics()
  return response.data
}

/**
 * 获取试卷列表
 * @returns {Promise<Array>} 试卷列表
 */
export async function getExamPaperList() {
  const response = await HS3Api.getExamPaperList()
  return response.data
}

/**
 * 清空试卷的题目数据
 * @param {string} examPaperId - 试卷ID
 * @returns {Promise<Object>} 清空结果
 */
export async function clearQuestions(examPaperId) {
  const response = await HS3Api.clearQuestions(examPaperId)
  return response.data
}

/**
 * 健康检查
 * @returns {Promise<Object>} 健康状态
 */
export async function healthCheck() {
  const response = await HS3Api.health()
  return response.data
}
