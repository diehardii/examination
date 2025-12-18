<template>
  <div class="app-container">
    <!-- 引入侧边栏组件 -->
    <SideBarMenu />

    <!-- 右侧内容区 -->
    <div class="main-content">
      <div class="content">
        <div class="card">
          <h1 class="header">请填写高考试卷的要求</h1>

          <!-- 表单和图片容器 -->
          <div class="form-image-container">
            <!-- 左边：表单 -->
            <div class="form-section">
              <form @submit.prevent="submitForm">
                <!-- 科目与后台生成选项 -->
                <div class="form-row subject-row">
                  <div class="form-group">
                    科目：
                    <select id="subject" v-model="form.subjectEn" class="select-control" required>
                      <option value="">-- 请选择科目 --</option>
                      <option value="高考">高考</option>
                    </select>
                  </div>
                  <label class="checkbox-label inline-checkbox">
                    <input type="checkbox" v-model="useAsyncMode" disabled>
                    后台生成(暂不支持)
                  </label>
                </div>
                <span class="hint small">当前仅支持前台同步生成</span>
                <br>
                
                <br>

                <!-- 错误和成功消息 -->
                <div v-if="error" class="error">{{ error }}</div>
                <div v-if="success" class="success">{{ success }}</div>

                <div class="task-list-section">
                  <div class="task-list-header">
                    <h3>生成任务</h3>
                    <button type="button" class="link-btn" @click="fetchTasks()" :disabled="taskLoading">
                      {{ taskLoading ? '刷新中...' : '刷新列表' }}
                    </button>
                  </div>
                  <div v-if="taskLoading" class="task-empty">任务数据加载中...</div>
                  <div v-else-if="!taskList.length" class="task-empty">暂无试卷生成任务，提交后可在此查看</div>
                  <div v-else class="task-list">
                    <div
                      v-for="task in taskList"
                      :key="task.id"
                      class="task-item"
                      :class="task.status.toLowerCase()"
                    >
                      <div class="task-info">
                        <div class="task-title-row">
                          <span class="task-name">{{ displayTaskName(task) }}</span>
                          <span class="task-status-pill" :class="task.status">{{ formatStatus(task.status) }}</span>
                        </div>
                        <div class="task-meta">
                          {{ task.subjectEn || '高考英语' }} · 提交于 {{ formatTimestamp(task.createdAt) }}
                        </div>
                        <el-progress
                          :percentage="Math.floor(task.progress || 0)"
                          :stroke-width="10"
                          :text-inside="true"
                          status="success"
                        />
                        <div v-if="task.status === 'FAILED' && task.message" class="task-error">{{ task.message }}</div>
                      </div>
                      <div class="task-actions">
                        <button
                          v-if="task.status === 'SUCCEEDED'"
                          type="button"
                          class="task-view-btn"
                          @click="viewTask(task)"
                        >查看</button>
                        <button
                          v-else
                          type="button"
                          class="task-view-btn disabled"
                          disabled
                        >{{ actionLabel(task.status) }}</button>
                      </div>
                    </div>
                  </div>
                </div>

                <!-- 提交按钮区域 -->
                <div class="submit-section">
                  <!-- 进度条 -->
                  <el-progress
                      v-if="isSubmitting"
                      :percentage="Math.floor(progress)"
                      :stroke-width="15"
                      :text-inside="true"
                      status="success"
                      style="margin-bottom: 20px; width: 100%"
                  />

                  <!-- 提交按钮 -->
                  <button
                      type="submit"
                      :disabled="isSubmitting"
                      class="submit-btn"
                  >
                    {{ isSubmitting ? '生成中...' : '生成试卷' }}
                  </button>
                </div>
              </form>
            </div>

            <!-- 右边：知识图谱 -->
            <div class="graph-section">
              <div class="graph-header">
                <h3>高考英语知识图谱</h3>
                <p v-if="selectedSubject">当前科目：{{ selectedSubject }}</p>
                <p v-else>请选择科目以查看相关知识图谱</p>
              </div>
              <div id="knowledge-graph-container" class="graph-container"></div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ElProgress } from 'element-plus';
