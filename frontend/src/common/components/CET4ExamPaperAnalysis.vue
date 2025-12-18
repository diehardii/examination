<template>
  <div class="exam-paper-analysis-container">
    <!-- 试卷选择区域 -->
    <div class="selection-area">
      <el-form :inline="true">
        <el-form-item label="选择试卷">
          <el-select 
            v-model="selectedPaperId" 
            placeholder="请选择试卷"
            @change="onPaperChange"
            filterable
            style="width: 300px;"
          >
            <el-option
              v-for="paper in paperList"
              :key="paper.id"
              :label="paper.examPaperEnName"
              :value="paper.id"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="selectedPaperId && !loading && analysis.has_data" class="analysis-content">
      <!-- 基本信息卡片 -->
      <el-row :gutter="20" class="overview-cards">
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ analysis.student_count || 0 }}</div>
            <div class="stat-label">答题人数</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ analysis.segments?.length || 0 }}</div>
            <div class="stat-label">题型数量</div>
          </el-card>
        </el-col>
        <el-col :span="8">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value avg">
              {{ calculateOverallPercent() }}%
            </div>
            <div class="stat-label">整体得分率</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表区域 -->
      <el-row :gutter="20" class="chart-row">
        <!-- 题型得分率柱状图 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">📊 各题型得分率</span>
            </template>
            <div ref="segmentBarChart" class="chart-container"></div>
          </el-card>
        </el-col>
        <!-- 难度分布饼图 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">🎯 试卷难度分布</span>
            </template>
            <div ref="difficultyPieChart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 难度分析表格 -->
      <el-card shadow="hover" class="difficulty-table-card">
        <template #header>
          <span class="card-title">📋 题型难度分析</span>
        </template>
        <el-table :data="analysis.difficulty_analysis || []" stripe>
          <el-table-column prop="segment" label="题型" width="200" />
          <el-table-column prop="percentage" label="得分率" width="200">
            <template #default="{ row }">
              <el-progress 
                :percentage="row.percentage" 
                :color="getProgressColor(row.percentage)"
                :stroke-width="15"
              >
                <span>{{ row.percentage }}%</span>
              </el-progress>
            </template>
          </el-table-column>
          <el-table-column prop="difficulty" label="难度等级" width="120">
            <template #default="{ row }">
              <el-tag :type="getDifficultyType(row.difficulty)">
                {{ row.difficulty }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="建议">
            <template #default="{ row }">
              {{ getDifficultySuggestion(row.difficulty, row.segment) }}
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-empty v-else-if="!selectedPaperId" description="请选择试卷查看分析" />
    <el-empty v-else-if="selectedPaperId && analysis && !analysis.has_data" :description="analysis.message || '暂无答题数据'" />
    <div v-else-if="loading" class="loading-container">
      <el-skeleton :rows="6" animated />
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import axios from 'axios';
import { fetchCET4ExamPaperAnalysis } from '@/common/service/learningAnalysisService';

export default {
  name: 'CET4ExamPaperAnalysis',
  setup() {
    const selectedPaperId = ref(null);
    const paperList = ref([]);
    const loading = ref(false);
    const analysis = ref({});

    // 图表引用
    const segmentBarChart = ref(null);
    const difficultyPieChart = ref(null);

    // 图表实例
    let charts = {};

    // 加载试卷列表
    const loadPaperList = async () => {
      try {
        const res = await axios.get('http://localhost:8080/api/cet4/exam-paper-en/');
        console.log('试卷列表响应:', res.data);
        // API 返回 { success: true, data: [...] } 格式
        paperList.value = res.data?.data || res.data || [];
      } catch (error) {
        console.error('加载试卷列表失败:', error);
        ElMessage.error('加载试卷列表失败');
      }
    };

    // 试卷选择变化
    const onPaperChange = async () => {
      if (!selectedPaperId.value) return;
      
      loading.value = true;
      try {
        const res = await fetchCET4ExamPaperAnalysis(selectedPaperId.value);
        analysis.value = res.data || {};
        
        // 先设置 loading 为 false，让 DOM 渲染
        loading.value = false;
        
        // 等待 DOM 更新完成后再渲染图表
        await nextTick();
        if (analysis.value.has_data) {
          renderCharts();
        }
      } catch (error) {
        console.error('加载试卷分析失败:', error);
        ElMessage.error('加载试卷分析失败');
        loading.value = false;
      }
    };

    // 计算整体得分率
    const calculateOverallPercent = () => {
      const percentages = analysis.value.percentages || [];
      if (percentages.length === 0) return 0;
      const sum = percentages.reduce((a, b) => a + b, 0);
      return Math.round(sum / percentages.length);
    };

    // 渲染图表
    const renderCharts = () => {
      renderSegmentBarChart();
      renderDifficultyPieChart();
    };

    // 题型得分率柱状图
    const renderSegmentBarChart = () => {
      if (!segmentBarChart.value) return;
      
      if (charts.segmentBar) charts.segmentBar.dispose();
      charts.segmentBar = echarts.init(segmentBarChart.value);

      const data = analysis.value;
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: { left: '3%', right: '4%', bottom: '15%', containLabel: true },
        xAxis: {
          type: 'category',
          data: data.segments || [],
          axisLabel: {
            interval: 0,
            rotate: 45,
            fontSize: 10
          }
        },
        yAxis: {
          type: 'value',
          max: 100,
          axisLabel: { formatter: '{value}%' }
        },
        series: [{
          type: 'bar',
          data: (data.percentages || []).map(v => ({
            value: v,
            itemStyle: {
              color: v >= 70 ? '#67C23A' : (v >= 50 ? '#E6A23C' : '#F56C6C')
            }
          })),
          label: {
            show: true,
            position: 'top',
            formatter: '{c}%'
          }
        }]
      };
      charts.segmentBar.setOption(option);
    };

    // 难度分布饼图
    const renderDifficultyPieChart = () => {
      if (!difficultyPieChart.value) return;
      
      if (charts.difficultyPie) charts.difficultyPie.dispose();
      charts.difficultyPie = echarts.init(difficultyPieChart.value);

      const difficultyData = analysis.value.difficulty_analysis || [];
      const counts = {
        '简单': 0,
        '中等': 0,
        '困难': 0
      };
      
      difficultyData.forEach(item => {
        counts[item.difficulty] = (counts[item.difficulty] || 0) + 1;
      });

      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}个题型 ({d}%)'
        },
        legend: {
          orient: 'vertical',
          left: 'left'
        },
        series: [{
          type: 'pie',
          radius: ['40%', '70%'],
          avoidLabelOverlap: false,
          itemStyle: {
            borderRadius: 10,
            borderColor: '#fff',
            borderWidth: 2
          },
          label: {
            show: true,
            formatter: '{b}\n{c}个'
          },
          data: [
            { value: counts['简单'], name: '简单', itemStyle: { color: '#67C23A' } },
            { value: counts['中等'], name: '中等', itemStyle: { color: '#E6A23C' } },
            { value: counts['困难'], name: '困难', itemStyle: { color: '#F56C6C' } }
          ].filter(d => d.value > 0)
        }]
      };
      charts.difficultyPie.setOption(option);
    };

    // 获取进度条颜色
    const getProgressColor = (percent) => {
      if (percent >= 70) return '#67C23A';
      if (percent >= 50) return '#E6A23C';
      return '#F56C6C';
    };

    // 获取难度标签类型
    const getDifficultyType = (difficulty) => {
      const types = {
        '简单': 'success',
        '中等': 'warning',
        '困难': 'danger'
      };
      return types[difficulty] || 'info';
    };

    // 获取难度建议
    const getDifficultySuggestion = (difficulty, segment) => {
      if (difficulty === '困难') {
        return `该题型得分率较低，建议增加${segment}的专项训练`;
      } else if (difficulty === '中等') {
        return `该题型表现中等，可适当加强练习`;
      } else {
        return `该题型掌握良好，继续保持`;
      }
    };

    // 窗口大小变化
    const handleResize = () => {
      Object.values(charts).forEach(chart => {
        if (chart && chart.resize) chart.resize();
      });
    };

    onMounted(() => {
      loadPaperList();
      window.addEventListener('resize', handleResize);
    });

    onUnmounted(() => {
      window.removeEventListener('resize', handleResize);
      Object.values(charts).forEach(chart => {
        if (chart && chart.dispose) chart.dispose();
      });
    });

    return {
      selectedPaperId,
      paperList,
      loading,
      analysis,
      segmentBarChart,
      difficultyPieChart,
      onPaperChange,
      calculateOverallPercent,
      getProgressColor,
      getDifficultyType,
      getDifficultySuggestion
    };
  }
};
</script>

<style scoped>
.exam-paper-analysis-container {
  padding: 20px;
}

.selection-area {
  margin-bottom: 20px;
  padding: 15px;
  background-color: #f5f7fa;
  border-radius: 8px;
}

.overview-cards {
  margin-bottom: 20px;
}

.stat-card {
  text-align: center;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.stat-value.avg {
  color: #409EFF;
}

.stat-label {
  font-size: 14px;
  color: #909399;
  margin-top: 5px;
}

.chart-row {
  margin-bottom: 20px;
}

.card-title {
  font-size: 16px;
  font-weight: bold;
}

.chart-container {
  height: 300px;
  width: 100%;
}

.difficulty-table-card {
  margin-top: 20px;
}

.loading-container {
  padding: 40px;
}
</style>
