<template>
  <div class="smart-qa-container">
    <SideBarMenu />
    <div class="content-area">
      <div class="header-block">
        <h1>智能问答</h1>
      </div>

      <el-card shadow="hover" class="analysis-card">
        <template #header>
          <div class="card-header">
            <span>您最近考试的情况总结与分析</span>
            <el-tag type="info" size="small" v-if="analysisUpdatedAt">{{ analysisUpdatedAt }}</el-tag>
          </div>
        </template>
        <div class="analysis-body" v-loading="loadingBackground">
          <el-empty 
            v-if="!loadingBackground && !analysisDocument" 
            description="暂无分析记录，请先完成一次考试"
            :image-size="120"
          />
          <div v-else class="analysis-content">
            <div 
              v-for="(section, index) in analysisSections" 
              :key="index" 
              class="analysis-section"
            >
              <h3 class="section-title">{{ section.title }}</h3>
              <div class="section-body" v-html="section.html"></div>
            </div>
          </div>
        </div>
      </el-card>

      <div class="chat-panel">
        <AITutorChat
          :messages="chatMessages"
          :thinking="aiThinking"
          @send-question="sendQuestion"
        />
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue';
import { onBeforeRouteLeave } from 'vue-router';
import { ElMessage } from 'element-plus';
import SideBarMenu from '@/common/components/SideBarMenu.vue';
import AITutorChat from '@/cet4/components/CET4AITutorChat.vue';
import smartQaApi from '@/cet4/api/CET4smartQA';
import { useAuthStore } from '@/common/stores/auth';
import { marked } from 'marked';

export default {
  name: 'CET4SmartQAView',
  components: {
    SideBarMenu,
    AITutorChat
  },
  setup() {
    const authStore = useAuthStore();
    const analysisDocument = ref('');
    const analysisUpdatedAt = ref('');
    const loadingBackground = ref(false);
    const chatMessages = ref([]);
    const aiThinking = ref(false);
    const hasSavedSession = ref(false);
    const segmentId = 'analysis';

    // 解析分析文档为结构化内容
    const analysisSections = computed(() => {
      if (!analysisDocument.value) return [];
      
      try {
        // 尝试解析JSON
        let content = analysisDocument.value;
        try {
          const parsed = JSON.parse(content);
          if (parsed.data) {
            const dataObj = typeof parsed.data === 'string' ? JSON.parse(parsed.data) : parsed.data;
            content = dataObj.output || content;
          } else if (parsed.output) {
            content = parsed.output;
          }
        } catch (e) {
          // 如果不是JSON，直接使用原文
        }

        // 按三级标题分段
        const sections = [];
        const lines = content.split('\n');
        let currentSection = null;
        let currentContent = [];

        lines.forEach(line => {
          const h3Match = line.match(/^###\s+(.+)/);
          if (h3Match) {
            // 保存上一段
            if (currentSection) {
              sections.push({
                title: currentSection,
                html: marked.parse(currentContent.join('\n'))
              });
            }
            // 开始新段
            currentSection = h3Match[1].trim();
            currentContent = [];
          } else if (currentSection) {
            currentContent.push(line);
          }
        });

        // 保存最后一段
        if (currentSection) {
          sections.push({
            title: currentSection,
            html: marked.parse(currentContent.join('\n'))
          });
        }

        return sections.length > 0 ? sections : [{
          title: '分析内容',
          html: marked.parse(content)
        }];
      } catch (e) {
        console.error('解析分析文档失败', e);
        return [{
          title: '分析内容',
          html: `<pre>${analysisDocument.value}</pre>`
        }];
      }
    });

    const formatTime = () => {
      const now = new Date();
      return now.toLocaleTimeString('zh-CN', { hour: '2-digit', minute: '2-digit' });
    };

    const fetchBackground = async () => {
      const userId = authStore.user?.id;
      if (!userId) return;
      loadingBackground.value = true;
      try {
        const res = await smartQaApi.fetchBackground(userId);
        if (res.success) {
          analysisDocument.value = res.data?.analysis_document || '';
          analysisUpdatedAt.value = res.data?.qa_document ? '已载入历史摘要' : '';
        } else {
          ElMessage.error(res.message || '获取分析背景失败');
        }
      } catch (error) {
        ElMessage.error('获取分析背景失败');
      } finally {
        loadingBackground.value = false;
      }
    };

    const sendQuestion = (question) => {
      const userId = authStore.user?.id;
      if (!userId) {
        ElMessage.error('请先登录后再进行提问');
        return;
      }
      chatMessages.value.push({ role: 'user', content: question, timestamp: formatTime() });
      aiThinking.value = true;
      const aiIndex = chatMessages.value.length;
      chatMessages.value.push({ role: 'ai', content: '', timestamp: formatTime() });

      smartQaApi.askQuestionStream(
        userId,
        question,
        (chunk) => {
          chatMessages.value[aiIndex].content += chunk;
        },
        () => {
          aiThinking.value = false;
        },
        (error) => {
          aiThinking.value = false;
          ElMessage.error('AI回答失败: ' + error.message);
          if (!chatMessages.value[aiIndex].content) {
            chatMessages.value[aiIndex].content = '抱歉，暂时无法回答，请稍后再试。';
          }
        },
        segmentId
      );
    };

    const saveSessionIfNeeded = async () => {
      const userId = authStore.user?.id;
      if (hasSavedSession.value || !userId || chatMessages.value.length === 0) return;
      try {
        await smartQaApi.endSession(userId, segmentId);
        hasSavedSession.value = true;
      } catch (error) {
        console.error('保存智能问答会话失败', error);
      }
    };

    onMounted(() => {
      fetchBackground();
    });

    onBeforeUnmount(async () => {
      await saveSessionIfNeeded();
    });

    onBeforeRouteLeave(async (to, from, next) => {
      await saveSessionIfNeeded();
      next();
    });

    return {
      analysisDocument,
      analysisUpdatedAt,
      loadingBackground,
      chatMessages,
      aiThinking,
      sendQuestion,
      analysisSections
    };
  }
};
</script>

<style scoped>
.smart-qa-container {
  display: flex;
  min-height: 100vh;
  background-color: #f5f7fa;
}

.content-area {
  flex: 1;
  margin-left: 260px;
  padding: 24px 24px 32px;
  box-sizing: border-box;
  display: flex;
  flex-direction: column;
  height: calc(100vh - 48px);
  overflow: hidden;
}

.chat-panel {
  flex: 0 0 auto;
  height: 45vh; /* 缩小对话空间约3行 */
  margin-top: 24px; /* 再下移约两行 */
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.08);
  overflow: hidden;
}