import SideBarMenu from '@/common/components/SideBarMenu.vue';

export default {
  name: 'HS3PaperGen',
  components: {
    SideBarMenu,
    ElProgress
  },
  data() {
    return {
      form: {
        subjectEn: '高考',
      },
      error: '',
      success: '',
      isSubmitting: false,
      progress: 0,
      progressInterval: null,
      selectedSubject: '高考',
      network: null,
      nodes: null,
      edges: null,
      neo4jConfig: {
        url: 'http://localhost:7474/db/subjects/tx/commit',
        username: 'neo4j',
        password: '12345678'
      },
      useAsyncMode: false,
      taskList: [],
      taskLoading: false,
      taskPollingTimer: null
    };
  },
  async mounted() {
    await this.$nextTick();
    this.initKnowledgeGraph();
    
    setTimeout(() => {
      this.testNeo4jConnection();
    }, 2000);

    this.fetchTasks();
  },
  watch: {
    'form.subjectEn'(newSubject) {
      if (newSubject) {
        this.selectedSubject = newSubject;
        this.searchKnowledgeBySubject(newSubject);
      } else {
        this.selectedSubject = '';
        this.clearGraph();
      }
    }
  },
  methods: {
    async fetchTasks(showLoading = true) {
      // 暂时返回空列表，后续实现后台任务功能时再启用
      this.taskList = [];
      this.taskLoading = false;
    },
    displayTaskName(task) {
      if (!task) return '高考试卷生成任务';
      return task.examPaperEnName || task.subjectEn || '高考试卷生成任务';
    },
    formatStatus(status) {
      switch ((status || '').toUpperCase()) {
        case 'SUCCEEDED':
          return '已完成';
        case 'FAILED':
          return '失败';
        case 'RUNNING':
          return '生成中';
        case 'PENDING':
          return '排队中';
        default:
          return status || '未知';
      }
    },
    actionLabel(status) {
      if ((status || '').toUpperCase() === 'FAILED') {
        return '已失败';
      }
      return '处理中';
    },
    formatTimestamp(ts) {
      if (!ts) return '--';
      const date = new Date(ts);
      if (Number.isNaN(date.getTime())) {
        return ts;
      }
      return date.toLocaleString();
    },
    async viewTask(task) {
      if (!task || task.status !== 'SUCCEEDED') {
        return;
      }
      // 后续实现
    },
    
    // 进度条控制方法
    startProgress() {
      this.progress = 0;
      const totalDurationSeconds = 5 * 60; // 5分钟（高考试卷segment较多）
      const step = 100 / totalDurationSeconds;
      this.progressInterval = setInterval(() => {
        this.progress = Math.min(this.progress + step, 99);
      }, 1000);
    },
    stopProgress() {
      clearInterval(this.progressInterval);
      this.progress = 100;
      setTimeout(() => this.progress = 0, 1000);
    },

    async submitForm() {
      this.error = '';
      this.success = '';
      
      if (!this.form.subjectEn) {
        this.error = '请选择科目';
        return;
      }

      this.isSubmitting = true;
      this.startProgress();

      try {
        // 调用后端同步生成接口
        const response = await fetch('http://localhost:8080/api/hs3/paper-gen/generate', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          credentials: 'include',
          body: JSON.stringify({
            subjectEn: this.form.subjectEn,
            exam_paper_en_source: 'AIfromreal'
          })
        });

        const result = await response.json();
        if (!response.ok || result.success === false) {
          throw new Error(result.message || '生成试卷失败');
        }

        // 处理返回结果
        const segments = Array.isArray(result.segments)
          ? result.segments
          : (Array.isArray(result.data?.segments) ? result.data.segments : []);
        
        const examPaperEnId = result.examPaperEnId
          || result.exam_paper_en_id
          || result.data?.examPaperEnId
          || '';
        
        const examPaperEnSource = result.exam_paper_en_source
          || result.examPaperEnSource
          || 'AIfromreal';

        const resultData = {
          segments,
          examPaperEnId,
          exam_paper_en_source: examPaperEnSource
        };

        // 存储结果到sessionStorage
        sessionStorage.setItem('hs3PaperGenResult', JSON.stringify(resultData));
        
        // 跳转到结果页面
        this.$router.push({ name: 'HS3PaperGenResult' });

      } catch (error) {
        this.error = '生成试卷时出错: ' + (error.message || '未知错误');
      } finally {
        this.stopProgress();
        this.isSubmitting = false;
      }
    },

    // 知识图谱相关方法
    initKnowledgeGraph() {
      if (typeof window.vis === 'undefined') {
        this.loadVisLibrary().then(() => {
          this.createGraph();
        });
      } else {
        this.createGraph();
      }
    },

    loadVisLibrary() {
      return new Promise((resolve, reject) => {
        if (typeof window.vis !== 'undefined') {
          resolve();
          return;
        }

        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = 'https://cdnjs.cloudflare.com/ajax/libs/vis/4.21.0/vis.min.css';
        document.head.appendChild(link);

        const script = document.createElement('script');
        script.src = 'https://cdnjs.cloudflare.com/ajax/libs/vis/4.21.0/vis.min.js';
        script.onload = () => resolve();
        script.onerror = () => reject(new Error('Failed to load vis.js'));
        document.head.appendChild(script);
      });
    },

    createGraph() {
      const container = document.getElementById('knowledge-graph-container');
      if (!container) {
        return;
      }

      this.nodes = new window.vis.DataSet([]);
      this.edges = new window.vis.DataSet([]);

      const data = {
        nodes: this.nodes,
        edges: this.edges
      };

      const options = {
        nodes: {
          shape: 'circle',
          size: 20,
          font: {
            size: 14,
            face: '微软雅黑',
            align: 'center'
          }
        },
        edges: {
          width: 2,
          arrows: {
            to: {
              enabled: true,
              scaleFactor: 0.5
            }
          },
          font: {
            size: 12,
            align: 'middle'
          }
        },
        physics: {
          stabilization: {
            iterations: 50
          },
          barnesHut: {
            gravitationalConstant: -2000,
            centralGravity: 0.3,
            springLength: 95,
            springConstant: 0.04,
            damping: 0.09,
            avoidOverlap: 0.1
          }
        }
      };

      this.network = new window.vis.Network(container, data, options);
      this.loadInitialData();
    },

    async loadInitialData() {
      try {
        const connectionTest = await this.testNeo4jConnection();
        if (!connectionTest) {
          return;
        }

        // 加载高考相关的知识图谱数据
        const queries = [
          "MATCH (n:Concept)-[r]-(m:Concept) WHERE n.name CONTAINS '高考' OR m.name CONTAINS '高考' RETURN n, r, m LIMIT 100",
          "MATCH (n)-[r]-(m) WHERE n.name CONTAINS '英语' OR m.name CONTAINS '英语' RETURN n, r, m LIMIT 100"
        ];

        let allGraphData = [];
        
        for (const query of queries) {
          try {
            const data = await this.executeNeo4jQuery(query);
            if (data && data.results && data.results[0] && data.results[0].data) {
              allGraphData = allGraphData.concat(data.results[0].data);
            }
          } catch (error) {
            console.error('Neo4j query error:', error);
          }
        }

        if (allGraphData.length > 0) {
          this.processGraphData(allGraphData);
        }
      } catch (error) {
        console.error('Load initial data error:', error);
      }
    },

    async searchKnowledgeBySubject(subjectName) {
      if (!this.nodes || !this.edges) return;
      this.nodes.clear();
      this.edges.clear();

      try {
        await this.fetchFuzzyMatch(subjectName);
      } catch (error) {
        console.error('Search knowledge error:', error);
      }
    },

    async fetchFuzzyMatch(searchTerm) {
      const queries = [
        `MATCH (n)-[r]-(m) WHERE n.name CONTAINS '${searchTerm}' OR m.name CONTAINS '${searchTerm}' RETURN n, r, m LIMIT 200`
      ];

      let allGraphData = [];
      
      for (const query of queries) {
        try {
          const data = await this.executeNeo4jQuery(query);
          if (data && data.results && data.results[0] && data.results[0].data) {
            allGraphData = allGraphData.concat(data.results[0].data);
          }
        } catch (error) {
          console.error('Fuzzy match error:', error);
        }
      }

      if (allGraphData.length > 0) {
        this.processGraphData(allGraphData);
      }
    },

    async executeNeo4jQuery(query) {
      const requestData = {
        "statements": [{
          "statement": query,
          "resultDataContents": ["graph"]
        }]
      };

      const auth = 'Basic ' + btoa(this.neo4jConfig.username + ':' + this.neo4jConfig.password);

      const response = await fetch(this.neo4jConfig.url, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': auth
        },
        body: JSON.stringify(requestData)
      });

      return await response.json();
    },

    processGraphData(graphData) {
      const nodeMap = new Map();
      const edgeMap = new Map();

      graphData.forEach((item) => {
        if (item.graph && item.graph.nodes && item.graph.relationships) {
          item.graph.nodes.forEach(node => {
            if (node.id && !nodeMap.has(node.id)) {
              const nodeData = {
                id: node.id,
                label: node.properties.name || 'Unknown',
                title: node.properties.name || 'Unknown',
                properties: node.properties,
                color: this.getNodeColor(node.properties.level, node.labels, false),
                size: this.getNodeSize(node.labels, false)
              };
              this.nodes.add(nodeData);
              nodeMap.set(node.id, node.properties.name || 'Unknown');
            }
          });
          
          item.graph.relationships.forEach(rel => {
            if (rel.id && !edgeMap.has(rel.id)) {
              const edgeData = {
                id: rel.id,
                from: rel.startNode,
                to: rel.endNode,
                label: rel.type,
                title: rel.type,
                properties: rel.properties,
                color: this.getEdgeColor(rel.type),
                width: 3
              };
              this.edges.add(edgeData);
              edgeMap.set(rel.id, true);
            }
          });
        }
      });
    },

    getNodeColor(level, labels, isStartNode = false) {
      if (level === 0) {
        return '#CCFFCC';
      }
      if (labels && labels.includes('Module')) {
        return '#E0FFFF';
      }
      if (labels && labels.includes('Concept')) {
        return '#FFFFE0';
      }
      return '#CCCCFF';
    },

    getNodeSize(labels, isStartNode = false) {
      if (labels && labels.includes('Module')) {
        return 35;
      }
      if (labels && labels.includes('Concept')) {
        return 25;
      }
      return 20;
    },

    getEdgeColor(relType) {
      const colorMap = {
        '包含课程': '#4CAF50',
        '包含模块': '#4CAF50',
        '包含': '#4CAF50',
        '使用': '#FF9800',
        '拥有': '#795548',
        '是一种': '#2196F3',
        '解决': '#607D8B',
        '需要': '#E91E63',
        '前置条件': '#FF5722',
        '相关': '#9C27B0',
        '依赖': '#F44336',
        '导致': '#00BCD4'
      };
      return colorMap[relType] || '#9E9E9E';
    },

    clearGraph() {
      if (this.nodes && this.edges) {
        this.nodes.clear();
        this.edges.clear();
      }
    },

    async testNeo4jConnection() {
      try {
        const testQuery = "MATCH (n) RETURN count(n) as nodeCount LIMIT 1";
        const data = await this.executeNeo4jQuery(testQuery);
        return data;
      } catch (error) {
        return null;
      }
    }
  },
  beforeUnmount() {
    if (this.progressInterval) {
      clearInterval(this.progressInterval);
    }
    if (this.taskPollingTimer) {
      clearInterval(this.taskPollingTimer);
    }
  }
};
</script>

