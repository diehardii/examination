<template>
  <div class="wrong-questions-container">
    <!-- 左侧菜单栏 -->
    <side-bar-menu />
    
    <!-- 右侧内容区 -->
    <div class="content-area">
      <div class="header">
        <h1>错题答疑</h1>
      </div>

      <el-alert
        v-if="unauthorized"
        title="登录信息已失效，请重新登录后查看错题"
        type="warning"
        show-icon
        class="auth-alert"
        :closable="false"
      />
      
      <!-- 排序控制区 -->
      <div class="sort-controls" v-if="!unauthorized">
        <div class="sort-type">
          <label>排序方式：</label>
          <el-radio-group v-model="sortBy" @change="handleSortChange">
            <el-radio value="time">按时间排序</el-radio>
            <el-radio value="percent">按正确率排序</el-radio>
          </el-radio-group>
        </div>
        
        <div class="sort-order" v-if="sortBy">
          <label>排序顺序：</label>
          <el-radio-group v-model="sortOrder" @change="handleSortChange">
            <el-radio value="ASC">正序 ↑</el-radio>
            <el-radio value="DESC">倒序 ↓</el-radio>
          </el-radio-group>
        </div>
      </div>
      
      <!-- 错题列表 -->
      <div class="wrong-questions-list" v-if="!unauthorized">
        <el-table
          :data="wrongQuestions"
          style="width: 100%"
          v-loading="loading"
          stripe
          border
        >
          <el-table-column type="index" label="序号" width="55" align="center">
            <template #default="{ $index }">
              {{ (currentPage - 1) * pageSize + $index + 1 }}
            </template>
          </el-table-column>
          
          <el-table-column prop="examPaperEnName" label="试卷名称" width="110" show-overflow-tooltip>
            <template #default="{ row }">
              <span class="paper-name">{{ row.examPaperEnName }}</span>
            </template>
          </el-table-column>
          
          <el-table-column prop="segmentId" label="题目段落ID" width="130" show-overflow-tooltip />
          
          <el-table-column prop="questionType" label="题型" width="100" align="center">
            <template #default="{ row }">
              <el-tag size="small">{{ row.questionType || '未知' }}</el-tag>
            </template>
          </el-table-column>
          
          <el-table-column label="答题情况" width="85" align="center">
            <template #default="{ row }">
              <span class="answer-stats-compact">
                {{ row.correctAnswersNumber }}/{{ row.numberOfQuestions }}
              </span>
            </template>
          </el-table-column>
          
          <el-table-column prop="correctAnswersPercent" label="正确率" width="110" sortable align="center">
            <template #default="{ row }">
              <el-progress
                :percentage="row.correctAnswersPercent"
                :color="getProgressColor(row.correctAnswersPercent)"
                :stroke-width="16"
              />
            </template>
          </el-table-column>
          
          <el-table-column prop="testEnTime" label="考试时间" width="150" sortable show-overflow-tooltip>
            <template #default="{ row }">
              {{ formatDateTime(row.testEnTime) }}
            </template>
          </el-table-column>
          
          <el-table-column label="操作" width="100" fixed="right" align="center">
            <template #default="{ row }">
              <el-button
                type="primary"
                size="small"
                @click="viewDetails(row)"
              >
                查看
              </el-button>
            </template>
          </el-table-column>
        </el-table>
        
        <!-- 分页组件 -->
        <div class="pagination-container" v-if="total > 0">
          <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[5, 10, 20, 50]"
            :total="total"
            layout="total, sizes, prev, pager, next, jumper"
            @size-change="handleSizeChange"
            @current-change="handlePageChange"
          />
        </div>
        
        <!-- 空状态 -->
        <el-empty
          v-if="!loading && wrongQuestions.length === 0"
          description="暂无错题记录"
          :image-size="200"
        />
      </div>
      
      <!-- 统计信息 -->
      <div class="statistics" v-if="!unauthorized && total > 0">
        <el-card shadow="hover">
          <template #header>
            <span>统计信息</span>
          </template>
          <div class="stat-content">
            <div class="stat-item">
              <span class="stat-label">总错题数：</span>
              <span class="stat-value">{{ total }}</span>
            </div>
          </div>
        </el-card>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, onMounted } from 'vue';
import { ElMessage } from 'element-plus';
import { useRouter } from 'vue-router';
import SideBarMenu from '@/common/components/SideBarMenu.vue';
import wrongQuestionsApi from '@/cet4/api/CET4wrongQuestions';

