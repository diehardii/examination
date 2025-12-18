<template>
  <div class="ai-tutor-chat">
    <div class="ai-tutor-header">
      <span class="ai-icon">🤖</span>
      <h2>AI辅导老师</h2>
    </div>
    
    <!-- 对话消息区域 -->
    <div class="chat-messages" ref="chatMessagesRef">
      <!-- 初始欢迎消息 -->
      <div v-if="messages.length === 0" class="message ai-message">
        <div class="message-avatar">🤖</div>
        <div class="message-content">
          <p>请问您有什么问题需要老师帮助吗？</p>
        </div>
      </div>
      
      <!-- 对话消息列表 -->
      <div 
        v-for="(msg, index) in messages" 
        :key="index"
        :class="['message', msg.role === 'user' ? 'user-message' : 'ai-message']"
      >
        <div class="message-avatar">
          {{ msg.role === 'user' ? '👤' : '🤖' }}
        </div>
        <div class="message-content">
          <p>{{ msg.content }}</p>
          <span class="message-time">{{ msg.timestamp }}</span>
        </div>
      </div>
      
      <!-- 加载中提示 -->
      <div v-if="thinking" class="message ai-message">
        <div class="message-avatar">🤖</div>
        <div class="message-content">
          <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>
    
    <!-- 输入区域 -->
    <div class="chat-input-area">
      <el-input
        v-model="currentQuestion"
        placeholder="在这里输入您的问题..."
        :disabled="thinking"
        @keydown.enter="handleSend"
      />
      <el-button
        type="primary"
        :loading="thinking"
        :disabled="!currentQuestion.trim() || thinking"
        @click="handleSend"
        class="send-button"
      >
        {{ thinking ? '思考中' : '发送' }}
      </el-button>
    </div>
  </div>
</template>

<script>
import { ref, nextTick, watch } from 'vue';

export default {
  name: 'AITutorChat',
  props: {
    messages: {
      type: Array,
      default: () => []
    },
    thinking: {
      type: Boolean,
      default: false
    }
  },
  emits: ['send-question'],
  setup(props, { emit }) {
    const currentQuestion = ref('');
    const chatMessagesRef = ref(null);

    // 滚动到底部
    const scrollToBottom = async () => {
      await nextTick();
      if (chatMessagesRef.value) {
        chatMessagesRef.value.scrollTop = chatMessagesRef.value.scrollHeight;
      }
    };

    // 发送消息
    const handleSend = () => {
      if (!currentQuestion.value.trim() || props.thinking) {
        return;
      }
      
      const question = currentQuestion.value.trim();
      emit('send-question', question);
      currentQuestion.value = '';
      scrollToBottom();
    };

    // 监听消息变化，自动滚动
    watch(() => props.messages.length, () => {
      scrollToBottom();
    });

    // 监听thinking状态变化，自动滚动
    watch(() => props.thinking, () => {
      scrollToBottom();
    });

    return {
      currentQuestion,
      chatMessagesRef,
      handleSend
    };
  }
};
</script>

<style scoped>
.ai-tutor-chat {
  display: flex;
  flex-direction: column;
  height: 100%;
  padding: 10px;
  background-color: #ffffff;
}

/* AI辅导老师标题 */
.ai-tutor-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border-radius: 8px;
  margin-bottom: 8px;
  box-shadow: 0 2px 6px rgba(102, 126, 234, 0.2);
}

.ai-icon {
  font-size: 20px;
}

.ai-tutor-header h2 {
  margin: 0;
  color: white;
  font-size: 16px;
  font-weight: 600;
}

/* 聊天消息区域 */
.chat-messages {
  flex: 1;
  overflow-y: auto;
  padding: 12px;
  background: #f8f9fa;
  border-radius: 8px;
  margin-bottom: 8px;
  display: flex;
  flex-direction: column;
  gap: 10px;
  min-height: 0;
}

.chat-messages::-webkit-scrollbar {
  width: 8px;
}

.chat-messages::-webkit-scrollbar-track {
  background: #f1f1f1;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb {
  background: #888;
  border-radius: 4px;
}

.chat-messages::-webkit-scrollbar-thumb:hover {
  background: #555;
}

/* 消息样式 */
.message {
  display: flex;
  gap: 12px;
  animation: fadeIn 0.3s ease-in;
}

@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.message-avatar {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  background: white;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
}

.message-content {
  flex: 1;
  padding: 8px 12px;
  border-radius: 8px;
  position: relative;
  max-width: 80%;
  font-size: 14px;
}

.ai-message .message-content {
  background: white;
  border: 1px solid #e4e7ed;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.user-message {
  flex-direction: row-reverse;
}

.user-message .message-content {
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  color: white;
  box-shadow: 0 2px 8px rgba(102, 126, 234, 0.3);
}

.message-content p {
  margin: 0;
  line-height: 1.6;
  word-wrap: break-word;
  white-space: pre-wrap;
}

.message-time {
  display: block;
  font-size: 12px;
  opacity: 0.7;
  margin-top: 8px;
}

.user-message .message-time {
  text-align: right;
  color: rgba(255, 255, 255, 0.8);
}

.ai-message .message-time {
  color: #909399;
}

/* 输入动画 */
.typing-indicator {
  display: flex;
  gap: 4px;
  padding: 8px 0;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #667eea;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
}

@keyframes typing {
  0%, 60%, 100% {
    transform: translateY(0);
    opacity: 0.7;
  }
  30% {
    transform: translateY(-10px);
    opacity: 1;
  }
}

/* 输入区域 */
.chat-input-area {
  display: flex;
  flex-direction: row;
  gap: 8px;
  padding: 8px;
  background: white;
  border-radius: 8px;
  box-shadow: 0 -1px 6px rgba(0, 0, 0, 0.05);
  align-items: center;
  margin-top: 12px; /* 让输入行下移约两行高度 */
}

.chat-input-area :deep(.el-input__wrapper) {
  border-radius: 6px;
  padding: 6px 12px;
}

.send-button {
  padding: 6px 16px;
  font-size: 14px;
  font-weight: 500;
  border-radius: 6px;
  background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
  border: none;
  white-space: nowrap;
  flex-shrink: 0;
}

.send-button:hover:not(:disabled) {
  opacity: 0.9;
}
</style>