<style scoped>
.app-container {
  display: flex;
  min-height: 100vh;
}

.main-content {
  flex: 1;
  padding: 20px;
  background-color: #f5f5f5;
  margin-left: 260px;
  width: calc(100% - 260px);
}

.content {
  padding: 20px;
}

.card {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
  padding: 20px;
}

.header {
  color: #333;
  margin-bottom: 20px;
  font-size: 24px;
}

.form-image-container {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.form-section {
  width: 50%;
}

.graph-section {
  width: 45%;
  height: 600px;
  overflow: hidden;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  background-color: #fafafa;
  border: 1px solid #e0e0e0;
}

.graph-header {
  padding: 15px;
  background-color: #fff;
  border-bottom: 1px solid #e0e0e0;
}

.graph-header h3 {
  margin: 0 0 5px 0;
  color: #333;
  font-size: 16px;
}

.graph-header p {
  margin: 0;
  color: #666;
  font-size: 12px;
}

.graph-container {
  width: 100%;
  height: calc(100% - 60px);
  background-color: #fff;
}

input[type="text"], select {
  width: 100%;
  padding: 8px;
  margin: 5px 0;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.subject-row {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  flex-wrap: wrap;
}

.inline-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 0;
  font-size: 14px;
  color: #333;
}

.inline-checkbox input {
  width: auto;
  margin: 0;
}

.task-list-section {
  margin-top: 20px;
  padding: 16px;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  background-color: #fafafa;
}

.task-list-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.task-list-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.link-btn {
  background: none;
  border: none;
  color: #4CAF50;
  cursor: pointer;
  font-size: 14px;
  padding: 4px 8px;
}

.link-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.task-empty {
  text-align: center;
  color: #888;
  font-size: 14px;
  padding: 20px 0;
}

.task-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.task-item {
  display: flex;
  gap: 16px;
  align-items: center;
  justify-content: space-between;
  padding: 12px;
  background-color: #fff;
  border: 1px solid #e0e0e0;
  border-radius: 6px;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
}

.task-info {
  flex: 1;
}

.task-title-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
}

