<template>
  <div class="test-result-container">
    <SideBarMenu />
    <div class="main-content">
      <!-- 训练模式：显示题目和答题区 -->
      <div v-if="isTrainingMode && !showTrainingAnswer" class="training-mode-container">
        <TrainingQuestionDisplay
          :question-data="trainingQuestionData"
          :user-training-answers="userTrainingAnswers"
          :user-training-answer="userTrainingAnswer"
          @submit-training-answer="handleSubmitTrainingAnswer"
          @back-to-wrong-question="exitTrainingMode"
        />
      </div>

      <!-- 训练答案显示模式 -->
      <div v-else-if="isTrainingMode && showTrainingAnswer" class="training-answer-container">
        <TrainingAnswerDisplay
          :question-data="trainingQuestionData"
          :user-training-answers="userTrainingAnswers"
          :user-training-answer="userTrainingAnswer"
          :ai-score="trainingAiScore"
          :ai-feedback="trainingAiFeedback"
          :ai-reasoning="trainingAiReasoning"
          @retry-training="retryTraining"
          @back-to-wrong-question="exitTrainingMode"
        />
      </div>

      <!-- 左右分栏模式（答疑模式） -->
      <div v-else-if="isTutoringMode" class="split-view-container">
        <!-- 左侧：错题展示区域 -->
        <div class="left-panel">
          <WrongQuestionDisplay
            :loading="loading"
            :error-message="errorMessage"
            :question-data="questionData"
            :user-answers="userAnswers"
            :correct-answers="correctAnswers"
            :user-answer="userAnswer"
            :ai-score="aiScore"
            :ai-feedback="aiFeedback"
            :ai-reasoning="aiReasoning"
            :raw-output="rawOutput"
            :generating-exam="generatingExam"
            :exam-paper-en-source="examPaperEnSource"
            :segment-id-self="segmentIdSelf"
            :show-tutoring-button="false"
            :show-retry-button="false"
            :show-raw-output-button="false"
            @show-raw-output="showRawOutputDialog = true"
            @retry-question="handleRetryQuestionType"
            @go-back="goBack"
          />
        </div>

        <!-- 右侧：AI辅导老师对话区 -->
        <div class="right-panel">
          <AITutorChat
            :messages="chatMessages"
            :thinking="aiThinking"
            @send-question="sendQuestion"
          />
        </div>
      </div>

      <!-- 全屏错题展示模式（默认模式） -->
      <div v-else class="fullscreen-container">
        <WrongQuestionDisplay
          :loading="loading"
          :error-message="errorMessage"
          :question-data="questionData"
          :user-answers="userAnswers"
          :correct-answers="correctAnswers"
          :user-answer="userAnswer"
          :ai-score="aiScore"
          :ai-feedback="aiFeedback"
          :ai-reasoning="aiReasoning"
          :raw-output="rawOutput"
          :generating-exam="generatingExam"
          :exam-paper-en-source="examPaperEnSource"
          :segment-id-self="segmentIdSelf"
          :show-tutoring-button="true"
          :show-retry-button="true"
          :show-raw-output-button="true"
          @show-raw-output="showRawOutputDialog = true"
          @retry-question="handleRetryQuestionType"
          @start-tutoring="startTutoringMode"
          @go-back="goBack"
        />
      </div>
    </div>

    <!-- DeepSeek原始输出对话框 -->
    <el-dialog
      v-model="showRawOutputDialog"
      title="🤖 DeepSeek API 原始输出"
      width="80%"
      :close-on-click-modal="false"
      class="raw-output-dialog"
    >
      <div class="raw-output-container">
        <div class="output-header">
          <el-tag type="info" size="large">生成时间: {{ generationTime }}</el-tag>
          <el-tag type="success" size="large">主题: {{ generatedTopic }}</el-tag>
          <el-button 
            type="primary" 
            size="small" 
            @click="copyRawOutput"
            icon="DocumentCopy"
          >
            复制到剪贴板
          </el-button>
        </div>
        <div class="output-content">
          <pre class="raw-output-text">{{ rawOutput }}</pre>
        </div>
      </div>
      <template #footer>
        <el-button @click="showRawOutputDialog = false">关闭</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, computed, nextTick } from 'vue';
