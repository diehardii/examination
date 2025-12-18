"""
CET4学情分析服务 - 核心业务逻辑
提供多种四级英语学情分析方案
"""
from datetime import datetime, timedelta
from .cet4_db_config import execute_query, execute_one


class CET4AnalyticsService:
    """CET4学情分析服务"""
    
    # CET4题型映射（segment_id -> 中文名称）
    SEGMENT_TYPE_MAP = {
        '1Writing1': '写作',
        '2NewsReport1': '新闻报道1',
        '2NewsReport2': '新闻报道2',
        '2NewsReport3': '新闻报道3',
        '2Conversation1': '对话1',
        '2Conversation2': '对话2',
        '2ListeningPassage1': '听力篇章1',
        '2ListeningPassage2': '听力篇章2',
        '2ListeningPassage3': '听力篇章3',
        '3BlankedCloze1': '选词填空',
        '3Matching1': '长篇匹配',
        '3ReadingPassage1': '阅读理解1',
        '3ReadingPassage2': '阅读理解2',
        '4Translation1': '翻译'
    }
    
    # CET4大类分类
    SECTION_MAP = {
        'writing': {
            'name': '写作',
            'segments': ['1Writing1'],
            'full_score': 106.5
        },
        'listening': {
            'name': '听力',
            'segments': ['2NewsReport1', '2NewsReport2', '2NewsReport3', 
                        '2Conversation1', '2Conversation2',
                        '2ListeningPassage1', '2ListeningPassage2', '2ListeningPassage3'],
            'full_score': 248.5
        },
        'reading': {
            'name': '阅读',
            'segments': ['3BlankedCloze1', '3Matching1', '3ReadingPassage1', '3ReadingPassage2'],
            'full_score': 248.5
        },
        'translation': {
            'name': '翻译',
            'segments': ['4Translation1'],
            'full_score': 106.5
        }
    }
    
    # CET4满分和及格线
    FULL_SCORE = 710
    PASS_SCORE = 425
    
    @staticmethod
    def get_student_score_trend(user_id):
        """
        1. 学生成绩变化趋势分析
        返回学生历次CET4考试的总分变化，用于折线图展示
        """
        sql = """
            SELECT 
                r.test_en_id,
                r.test_en_score as score,
                r.correct_number,
                r.test_en_time as test_time,
                e.exam_paper_en_name as paper_name
            FROM user_test_record_en r
            LEFT JOIN exam_paper_en e ON r.exam_paper_en_id = e.id
            WHERE r.user_id = %s
            ORDER BY r.test_en_time ASC
        """
        records = execute_query(sql, (user_id,))
        
        result = {
            'labels': [],          # X轴标签(考试日期)
            'scores': [],          # 分数序列
            'trend': [],           # 移动平均趋势线
            'test_ids': [],        # 考试ID（用于跳转详情）
            'paper_names': [],     # 试卷名称
            'average': 0,          # 平均分
            'highest': 0,          # 最高分
            'lowest': 710,         # 最低分
            'count': len(records), # 考试次数
            'pass_line': 425       # 及格线
        }
        
        if not records:
            return result
            
        total = 0
        for i, r in enumerate(records):
            label = r['test_time'].strftime('%m-%d') if r['test_time'] else f"考试{i+1}"
            result['labels'].append(label)
            
            score = float(r['score']) if r['score'] else 0
            result['scores'].append(round(score, 1))
            result['test_ids'].append(r['test_en_id'])
            result['paper_names'].append(r['paper_name'] or '未命名试卷')
            
            total += score
            if score > result['highest']:
                result['highest'] = round(score, 1)
            if score < result['lowest']:
                result['lowest'] = round(score, 1)
        
        result['average'] = round(total / len(records), 1) if records else 0
        
        # 计算趋势(3期移动平均)
        window = 3
        for i in range(len(result['scores'])):
            start = max(0, i - window + 1)
            avg = sum(result['scores'][start:i+1]) / (i - start + 1)
            result['trend'].append(round(avg, 1))
        
        return result

    @staticmethod
    def get_student_segment_analysis(user_id):
        """
        2. 学生各题型得分率分析
        返回学生在CET4各个segment的平均得分率，用于柱状图/雷达图展示
        """
        sql = """
            SELECT 
                s.segment_id,
                s.question_type,
                AVG(s.correct_answers_percent) as avg_percent,
                AVG(s.score) as avg_score,
                SUM(s.correct_answers_number) as total_correct,
                SUM(s.number_of_questions) as total_questions,
                COUNT(*) as test_count
            FROM user_test_record_segment_en s
            JOIN user_test_record_en r ON s.test_en_id = r.test_en_id
            WHERE r.user_id = %s
            GROUP BY s.segment_id, s.question_type
            ORDER BY s.segment_id
        """
        records = execute_query(sql, (user_id,))
        
        result = {
            'segments': [],       # 题型名称
            'segment_ids': [],    # segment_id
            'percentages': [],    # 得分率
            'scores': [],         # 平均得分
            'radar_data': [],     # 雷达图数据
            'test_count': 0       # 测试次数
        }
        
        for r in records:
            segment_id = r['segment_id']
            segment_name = CET4AnalyticsService.SEGMENT_TYPE_MAP.get(segment_id, segment_id)
            
            result['segments'].append(segment_name)
            result['segment_ids'].append(segment_id)
            
            percent = float(r['avg_percent']) if r['avg_percent'] else 0
            result['percentages'].append(round(percent, 1))
            
            score = float(r['avg_score']) if r['avg_score'] else 0
            result['scores'].append(round(score, 1))
            
            result['radar_data'].append({
                'name': segment_name,
                'value': round(percent, 1),
                'segment_id': segment_id
            })
            
            result['test_count'] = max(result['test_count'], r['test_count'])
        
        return result

    @staticmethod
    def get_student_section_analysis(user_id):
        """
        3. 学生大类题型分析(听力/阅读/写作/翻译)
        用于饼图展示四大板块得分情况
        """
        sql = """
            SELECT 
                s.segment_id,
                AVG(s.correct_answers_percent) as avg_percent,
                AVG(s.score) as avg_score
            FROM user_test_record_segment_en s
            JOIN user_test_record_en r ON s.test_en_id = r.test_en_id
            WHERE r.user_id = %s
            GROUP BY s.segment_id
        """
        records = execute_query(sql, (user_id,))
        
        # 按大类汇总
        section_data = {}
        for section_key, section_info in CET4AnalyticsService.SECTION_MAP.items():
            section_data[section_key] = {
                'name': section_info['name'],
                'total_percent': 0,
                'total_score': 0,
                'count': 0,
                'full_score': section_info['full_score']
            }
        
        for r in records:
            segment_id = r['segment_id']
            percent = float(r['avg_percent']) if r['avg_percent'] else 0
            score = float(r['avg_score']) if r['avg_score'] else 0
            
            for section_key, section_info in CET4AnalyticsService.SECTION_MAP.items():
                if segment_id in section_info['segments']:
                    section_data[section_key]['total_percent'] += percent
                    section_data[section_key]['total_score'] += score
                    section_data[section_key]['count'] += 1
                    break
        
        result = {
            'sections': [],       # 板块名称
            'percentages': [],    # 得分率
            'scores': [],         # 平均得分
            'full_scores': [],    # 满分
            'pie_data': []        # 饼图数据
        }
        
        for section_key, data in section_data.items():
            avg_percent = data['total_percent'] / data['count'] if data['count'] > 0 else 0
            avg_score = data['total_score'] if data['count'] > 0 else 0
            
            result['sections'].append(data['name'])
            result['percentages'].append(round(avg_percent, 1))
            result['scores'].append(round(avg_score, 1))
            result['full_scores'].append(data['full_score'])
            result['pie_data'].append({
                'name': data['name'],
                'value': round(avg_score, 1),
                'percentage': round(avg_percent, 1)
            })
        
        return result

    @staticmethod
    def get_student_weak_points(user_id):
        """
        4. 学生薄弱点分析
        找出得分率最低的5个题型，用于重点提升建议
        """
        sql = """
            SELECT 
                s.segment_id,
                s.question_type,
                AVG(s.correct_answers_percent) as avg_percent,
                SUM(s.number_of_questions - s.correct_answers_number) as wrong_count,
                SUM(s.number_of_questions) as total_questions,
                COUNT(*) as test_count
            FROM user_test_record_segment_en s
            JOIN user_test_record_en r ON s.test_en_id = r.test_en_id
            WHERE r.user_id = %s
            GROUP BY s.segment_id, s.question_type
            ORDER BY avg_percent ASC
            LIMIT 5
        """
        records = execute_query(sql, (user_id,))
        
        result = {
            'weak_points': [],
            'segments': [],
            'percentages': []
        }
        
        for r in records:
            segment_id = r['segment_id']
            segment_name = CET4AnalyticsService.SEGMENT_TYPE_MAP.get(segment_id, segment_id)
            avg_percent = round(float(r['avg_percent']) if r['avg_percent'] else 0, 1)
            
            result['weak_points'].append({
                'segment_id': segment_id,
                'segment_name': segment_name,
                'avg_percent': avg_percent,
                'wrong_count': int(r['wrong_count']) if r['wrong_count'] else 0,
                'total_questions': int(r['total_questions']) if r['total_questions'] else 0,
                'test_count': r['test_count'],
                'suggestion': CET4AnalyticsService._get_improvement_suggestion(segment_id, avg_percent)
            })
            result['segments'].append(segment_name)
            result['percentages'].append(avg_percent)
        
        return result
    
    @staticmethod
    def _get_improvement_suggestion(segment_id, percent):
        """根据题型和得分率给出提升建议"""
        suggestions = {
            '1Writing1': '建议多阅读范文，积累写作素材和句型模板',
            '2NewsReport': '建议多听VOA/BBC慢速新闻，熟悉新闻类听力特点',
            '2Conversation': '建议多练习场景对话听力，注意关键词捕捉',
            '2ListeningPassage': '建议精听长文章，训练长时间注意力集中',
            '3BlankedCloze1': '建议扩大词汇量，掌握词性和搭配',
            '3Matching1': '建议练习快速定位和信息匹配技巧',
            '3ReadingPassage': '建议提高阅读速度，掌握细节题和主旨题技巧',
            '4Translation1': '建议积累翻译常用表达，练习中英文转换'
        }
        
        # 匹配建议
        for key, suggestion in suggestions.items():
            if segment_id.startswith(key.replace('1', '').replace('Passage', '')):
                if percent < 40:
                    return f"【急需提升】{suggestion}"
                elif percent < 60:
                    return f"【需要加强】{suggestion}"
                else:
                    return f"【继续保持】{suggestion}"
        
        return "建议针对性练习该题型"

    @staticmethod
    def get_student_progress_comparison(user_id):
        """
        5. 学生进步情况对比
        对比最近几次考试与之前的成绩变化
        """
        sql = """
            SELECT 
                r.test_en_id,
                r.test_en_score as score,
                r.test_en_time as test_time
            FROM user_test_record_en r
            WHERE r.user_id = %s
            ORDER BY r.test_en_time DESC
            LIMIT 10
        """
        records = execute_query(sql, (user_id,))
        
        if len(records) < 2:
            return {
                'has_data': False,
                'message': '数据不足，至少需要2次考试记录才能分析进步情况'
            }
        
        # 计算前半部分（近期）和后半部分（早期）的平均分
        mid = len(records) // 2
        recent = records[:mid]
        earlier = records[mid:]
        
        recent_avg = sum(float(r['score']) for r in recent) / len(recent)
        earlier_avg = sum(float(r['score']) for r in earlier) / len(earlier)
        
        progress = recent_avg - earlier_avg
        progress_percent = (progress / earlier_avg * 100) if earlier_avg > 0 else 0
        
        return {
            'has_data': True,
            'recent_avg': round(recent_avg, 1),
            'earlier_avg': round(earlier_avg, 1),
            'progress': round(progress, 1),
            'progress_percent': round(progress_percent, 1),
            'is_improving': progress > 0,
            'recent_count': len(recent),
            'earlier_count': len(earlier),
            'comparison': [
                {'name': '早期平均', 'value': round(earlier_avg, 1)},
                {'name': '近期平均', 'value': round(recent_avg, 1)}
            ],
            'evaluation': CET4AnalyticsService._get_progress_evaluation(progress, progress_percent)
        }
    
    @staticmethod
    def _get_progress_evaluation(progress, progress_percent):
        """根据进步幅度给出评价"""
        if progress > 50:
            return "进步显著！继续保持这种学习势头！"
        elif progress > 20:
            return "稳步提升，学习效果良好"
        elif progress > 0:
            return "略有进步，建议加强薄弱环节训练"
        elif progress > -20:
            return "成绩略有波动，需要调整学习方法"
        else:
            return "成绩下滑明显，建议检查学习状态和方法"

    @staticmethod
    def get_class_overview(class_id):
        """
        6. 班级整体情况概览
        展示班级CET4考试的整体统计数据
        """
        # 获取班级学生列表
        sql_students = """
            SELECT s.student_id, u.username, u.real_name
            FROM students s
            JOIN users u ON s.student_id = u.id
            WHERE s.class_id = %s
        """
        students = execute_query(sql_students, (class_id,))
        
        if not students:
            return {'has_data': False, 'message': '班级没有学生'}
        
        student_ids = [s['student_id'] for s in students]
        placeholders = ','.join(['%s'] * len(student_ids))
        
        # 获取班级所有考试记录
        sql_records = f"""
            SELECT 
                r.user_id,
                r.test_en_score as score,
                r.correct_number,
                r.test_en_time as test_time,
                e.exam_paper_en_name as paper_name
            FROM user_test_record_en r
            LEFT JOIN exam_paper_en e ON r.exam_paper_en_id = e.id
            WHERE r.user_id IN ({placeholders})
            ORDER BY r.test_en_time DESC
        """
        records = execute_query(sql_records, tuple(student_ids))
        
        if not records:
            return {
                'has_data': False, 
                'student_count': len(students),
                'message': '班级暂无考试记录'
            }
        
        # 统计
        scores = [float(r['score']) for r in records if r['score']]
        pass_count = len([s for s in scores if s >= 425])
        excellent_count = len([s for s in scores if s >= 550])
        
        return {
            'has_data': True,
            'student_count': len(students),
            'test_count': len(records),
            'average': round(sum(scores) / len(scores), 1) if scores else 0,
            'highest': round(max(scores), 1) if scores else 0,
            'lowest': round(min(scores), 1) if scores else 0,
            'pass_count': pass_count,
            'pass_rate': round(pass_count / len(scores) * 100, 1) if scores else 0,
            'excellent_count': excellent_count,
            'excellent_rate': round(excellent_count / len(scores) * 100, 1) if scores else 0,
            'pass_line': 425,
            'excellent_line': 550
        }

    @staticmethod
    def get_class_score_distribution(class_id):
        """
        7. 班级成绩分布
        按分数段统计人次分布，用于柱状图展示
        """
        sql_students = """
            SELECT student_id FROM students WHERE class_id = %s
        """
        students = execute_query(sql_students, (class_id,))
        
        if not students:
            return {'has_data': False}
        
        student_ids = [s['student_id'] for s in students]
        placeholders = ','.join(['%s'] * len(student_ids))
        
        sql = f"""
            SELECT test_en_score as score
            FROM user_test_record_en
            WHERE user_id IN ({placeholders})
        """
        records = execute_query(sql, tuple(student_ids))
        
        # CET4分数段分布
        distribution = {
            '0-300': 0,
            '300-350': 0,
            '350-400': 0,
            '400-425': 0,
            '425-500': 0,
            '500-550': 0,
            '550-600': 0,
            '600+': 0
        }
        
        for r in records:
            score = float(r['score']) if r['score'] else 0
            if score < 300:
                distribution['0-300'] += 1
            elif score < 350:
                distribution['300-350'] += 1
            elif score < 400:
                distribution['350-400'] += 1
            elif score < 425:
                distribution['400-425'] += 1
            elif score < 500:
                distribution['425-500'] += 1
            elif score < 550:
                distribution['500-550'] += 1
            elif score < 600:
                distribution['550-600'] += 1
            else:
                distribution['600+'] += 1
        
        return {
            'has_data': True,
            'labels': list(distribution.keys()),
            'counts': list(distribution.values()),
            'total': sum(distribution.values()),
            'colors': ['#ff4d4f', '#ff7a45', '#ffa940', '#ffc53d', 
                      '#73d13d', '#36cfc9', '#40a9ff', '#9254de']  # 从红到紫的渐变色
        }

    @staticmethod
    def get_class_segment_comparison(class_id):
        """
        8. 班级各题型平均得分率对比
        用于雷达图或柱状图展示班级整体在各题型的表现
        """
        sql_students = """
            SELECT student_id FROM students WHERE class_id = %s
        """
        students = execute_query(sql_students, (class_id,))
        
        if not students:
            return {'has_data': False}
        
        student_ids = [s['student_id'] for s in students]
        placeholders = ','.join(['%s'] * len(student_ids))
        
        sql = f"""
            SELECT 
                s.segment_id,
                AVG(s.correct_answers_percent) as avg_percent,
                AVG(s.score) as avg_score,
                COUNT(DISTINCT r.user_id) as student_count
            FROM user_test_record_segment_en s
            JOIN user_test_record_en r ON s.test_en_id = r.test_en_id
            WHERE r.user_id IN ({placeholders})
            GROUP BY s.segment_id
            ORDER BY s.segment_id
        """
        records = execute_query(sql, tuple(student_ids))
        
        result = {
            'has_data': True,
            'segments': [],
            'segment_ids': [],
            'percentages': [],
            'scores': [],
            'radar_data': []
        }
        
        for r in records:
            segment_id = r['segment_id']
            segment_name = CET4AnalyticsService.SEGMENT_TYPE_MAP.get(segment_id, segment_id)
            avg_percent = round(float(r['avg_percent']) if r['avg_percent'] else 0, 1)
            
            result['segments'].append(segment_name)
            result['segment_ids'].append(segment_id)
            result['percentages'].append(avg_percent)
            result['scores'].append(round(float(r['avg_score']) if r['avg_score'] else 0, 1))
            result['radar_data'].append({
                'name': segment_name,
                'value': avg_percent
            })
        
        return result

    @staticmethod
    def get_class_student_ranking(class_id, limit=20):
        """
        9. 班级学生排名
        按平均分排名，用于排行榜展示
        """
        sql_students = """
            SELECT s.student_id, u.username, u.real_name
            FROM students s
            JOIN users u ON s.student_id = u.id
            WHERE s.class_id = %s
        """
        students = execute_query(sql_students, (class_id,))
        
        if not students:
            return {'has_data': False}
        
        student_map = {s['student_id']: s for s in students}
        student_ids = list(student_map.keys())
        placeholders = ','.join(['%s'] * len(student_ids))
        
        # 获取每个学生的平均分
        sql = f"""
            SELECT 
                user_id,
                AVG(test_en_score) as avg_score,
                MAX(test_en_score) as highest_score,
                MIN(test_en_score) as lowest_score,
                COUNT(*) as test_count
            FROM user_test_record_en
            WHERE user_id IN ({placeholders})
            GROUP BY user_id
            ORDER BY avg_score DESC
            LIMIT {limit}
        """
        records = execute_query(sql, tuple(student_ids))
        
        result = {
            'has_data': True,
            'rankings': [],
            'names': [],
            'scores': [],
            'bar_data': []
        }
        
        for i, r in enumerate(records):
            user_id = r['user_id']
            student = student_map.get(user_id, {})
            name = student.get('real_name') or student.get('username') or f'学生{user_id}'
            avg_score = round(float(r['avg_score']) if r['avg_score'] else 0, 1)
            
            result['rankings'].append({
                'rank': i + 1,
                'user_id': user_id,
                'name': name,
                'avg_score': avg_score,
                'highest_score': round(float(r['highest_score']) if r['highest_score'] else 0, 1),
                'lowest_score': round(float(r['lowest_score']) if r['lowest_score'] else 0, 1),
                'test_count': r['test_count'],
                'is_pass': avg_score >= 425
            })
            result['names'].append(name if len(name) <= 4 else name[:4] + '...')
            result['scores'].append(avg_score)
            result['bar_data'].append({
                'name': name,
                'value': avg_score,
                'rank': i + 1
            })
        
        return result

    @staticmethod
    def get_class_trend(class_id):
        """
        10. 班级成绩趋势(按考试日期)
        展示班级平均分、最高分、最低分的变化趋势
        """
        sql_students = """
            SELECT student_id FROM students WHERE class_id = %s
        """
        students = execute_query(sql_students, (class_id,))
        
        if not students:
            return {'has_data': False}
        
        student_ids = [s['student_id'] for s in students]
        placeholders = ','.join(['%s'] * len(student_ids))
        
        sql = f"""
            SELECT 
                DATE(test_en_time) as test_date,
                AVG(test_en_score) as avg_score,
                MAX(test_en_score) as max_score,
                MIN(test_en_score) as min_score,
                COUNT(*) as count,
                COUNT(DISTINCT user_id) as student_count
            FROM user_test_record_en
            WHERE user_id IN ({placeholders}) AND test_en_time IS NOT NULL
            GROUP BY DATE(test_en_time)
            ORDER BY test_date ASC
        """
        records = execute_query(sql, tuple(student_ids))
        
        result = {
            'has_data': bool(records),
            'dates': [],
            'avg_scores': [],
            'max_scores': [],
            'min_scores': [],
            'pass_line': [425] * len(records) if records else []
        }
        
        for r in records:
            result['dates'].append(r['test_date'].strftime('%m-%d') if r['test_date'] else '')
            result['avg_scores'].append(round(float(r['avg_score']) if r['avg_score'] else 0, 1))
            result['max_scores'].append(round(float(r['max_score']) if r['max_score'] else 0, 1))
            result['min_scores'].append(round(float(r['min_score']) if r['min_score'] else 0, 1))
        
        return result

    @staticmethod
    def get_student_vs_class_average(user_id, class_id):
        """
        11. 学生与班级平均对比
        对比学生在各题型上与班级平均的差异
        """
        # 获取班级学生
        sql_students = """
            SELECT student_id FROM students WHERE class_id = %s
        """
        students = execute_query(sql_students, (class_id,))
        
        if not students:
            return {'has_data': False, 'message': '班级数据不存在'}
        
        student_ids = [s['student_id'] for s in students]
        placeholders = ','.join(['%s'] * len(student_ids))
        
        # 班级各题型平均
        sql_class = f"""
            SELECT 
                s.segment_id,
                AVG(s.correct_answers_percent) as avg_percent
            FROM user_test_record_segment_en s
            JOIN user_test_record_en r ON s.test_en_id = r.test_en_id
            WHERE r.user_id IN ({placeholders})
            GROUP BY s.segment_id
        """
        class_records = execute_query(sql_class, tuple(student_ids))
        class_data = {r['segment_id']: float(r['avg_percent']) if r['avg_percent'] else 0 for r in class_records}
        
        # 学生各题型平均
        sql_student = """
            SELECT 
                s.segment_id,
                AVG(s.correct_answers_percent) as avg_percent
            FROM user_test_record_segment_en s
            JOIN user_test_record_en r ON s.test_en_id = r.test_en_id
            WHERE r.user_id = %s
            GROUP BY s.segment_id
        """
        student_records = execute_query(sql_student, (user_id,))
        student_data = {r['segment_id']: float(r['avg_percent']) if r['avg_percent'] else 0 for r in student_records}
        
        # 合并数据
        all_segments = sorted(set(class_data.keys()) | set(student_data.keys()))
        
        result = {
            'has_data': True,
            'segments': [],
            'segment_ids': [],
            'student_scores': [],
            'class_scores': [],
            'differences': [],
            'comparison_data': []
        }
        
        for seg in all_segments:
            seg_name = CET4AnalyticsService.SEGMENT_TYPE_MAP.get(seg, seg)
            s_score = round(student_data.get(seg, 0), 1)
            c_score = round(class_data.get(seg, 0), 1)
            diff = round(s_score - c_score, 1)
            
            result['segments'].append(seg_name)
            result['segment_ids'].append(seg)
            result['student_scores'].append(s_score)
            result['class_scores'].append(c_score)
            result['differences'].append(diff)
            result['comparison_data'].append({
                'segment': seg_name,
                'student': s_score,
                'class': c_score,
                'diff': diff,
                'status': '领先' if diff > 0 else ('落后' if diff < 0 else '持平')
            })
        
        return result

    @staticmethod
    def get_exam_paper_analysis(exam_paper_id):
        """
        12. 试卷分析(全体做题学生)
        分析某份试卷的整体答题情况
        """
        sql = """
            SELECT 
                s.segment_id,
                AVG(s.correct_answers_percent) as avg_percent,
                AVG(s.score) as avg_score,
                COUNT(DISTINCT r.user_id) as student_count,
                SUM(s.correct_answers_number) as total_correct,
                SUM(s.number_of_questions) as total_questions
            FROM user_test_record_segment_en s
            JOIN user_test_record_en r ON s.test_en_id = r.test_en_id
            WHERE r.exam_paper_en_id = %s
            GROUP BY s.segment_id
            ORDER BY s.segment_id
        """
        records = execute_query(sql, (exam_paper_id,))
        
        if not records:
            return {'has_data': False, 'message': '暂无答题记录'}
        
        result = {
            'has_data': True,
            'segments': [],
            'segment_ids': [],
            'percentages': [],
            'scores': [],
            'student_count': records[0]['student_count'] if records else 0,
            'difficulty_analysis': []
        }
        
        for r in records:
            seg_name = CET4AnalyticsService.SEGMENT_TYPE_MAP.get(r['segment_id'], r['segment_id'])
            avg_percent = round(float(r['avg_percent']) if r['avg_percent'] else 0, 1)
            
            result['segments'].append(seg_name)
            result['segment_ids'].append(r['segment_id'])
            result['percentages'].append(avg_percent)
            result['scores'].append(round(float(r['avg_score']) if r['avg_score'] else 0, 1))
            
            # 难度分析
            difficulty = '简单' if avg_percent >= 70 else ('中等' if avg_percent >= 50 else '困难')
            result['difficulty_analysis'].append({
                'segment': seg_name,
                'percentage': avg_percent,
                'difficulty': difficulty
            })
        
        return result

    @staticmethod
    def get_student_exam_history(user_id):
        """
        13. 学生考试历史记录
        列出学生所有CET4考试记录
        """
        sql = """
            SELECT 
                r.test_en_id,
                r.test_en_score as score,
                r.correct_number,
                r.test_en_time as test_time,
                e.exam_paper_en_name as paper_name,
                e.exam_paper_en_subject as subject,
                e.exam_paper_en_source as source
            FROM user_test_record_en r
            LEFT JOIN exam_paper_en e ON r.exam_paper_en_id = e.id
            WHERE r.user_id = %s
            ORDER BY r.test_en_time DESC
        """
        records = execute_query(sql, (user_id,))
        
        result = []
        for r in records:
            score = round(float(r['score']) if r['score'] else 0, 1)
            result.append({
                'test_id': r['test_en_id'],
                'score': score,
                'correct_number': r['correct_number'],
                'test_time': r['test_time'].strftime('%Y-%m-%d %H:%M') if r['test_time'] else '',
                'paper_name': r['paper_name'] or '未命名试卷',
                'subject': r['subject'] or 'CET4',
                'source': r['source'] or 'real',
                'is_pass': score >= 425,
                'level': CET4AnalyticsService._get_score_level(score)
            })
        
        return result
    
    @staticmethod
    def _get_score_level(score):
        """根据分数判断等级"""
        if score >= 600:
            return '优秀'
        elif score >= 550:
            return '良好'
        elif score >= 425:
            return '及格'
        else:
            return '未及格'

    @staticmethod
    def get_listening_vs_reading_analysis(user_id):
        """
        14. 听力与阅读能力对比分析
        对比学生历次考试中听力和阅读的表现变化
        """
        sql = """
            SELECT 
                r.test_en_id,
                s.segment_id,
                s.correct_answers_percent as percent,
                r.test_en_time as test_time
            FROM user_test_record_segment_en s
            JOIN user_test_record_en r ON s.test_en_id = r.test_en_id
            WHERE r.user_id = %s
            ORDER BY r.test_en_time ASC
        """
        records = execute_query(sql, (user_id,))
        
        # 按考试分组
        exam_data = {}
        for r in records:
            test_id = r['test_en_id']
            test_time = r['test_time']
            if test_id not in exam_data:
                exam_data[test_id] = {
                    'test_time': test_time,
                    'listening': [],
                    'reading': []
                }
            
            segment_id = r['segment_id']
            percent = float(r['percent']) if r['percent'] else 0
            
            if segment_id in CET4AnalyticsService.SECTION_MAP['listening']['segments']:
                exam_data[test_id]['listening'].append(percent)
            elif segment_id in CET4AnalyticsService.SECTION_MAP['reading']['segments']:
                exam_data[test_id]['reading'].append(percent)
        
        result = {
            'labels': [],
            'listening': [],
            'reading': [],
            'comparison': []
        }
        
        sorted_exams = sorted(exam_data.items(), key=lambda x: x[1]['test_time'] or datetime.min)
        
        for test_id, data in sorted_exams:
            if data['test_time']:
                result['labels'].append(data['test_time'].strftime('%m-%d'))
                
                listening_avg = sum(data['listening']) / len(data['listening']) if data['listening'] else 0
                reading_avg = sum(data['reading']) / len(data['reading']) if data['reading'] else 0
                
                result['listening'].append(round(listening_avg, 1))
                result['reading'].append(round(reading_avg, 1))
                result['comparison'].append({
                    'date': data['test_time'].strftime('%m-%d'),
                    'listening': round(listening_avg, 1),
                    'reading': round(reading_avg, 1),
                    'diff': round(listening_avg - reading_avg, 1)
                })
        
        return result

    @staticmethod
    def get_class_weak_points(class_id):
        """
        15. 班级整体薄弱点分析
        找出班级整体得分率最低和最高的题型
        """
        sql_students = """
            SELECT student_id FROM students WHERE class_id = %s
        """
        students = execute_query(sql_students, (class_id,))
        
        if not students:
            return {'has_data': False}
        
        student_ids = [s['student_id'] for s in students]
        placeholders = ','.join(['%s'] * len(student_ids))
        
        sql = f"""
            SELECT 
                s.segment_id,
                AVG(s.correct_answers_percent) as avg_percent,
                COUNT(DISTINCT r.user_id) as student_count
            FROM user_test_record_segment_en s
            JOIN user_test_record_en r ON s.test_en_id = r.test_en_id
            WHERE r.user_id IN ({placeholders})
            GROUP BY s.segment_id
            ORDER BY avg_percent ASC
        """
        records = execute_query(sql, tuple(student_ids))
        
        result = {
            'has_data': True,
            'weak_points': [],
            'strong_points': [],
            'all_segments': []
        }
        
        for i, r in enumerate(records):
            seg_name = CET4AnalyticsService.SEGMENT_TYPE_MAP.get(r['segment_id'], r['segment_id'])
            avg_percent = round(float(r['avg_percent']) if r['avg_percent'] else 0, 1)
            
            data = {
                'segment_id': r['segment_id'],
                'segment_name': seg_name,
                'avg_percent': avg_percent,
                'student_count': r['student_count']
            }
            
            result['all_segments'].append(data)
            
            if i < 5:  # 最弱的5个
                result['weak_points'].append(data)
        
        # 最强的5个（从高到低）
        result['strong_points'] = sorted(records[-5:], 
            key=lambda x: float(x['avg_percent']) if x['avg_percent'] else 0, reverse=True)
        result['strong_points'] = [
            {
                'segment_id': r['segment_id'],
                'segment_name': CET4AnalyticsService.SEGMENT_TYPE_MAP.get(r['segment_id'], r['segment_id']),
                'avg_percent': round(float(r['avg_percent']) if r['avg_percent'] else 0, 1),
                'student_count': r['student_count']
            }
            for r in result['strong_points']
        ]
        
        return result

    @staticmethod
    def get_time_period_comparison(user_id, days=30):
        """
        16. 时间段对比分析
        对比最近N天与之前的各题型表现
        """
        cutoff_date = datetime.now() - timedelta(days=days)
        
        sql = """
            SELECT 
                s.segment_id,
                s.correct_answers_percent as percent,
                r.test_en_time as test_time
            FROM user_test_record_segment_en s
            JOIN user_test_record_en r ON s.test_en_id = r.test_en_id
            WHERE r.user_id = %s
        """
        records = execute_query(sql, (user_id,))
        
        recent_data = {}
        earlier_data = {}
        
        for r in records:
            segment_id = r['segment_id']
            percent = float(r['percent']) if r['percent'] else 0
            test_time = r['test_time']
            
            if test_time and test_time >= cutoff_date:
                if segment_id not in recent_data:
                    recent_data[segment_id] = []
                recent_data[segment_id].append(percent)
            elif test_time:
                if segment_id not in earlier_data:
                    earlier_data[segment_id] = []
                earlier_data[segment_id].append(percent)
        
        all_segments = sorted(set(recent_data.keys()) | set(earlier_data.keys()))
        
        result = {
            'period': f'最近{days}天',
            'segments': [],
            'segment_ids': [],
            'recent': [],
            'earlier': [],
            'improvement': [],
            'comparison_data': []
        }
        
        for seg in all_segments:
            seg_name = CET4AnalyticsService.SEGMENT_TYPE_MAP.get(seg, seg)
            
            recent_list = recent_data.get(seg, [])
            earlier_list = earlier_data.get(seg, [])
            
            recent_avg = sum(recent_list) / len(recent_list) if recent_list else 0
            earlier_avg = sum(earlier_list) / len(earlier_list) if earlier_list else 0
            improvement = recent_avg - earlier_avg
            
            result['segments'].append(seg_name)
            result['segment_ids'].append(seg)
            result['recent'].append(round(recent_avg, 1))
            result['earlier'].append(round(earlier_avg, 1))
            result['improvement'].append(round(improvement, 1))
            result['comparison_data'].append({
                'segment': seg_name,
                'recent': round(recent_avg, 1),
                'earlier': round(earlier_avg, 1),
                'improvement': round(improvement, 1),
                'trend': '↑' if improvement > 0 else ('↓' if improvement < 0 else '→')
            })
        
        return result


# 创建服务实例
cet4_analytics_service = CET4AnalyticsService()
