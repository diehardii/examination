<template>
  <div class="student-analysis-container">
    <!-- 学生选择区域 -->
    <div class="selection-area">
      <el-form :inline="true">
        <el-form-item label="选择学生">
          <el-select 
            v-model="selectedStudentId" 
            placeholder="请选择学生"
            @change="onStudentChange"
            filterable
            style="width: 200px;"
          >
            <el-option
              v-for="student in studentList"
              :key="student.studentId"
              :label="student.studentName || student.username"
              :value="student.studentId"
            />
          </el-select>
        </el-form-item>
        <el-form-item v-if="selectedClassId">
          <el-tag type="info">班级: {{ selectedClassName }}</el-tag>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="selectedStudentId && !loading" class="analysis-content">
      <!-- 成绩概览卡片 -->
      <el-row :gutter="20" class="overview-cards">
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ scoreTrend.count || 0 }}</div>
            <div class="stat-label">考试次数</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value" :class="{ 'pass': scoreTrend.average >= 425 }">
              {{ scoreTrend.average || 0 }}
            </div>
            <div class="stat-label">平均分</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value highlight">{{ scoreTrend.highest || 0 }}</div>
            <div class="stat-label">最高分</div>
          </el-card>
        </el-col>
        <el-col :span="6">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ scoreTrend.lowest || 0 }}</div>
            <div class="stat-label">最低分</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表区域 -->
      <el-row :gutter="20" class="chart-row">
        <!-- 成绩变化趋势 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">📈 成绩变化趋势</span>
            </template>
            <div ref="scoreTrendChart" class="chart-container"></div>
          </el-card>
        </el-col>
        <!-- 题型得分率雷达图 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">🎯 各题型得分率</span>
            </template>
            <div ref="segmentRadarChart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="chart-row">
        <!-- 四大板块饼图 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">📊 四大板块得分分布</span>
            </template>
            <div ref="sectionPieChart" class="chart-container"></div>
          </el-card>
        </el-col>
        <!-- 听力vs阅读对比 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">🎧 听力与阅读能力对比</span>
            </template>
            <div ref="listeningReadingChart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <el-row :gutter="20" class="chart-row">
        <!-- 进步情况 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">📈 进步情况分析</span>
            </template>
            <div v-if="progressData.has_data" class="progress-content">
              <div ref="progressChart" class="chart-container-small"></div>
              <div class="progress-info">
                <el-tag :type="progressData.is_improving ? 'success' : 'danger'" size="large">
                  {{ progressData.is_improving ? '↑ 进步' : '↓ 退步' }} 
                  {{ Math.abs(progressData.progress) }} 分
                </el-tag>
                <p class="evaluation">{{ progressData.evaluation }}</p>
              </div>
            </div>
            <el-empty v-else :description="progressData.message || '数据不足'" />
          </el-card>
        </el-col>
        <!-- 薄弱点分析 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">⚠️ 薄弱点分析</span>
            </template>
            <div ref="weakPointsChart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 薄弱点建议列表 -->
      <el-card shadow="hover" class="suggestion-card">
        <template #header>
          <span class="card-title">💡 提升建议</span>
        </template>
        <el-table :data="weakPoints.weak_points || []" stripe>
          <el-table-column prop="segment_name" label="题型" width="150" />
          <el-table-column prop="avg_percent" label="得分率" width="100">
            <template #default="{ row }">
              <el-progress 
                :percentage="row.avg_percent" 
                :color="getProgressColor(row.avg_percent)"
                :stroke-width="10"
              />
            </template>
          </el-table-column>
          <el-table-column prop="wrong_count" label="错题数" width="100" />
          <el-table-column prop="suggestion" label="提升建议" />
        </el-table>
      </el-card>

      <!-- 考试历史记录 -->
      <el-card shadow="hover" class="history-card">
        <template #header>
          <span class="card-title">📋 考试历史记录</span>
        </template>
        <el-table :data="examHistory" stripe max-height="400">
          <el-table-column prop="test_time" label="考试时间" width="160" />
          <el-table-column prop="paper_name" label="试卷名称" />
          <el-table-column prop="score" label="得分" width="100">
            <template #default="{ row }">
              <el-tag :type="row.is_pass ? 'success' : 'danger'">
                {{ row.score }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column prop="level" label="等级" width="100">
            <template #default="{ row }">
              <el-tag :type="getLevelType(row.level)">{{ row.level }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-empty v-else-if="!selectedStudentId" description="请先选择学生查看学情分析" />
    <div v-else-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, nextTick, watch } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import {
  fetchTeacherAllAssignments,
  fetchClassesByStage,
  fetchClassStudents,
  fetchCET4StudentScoreTrend,
  fetchCET4StudentSegmentAnalysis,
  fetchCET4StudentSectionAnalysis,
  fetchCET4StudentWeakPoints,
  fetchCET4StudentProgress,
  fetchCET4StudentExamHistory,
  fetchCET4ListeningReadingAnalysis
} from '@/common/service/learningAnalysisService';

export default {
  name: 'CET4StudentAnalysis',
  props: {
    classId: {
      type: Number,
      default: null
    },
    className: {
      type: String,
      default: ''
    }
  },
  setup(props) {
    const selectedStudentId = ref(null);
    const selectedClassId = ref(props.classId);
    const selectedClassName = ref(props.className);
    const studentList = ref([]);
    const loading = ref(false);

    // 数据状态
    const scoreTrend = ref({});
    const segmentAnalysis = ref({});
    const sectionAnalysis = ref({});
    const weakPoints = ref({});
    const progressData = ref({});
    const examHistory = ref([]);
    const listeningReadingData = ref({});

    // 图表引用
    const scoreTrendChart = ref(null);
    const segmentRadarChart = ref(null);
    const sectionPieChart = ref(null);
    const listeningReadingChart = ref(null);
    const progressChart = ref(null);
    const weakPointsChart = ref(null);

    // 图表实例
    let charts = {};

    // 获取当前用户
    const getCurrentUser = () => {
      const userStr = localStorage.getItem('user');
      return userStr ? JSON.parse(userStr) : null;
    };

    // 加载学生列表
    const loadStudentList = async () => {
      console.log('加载学生列表, selectedClassId:', selectedClassId.value);
      if (!selectedClassId.value) {
        // 如果没有传入班级，尝试从教师获取
        const user = getCurrentUser();
        console.log('当前用户:', user);
        if (!user) return;
        
        try {
          const assignments = await fetchTeacherAllAssignments(user.id);
          console.log('教师班级分配:', assignments);
          for (const stageId in assignments) {
            const classIds = assignments[stageId];
            if (classIds && classIds.length > 0) {
              const classes = await fetchClassesByStage(parseInt(stageId));
              const teacherClass = classes.find(c => classIds.includes(c.classId));
              if (teacherClass) {
                selectedClassId.value = teacherClass.classId;
                selectedClassName.value = `${teacherClass.stageName} - ${teacherClass.gradeName} - ${teacherClass.classCode}`;
                console.log('找到班级:', selectedClassId.value, selectedClassName.value);
                break;
              }
            }
          }
        } catch (error) {
          console.error('加载班级失败:', error);
        }
      }

      if (selectedClassId.value) {
        try {
          const data = await fetchClassStudents(selectedClassId.value);
          console.log('学生列表:', data);
          studentList.value = data.students || [];
        } catch (error) {
          console.error('加载学生列表失败:', error);
        }
      }
    };

    // 学生选择变化
    const onStudentChange = async () => {
      console.log('选择学生:', selectedStudentId.value);
      if (!selectedStudentId.value) return;
      await loadAllAnalytics();
    };

    // 加载所有分析数据
    const loadAllAnalytics = async () => {
      if (!selectedStudentId.value) return;
      
      loading.value = true;
      console.log('开始加载学生分析数据...');
      try {
        const [
          trendRes,
          segmentRes,
          sectionRes,
          weakRes,
          progressRes,
          historyRes,
          lrRes
        ] = await Promise.all([
          fetchCET4StudentScoreTrend(selectedStudentId.value),
          fetchCET4StudentSegmentAnalysis(selectedStudentId.value),
          fetchCET4StudentSectionAnalysis(selectedStudentId.value),
          fetchCET4StudentWeakPoints(selectedStudentId.value),
          fetchCET4StudentProgress(selectedStudentId.value),
          fetchCET4StudentExamHistory(selectedStudentId.value),
          fetchCET4ListeningReadingAnalysis(selectedStudentId.value)
        ]);

        console.log('API返回 - 趋势:', trendRes);
        console.log('API返回 - 题型:', segmentRes);

        scoreTrend.value = trendRes.data || {};
        segmentAnalysis.value = segmentRes.data || {};
        sectionAnalysis.value = sectionRes.data || {};
        weakPoints.value = weakRes.data || {};
        progressData.value = progressRes.data || {};
        examHistory.value = historyRes.data || [];
        listeningReadingData.value = lrRes.data || {};

        console.log('数据赋值后 - scoreTrend:', scoreTrend.value);

        // 先设置 loading 为 false，让 DOM 渲染
        loading.value = false;
        
        // 等待 DOM 更新完成后再渲染图表
        await nextTick();
        console.log('图表容器:', scoreTrendChart.value, segmentRadarChart.value);
        renderCharts();
      } catch (error) {
        console.error('加载分析数据失败:', error);
        ElMessage.error('加载分析数据失败');
        loading.value = false;
      }
    };

    // 渲染所有图表
    const renderCharts = () => {
      renderScoreTrendChart();
      renderSegmentRadarChart();
      renderSectionPieChart();
      renderListeningReadingChart();
      renderProgressChart();
      renderWeakPointsChart();
    };

    // 1. 成绩趋势折线图
    const renderScoreTrendChart = () => {
      if (!scoreTrendChart.value) return;
      
      if (charts.scoreTrend) charts.scoreTrend.dispose();
      charts.scoreTrend = echarts.init(scoreTrendChart.value);

      const data = scoreTrend.value;
      const option = {
        tooltip: {
          trigger: 'axis',
          formatter: (params) => {
            const idx = params[0].dataIndex;
            return `${data.paper_names?.[idx] || '考试'}<br/>
                    日期: ${data.labels?.[idx]}<br/>
                    得分: ${params[0].value}分<br/>
                    趋势: ${params[1]?.value || '-'}分`;
          }
        },
        legend: {
          data: ['得分', '趋势线', '及格线']
        },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: data.labels || []
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 710
        },
        series: [
          {
            name: '得分',
            type: 'line',
            data: data.scores || [],
            smooth: true,
            itemStyle: { color: '#409EFF' },
            areaStyle: { color: 'rgba(64, 158, 255, 0.2)' }
          },
          {
            name: '趋势线',
            type: 'line',
            data: data.trend || [],
            smooth: true,
            lineStyle: { type: 'dashed', color: '#E6A23C' }
          },
          {
            name: '及格线',
            type: 'line',
            data: (data.labels || []).map(() => 425),
            lineStyle: { type: 'dashed', color: '#F56C6C' },
            symbol: 'none'
          }
        ]
      };
      charts.scoreTrend.setOption(option);
    };

    // 2. 题型雷达图
    const renderSegmentRadarChart = () => {
      if (!segmentRadarChart.value) return;
      
      if (charts.segmentRadar) charts.segmentRadar.dispose();
      charts.segmentRadar = echarts.init(segmentRadarChart.value);

      const data = segmentAnalysis.value;
      const indicator = (data.segments || []).map((name, i) => ({
        name: name.length > 6 ? name.substring(0, 6) + '...' : name,
        max: 100
      }));

      const option = {
        tooltip: {},
        radar: {
          indicator: indicator,
          shape: 'polygon'
        },
        series: [{
          type: 'radar',
          data: [{
            value: data.percentages || [],
            name: '得分率',
            areaStyle: { color: 'rgba(64, 158, 255, 0.4)' },
            lineStyle: { color: '#409EFF' }
          }]
        }]
      };
      charts.segmentRadar.setOption(option);
    };

    // 3. 四大板块饼图
    const renderSectionPieChart = () => {
      if (!sectionPieChart.value) return;
      
      if (charts.sectionPie) charts.sectionPie.dispose();
      charts.sectionPie = echarts.init(sectionPieChart.value);

      const data = sectionAnalysis.value;
      const option = {
        tooltip: {
          trigger: 'item',
          formatter: '{b}: {c}分 ({d}%)'
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
            formatter: '{b}\n{c}分'
          },
          data: data.pie_data || []
        }]
      };
      charts.sectionPie.setOption(option);
    };

    // 4. 听力阅读对比
    const renderListeningReadingChart = () => {
      if (!listeningReadingChart.value) return;
      
      if (charts.listeningReading) charts.listeningReading.dispose();
      charts.listeningReading = echarts.init(listeningReadingChart.value);

      const data = listeningReadingData.value;
      const option = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['听力', '阅读'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: data.labels || []
        },
        yAxis: {
          type: 'value',
          max: 100,
          axisLabel: { formatter: '{value}%' }
        },
        series: [
          {
            name: '听力',
            type: 'line',
            data: data.listening || [],
            smooth: true,
            itemStyle: { color: '#67C23A' }
          },
          {
            name: '阅读',
            type: 'line',
            data: data.reading || [],
            smooth: true,
            itemStyle: { color: '#E6A23C' }
          }
        ]
      };
      charts.listeningReading.setOption(option);
    };

    // 5. 进步对比图
    const renderProgressChart = () => {
      if (!progressChart.value || !progressData.value.has_data) return;
      
      if (charts.progress) charts.progress.dispose();
      charts.progress = echarts.init(progressChart.value);

      const data = progressData.value;
      const option = {
        tooltip: {},
        xAxis: {
          type: 'category',
          data: ['早期平均', '近期平均']
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 710
        },
        series: [{
          type: 'bar',
          data: [
            { value: data.earlier_avg, itemStyle: { color: '#909399' } },
            { value: data.recent_avg, itemStyle: { color: data.is_improving ? '#67C23A' : '#F56C6C' } }
          ],
          label: {
            show: true,
            position: 'top',
            formatter: '{c}分'
          }
        }]
      };
      charts.progress.setOption(option);
    };

    // 6. 薄弱点柱状图
    const renderWeakPointsChart = () => {
      if (!weakPointsChart.value) return;
      
      if (charts.weakPoints) charts.weakPoints.dispose();
      charts.weakPoints = echarts.init(weakPointsChart.value);

      const data = weakPoints.value;
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: data.segments || [],
          axisLabel: {
            interval: 0,
            rotate: 30
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
              color: v < 40 ? '#F56C6C' : (v < 60 ? '#E6A23C' : '#67C23A')
            }
          })),
          label: {
            show: true,
            position: 'top',
            formatter: '{c}%'
          }
        }]
      };
      charts.weakPoints.setOption(option);
    };

    // 获取进度条颜色
    const getProgressColor = (percent) => {
      if (percent < 40) return '#F56C6C';
      if (percent < 60) return '#E6A23C';
      return '#67C23A';
    };

    // 获取等级标签类型
    const getLevelType = (level) => {
      const types = {
        '优秀': 'success',
        '良好': '',
        '及格': 'warning',
        '未及格': 'danger'
      };
      return types[level] || 'info';
    };

    // 窗口大小变化时重绘图表
    const handleResize = () => {
      Object.values(charts).forEach(chart => {
        if (chart && chart.resize) chart.resize();
      });
    };

    // 监听classId变化
    watch(() => props.classId, (newVal) => {
      if (newVal) {
        selectedClassId.value = newVal;
        selectedClassName.value = props.className;
        loadStudentList();
      }
    });

    onMounted(() => {
      console.log('=== CET4StudentAnalysis 组件已挂载 ===');
      loadStudentList();
      window.addEventListener('resize', handleResize);
    });

    onUnmounted(() => {
      window.removeEventListener('resize', handleResize);
      Object.values(charts).forEach(chart => {
        if (chart && chart.dispose) chart.dispose();
      });
    });

    return {
      selectedStudentId,
      selectedClassId,
      selectedClassName,
      studentList,
      loading,
      scoreTrend,
      segmentAnalysis,
      sectionAnalysis,
      weakPoints,
      progressData,
      examHistory,
      listeningReadingData,
      scoreTrendChart,
      segmentRadarChart,
      sectionPieChart,
      listeningReadingChart,
      progressChart,
      weakPointsChart,
      onStudentChange,
      getProgressColor,
      getLevelType
    };
  }
};
</script>

<style scoped>
.student-analysis-container {
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
  cursor: default;
}

.stat-value {
  font-size: 32px;
  font-weight: bold;
  color: #303133;
}

.stat-value.pass {
  color: #67C23A;
}

.stat-value.highlight {
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

.chart-container-small {
  height: 200px;
  width: 100%;
}

.progress-content {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.progress-info {
  text-align: center;
  margin-top: 15px;
}

.evaluation {
  color: #606266;
  margin-top: 10px;
}

.suggestion-card,
.history-card {
  margin-bottom: 20px;
}

.loading-container {
  padding: 40px;
}
</style>
