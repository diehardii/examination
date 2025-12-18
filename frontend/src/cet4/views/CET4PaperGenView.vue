<template>
  <div class="app-container">
    <!-- 引入侧边栏组件 -->
    <SideBarMenu />

    <!-- 右侧内容区 -->
    <div class="main-content">
      <div class="content">
        <div class="card">
          <h1 class="header">请填写试卷的要求</h1>

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
                      <option value="CET4">CET4</option>
                      <option value="CET6">CET6</option>
                    </select>
                  </div>
                  <label class="checkbox-label inline-checkbox">
                    <input type="checkbox" v-model="useAsyncMode">
                    后台生成
                  </label>
                  <div v-if="useAsyncMode" class="form-group inline-number">
                    <label for="async-count" class="number-label">生成份数：</label>
                    <input
                      id="async-count"
                      type="number"
                      min="1"
                      v-model.number="asyncTaskCount"
                      class="number-input"
                    >
                  </div>
                </div>
                <span class="hint small">勾选后任务将在后台生成，稍后可在下方任务列表查看进度</span>
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
                          {{ task.subjectEn || '未知科目' }} · 提交于 {{ formatTimestamp(task.createdAt) }}
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
                  <!-- 修改点2：添加进度条 -->
                  <el-progress
                      v-if="isSubmitting"
                      :percentage="Math.floor(progress)"
                      :stroke-width="15"
                      :text-inside="true"
                      status="success"
                      style="margin-bottom: 20px; width: 100%"
                  />

                  <!-- 修改点1：修改提交按钮，添加加载状态 -->
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
                <h3>知识图谱</h3>
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
import { getSubjectsEn } from '@/common/service/subjectService';
import SideBarMenu from '@/common/components/SideBarMenu.vue';
import knowledgeManagerApi from '@/cet4/api/CET4knowledgeManager';