import { useRoute, useRouter, onBeforeRouteLeave } from 'vue-router';
import { ElMessage } from 'element-plus';
import SideBarMenu from '@/common/components/SideBarMenu.vue';
import WrongQuestionDisplay from './CET4WrongQuestionDisplay.vue';
import AITutorChat from './CET4AITutorChat.vue';
import TrainingQuestionDisplay from './CET4TrainingQuestionDisplay.vue';
import TrainingAnswerDisplay from './CET4TrainingAnswerDisplay.vue';
import wrongQuestionsApi from '@/cet4/api/CET4wrongQuestions';
import examGeneratorApi from '@/cet4/api/CET4examGenerator';
import aiTutorApi from '@/cet4/api/CET4aiTutor';
import axios from 'axios';
import { useAuthStore } from '@/common/stores/auth';

export default {
  name: 'CET4WrongQuestionDetail',
  components: {
    SideBarMenu,
    WrongQuestionDisplay,
    AITutorChat,
    TrainingQuestionDisplay,
    TrainingAnswerDisplay
  },
  setup() {
    const route = useRoute();
    const router = useRouter();
    const authStore = useAuthStore();
    
    // 基础状态
    const loading = ref(true);
    const errorMessage = ref('');
    const questionData = ref(null);
    const userAnswers = ref({});
    const correctAnswers = ref({});
    
    // 主观题数据
    const userAnswer = ref('');
    const aiScore = ref(undefined);
    const aiFeedback = ref('');
    const aiReasoning = ref('');

    // DeepSeek原始输出相关
    const showRawOutputDialog = ref(false);
    const rawOutput = ref('');
    const generationTime = ref('');
    const generatedTopic = ref('');
    const generatingExam = ref(false);

    // Metadata信息
    const examPaperEnSource = ref('');
    const segmentIdSelf = ref('');

    // AI辅导老师状态
    const chatMessages = ref([]);
    const aiThinking = ref(false);
    const currentUserId = computed(() => authStore.user?.id ?? null);

    // 训练模式状态
    const isTrainingMode = ref(false);
    const showTrainingAnswer = ref(false);
    const trainingQuestionData = ref(null);
    const userTrainingAnswers = ref({});
    const userTrainingAnswer = ref('');
    const trainingAiScore = ref(undefined);
    const trainingAiFeedback = ref('');
    const trainingAiReasoning = ref('');

    // 答疑模式状态
    const isTutoringMode = ref(false);
    const hasSavedSession = ref(false);

    // 从路由参数中获取
    const testEnId = route.params.testEnId;
    const segmentId = route.params.segmentId;

    // 获取题型
    const questionType = computed(() => {
      if (!questionData.value) return '';
      
      if (questionData.value.metadata && questionData.value.metadata.question_type) {
        return questionData.value.metadata.question_type;
      }
      
      if (questionData.value.question_type) {
        return questionData.value.question_type;
      }
      
      if (questionData.value.part_id) {
        const partId = questionData.value.part_id;
        if (partId === 'Section A') return 'BlankedCloze';
        if (partId === 'Section B') return 'Matching';
        if (partId === 'Section C') return 'ReadingComprehension';
      }
      
      if (questionData.value.unit_type) {
        const unitType = questionData.value.unit_type.toLowerCase();
        if (unitType.includes('news')) return 'NewsReport';
        if (unitType.includes('conversation')) return 'Conversation';
        if (unitType.includes('passage')) return 'ListeningPassage';
      }
      
      return '';
    });

    // 获取错题详情
    const fetchWrongQuestionDetail = async () => {
      loading.value = true;
      errorMessage.value = '';

      try {
        console.debug('[WrongQuestionDetail] start fetch', { testEnId, segmentId });
        const response = await wrongQuestionsApi.getWrongQuestionDetail(
          testEnId,
          segmentId
        );

        console.debug('[WrongQuestionDetail] api response', response);
        if (response.success) {
          const mergeMeta = (target, meta) => {
            if (!target || !meta) return target;
            target.metadata = target.metadata || meta;
            if (!target.question_type && meta.question_type) target.question_type = meta.question_type;
            if (!target.unit_type && meta.unit_type) target.unit_type = meta.unit_type;
            if (!target.part_id && meta.part_id) target.part_id = meta.part_id;
            return target;
          };

          const normalizeContent = (raw) => {
            let base = raw;
            // 纯字符串 -> JSON
            if (typeof base === 'string') {
              try { base = JSON.parse(base); } catch (_) { return base; }
            }

            // documentJson 优先解析
            if (base && typeof base.documentJson === 'string') {
              try {
                const parsedDoc = JSON.parse(base.documentJson);
                return mergeMeta({ ...parsedDoc }, base.metadata || base);
              } catch (_) {
                // fall through
              }
            }

            // document 解析
            if (base && typeof base.document === 'string') {
              try {
                const parsedDoc = JSON.parse(base.document);
                return mergeMeta({ ...parsedDoc }, base.metadata || base);
              } catch (_) {
                return mergeMeta(base, base.metadata || base);
              }
            }

            // 已是对象，尝试补充元数据
            return mergeMeta(base, base.metadata || base);
          };

          questionData.value = normalizeContent(response.data.questionContent);
          console.debug('[WrongQuestionDetail] normalized questionData', questionData.value);

          userAnswers.value = response.data.userAnswers || {};
          correctAnswers.value = response.data.correctAnswers || {};
          console.debug('[WrongQuestionDetail] answers', { userAnswers: userAnswers.value, correctAnswers: correctAnswers.value });
          
          if (response.data.userAnswer) {
            userAnswer.value = response.data.userAnswer;
          }
          if (response.data.aiScore !== undefined) {
            aiScore.value = response.data.aiScore;
          }
          if (response.data.aiFeedback) {
            aiFeedback.value = response.data.aiFeedback;
          }
          if (response.data.aiReasoning) {
            aiReasoning.value = response.data.aiReasoning;
          }
          console.debug('[WrongQuestionDetail] subjective info', { userAnswer: userAnswer.value, aiScore: aiScore.value, aiFeedback: aiFeedback.value, aiReasoning: aiReasoning.value });

          // 提取metadata
          if (questionData.value) {
            const metadata = questionData.value.metadata || questionData.value;
            
            if (metadata.examPaperEnSource) {
              examPaperEnSource.value = metadata.examPaperEnSource;
            } else if (metadata.exam_paper_en_source) {
              examPaperEnSource.value = metadata.exam_paper_en_source;
            }
            
            if (metadata.segmentId) {
              segmentIdSelf.value = metadata.segmentId;
            } else if (metadata.segment_id) {
              segmentIdSelf.value = metadata.segment_id;
            } else if (segmentId) {
              segmentIdSelf.value = segmentId;
            }
          }
        } else {
          errorMessage.value = response.message || '获取错题详情失败';
          console.warn('[WrongQuestionDetail] api returned failure', errorMessage.value);
          ElMessage.error(errorMessage.value);
        }
      } catch (error) {
        console.error('[WrongQuestionDetail] fetch failed', error);
        errorMessage.value = '获取错题详情失败，请稍后重试';
        ElMessage.error(errorMessage.value);
      } finally {
        loading.value = false;
        console.debug('[WrongQuestionDetail] fetch done, loading=', loading.value);
      }
    };

    // 复制原始输出
    const copyRawOutput = async () => {
      try {
        await navigator.clipboard.writeText(rawOutput.value);
        ElMessage.success('已复制到剪贴板');
      } catch (error) {
        const textarea = document.createElement('textarea');
        textarea.value = rawOutput.value;
        textarea.style.position = 'fixed';
        textarea.style.opacity = '0';
        document.body.appendChild(textarea);
        textarea.select();
        try {
          document.execCommand('copy');
          ElMessage.success('已复制到剪贴板');
        } catch (e) {
          ElMessage.error('复制失败，请手动复制');
        }
        document.body.removeChild(textarea);
      }
    };

    // 错题同类型再训练
    const handleRetryQuestionType = async () => {
      if (!questionData.value) {
        ElMessage.error('题目数据未加载');
        return;
      }
      
      generatingExam.value = true;
      errorMessage.value = '';

      try {
        const sourceToUse = 'AIfromWrongBank';  // 标记为错题训练
        const qt = questionType.value;

        // 组装 Coze 所需四个参数
        const examTopic = questionData.value?.topic || 'CET4 Training';
        const segmentId = segmentIdSelf.value || (questionData.value?.metadata?.segment_id) || '';
        const docJson = JSON.stringify(questionData.value);

        if (!examTopic || !docJson || !segmentId) {
          throw new Error('缺少生成所需参数（topic/document/segmentId）');
        }

        ElMessage.info('正在调用Coze生成训练题目，请稍候...');

        const resp = await axios.post('http://localhost:8080/api/cet4/paper-gen/coze/generate-single', {
          examTopic,
          inputExamPaperSamp: docJson,
          examPaperEnSource: sourceToUse,
          segmentIdSelf: segmentId
        }, { withCredentials: true, timeout: 30 * 60 * 1000 });

        const data = resp?.data || {};
        if (data.success && data.data) {
          ElMessage.success('训练题目生成成功！正在跳转...');

          const output = data.data;
          const trainData = {
            questions: [{
              questionType: qt,
              segmentIdSelf: segmentId + '_retry',
              documentJson: typeof output === 'string' ? output : JSON.stringify(output),
              examPaperEnSource: sourceToUse
            }],
            examPaperEnSource: sourceToUse,
            totalCount: 1,
            paperName: `错题再训练-${qt}`,
            timestamp: Date.now()
          };

          // 存储到 sessionStorage
          sessionStorage.setItem('intensiveTrainQuestions', JSON.stringify(trainData));

          // 跳转到预览页面，带上错题再训练标记
          router.push({
            name: 'CET4IntensiveTrainPreview',
            query: {
              exam_paper_en_source: trainData.examPaperEnSource,
              from_wrong_question: 'true'  // 标记为错题再训练模式
            }
          });
        } else {
          const msg = data.message || '生成训练题目失败';
          errorMessage.value = msg;
          ElMessage.error(msg);
        }
      } catch (error) {
        const serverMsg = error.response?.data?.message || error.message || '生成训练题目失败，请稍后重试';
        errorMessage.value = serverMsg;
        ElMessage.error(serverMsg);
      } finally {
        generatingExam.value = false;
      }
    };

    // 提交训练答案
    const handleSubmitTrainingAnswer = async (answers) => {
      try {
        ElMessage.info('正在评分中，请稍候...');
        
        // 根据题型处理答案
        const qType = questionType.value;
        
        if (qType === 'Writing' || qType === 'Translation') {
          // 主观题需要AI评分
          const response = await axios.post('/api/ai/grade-subjective', {
            questionData: trainingQuestionData.value,
            userAnswer: answers.subjectiveAnswer,
            questionType: qType
          });
          
          if (response.data.success) {
            trainingAiScore.value = response.data.score;
            trainingAiFeedback.value = response.data.feedback;
            trainingAiReasoning.value = response.data.reasoning;
            userTrainingAnswer.value = answers.subjectiveAnswer;
          }
        } else {
          // 客观题直接对比答案
          userTrainingAnswers.value = answers.objectiveAnswers || {};
        }
        
        // 显示答案
        showTrainingAnswer.value = true;
        ElMessage.success('答题完成！');
      } catch (error) {
        ElMessage.error('提交答案失败，请稍后重试');
      }
    };

    // 重新训练
    const retryTraining = () => {
      handleRetryQuestionType();
    };

    // 退出训练模式
    const exitTrainingMode = () => {
      isTrainingMode.value = false;
      showTrainingAnswer.value = false;
      trainingQuestionData.value = null;
      userTrainingAnswers.value = {};
      userTrainingAnswer.value = '';
      trainingAiScore.value = undefined;
      trainingAiFeedback.value = '';
      trainingAiReasoning.value = '';
    };

    // 进入答疑模式
    const startTutoringMode = () => {
      isTutoringMode.value = true;
      ElMessage.success('已进入答疑模式');
    };

    // 返回上一页
    const goBack = () => {
      router.push({ name: 'WrongQuestions' });
    };

    // ==================== AI辅导老师功能 ====================
    
    // 格式化时间
    const formatTime = () => {
      const now = new Date();
      return now.toLocaleTimeString('zh-CN', { 
        hour: '2-digit', 
        minute: '2-digit' 
      });
    };
    
    // 滚动到聊天底部
    const scrollToBottom = () => {
      nextTick(() => {
        const chatContainer = document.querySelector('.chat-messages');
        if (chatContainer) {
          chatContainer.scrollTop = chatContainer.scrollHeight;
        }
      });
    };
    
    // 准备用户答案列表
    const prepareUserAnswersList = () => {
      const answersList = [];
      for (const [questionNumber, answer] of Object.entries(userAnswers.value)) {
        answersList.push({
          question_number: parseInt(questionNumber) || questionNumber,
          user_answer: answer
        });
      }
      return answersList;
    };
    
    // 发送问题给AI
    const sendQuestion = async (question) => {
      if (!questionData.value || !segmentIdSelf.value) {
        ElMessage.error('题目数据未加载完成，请稍候');
        return;
      }

      if (!currentUserId.value) {
        ElMessage.error('请先登录后再进行提问');
        return;
      }
      
      // 添加用户消息到聊天记录
      chatMessages.value.push({
        role: 'user',
        content: question,
        timestamp: formatTime()
      });
      
      // 开始思考
      aiThinking.value = true;
      
      try {
        const document = JSON.stringify(questionData.value);
        const userAnswersList = prepareUserAnswersList();
        const questionTypeValue = questionType.value || 'Unknown';
        
        // 创建AI消息对象(用于流式更新)
        const aiMessageIndex = chatMessages.value.length;
        chatMessages.value.push({
          role: 'ai',
          content: '',
          timestamp: formatTime()
        });
        
        // 使用流式API
        aiTutorApi.askQuestionStream(
          currentUserId.value,
          segmentIdSelf.value,
          questionTypeValue,
          document,
          userAnswersList,
          question,
          // onChunk: 接收数据块
          (chunk) => {
            // 直接修改数组中的元素，触发Vue响应式更新
            chatMessages.value[aiMessageIndex].content += chunk;
            // 自动滚动到底部
            scrollToBottom();
          },
          // onComplete: 完成时
          (data) => {
            aiThinking.value = false;
            scrollToBottom();
          },
          // onError: 错误时
          (error) => {
            aiThinking.value = false;
            ElMessage.error('AI回答失败: ' + error.message);
            
            if (chatMessages.value[aiMessageIndex].content === '') {
              chatMessages.value[aiMessageIndex].content = '抱歉，我遇到了一些问题，请稍后再试。';
            }
          }
        );
        
      } catch (error) {
        aiThinking.value = false;
        ElMessage.error('发送问题失败，请稍后重试');
        
        chatMessages.value.push({
          role: 'ai',
          content: '抱歉，我遇到了一些问题，请稍后再试。',
          timestamp: formatTime()
        });
      } finally {
        aiThinking.value = false;
      }
    };
    
    // 组件挂载
    onMounted(() => {
      fetchWrongQuestionDetail();
    });
    
    // 组件卸载前保存会话
    const saveSessionIfNeeded = async () => {
      console.log('[SaveSession] 检查保存条件...');
      console.log('[SaveSession] hasSavedSession:', hasSavedSession.value);
      console.log('[SaveSession] currentUserId:', currentUserId.value);
      console.log('[SaveSession] segmentIdSelf:', segmentIdSelf.value);
      console.log('[SaveSession] chatMessages.length:', chatMessages.value.length);
      
      if (hasSavedSession.value) {
        console.log('[SaveSession] 已经保存过了，跳过');
        return;
      }
      if (!currentUserId.value || !segmentIdSelf.value || chatMessages.value.length === 0) {
        console.log('[SaveSession] 不满足保存条件，跳过');
        return;
      }
      
      console.log('[SaveSession] 开始保存会话到ChromaDB...');
      try {
        await aiTutorApi.endSession(currentUserId.value, segmentIdSelf.value);
        hasSavedSession.value = true;
        console.log('[SaveSession] ✅ 会话保存成功');
        ElMessage.success('对话已保存');
      } catch (error) {
        console.error('[SaveSession] ❌ 保存AI辅导会话失败:', error);
        ElMessage.error('保存对话失败');
      }
    };

    onBeforeUnmount(async () => {
      console.log('[Component] onBeforeUnmount 触发');
      await saveSessionIfNeeded();
    });

    onBeforeRouteLeave(async (to, from, next) => {
      console.log('[Component] onBeforeRouteLeave 触发，目标路由:', to.path);
      await saveSessionIfNeeded();
      next();
    });

    return {
      // 基础数据
      loading,
      errorMessage,
      questionData,
      userAnswers,
      correctAnswers,
      userAnswer,
      aiScore,
      aiFeedback,
      aiReasoning,
      // DeepSeek输出
      showRawOutputDialog,
      rawOutput,
      generationTime,
      generatedTopic,
      generatingExam,
      // Metadata
      examPaperEnSource,
      segmentIdSelf,
      // AI辅导
      chatMessages,
      aiThinking,
      // 训练模式
      isTrainingMode,
      showTrainingAnswer,
      trainingQuestionData,
      userTrainingAnswers,
      userTrainingAnswer,
      trainingAiScore,
      trainingAiFeedback,
      trainingAiReasoning,
      // 答疑模式
      isTutoringMode,
      questionType,
      // 方法
      copyRawOutput,
      handleRetryQuestionType,
      handleSubmitTrainingAnswer,
      retryTraining,
      exitTrainingMode,
      startTutoringMode,
      goBack,
      sendQuestion
    };
  }
};
</script>

