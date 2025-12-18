<template>
  <div class="class-analysis-container">
    <!-- 班级选择区域 -->
    <div class="selection-area">
      <el-form :inline="true">
        <el-form-item label="选择班级">
          <el-select 
            v-model="selectedClassId" 
            placeholder="请选择班级"
            @change="onClassChange"
            style="width: 250px;"
          >
            <el-option
              v-for="cls in teacherClasses"
              :key="cls.classId"
              :label="cls.displayName"
              :value="cls.classId"
            />
          </el-select>
        </el-form-item>
      </el-form>
    </div>

    <div v-if="selectedClassId && !loading" class="analysis-content">
      <!-- 班级概览卡片 -->
      <el-row :gutter="20" class="overview-cards">
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ overview.student_count || 0 }}</div>
            <div class="stat-label">学生人数</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value">{{ overview.test_count || 0 }}</div>
            <div class="stat-label">考试人次</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value avg">{{ overview.average || 0 }}</div>
            <div class="stat-label">班级均分</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value high">{{ overview.highest || 0 }}</div>
            <div class="stat-label">最高分</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value pass">{{ overview.pass_rate || 0 }}%</div>
            <div class="stat-label">及格率</div>
          </el-card>
        </el-col>
        <el-col :span="4">
          <el-card shadow="hover" class="stat-card">
            <div class="stat-value excellent">{{ overview.excellent_rate || 0 }}%</div>
            <div class="stat-label">优秀率(≥550)</div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表第一行 -->
      <el-row :gutter="20" class="chart-row">
        <!-- 成绩分布 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">📊 成绩分布</span>
            </template>
            <div ref="distributionChart" class="chart-container"></div>
          </el-card>
        </el-col>
        <!-- 班级成绩趋势 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">📈 班级成绩趋势</span>
            </template>
            <div ref="trendChart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 图表第二行 -->
      <el-row :gutter="20" class="chart-row">
        <!-- 题型雷达图 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">🎯 班级题型得分率</span>
            </template>
            <div ref="segmentRadarChart" class="chart-container"></div>
          </el-card>
        </el-col>
        <!-- 学生排名 -->
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">🏆 学生排名TOP10</span>
            </template>
            <div ref="rankingChart" class="chart-container"></div>
          </el-card>
        </el-col>
      </el-row>

      <!-- 薄弱点分析 -->
      <el-row :gutter="20" class="chart-row">
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">⚠️ 班级薄弱题型 (需加强)</span>
            </template>
            <el-table :data="weakPoints.weak_points || []" stripe>
              <el-table-column type="index" width="60" label="排名" />
              <el-table-column prop="segment_name" label="题型" />
              <el-table-column prop="avg_percent" label="平均得分率" width="150">
                <template #default="{ row }">
                  <el-progress 
                    :percentage="row.avg_percent" 
                    :color="getProgressColor(row.avg_percent)"
                    :stroke-width="12"
                  />
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
        <el-col :span="12">
          <el-card shadow="hover">
            <template #header>
              <span class="card-title">✅ 班级优势题型 (表现良好)</span>
            </template>
            <el-table :data="weakPoints.strong_points || []" stripe>
              <el-table-column type="index" width="60" label="排名" />
              <el-table-column prop="segment_name" label="题型" />
              <el-table-column prop="avg_percent" label="平均得分率" width="150">
                <template #default="{ row }">
                  <el-progress 
                    :percentage="row.avg_percent" 
                    :color="getProgressColor(row.avg_percent)"
                    :stroke-width="12"
                  />
                </template>
              </el-table-column>
            </el-table>
          </el-card>
        </el-col>
      </el-row>

      <!-- 完整学生排名表 -->
      <el-card shadow="hover" class="ranking-table-card">
        <template #header>
          <span class="card-title">📋 学生成绩排名</span>
        </template>
        <el-table :data="ranking.rankings || []" stripe max-height="500">
          <el-table-column prop="rank" label="排名" width="80">
            <template #default="{ row }">
              <el-tag v-if="row.rank <= 3" :type="getRankType(row.rank)">
                {{ row.rank }}
              </el-tag>
              <span v-else>{{ row.rank }}</span>
            </template>
          </el-table-column>
          <el-table-column prop="name" label="姓名" width="120" />
          <el-table-column prop="avg_score" label="平均分" width="100">
            <template #default="{ row }">
              <span :class="{ 'pass-score': row.is_pass, 'fail-score': !row.is_pass }">
                {{ row.avg_score }}
              </span>
            </template>
          </el-table-column>
          <el-table-column prop="highest_score" label="最高分" width="100" />
          <el-table-column prop="lowest_score" label="最低分" width="100" />
          <el-table-column prop="test_count" label="考试次数" width="100" />
          <el-table-column label="状态" width="100">
            <template #default="{ row }">
              <el-tag :type="row.is_pass ? 'success' : 'danger'">
                {{ row.is_pass ? '已过线' : '未过线' }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="操作" width="120">
            <template #default="{ row }">
              <el-button type="primary" size="small" @click="viewStudentDetail(row)">
                查看详情
              </el-button>
            </template>
          </el-table-column>
        </el-table>
      </el-card>
    </div>

    <el-empty v-else-if="!selectedClassId" description="请选择班级查看学情分析" />
    <div v-else-if="loading" class="loading-container">
      <el-skeleton :rows="10" animated />
    </div>
  </div>
</template>

<script>
import { ref, onMounted, onUnmounted, nextTick } from 'vue';
import { ElMessage } from 'element-plus';
import * as echarts from 'echarts';
import {
  fetchTeacherAllAssignments,
  fetchClassesByStage,
  fetchCET4ClassOverview,
  fetchCET4ClassScoreDistribution,
  fetchCET4ClassSegmentComparison,
  fetchCET4ClassStudentRanking,
  fetchCET4ClassTrend,
  fetchCET4ClassWeakPoints
} from '@/common/service/learningAnalysisService';

export default {
  name: 'CET4ClassAnalysis',
  emits: ['view-student'],
  setup(props, { emit }) {
    const selectedClassId = ref(null);
    const teacherClasses = ref([]);
    const loading = ref(false);

    // 数据状态
    const overview = ref({});
    const distribution = ref({});
    const segmentComparison = ref({});
    const ranking = ref({});
    const trend = ref({});
    const weakPoints = ref({});

    // 图表引用
    const distributionChart = ref(null);
    const trendChart = ref(null);
    const segmentRadarChart = ref(null);
    const rankingChart = ref(null);

    // 图表实例
    let charts = {};

    // 获取当前用户
    const getCurrentUser = () => {
      const userStr = localStorage.getItem('user');
      return userStr ? JSON.parse(userStr) : null;
    };

    // 加载教师班级列表
    const loadTeacherClasses = async () => {
      const user = getCurrentUser();
      console.log('当前用户:', user);
      if (!user || !user.id) {
        ElMessage.warning('未获取到用户信息');
        return;
      }

      try {
        const assignments = await fetchTeacherAllAssignments(user.id);
        console.log('教师班级分配:', assignments);
        const allClasses = [];
        
        for (const stageId in assignments) {
          const classIds = assignments[stageId];
          if (classIds && classIds.length > 0) {
            const stageClasses = await fetchClassesByStage(parseInt(stageId));
            const teacherStageClasses = stageClasses.filter(cls => classIds.includes(cls.classId));
            
            teacherStageClasses.forEach(cls => {
              cls.displayName = `${cls.stageName} - ${cls.gradeName} - ${cls.classCode}`;
            });
            
            allClasses.push(...teacherStageClasses);
          }
        }

        teacherClasses.value = allClasses;
      } catch (error) {
        console.error('加载班级列表失败:', error);
        ElMessage.error('加载班级列表失败');
      }
    };

    // 班级选择变化
    const onClassChange = async () => {
      if (!selectedClassId.value) return;
      await loadAllAnalytics();
    };

    // 加载所有分析数据
    const loadAllAnalytics = async () => {
      if (!selectedClassId.value) return;
      
      loading.value = true;
      try {
        const [
          overviewRes,
          distributionRes,
          segmentRes,
          rankingRes,
          trendRes,
          weakRes
        ] = await Promise.all([
          fetchCET4ClassOverview(selectedClassId.value),
          fetchCET4ClassScoreDistribution(selectedClassId.value),
          fetchCET4ClassSegmentComparison(selectedClassId.value),
          fetchCET4ClassStudentRanking(selectedClassId.value, 50),
          fetchCET4ClassTrend(selectedClassId.value),
          fetchCET4ClassWeakPoints(selectedClassId.value)
        ]);

        overview.value = overviewRes.data || {};
        distribution.value = distributionRes.data || {};
        segmentComparison.value = segmentRes.data || {};
        ranking.value = rankingRes.data || {};
        trend.value = trendRes.data || {};
        weakPoints.value = weakRes.data || {};

        console.log('趋势数据 has_data:', trend.value.has_data, '日期数量:', trend.value.dates?.length);
        console.log('分布数据 has_data:', distribution.value.has_data, '标签:', distribution.value.labels);

        // 先设置 loading 为 false，让 DOM 渲染
        loading.value = false;
        
        // 等待 DOM 更新完成后再渲染图表
        await nextTick();
        console.log('图表容器 - 分布:', distributionChart.value, '趋势:', trendChart.value);
        renderCharts();
      } catch (error) {
        console.error('加载分析数据失败:', error);
        ElMessage.error('加载分析数据失败');
        loading.value = false;
      }
    };

    // 渲染所有图表
    const renderCharts = () => {
      renderDistributionChart();
      renderTrendChart();
      renderSegmentRadarChart();
      renderRankingChart();
    };

    // 1. 成绩分布柱状图
    const renderDistributionChart = () => {
      if (!distributionChart.value) return;
      
      if (charts.distribution) charts.distribution.dispose();
      charts.distribution = echarts.init(distributionChart.value);

      const data = distribution.value;
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: data.labels || [],
          axisLabel: { interval: 0, rotate: 30 }
        },
        yAxis: {
          type: 'value',
          name: '人次'
        },
        series: [{
          type: 'bar',
          data: (data.counts || []).map((count, index) => ({
            value: count,
            itemStyle: { color: data.colors?.[index] || '#409EFF' }
          })),
          label: {
            show: true,
            position: 'top',
            formatter: '{c}人次'
          }
        }]
      };
      charts.distribution.setOption(option);
    };

    // 2. 班级成绩趋势
    const renderTrendChart = () => {
      if (!trendChart.value) return;
      if (!trend.value || !trend.value.has_data) return;
      
      if (charts.trend) charts.trend.dispose();
      charts.trend = echarts.init(trendChart.value);

      const data = trend.value;
      const option = {
        tooltip: { trigger: 'axis' },
        legend: { data: ['平均分', '最高分', '最低分', '及格线'] },
        grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'category',
          data: data.dates || []
        },
        yAxis: {
          type: 'value',
          min: 0,
          max: 710
        },
        series: [
          {
            name: '平均分',
            type: 'line',
            data: data.avg_scores || [],
            smooth: true,
            itemStyle: { color: '#409EFF' }
          },
          {
            name: '最高分',
            type: 'line',
            data: data.max_scores || [],
            smooth: true,
            itemStyle: { color: '#67C23A' }
          },
          {
            name: '最低分',
            type: 'line',
            data: data.min_scores || [],
            smooth: true,
            itemStyle: { color: '#F56C6C' }
          },
          {
            name: '及格线',
            type: 'line',
            data: data.pass_line || [],
            lineStyle: { type: 'dashed', color: '#E6A23C' },
            symbol: 'none'
          }
        ]
      };
      charts.trend.setOption(option);
    };

    // 3. 题型雷达图
    const renderSegmentRadarChart = () => {
      if (!segmentRadarChart.value) return;
      
      if (charts.segmentRadar) charts.segmentRadar.dispose();
      charts.segmentRadar = echarts.init(segmentRadarChart.value);

      const data = segmentComparison.value;
      const indicator = (data.segments || []).map((name) => ({
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
            name: '班级平均得分率',
            areaStyle: { color: 'rgba(103, 194, 58, 0.4)' },
            lineStyle: { color: '#67C23A' }
          }]
        }]
      };
      charts.segmentRadar.setOption(option);
    };

    // 4. 排名横向柱状图
    const renderRankingChart = () => {
      if (!rankingChart.value) return;
      
      if (charts.ranking) charts.ranking.dispose();
      charts.ranking = echarts.init(rankingChart.value);

      const data = ranking.value;
      const top10 = (data.rankings || []).slice(0, 10);
      
      const option = {
        tooltip: {
          trigger: 'axis',
          axisPointer: { type: 'shadow' }
        },
        grid: { left: '3%', right: '10%', bottom: '3%', containLabel: true },
        xAxis: {
          type: 'value',
          max: 710
        },
        yAxis: {
          type: 'category',
          data: top10.map(r => r.name).reverse(),
          axisLabel: {
            width: 60,
            overflow: 'truncate'
          }
        },
        series: [{
          type: 'bar',
          data: top10.map(r => ({
            value: r.avg_score,
            itemStyle: { 
              color: r.is_pass ? '#67C23A' : '#F56C6C' 
            }
          })).reverse(),
          label: {
            show: true,
            position: 'right',
            formatter: '{c}分'
          }
        }]
      };
      charts.ranking.setOption(option);
    };

    // 获取进度条颜色
    const getProgressColor = (percent) => {
      if (percent < 40) return '#F56C6C';
      if (percent < 60) return '#E6A23C';
      return '#67C23A';
    };

    // 获取排名标签类型
    const getRankType = (rank) => {
      if (rank === 1) return 'danger';
      if (rank === 2) return 'warning';
      if (rank === 3) return 'success';
      return 'info';
    };

    // 查看学生详情
    const viewStudentDetail = (student) => {
      emit('view-student', {
        studentId: student.user_id,
        studentName: student.name,
        classId: selectedClassId.value
      });
    };

    // 窗口大小变化
    const handleResize = () => {
      Object.values(charts).forEach(chart => {
        if (chart && chart.resize) chart.resize();
      });
    };

    onMounted(() => {
      console.log('=== CET4ClassAnalysis 组件已挂载 ===');
      loadTeacherClasses();
      window.addEventListener('resize', handleResize);
    });

    onUnmounted(() => {
      window.removeEventListener('resize', handleResize);
      Object.values(charts).forEach(chart => {
        if (chart && chart.dispose) chart.dispose();
      });
    });

    return {
      selectedClassId,
      teacherClasses,
      loading,
      overview,
      distribution,
      segmentComparison,
      ranking,
      trend,
      weakPoints,
      distributionChart,
      trendChart,
      segmentRadarChart,
      rankingChart,
      onClassChange,
      getProgressColor,
      getRankType,
      viewStudentDetail
    };
  }
};
</script>

<style scoped>
.class-analysis-container {
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
  font-size: 28px;
  font-weight: bold;
  color: #303133;
}

.stat-value.avg {
  color: #409EFF;
}

.stat-value.high {
  color: #67C23A;
}

.stat-value.pass {
  color: #E6A23C;
}

.stat-value.excellent {
  color: #9254de;
}

.stat-label {
  font-size: 13px;
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

.ranking-table-card {
  margin-top: 20px;
}

.pass-score {
  color: #67C23A;
  font-weight: bold;
}

.fail-score {
  color: #F56C6C;
  font-weight: bold;
}

.loading-container {
  padding: 40px;
}
</style>