.header-block {
  margin-bottom: 12px;
  margin-left: 2em;
}

.header-block h1 {
  margin: 0;
  padding: 8px 12px;
  font-size: 16px;
  color: white;
  font-weight: 600;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.2);
  display: inline-block;
}

.analysis-card {
  width: 100%;
  margin-bottom: 12px;
  margin-top: 12px; /* 上半部分整体下移约两行 */
  flex: 1;
  max-height: 70vh; /* 扩大分析展示空间约3行 */
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.analysis-card :deep(.el-card__body) {
  flex: 1;
  overflow: hidden;
  padding: 0;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
}

.analysis-body {
  height: 100%;
  overflow-y: auto;
  background: #fff;
  border-radius: 8px;
  padding: 16px;
}

.analysis-content {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.analysis-section {
  border-left: 4px solid #409eff;
  padding-left: 16px;
}

.section-title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
  margin: 0 0 12px 0;
}

.section-body {
  color: #606266;
  line-height: 1.8;
}

.section-body :deep(h4) {
  font-size: 16px;
  font-weight: 600;
  color: #409eff;
  margin: 16px 0 8px 0;
}

.section-body :deep(ul),
.section-body :deep(ol) {
  margin: 8px 0;
  padding-left: 24px;
}

.section-body :deep(li) {
  margin: 4px 0;
  line-height: 1.6;
}

.section-body :deep(strong) {
  color: #303133;
  font-weight: 600;
}

.section-body :deep(p) {
  margin: 8px 0;
}

.section-body :deep(code) {
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 3px;
  font-family: 'SFMono-Regular', Consolas, monospace;
  color: #e74c3c;
}

.analysis-body::-webkit-scrollbar {
  width: 8px;
}

.analysis-body::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.analysis-body::-webkit-scrollbar-thumb {
  background: #c0c4cc;
  border-radius: 4px;
}

.analysis-body::-webkit-scrollbar-thumb:hover {
  background: #909399;
}

@media (max-width: 1024px) {
  .content-area {
    margin-left: 0;
  }
}
</style>