.task-name {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.task-status-pill {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 12px;
  background-color: #e0e0e0;
  color: #333;
}

.task-status-pill.SUCCEEDED {
  background-color: #e8f5e9;
  color: #2e7d32;
}

.task-status-pill.RUNNING,
.task-status-pill.PENDING {
  background-color: #fff3cd;
  color: #8a6d3b;
}

.task-status-pill.FAILED {
  background-color: #fdecea;
  color: #c62828;
}

.task-meta {
  font-size: 13px;
  color: #666;
  margin: 6px 0;
}

.task-error {
  margin-top: 6px;
  color: #c62828;
  font-size: 13px;
}

.task-actions {
  display: flex;
  align-items: center;
}

.task-view-btn {
  padding: 6px 14px;
  border: none;
  border-radius: 4px;
  background-color: #4CAF50;
  color: #fff;
  cursor: pointer;
  font-size: 14px;
}

.task-view-btn.disabled {
  background-color: #ccc;
  cursor: not-allowed;
}

.submit-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

.submit-btn {
  background-color: #4CAF50;
  color: white;
  padding: 10px 20px;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 16px;
  width: 100%;
}

.submit-btn:hover:not(:disabled) {
  background-color: #45a049;
}

.submit-btn:disabled {
  background-color: #cccccc;
  cursor: not-allowed;
}

.hint {
  color: #666;
  font-size: 0.9em;
}

.hint.small {
  font-size: 0.85em;
  margin-left: 8px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: #333;
}

.error {
  color: red;
  margin-top: 10px;
}

.success {
  color: green;
  margin-top: 10px;
}

.select-control {
  width: 100%;
  padding: 8px;
  border: 1px solid #ddd;
  border-radius: 4px;
}

.form-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
}

.form-group {
  flex: 1;
}
</style>
