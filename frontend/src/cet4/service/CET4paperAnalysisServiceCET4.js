import paperAnalysisApiCET4 from '@/cet4/api/CET4paperAnalysisCET4';

const pickConcurrentApi = (file) => {
  const name = (file?.name || '').toLowerCase();
  const isPdf = name.endsWith('.pdf');
  const isPlain = name.endsWith('.txt') || name.endsWith('.doc') || name.endsWith('.docx');
  const isImage = name.endsWith('.png') || name.endsWith('.jpg') || name.endsWith('.jpeg');

  if (isPlain) return paperAnalysisApiCET4.parsePlainConcurrent;
  if (isImage) return paperAnalysisApiCET4.parseImageConcurrent;
  return paperAnalysisApiCET4.parsePdfConcurrent;
};

const upload = async (file, structure = false) => {
  const formData = new FormData();
  formData.append('file', file);

  try {
    const caller = pickConcurrentApi(file);
    const response = await caller(formData, { structure });
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('当前未登录或会话已过期，请重新登录。');
    }
    const serverMessage = error.response?.data?.message || error.response?.data?.error;
    const statusLabel = error.response?.status ?? 'network';
    throw new Error(serverMessage || `解析试卷失败，请稍后重试（status ${statusLabel}）`);
  }
};

export const extractPaperTextCET4 = async (file) => upload(file, false);

export const extractPaperStructureCET4 = async (file) => upload(file, true);

export const storeSectionAInChromaCET4 = async (payload) => {
  try {
    const response = await paperAnalysisApiCET4.storeSectionA(payload);
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('当前未登录或会话已过期，请重新登录。');
    }
    throw new Error(error.response?.data?.message || '写入 ChromaDB 失败，请稍后重试');
  }
};

export const storeSectionBInChromaCET4 = async (payload) => {
  try {
    const response = await paperAnalysisApiCET4.storeSectionB(payload);
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('当前未登录或会话已过期，请重新登录。');
    }
    throw new Error(error.response?.data?.message || '写入 ChromaDB 失败，请稍后重试');
  }
};

export const querySectionAFromChromaCET4 = async (examPaperId, questionType = '选词填空') => {
  try {
    const response = await paperAnalysisApiCET4.querySectionA({ examPaperId, questionType });
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('当前未登录或会话已过期，请重新登录。');
    }
    throw new Error(error.response?.data?.message || '查询 ChromaDB 失败，请稍后重试');
  }
};

export const storeSectionCInChromaCET4 = async (payload) => {
  try {
    const response = await paperAnalysisApiCET4.storeSectionC(payload);
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('当前未登录或会话已过期，请重新登录。');
    }
    throw new Error(error.response?.data?.message || '写入 ChromaDB 失败，请稍后重试');
  }
};

export const storeWritingInChromaCET4 = async (payload) => {
  try {
    const response = await paperAnalysisApiCET4.storeWriting(payload);
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('当前未登录或会话已过期，请重新登录。');
    }
    throw new Error(error.response?.data?.message || '写入 ChromaDB 失败，请稍后重试');
  }
};

export const storeTranslationInChromaCET4 = async (payload) => {
  try {
    const response = await paperAnalysisApiCET4.storeTranslation(payload);
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('当前未登录或会话已过期，请重新登录。');
    }
    throw new Error(error.response?.data?.message || '写入 ChromaDB 失败，请稍后重试');
  }
};

export const storePart2ABInChromaCET4 = async (payload) => {
  try {
    const response = await paperAnalysisApiCET4.storePart2AB(payload);
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('当前未登录或会话已过期，请重新登录。');
    }
    throw new Error(error.response?.data?.message || '写入 ChromaDB 失败，请稍后重试');
  }
};

export const storePart2CInChromaCET4 = async (payload) => {
  try {
    const response = await paperAnalysisApiCET4.storePart2C(payload);
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('当前未登录或会话已过期，请重新登录。');
    }
    throw new Error(error.response?.data?.message || '写入 ChromaDB 失败，请稍后重试');
  }
};

export const queryExamPaperUnitsCET4 = async (examPaperId, examPaperName) => {
  try {
    const params = {};
    if (examPaperId) params.examPaperId = examPaperId;
    if (examPaperName) params.examPaperName = examPaperName;
    const response = await paperAnalysisApiCET4.queryExamPaperUnits(params);
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('当前未登录或会话已过期，请重新登录。');
    }
    throw new Error(error.response?.data?.message || '查询试卷单元失败，请稍后重试');
  }
};

export const submitPaperAnalysisTask = async (file, asyncMode = true) => {
  if (!file) {
    throw new Error('请先选择文件');
  }
  const formData = new FormData();
  formData.append('file', file);
  try {
    const response = await paperAnalysisApiCET4.submitTask(formData, { asyncMode });
    return response.data;
  } catch (error) {
    if (error.response && error.response.status === 401) {
      throw new Error('当前未登录或会话已过期，请重新登录。');
    }
    const message = error.response?.data?.message || '提交解析任务失败，请稍后重试';
    throw new Error(message);
  }
};

export const listPaperAnalysisTasks = async (limit = 20) => {
  try {
    const response = await paperAnalysisApiCET4.listTasks(limit);
    return response.data;
  } catch (error) {
    return [];
  }
};

export const fetchPaperAnalysisTaskResult = async (taskId) => {
  if (!taskId) {
    throw new Error('任务ID不能为空');
  }
  const response = await paperAnalysisApiCET4.getTaskResult(taskId);
  return response.data;
};

export const deletePaperAnalysisTask = async (taskId) => {
  if (!taskId) return;
  try {
    await paperAnalysisApiCET4.deleteTask(taskId);
  } catch (error) {
  }
};