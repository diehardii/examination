<template>
  <div class="modal-overlay" v-if="visible" @click.self="handleCancel">
    <div class="modal-content">
      <h3>保存试卷信息</h3>
      
      <div class="form-group">
        <label for="paper-name">试卷名称 <span class="required">*</span></label>
        <input
          id="paper-name"
          type="text"
          :value="name"
          @input="$emit('update:name', $event.target.value)"
          placeholder="请输入试卷名称"
          :disabled="saving"
        />
      </div>
      
      <div class="form-group">
        <label for="paper-desc">试卷描述</label>
        <textarea
          id="paper-desc"
          :value="description"
          @input="$emit('update:description', $event.target.value)"
          placeholder="请输入试卷描述（可选）"
          rows="3"
          :disabled="saving"
        ></textarea>
      </div>
      
      <div v-if="error" class="error-message">{{ error }}</div>
      
      <div class="modal-actions">
        <button 
          class="btn-cancel" 
          @click="handleCancel"
          :disabled="saving"
        >
          取消
        </button>
        <button 
          class="btn-confirm" 
          @click="handleConfirm"
          :disabled="saving"
        >
          {{ saving ? '保存中...' : '确认保存' }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'HS3SaveMetaModal',
  props: {
    visible: {
      type: Boolean,
      default: false
    },
    saving: {
      type: Boolean,
      default: false
    },
    name: {
      type: String,
      default: ''
    },
    description: {
      type: String,
      default: ''
    },
    error: {
      type: String,
      default: ''
    }
  },
  emits: ['update:name', 'update:description', 'cancel', 'confirm'],
  methods: {
    handleCancel() {
      if (!this.saving) {
        this.$emit('cancel');
      }
    },
    handleConfirm() {
      if (!this.saving) {
        this.$emit('confirm');
      }
    }
  }
};
</script>

<style scoped>
.modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: white;
  padding: 24px;
  border-radius: 12px;
  width: 90%;
  max-width: 450px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.15);
}

.modal-content h3 {
  margin: 0 0 20px 0;
  color: #2c3e50;
  font-size: 18px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 6px;
  color: #546e7a;
  font-size: 14px;
  font-weight: 500;
}

.required {
  color: #e53935;
}

.form-group input,
.form-group textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 8px;
  font-size: 14px;
  transition: border-color 0.2s;
  box-sizing: border-box;
}

.form-group input:focus,
.form-group textarea:focus {
  outline: none;
  border-color: #4caf50;
}

.form-group input:disabled,
.form-group textarea:disabled {
  background: #f5f5f5;
  cursor: not-allowed;
}

.error-message {
  color: #e53935;
  font-size: 13px;
  margin-bottom: 16px;
  padding: 8px 12px;
  background: #ffebee;
  border-radius: 6px;
}

.modal-actions {
  display: flex;
  gap: 12px;
  justify-content: flex-end;
  margin-top: 20px;
}

.btn-cancel,
.btn-confirm {
  padding: 10px 24px;
  border: none;
  border-radius: 8px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-cancel {
  background: #f0f0f0;
  color: #666;
}

.btn-cancel:hover:not(:disabled) {
  background: #e0e0e0;
}

.btn-confirm {
  background: linear-gradient(135deg, #4caf50 0%, #45a049 100%);
  color: white;
}

.btn-confirm:hover:not(:disabled) {
  transform: translateY(-1px);
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.35);
}

.btn-cancel:disabled,
.btn-confirm:disabled {
  opacity: 0.6;
  cursor: not-allowed;
  transform: none;
}
</style>
