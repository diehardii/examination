<template>
  <div v-if="visible" class="modal-mask">
    <div class="modal-container">
      <h3 class="modal-title">{{ title }}</h3>
      <label class="modal-label">试卷名称</label>
      <input
        class="modal-input"
        type="text"
        :placeholder="namePlaceholder"
        :value="name"
        :disabled="saving"
        @input="onNameInput"
      />

      <label class="modal-label">试卷描述</label>
      <textarea
        class="modal-textarea"
        rows="3"
        :placeholder="descPlaceholder"
        :value="description"
        :disabled="saving"
        @input="onDescInput"
      ></textarea>

      <div v-if="error" class="modal-error">{{ error }}</div>

      <div class="modal-actions">
        <button class="submit-btn" @click="$emit('cancel')" :disabled="saving">取消</button>
        <button class="submit-btn" @click="emitConfirm" :disabled="saving">
          {{ saving ? loadingText : confirmText }}
        </button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'CET4SaveMetaModal',
  props: {
    visible: { type: Boolean, default: false },
    saving: { type: Boolean, default: false },
    name: { type: String, default: '' },
    description: { type: String, default: '' },
    error: { type: String, default: '' },
    title: { type: String, default: '填写试卷信息' },
    confirmText: { type: String, default: '确认保存' },
    loadingText: { type: String, default: '保存中...' },
    namePlaceholder: { type: String, default: '例如：AI试卷生成1' },
    descPlaceholder: { type: String, default: '请输入试卷描述' }
  },
  emits: ['update:name', 'update:description', 'confirm', 'cancel'],
  methods: {
    onNameInput(event) {
      this.$emit('update:name', event.target.value)
    },
    onDescInput(event) {
      this.$emit('update:description', event.target.value)
    },
    emitConfirm() {
      this.$emit('confirm')
    }
  }
}
</script>

<style scoped>
.modal-mask {
  position: fixed;
  z-index: 999;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
}

.modal-container {
  background: #fff;
  border-radius: 8px;
  padding: 20px;
  width: 420px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12);
}

.modal-title {
  margin: 0 0 16px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.modal-label {
  display: block;
  margin-bottom: 6px;
  font-size: 13px;
  color: #555;
}

.modal-input,
.modal-textarea {
  width: 100%;
  border: 1px solid #dcdfe6;
  border-radius: 6px;
  padding: 10px 12px;
  font-size: 14px;
  box-sizing: border-box;
  margin-bottom: 12px;
  outline: none;
  transition: border-color 0.2s ease;
}

.modal-input:focus,
.modal-textarea:focus {
  border-color: #409eff;
}

.modal-textarea {
  resize: vertical;
}

.modal-error {
  color: #f56c6c;
  margin-bottom: 12px;
  font-size: 13px;
}

.modal-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
}

.submit-btn {
  border: none;
  background: #409eff;
  color: #fff;
  padding: 10px 16px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s ease;
}

.submit-btn:disabled {
  background: #a0cfff;
  cursor: not-allowed;
}

.submit-btn:not(:disabled):hover {
  background: #66b1ff;
}
</style>