export default {
  name: 'CET4WrongQuestionsView',
  components: {
    SideBarMenu
  },
  setup() {
    const router = useRouter();
    const loading = ref(false);
    const wrongQuestions = ref([]);
    const sortBy = ref('time');
    const sortOrder = ref('DESC');
    const unauthorized = ref(false);
    
    // 分页相关
    const currentPage = ref(1);
    const pageSize = ref(10);
    const total = ref(0);
    
    // 获取错题列表
    const fetchWrongQuestions = async () => {
      loading.value = true;
      unauthorized.value = false;
      try {
        const response = await wrongQuestionsApi.getWrongQuestionsList(
          sortBy.value,
          sortOrder.value,
          currentPage.value,
          pageSize.value
        );
        
        if (response.success) {
          wrongQuestions.value = response.data || [];
          total.value = response.total || 0;
        } else {
          ElMessage.error(response.message || '获取错题列表失败');
        }
      } catch (error) {
        if (error.response?.status === 401) {
          unauthorized.value = true;
          wrongQuestions.value = [];
          total.value = 0;
          ElMessage.error('登录信息已失效，请重新登录后查看错题');
        } else {
          ElMessage.error('获取错题列表失败，请稍后重试');
        }
      } finally {
        loading.value = false;
      }
    };
    
    // 处理排序变化
    const handleSortChange = () => {
      currentPage.value = 1; // 排序变化时重置到第一页
      fetchWrongQuestions();
    };
    
    // 处理页码变化
    const handlePageChange = (page) => {
      currentPage.value = page;
      fetchWrongQuestions();
    };
    
    // 处理每页数量变化
    const handleSizeChange = (size) => {
      pageSize.value = size;
      currentPage.value = 1; // 每页数量变化时重置到第一页
      fetchWrongQuestions();
    };
    
    // 格式化日期时间
    const formatDateTime = (dateTime) => {
      if (!dateTime) return '-';
      const date = new Date(dateTime);
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
      });
    };
    
    // 获取进度条颜色
    const getProgressColor = (percent) => {
      if (percent >= 80) return '#67c23a';
      if (percent >= 60) return '#e6a23c';
      return '#f56c6c';
    };
    
    // 查看详情
    const viewDetails = (row) => {
      // 跳转到错题详情页
      router.push({
        name: 'WrongQuestionDetail',
        params: { 
          testEnId: row.testEnId,
          segmentId: row.segmentId
        }
      });
    };
    
    onMounted(() => {
      fetchWrongQuestions();
    });
    
    return {
      loading,
      wrongQuestions,
      sortBy,
      sortOrder,
      currentPage,
      pageSize,
      total,
      handleSortChange,
      handlePageChange,
      handleSizeChange,
      formatDateTime,
      getProgressColor,
      viewDetails,
      unauthorized
    };
  }
};
</script>

<style scoped>
.wrong-questions-container {
  display: flex;
  min-height: 100vh;
  background-color: #f5f7fa;
  position: relative;
}

.content-area {
  flex: 1;
  margin-left: 250px;
  padding: 30px;
  /* 防止内容溢出时遮盖左侧菜单 */
  position: relative;
  z-index: 1;
  /* 确保内容区不会超出边界 */
  max-width: calc(100vw - 250px);
  box-sizing: border-box;
}

.header {
  /* 固定顶部标题 */
  position: sticky;
  top: 0;
  z-index: 10;
  background-color: #f5f7fa;
  padding-bottom: 20px;
  margin-bottom: 10px;
  /* 添加边框以区分 */
  border-bottom: 2px solid #e4e7ed;
  /* 增加左侧内边距，避免文字被遮盖 */
  padding-left: 12px;
}

.header h1 {
  font-size: 28px;
  color: #303133;
  margin: 0;
}

.auth-alert {
  margin-bottom: 20px;
}

.sort-controls {
  background: white;
  padding: 20px;
  border-radius: 8px;
  margin-bottom: 20px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.sort-type,
.sort-order {
  margin-bottom: 15px;
}

.sort-type:last-child,
.sort-order:last-child {
  margin-bottom: 0;
}

.sort-type label,
.sort-order label {
  font-weight: bold;
  color: #606266;
  margin-right: 15px;
}

.wrong-questions-list {
  background: white;
  padding: 20px;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
  margin-bottom: 20px;
  /* 防止表格溢出容器 */
  overflow-x: auto;
  width: 100%;
}

.pagination-container {
  display: flex;
  justify-content: center;
  padding: 20px 0 10px;
  border-top: 1px solid #ebeef5;
  margin-top: 15px;
}

.paper-name {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
  color: #409eff;
  cursor: pointer;
  font-size: 14px;
}

.paper-name:hover {
  text-decoration: underline;
}

.answer-stats {
  font-weight: bold;
  color: #606266;
}

.answer-stats-compact {
  font-weight: 600;
  color: #606266;
  font-size: 13px;
  white-space: nowrap;
}

.statistics {
  background: white;
  border-radius: 8px;
  box-shadow: 0 2px 12px 0 rgba(0, 0, 0, 0.1);
}

.stat-content {
  display: flex;
  gap: 40px;
}

.stat-item {
  display: flex;
  align-items: center;
}

.stat-label {
  font-weight: normal;
  color: #909399;
  margin-right: 8px;
}

.stat-value {
  font-size: 24px;
  font-weight: bold;
  color: #409eff;
}

/* Element Plus 表格样式优化 */
:deep(.el-table) {
  font-size: 14px;
}

:deep(.el-table th) {
  background-color: #f5f7fa;
  color: #606266;
  font-weight: bold;
}

:deep(.el-progress__text) {
  font-size: 12px !important;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .content-area {
    margin-left: 0;
    padding: 15px;
    max-width: 100vw;
  }
  
  .stat-content {
    flex-direction: column;
    gap: 15px;
  }
  
  .header h1 {
    font-size: 22px;
  }
}

/* 确保表格在大屏幕上也不会溢出 */
@media (min-width: 769px) {
  .wrong-questions-list {
    /* 限制表格容器的最大宽度 */
    max-width: 100%;
  }
}
</style>