<style scoped>
.test-result-container {
  display: flex;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.main-content {
  flex: 1;
  margin-left: 250px;
  width: calc(100% - 250px);
  box-sizing: border-box;
  overflow: hidden;
}

/* 训练模式容器 */
.training-mode-container,
.training-answer-container {
  width: 100%;
  padding: 20px;
  min-height: calc(100vh - 40px);
  overflow-y: auto;
}

/* 全屏错题展示容器 */
.fullscreen-container {
  width: 100%;
  padding: 20px;
  min-height: calc(100vh - 40px);
  overflow-y: auto;
}

/* 左右分栏容器（答疑模式） */
.split-view-container {
  display: flex;
  height: calc(100vh - 0px);
  gap: 0;
}

/* 左侧面板 - 错题展示 */
.left-panel {
  flex: 0 0 50%;
  overflow-y: auto;
  background-color: #f5f7fa;
  border-right: 2px solid #e4e7ed;
}

/* 右侧面板 - AI对话 */
.right-panel {
  flex: 0 0 50%;
  overflow: hidden;
  background-color: #ffffff;
  display: flex;
  flex-direction: column;
}

@media (max-width: 768px) {
  .main-content {
    margin-left: 0;
    width: 100%;
  }

  .split-view-container {
    flex-direction: column;
    height: auto;
  }

  .left-panel,
  .right-panel {
    flex: 0 0 auto;
    width: 100%;
    border-right: none;
  }

  .right-panel {
    border-top: 2px solid #e4e7ed;
    min-height: 500px;
  }
}

/* DeepSeek原始输出对话框样式 */
.raw-output-dialog {
  .raw-output-container {
    max-height: 70vh;
    overflow-y: auto;
  }

  .output-header {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 20px;
    padding: 16px;
    background: #f5f7fa;
    border-radius: 8px;
    flex-wrap: wrap;
  }

  .output-header .el-tag {
    font-size: 14px;
    padding: 8px 16px;
  }

  .output-content {
    background: #1e1e1e;
    border-radius: 8px;
    padding: 20px;
    box-shadow: inset 0 2px 8px rgba(0, 0, 0, 0.3);
  }

  .raw-output-text {
    color: #d4d4d4;
    font-family: 'Consolas', 'Monaco', 'Courier New', monospace;
    font-size: 13px;
    line-height: 1.6;
    margin: 0;
    white-space: pre-wrap;
    word-break: break-word;
    max-height: 60vh;
    overflow-y: auto;
  }

  .raw-output-text::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }

  .raw-output-text::-webkit-scrollbar-track {
    background: #2d2d2d;
    border-radius: 4px;
  }

  .raw-output-text::-webkit-scrollbar-thumb {
    background: #555;
    border-radius: 4px;
  }

  .raw-output-text::-webkit-scrollbar-thumb:hover {
    background: #777;
  }
}

/* Element Plus 对话框样式覆盖 */
:deep(.el-dialog__header) {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  padding: 20px;
  border-radius: 8px 8px 0 0;
}

:deep(.el-dialog__title) {
  color: white;
  font-size: 18px;
  font-weight: 600;
}

:deep(.el-dialog__headerbtn .el-dialog__close) {
  color: white;
  font-size: 20px;
}

:deep(.el-dialog__body) {
  padding: 20px;
}

:deep(.el-dialog__footer) {
  padding: 15px 20px;
  border-top: 1px solid #e4e7ed;
}
</style>