export default {
  name: 'PaperGen',
  components: {
    SideBarMenu,
    ElProgress // 修改点4：注册进度条组件
  },
  data() {
    return {
      form: {
        subjectEn: '',
      },
      subjects: [],
      error: '',
      success: '',
      // 修改点5：添加加载状态相关数据
      isSubmitting: false,
      progress: 0,
      progressInterval: null,
      // 知识图谱相关数据
      selectedSubject: '',
      network: null,
      nodes: null,
      edges: null,
      neo4jConfig: {
        url: 'http://localhost:7474/db/subjects/tx/commit',
        username: 'neo4j',
        password: '12345678'
      },
      useAsyncMode: false,
      asyncTaskCount: 1,
      taskList: [],
      taskLoading: false,
      taskPollingTimer: null
    };
  },
  async created() {
    // 不再需要从后端获取科目列表
  },
  async mounted() {
    // 等待DOM渲染完成后初始化知识图谱
    await this.$nextTick();
    this.initKnowledgeGraph();
    
    // 测试Neo4j连接
    setTimeout(() => {
      this.testNeo4jConnection();
    }, 2000);

    this.fetchTasks();
    this.taskPollingTimer = setInterval(() => {
      this.fetchTasks(false);
    }, 15000);
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
      if (showLoading) {
        this.taskLoading = true;
      }
      try {
        const response = await fetch('http://localhost:8080/api/cet4/paper-gen/tasks?limit=20', {
          method: 'GET',
          credentials: 'include'
        });
        if (!response.ok) {
          throw new Error('获取任务列表失败');
        }
        const data = await response.json();
        this.taskList = Array.isArray(data) ? data : [];
      } catch (error) {
        this.taskList = [];
      } finally {
        if (showLoading) {
          this.taskLoading = false;
        }
      }
    },
    displayTaskName(task) {
      if (!task) return '试卷生成任务';
      return task.examPaperEnName || task.subjectEn || '试卷生成任务';
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
      try {
        const response = await fetch(`http://localhost:8080/api/cet4/paper-gen/tasks/${task.id}/result`, {
          method: 'GET',
          credentials: 'include'
        });
        const result = await response.json();
        if (!response.ok || result.success === false) {
          throw new Error(result.message || '获取任务结果失败');
        }
        const units = Array.isArray(result.units)
          ? result.units
          : (Array.isArray(result.data?.units) ? result.data.units : []);
        const examPaperEnId = result.examPaperEnId
          || result.exam_paper_en_id
          || result.data?.examPaperEnId
          || result.data?.exam_paper_en_id
          || '';
        const examPaperEnSource = result.exam_paper_en_source
          || result.examPaperEnSource
          || result.data?.exam_paper_en_source
          || 'AIfromreal';
        const resultData = {
          units,
          examPaperEnId,
          exam_paper_en_source: examPaperEnSource
        };
        sessionStorage.setItem('paperGenResult', JSON.stringify(resultData));
        this.$router.push({ name: 'PaperGenResult' });
        this.removeTaskFromList(task.id);
        await this.deleteTaskOnServer(task.id);
      } catch (error) {
        this.error = '打开试卷结果失败: ' + (error.message || '未知错误');
      }
    },
    removeTaskFromList(taskId) {
      if (!taskId) {
        return;
      }
      this.taskList = this.taskList.filter(task => task.id !== taskId);
    },
    async deleteTaskOnServer(taskId) {
      if (!taskId) {
        return;
      }
      try {
        const response = await fetch(`http://localhost:8080/api/cet4/paper-gen/tasks/${taskId}`, {
          method: 'DELETE',
          credentials: 'include'
        });
        if (!response.ok) {
          throw new Error('删除任务失败');
        }
      } catch (error) {
      }
    },
    // 修改点6：添加进度条控制方法
    startProgress() {
      this.progress = 0;
      const totalDurationSeconds = 4 * 60; // 4分钟
      const step = 100 / totalDurationSeconds;
      this.progressInterval = setInterval(() => {
        this.progress = Math.min(this.progress + step, 99); // 预留1%由stopProgress收尾
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
      const asyncMode = this.useAsyncMode;
      const taskCount = asyncMode ? Math.max(1, Number(this.asyncTaskCount) || 1) : 1;
      this.isSubmitting = true;
      if (!asyncMode) {
        this.startProgress();
      } else {
        this.progress = 0;
      }

      try {
        if (asyncMode) {
          for (let i = 0; i < taskCount; i++) {
            const result = await this.createTaskRequest(true);
            if (!result) {
              throw new Error('生成任务失败');
            }
            await this.fetchTasks(false);
          }
          this.success = `已提交 ${taskCount} 个后台生成任务，稍后可在任务列表查看进度`;
        } else {
          const result = await this.createTaskRequest(false);
          const units = Array.isArray(result.units)
            ? result.units
            : (Array.isArray(result.data?.units) ? result.data.units : []);
          const examPaperEnId = result.examPaperEnId
            || result.exam_paper_en_id
            || result.data?.examPaperEnId
            || result.data?.exam_paper_en_id
            || '';
          const examPaperEnSource = result.exam_paper_en_source
            || result.examPaperEnSource
            || result.data?.exam_paper_en_source
            || 'AIfromreal';
          const resultData = {
            units,
            examPaperEnId,
            exam_paper_en_source: examPaperEnSource
          };
          sessionStorage.setItem('paperGenResult', JSON.stringify(resultData));
          this.$router.push({
            name: 'PaperGenResult'
          });
        }
      } catch (error) {
        this.error = '生成试卷时出错: ' + (error.message || '未知错误');
      } finally {
        if (!asyncMode) {
          this.stopProgress();
        }
        this.isSubmitting = false;
      }
    },

    async createTaskRequest(asyncModeFlag) {
      const response = await fetch('http://localhost:8080/api/cet4/paper-gen/tasks', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        credentials: 'include',
        body: JSON.stringify({
          subjectEn: this.form.subjectEn,
          exam_paper_en_source: 'AIfromreal',
          asyncMode: asyncModeFlag
        })
      });

      const result = await response.json();
      if (!response.ok || result.success === false) {
        throw new Error(result.message || '生成试卷失败');
      }
      return result;
    },

    // 知识图谱相关方法
    initKnowledgeGraph() {
      // 动态加载vis.js库
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
        // 检查是否已加载
        if (typeof window.vis !== 'undefined') {
          resolve();
          return;
        }

        // 加载CSS
        const link = document.createElement('link');
        link.rel = 'stylesheet';
        link.href = 'https://cdnjs.cloudflare.com/ajax/libs/vis/4.21.0/vis.min.css';
        document.head.appendChild(link);

        // 加载JS
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

      // 创建节点和边的数据
      this.nodes = new window.vis.DataSet([]);
      this.edges = new window.vis.DataSet([]);

      // 网络数据
      const data = {
        nodes: this.nodes,
        edges: this.edges
      };

      // 网络选项
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

      // 创建网络
      this.network = new window.vis.Network(container, data, options);

      // 添加节点点击事件
      this.network.on("click", (params) => {
        if (params.nodes.length > 0) {
          const nodeId = params.nodes[0];
          this.handleNodeClick(nodeId);
        }
      });

      // 添加加载完成事件
      this.network.on("stabilizationIterationsDone", () => {
        // 网络稳定化完成
      });

      // 加载初始数据
      this.loadInitialData();
    },

    async loadInitialData() {
      try {
        // 先测试连接
        const connectionTest = await this.testNeo4jConnection();
        if (!connectionTest) {
          return;
        }

        // 使用多个查询来确保获取所有相关数据，参考工作代码的完整查询策略
        const queries = [
          // 查询1: 获取Concept节点之间的关系
          "MATCH (n:Concept)-[r]-(m:Concept) RETURN n, r, m",
          // 查询2: 获取Module节点之间的关系
          "MATCH (n:Module)-[r]-(m:Module) RETURN n, r, m",
          // 查询3: 获取Course节点之间的关系
          "MATCH (n:Course)-[r]-(m:Course) RETURN n, r, m",
          // 查询4: 获取Concept和Module之间的关系
          "MATCH (n:Concept)-[r]-(m:Module) RETURN n, r, m",
          // 查询5: 获取Concept和Course之间的关系
          "MATCH (n:Concept)-[r]-(m:Course) RETURN n, r, m",
          // 查询6: 获取Module和Course之间的关系
          "MATCH (n:Module)-[r]-(m:Course) RETURN n, r, m",
          // 查询7: 获取所有其他类型节点的关系
          "MATCH (n)-[r]-(m) WHERE NOT (n:Concept OR n:Module OR n:Course) OR NOT (m:Concept OR m:Module OR m:Course) RETURN n, r, m",
          // 查询8: 获取所有单独的节点（没有关系的节点）
          "MATCH (n) WHERE NOT (n)-[]-() RETURN n, null as r, null as m"
        ];

        let allGraphData = [];
        
        for (const query of queries) {
          try {
            const data = await this.executeNeo4jQuery(query);
            
            if (data && data.results && data.results[0] && data.results[0].data) {
              allGraphData = allGraphData.concat(data.results[0].data);
            }
          } catch (error) {
          }
        }

        if (allGraphData.length > 0) {
          this.processGraphData(allGraphData);
        }
      } catch (error) {
      }
    },

    async searchKnowledgeBySubject(subjectName) {
      if (!this.nodes || !this.edges) return;

      // 清空当前图谱
      this.nodes.clear();
      this.edges.clear();

      try {
        // 首先检查是否存在精确匹配的知识点
        await this.checkExactMatch(subjectName);
      } catch (error) {
      }
    },

    async checkExactMatch(searchTerm) {
      const checkQuery = `MATCH (n) WHERE n.name = '${searchTerm}' RETURN n LIMIT 1`;
      
      try {
        const data = await this.executeNeo4jQuery(checkQuery);
        
        if (data.results && data.results[0] && data.results[0].data && data.results[0].data.length > 0) {
          // 找到精确匹配，获取所有级联相关的数据
          await this.fetchCascadingData(searchTerm);
        } else {
          // 没有找到精确匹配，尝试模糊匹配
          await this.fetchFuzzyMatch(searchTerm);
        }
      } catch (error) {
        await this.fetchFuzzyMatch(searchTerm);
      }
    },

    async fetchCascadingData(searchTerm) {
      // 使用多个查询来获取级联相关的所有数据
      const queries = [
        // 查询1: 精确匹配的节点及其直接关系
        `MATCH (n)-[r]-(m) WHERE n.name = '${searchTerm}' RETURN n, r, m LIMIT 200`,
        // 查询2: 通过路径扩展查找所有级联相关的节点和关系
        `MATCH path = (start)-[*1..5]-(related) WHERE start.name = '${searchTerm}' UNWIND relationships(path) as r UNWIND nodes(path) as n RETURN DISTINCT n, r LIMIT 1000`,
        // 查询3: 反向路径查找（从其他节点到目标节点）
        `MATCH path = (related)-[*1..5]-(start) WHERE start.name = '${searchTerm}' UNWIND relationships(path) as r UNWIND nodes(path) as n RETURN DISTINCT n, r LIMIT 1000`
      ];

      let allGraphData = [];
      
      for (const query of queries) {
        try {
          const data = await this.executeNeo4jQuery(query);
          
          if (data && data.results && data.results[0] && data.results[0].data) {
            allGraphData = allGraphData.concat(data.results[0].data);
          }
        } catch (error) {
        }
      }

      if (allGraphData.length > 0) {
        this.processGraphData(allGraphData);
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

      // 处理每个数据项
      graphData.forEach((item, index) => {
        // 检查数据结构，支持不同的数据格式
        if (item.graph && item.graph.nodes && item.graph.relationships) {
          // 处理graph格式的数据
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
        } else if (item.row && item.row.length >= 3) {
          // 处理row格式的数据
          const n = item.row[0];
          const r = item.row[1];
          const m = item.row[2];

          // 检查是否是单独节点（没有关系的节点）
          if (r === null && m === null) {
            // 处理单独节点
            const nodeId = n.identity || n.id;
            if (n && nodeId !== undefined && !nodeMap.has(nodeId)) {
              const nodeData = {
                id: nodeId,
                label: n.properties?.name || n.name || 'Unknown',
                title: n.properties?.name || n.name || 'Unknown',
                properties: n.properties || n,
                color: this.getNodeColor(n.properties?.level || n.level, n.labels || [], false),
                size: this.getNodeSize(n.labels || [], false)
              };
              this.nodes.add(nodeData);
              nodeMap.set(nodeId, n.properties?.name || n.name || 'Unknown');
            }
            return;
          }

          // 添加节点n
          const nodeId1 = n.identity || n.id;
          if (n && nodeId1 !== undefined && !nodeMap.has(nodeId1)) {
            const nodeData = {
              id: nodeId1,
              label: n.properties?.name || n.name || 'Unknown',
              title: n.properties?.name || n.name || 'Unknown',
              properties: n.properties || n,
              color: this.getNodeColor(n.properties?.level || n.level, n.labels || [], false),
              size: this.getNodeSize(n.labels || [], false)
            };
            this.nodes.add(nodeData);
            nodeMap.set(nodeId1, n.properties?.name || n.name || 'Unknown');
          }

          // 添加节点m
          const nodeId2 = m.identity || m.id;
          if (m && nodeId2 !== undefined && !nodeMap.has(nodeId2)) {
            const nodeData = {
              id: nodeId2,
              label: m.properties?.name || m.name || 'Unknown',
              title: m.properties?.name || m.name || 'Unknown',
              properties: m.properties || m,
              color: this.getNodeColor(m.properties?.level || m.level, m.labels || [], false),
              size: this.getNodeSize(m.labels || [], false)
            };
            this.nodes.add(nodeData);
            nodeMap.set(nodeId2, m.properties?.name || m.name || 'Unknown');
          }

          // 添加边（只有当关系存在时）
          if (r !== null) {
            const relId = r.identity || r.id;
            if (r && relId !== undefined && !edgeMap.has(relId)) {
              const edgeData = {
                id: relId,
                from: r.start || r.startNode || nodeId1,
                to: r.end || r.endNode || nodeId2,
                label: r.type || 'RELATED',
                title: r.type || 'RELATED',
                properties: r.properties || r,
                color: this.getEdgeColor(r.type || 'RELATED'),
                width: 3
              };
              this.edges.add(edgeData);
              edgeMap.set(relId, true);
            }
          }
        }
      });
    },

    getNodeColor(level, labels, isStartNode = false) {
      if (level === 0) {
        return '#CCFFCC'; // 起始节点使用粉色
      }
      
      // 根据标签类型设置颜色
      if (labels && labels.includes('Module')) {
        return '#E0FFFF'; // Module节点使用青色
      }
      
      if (labels && labels.includes('Concept')) {
        return '#FFFFE0'; // Concept节点使用黄色
      }
      
      return '#CCCCFF'; // 默认颜色
    },

    getNodeSize(labels, isStartNode = false) {
      if (labels && labels.includes('Module')) {
        return 35; // Module节点中等大小
      }
      
      if (labels && labels.includes('Concept')) {
        return 25; // Concept节点中等大小
      }
      
      return 20; // 默认大小
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

    async handleNodeClick(nodeId) {
      const node = this.nodes.get(nodeId);
      if (!node) return;

      const name = (node.properties?.name || node.label || '').trim();
      const description = (node.properties?.description || '').trim();
      let fragments = [];

      if (name) {
        try {
          const response = await knowledgeManagerApi.queryKnowledgePointContent(name);
          fragments = this.extractContentFragments(response, name);
        } catch (error) {
        }
      }

      const displayText = this.composeKnowledgePointText(name, description, fragments);
      this.appendKnowledgePointToForm(displayText);
    },

    extractContentFragments(response, knowledgePointName) {
      const fragments = [];
      if (!response || !response.data) {
        return fragments;
      }

      const trimmedName = knowledgePointName?.trim() || '';
      const payload = response.data;
      const resultCandidates = Array.isArray(payload?.data?.results)
        ? payload.data.results
        : Array.isArray(payload?.results)
          ? payload.results
          : Array.isArray(payload?.data)
            ? payload.data
            : [];

      resultCandidates.forEach((result) => {
        if (!result) return;

        const metadata = result.metadata || {};
        const itemType = result.itemType || metadata.item_type;
        const knowledgePoint = result.knowledgePointName || metadata.knowledge_point_name || '';

        if (itemType !== 'content_fragment') return;
        if (trimmedName && trimmedName !== knowledgePoint) return;

        const documentField = result.document ?? result.documents;
        if (typeof documentField === 'string') {
          const text = documentField.trim();
          if (text) fragments.push(text);
        } else if (Array.isArray(documentField)) {
          documentField.forEach((doc) => {
            if (typeof doc === 'string') {
              const text = doc.trim();
              if (text) fragments.push(text);
            }
          });
        }
      });

      return fragments;
    },

    composeKnowledgePointText(name, description, fragments) {
      const titlePrefix = name ? `知识点名称：${name}` : '知识点名称：';
      const sanitizedDescription = description || '';
      const contentPart = fragments.length > 0 ? `知识点内容：${fragments.join('；')}` : '';
      let innerText = sanitizedDescription;

      if (innerText && contentPart) {
        innerText = `${innerText}；${contentPart}`;
      } else if (!innerText && contentPart) {
        innerText = contentPart;
      }

      if (innerText) {
        return `${titlePrefix}(${innerText})`;
      }
      return titlePrefix;
    },

    appendKnowledgePointToForm(text) {
      if (!text) return;
      if (this.form.description && this.form.description.trim()) {
        this.form.description += ', ' + text;
      } else {
        this.form.description = text;
      }
    },

    clearGraph() {
      if (this.nodes && this.edges) {
        this.nodes.clear();
        this.edges.clear();
      }
    },

    // 调试方法：测试Neo4j连接
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
  // 修改点9：组件销毁时清理定时器
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
/* 原有样式保持不变 */
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

/* 试卷内容区域样式 */
.content-section {
  margin: 10px 0;
}

.content-section label {
  display: block;
  font-weight: bold;
  margin-bottom: 8px;
  color: #333;
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

.content-textarea {
  width: 100%;
  min-height: 80px;
  max-height: 140px;
  padding: 12px;
  margin: 5px 0;
  border: 1px solid #ddd;
  border-radius: 4px;
  font-family: inherit;
  font-size: 14px;
  line-height: 1.5;
  resize: vertical;
  box-sizing: border-box;
}

.content-textarea:focus {
  outline: none;
  border-color: #4CAF50;
  box-shadow: 0 0 5px rgba(76, 175, 80, 0.3);
}

.content-textarea::placeholder {
  color: #999;
  font-style: italic;
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

.task-view-btn + .task-view-btn {
  margin-left: 8px;
}

/* 提交按钮区域样式 */
.submit-section {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #eee;
}

/* 修改点10：调整提交按钮样式 */
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

.option-row {
  display: flex;
  align-items: center;
  gap: 10px;
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