"""
CET4服务路由 - 注册所有CET4相关的API端点
"""
from flask import Blueprint, request, jsonify
import logging
import json
import time

from .cet4_exam_generator_service import exam_generator_service
from .cet4_exam_ana_service import exam_analysis_service
from .cet4_exam_ana_listening_service import listening_analysis_service
from .cet4_question_answer import qa_service
from .cet4_smart_question_answer import smart_qa_service
from .cet4_analytics_service import cet4_analytics_service
from common.response import success_response, error_response
from common.validators import validate_required_fields

logger = logging.getLogger(__name__)

# 创建蓝图
cet4_bp = Blueprint('cet4', __name__)


@cet4_bp.route('/generate-exam', methods=['POST'])
def generate_exam():
    """
    生成试卷题目
    
    请求体:
    {
        "inputExamPaperSamp": "试卷样例JSON字符串",
        "examTopic": "考试主题",
        "model": "deepseek-reasoner",  # 可选
        "examPaperEnSource": "AIfromself",  # 可选，来源标识
        "segmentIdSelf": "3BlankedCloze1"  # 可选，自定义segment_id
    }
    
    响应:
    {
        "success": true,
        "data": "生成的题目JSON字符串",
        "message": "试卷生成成功"
    }
    """
    try:
        # 获取请求数据
        data = request.get_json()
        
        # 验证必需字段
        validation_error = validate_required_fields(data, ['inputExamPaperSamp', 'examTopic'])
        if validation_error:
            return error_response(validation_error, 400)
        
        input_exam_paper_samp = data.get('inputExamPaperSamp')
        exam_topic = data.get('examTopic')
        model = data.get('model', 'deepseek-reasoner')
        exam_paper_en_source = data.get('examPaperEnSource')
        segment_id_self = data.get('segmentIdSelf')
        
        logger.info(f"收到试卷生成请求 - 主题: {exam_topic}, 模型: {model}, 来源: {exam_paper_en_source}, segment_id: {segment_id_self}")
        
        # 调用服务生成试卷
        result = exam_generator_service.generate_exam_question(
            input_exam_paper_samp=input_exam_paper_samp,
            exam_topic=exam_topic,
            model=model,
            exam_paper_en_source=exam_paper_en_source,
            segment_id_self=segment_id_self
        )
        
        if result['success']:
            logger.info("试卷生成成功")
            return success_response(data=result['data'], message=result['message'])
        else:
            logger.error(f"试卷生成失败: {result['message']}")
            return error_response(result['message'], 500)
            
    except Exception as e:
        logger.error(f"生成试卷异常: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analyze-exam', methods=['POST'])
def analyze_exam():
    """解析写作/阅读/翻译"""
    try:
        data = request.get_json() or {}
        topics = data.get('topics', [])
        input_file = data.get('inputFile', '')
        model = data.get('model', 'deepseek-reasoner')

        if not input_file:
            return error_response("inputFile不能为空", 400)

        result = exam_analysis_service.analyze_exam(topics=topics, input_file=input_file, model=model)
        if result.get('success'):
            return success_response(data=result.get('data'), message=result.get('message', '解析成功'))
        return error_response(result.get('message', '解析失败'), 500)
    except Exception as e:
        logger.error("解析写作/阅读/翻译异常", exc_info=True)
        return error_response(f"服务器内部错误: {e}", 500)


@cet4_bp.route('/analyze-listening', methods=['POST'])
def analyze_listening():
    """解析听力部分"""
    try:
        data = request.get_json() or {}
        topics = data.get('topics', [])
        input_file = data.get('inputFile', '')
        model = data.get('model', 'deepseek-reasoner')

        if not input_file:
            return error_response("inputFile不能为空", 400)

        result = listening_analysis_service.analyze_listening(topics=topics, input_file=input_file, model=model)
        if result.get('success'):
            return success_response(data=result.get('data'), message=result.get('message', '解析成功'))
        return error_response(result.get('message', '解析失败'), 500)
    except Exception as e:
        logger.error("解析听力异常", exc_info=True)
        return error_response(f"服务器内部错误: {e}", 500)


@cet4_bp.route('/health', methods=['GET'])
def health():
    """健康检查"""
    return success_response(message="CET4服务运行正常")


@cet4_bp.route('/init-conversation', methods=['POST'])
def init_conversation():
    """
    初始化对话
    
    请求体:
    {
        "userId": "用户唯一标识",
        "examId": "考试唯一标识"
    }
    
    响应:
    {
        "success": true,
        "data": "初始化信息",
        "message": "对话初始化成功"
    }
    """
    try:
        data = request.get_json()
        validation_error = validate_required_fields(data, ['userId', 'examId'])
        if validation_error:
            return error_response(validation_error, 400)
        
        user_id = data.get('userId')
        exam_id = data.get('examId')
        
        logger.info(f"初始化对话 - 用户: {user_id}, 考试: {exam_id}")
        
        # 调用QA服务初始化对话
        result = qa_service.init_conversation(user_id, exam_id)
        
        if result['success']:
            logger.info("对话初始化成功")
            return success_response(data=result['data'], message=result['message'])
        else:
            logger.error(f"对话初始化失败: {result['message']}")
            return error_response(result['message'], 500)
            
    except Exception as e:
        logger.error(f"初始化对话异常: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/tutoring/ask', methods=['POST'])
def ask_question():
    """
    学生向AI辅导老师提问
    
    请求体:
    {
        "user_id": 123,
        "segment_id": "3BlankedCloze1",
        "question_type": "Matching",
        "document": "题目的JSON字符串",
        "user_answers": [
            {"question_number": 1, "user_answer": "A"},
            {"question_number": 2, "user_answer": "B"}
        ],
        "question": "学生的问题"
    }
    
    响应:
    {
        "success": true,
        "data": {
            "answer": "AI老师的回答",
            "round_id": 1,
            "total_rounds": 1
        }
    }
    """
    try:
        data = request.get_json()
        
        # 验证必需字段
        required_fields = ['user_id', 'segment_id', 'question_type', 'document', 'user_answers', 'question']
        validation_error = validate_required_fields(data, required_fields)
        if validation_error:
            return error_response(validation_error, 400)
        
        user_id = data.get('user_id')
        segment_id = data.get('segment_id')
        question_type = data.get('question_type')
        document = data.get('document')
        user_answers = data.get('user_answers')
        question = data.get('question')
        
        logger.info(f"=== 收到提问请求 ===")
        logger.info(f"用户ID: {user_id}")
        logger.info(f"Segment ID: {segment_id}")
        logger.info(f"问题类型: {question_type}")
        logger.info(f"问题内容: {question[:100]}...")
        logger.info(f"用户答案数量: {len(user_answers)}")
        
        # 调用服务处理问题
        logger.info(f"开始调用 qa_service.process_question")
        result = qa_service.process_question(
            user_id=user_id,
            segment_id=segment_id,
            question_type=question_type,
            document=document,
            user_answers=user_answers,
            question=question
        )
        logger.info(f"qa_service.process_question 调用完成")
        
        if result.get('success'):
            logger.info(f"问题处理成功 - 轮次: {result.get('round_id')}")
            return success_response(data=result, message="回答成功")
        else:
            error_msg = result.get('error', '未知错误')
            logger.error(f"问题处理失败: {error_msg}")
            return error_response(error_msg, 500)
    
    except Exception as e:
        logger.error(f"处理提问异常: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/end-conversation', methods=['POST'])
def end_conversation():
    """
    结束会话
    
    请求体:
    {
        "userId": "用户唯一标识"
    }
    
    响应:
    {
        "success": true,
        "data": null,
        "message": "会话结束成功"
    }
    """
    try:
        data = request.get_json()
        validation_error = validate_required_fields(data, ['userId'])
        if validation_error:
            return error_response(validation_error, 400)
        
        user_id = data.get('userId')
        
        logger.info(f"结束会话 - 用户: {user_id}")
        
        # 调用QA服务结束会话
        result = qa_service.end_conversation(user_id)
        
        if result['success']:
            logger.info("会话结束成功")
            return success_response(data=result['data'], message=result['message'])
        else:
            logger.error(f"会话结束失败: {result['message']}")
            return error_response(result['message'], 500)
            
    except Exception as e:
        logger.error(f"结束会话异常: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/tutoring/end-session', methods=['POST'])
def end_tutoring_session():
    """
    结束辅导会话，保存对话到ChromaDB
    
    请求体:
    {
        "user_id": 123,
        "segment_id": "3BlankedCloze1"
    }
    
    响应:
    {
        "success": true,
        "message": "会话已保存"
    }
    """
    try:
        data = request.get_json()
        
        logger.info(f"\n{'='*60}")
        logger.info(f"=== 收到结束会话请求 ===")
        logger.info(f"{'='*60}")
        logger.info(f"请求数据: {json.dumps(data, ensure_ascii=False)}")
        
        # 验证必需字段
        validation_error = validate_required_fields(data, ['user_id', 'segment_id'])
        if validation_error:
            logger.error(f"❌ 参数验证失败: {validation_error}")
            return error_response(validation_error, 400)
        
        user_id = data.get('user_id')
        segment_id = data.get('segment_id')
        
        logger.info(f"结束辅导会话 - 用户: {user_id}, segment_id: {segment_id}")
        
        # 结束会话并保存
        qa_service.end_session(user_id, segment_id)
        
        logger.info("✅ 会话已成功保存到ChromaDB")
        return success_response(message="会话已保存")
    
    except Exception as e:
        logger.error(f"❌ 结束会话异常: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/tutoring/ask-stream', methods=['POST'])
def ask_question_stream():
    """
    学生向AI辅导老师提问 (流式SSE响应)
    使用DeepSeek API进行流式回答
    
    请求体: 与 /tutoring/ask 相同
    响应: Server-Sent Events (SSE) 流式数据
    """
    from flask import Response, stream_with_context
    
    def generate():
        try:
            data = request.get_json()
            
            # 验证必需字段
            required_fields = ['user_id', 'segment_id', 'question_type', 'document', 'user_answers', 'question']
            validation_error = validate_required_fields(data, required_fields)
            if validation_error:
                yield f"data: {json.dumps({'type': 'error', 'message': validation_error}, ensure_ascii=False)}\n\n"
                return
            
            user_id = data.get('user_id')
            segment_id = data.get('segment_id')
            question_type = data.get('question_type')
            document = data.get('document')
            user_answers = data.get('user_answers')
            question = data.get('question')
            
            logger.info(f"=== 收到流式提问请求 ===")
            logger.info(f"用户ID: {user_id}, Segment ID: {segment_id}")
            
            # 调用服务处理问题（流式）
            event_count = 0
            for event in qa_service.process_question_stream(
                user_id=user_id,
                segment_id=segment_id,
                question_type=question_type,
                document=document,
                user_answers=user_answers,
                question=question
            ):
                event_count += 1
                event_json = json.dumps(event, ensure_ascii=False)
                logger.info(f"发送事件 #{event_count}: type={event.get('type')}, content_length={len(event.get('content', ''))}")
                yield f"data: {event_json}\n\n"
            
            logger.info(f"流式回答发送完成，共发送 {event_count} 个事件")
        
        except Exception as e:
            logger.error(f"流式处理异常: {e}", exc_info=True)
            yield f"data: {json.dumps({'type': 'error', 'message': str(e)}, ensure_ascii=False)}\n\n"
    
    return Response(stream_with_context(generate()), mimetype='text/event-stream')


@cet4_bp.route('/smart-qa/background', methods=['GET'])
def smart_qa_background():
    """获取智能问答的考试分析与历史摘要"""
    try:
        user_id = request.args.get('user_id', type=int)
        if not user_id:
            return error_response("user_id不能为空", 400)

        data = smart_qa_service.get_background_documents(user_id)
        return success_response(data=data, message="背景加载成功")
    except Exception as e:
        logger.error(f"获取智能问答背景失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/smart-qa/ask-stream', methods=['POST'])
def smart_qa_ask_stream():
    """智能问答流式接口：基于考试分析的自由问答"""
    from flask import Response, stream_with_context

    def generate():
        try:
            data = request.get_json() or {}
            validation_error = validate_required_fields(data, ['user_id', 'question'])
            if validation_error:
                yield f"data: {json.dumps({'type': 'error', 'message': validation_error}, ensure_ascii=False)}\n\n"
                return

            user_id = data.get('user_id')
            question = data.get('question')
            segment_id = data.get('segment_id', 'analysis')

            event_count = 0
            for event in smart_qa_service.process_question_stream(user_id=user_id, question=question, segment_id=segment_id):
                event_count += 1
                yield f"data: {json.dumps(event, ensure_ascii=False)}\n\n"
        except Exception as e:
            logger.error(f"智能问答流式处理异常: {e}", exc_info=True)
            yield f"data: {json.dumps({'type': 'error', 'message': str(e)}, ensure_ascii=False)}\n\n"

    return Response(stream_with_context(generate()), mimetype='text/event-stream')


@cet4_bp.route('/smart-qa/end-session', methods=['POST'])
def end_smart_qa_session():
    """结束智能问答会话并保存摘要"""
    try:
        data = request.get_json() or {}
        validation_error = validate_required_fields(data, ['user_id'])
        if validation_error:
            return error_response(validation_error, 400)

        user_id = data.get('user_id')
        segment_id = data.get('segment_id', 'analysis')
        smart_qa_service.end_session(user_id, segment_id)
        return success_response(message="智能问答会话已保存")
    except Exception as e:
        logger.error(f"结束智能问答会话失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


# ==================== CET4 学情分析 API ====================

# === 学生个人分析 ===

@cet4_bp.route('/analytics/student/score-trend/<int:user_id>', methods=['GET'])
def get_student_score_trend(user_id):
    """1. 学生成绩变化趋势 - 用于折线图"""
    try:
        data = cet4_analytics_service.get_student_score_trend(user_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取学生成绩趋势失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/student/segment-analysis/<int:user_id>', methods=['GET'])
def get_student_segment_analysis(user_id):
    """2. 学生各题型得分率分析 - 用于柱状图/雷达图"""
    try:
        data = cet4_analytics_service.get_student_segment_analysis(user_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取学生题型分析失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/student/section-analysis/<int:user_id>', methods=['GET'])
def get_student_section_analysis(user_id):
    """3. 学生大类题型分析(听力/阅读/写作/翻译) - 用于饼图"""
    try:
        data = cet4_analytics_service.get_student_section_analysis(user_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取学生板块分析失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/student/weak-points/<int:user_id>', methods=['GET'])
def get_student_weak_points(user_id):
    """4. 学生薄弱点分析 - 用于列表展示"""
    try:
        data = cet4_analytics_service.get_student_weak_points(user_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取学生薄弱点失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/student/progress/<int:user_id>', methods=['GET'])
def get_student_progress(user_id):
    """5. 学生进步情况对比 - 用于对比图"""
    try:
        data = cet4_analytics_service.get_student_progress_comparison(user_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取学生进步情况失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/student/exam-history/<int:user_id>', methods=['GET'])
def get_student_exam_history(user_id):
    """13. 学生考试历史记录 - 用于表格展示"""
    try:
        data = cet4_analytics_service.get_student_exam_history(user_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取学生考试历史失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/student/listening-reading/<int:user_id>', methods=['GET'])
def get_listening_reading_analysis(user_id):
    """14. 听力与阅读能力对比 - 用于双折线图"""
    try:
        data = cet4_analytics_service.get_listening_vs_reading_analysis(user_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取听力阅读对比失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/student/time-comparison/<int:user_id>', methods=['GET'])
def get_time_comparison(user_id):
    """16. 时间段对比分析 - 用于对比柱状图"""
    try:
        days = request.args.get('days', 30, type=int)
        data = cet4_analytics_service.get_time_period_comparison(user_id, days)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取时间段对比失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/student/vs-class/<int:user_id>/<int:class_id>', methods=['GET'])
def get_student_vs_class(user_id, class_id):
    """11. 学生与班级平均对比 - 用于对比雷达图"""
    try:
        data = cet4_analytics_service.get_student_vs_class_average(user_id, class_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取学生班级对比失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


# === 班级分析 ===

@cet4_bp.route('/analytics/class/overview/<int:class_id>', methods=['GET'])
def get_class_overview(class_id):
    """6. 班级整体情况概览 - 用于统计卡片"""
    try:
        data = cet4_analytics_service.get_class_overview(class_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取班级概览失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/class/score-distribution/<int:class_id>', methods=['GET'])
def get_class_score_distribution(class_id):
    """7. 班级成绩分布 - 用于分布柱状图"""
    try:
        data = cet4_analytics_service.get_class_score_distribution(class_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取班级成绩分布失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/class/segment-comparison/<int:class_id>', methods=['GET'])
def get_class_segment_comparison(class_id):
    """8. 班级各题型平均得分率 - 用于雷达图"""
    try:
        data = cet4_analytics_service.get_class_segment_comparison(class_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取班级题型对比失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/class/student-ranking/<int:class_id>', methods=['GET'])
def get_class_student_ranking(class_id):
    """9. 班级学生排名 - 用于排行榜"""
    try:
        limit = request.args.get('limit', 20, type=int)
        data = cet4_analytics_service.get_class_student_ranking(class_id, limit)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取班级排名失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/class/trend/<int:class_id>', methods=['GET'])
def get_class_trend(class_id):
    """10. 班级成绩趋势 - 用于多折线图"""
    try:
        data = cet4_analytics_service.get_class_trend(class_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取班级趋势失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


@cet4_bp.route('/analytics/class/weak-points/<int:class_id>', methods=['GET'])
def get_class_weak_points(class_id):
    """15. 班级薄弱点分析 - 用于对比展示"""
    try:
        data = cet4_analytics_service.get_class_weak_points(class_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取班级薄弱点失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


# === 试卷分析 ===

@cet4_bp.route('/analytics/exam-paper/<int:exam_paper_id>', methods=['GET'])
def get_exam_paper_analysis(exam_paper_id):
    """12. 试卷分析 - 用于试卷难度分析"""
    try:
        data = cet4_analytics_service.get_exam_paper_analysis(exam_paper_id)
        return success_response(data=data, message='获取成功')
    except Exception as e:
        logger.error(f"获取试卷分析失败: {e}", exc_info=True)
        return error_response(f"服务器内部错误: {str(e)}", 500)


# === 学情分析健康检查 ===

@cet4_bp.route('/analytics/health', methods=['GET'])
def analytics_health_check():
    """学情分析健康检查"""
    return success_response(data={
        'status': 'healthy',
        'service': 'CET4-learning-analytics',
        'endpoints': {
            'student': [
                '/analytics/student/score-trend/<user_id>',
                '/analytics/student/segment-analysis/<user_id>',
                '/analytics/student/section-analysis/<user_id>',
                '/analytics/student/weak-points/<user_id>',
                '/analytics/student/progress/<user_id>',
                '/analytics/student/exam-history/<user_id>',
                '/analytics/student/listening-reading/<user_id>',
                '/analytics/student/time-comparison/<user_id>',
                '/analytics/student/vs-class/<user_id>/<class_id>'
            ],
            'class': [
                '/analytics/class/overview/<class_id>',
                '/analytics/class/score-distribution/<class_id>',
                '/analytics/class/segment-comparison/<class_id>',
                '/analytics/class/student-ranking/<class_id>',
                '/analytics/class/trend/<class_id>',
                '/analytics/class/weak-points/<class_id>'
            ],
            'exam': [
                '/analytics/exam-paper/<exam_paper_id>'
            ]
        }
    }, message='CET4学情分析服务运行正常')
