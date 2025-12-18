"""
主应用入口 - 统一管理所有Python后台服务
支持单独启动各个服务或统一启动所有服务
"""
from flask import Flask, jsonify
from flask_cors import CORS
import logging
from pathlib import Path

from config import Config, ServicePorts
from common.logger import setup_logger
from services.audio.audio_clone_service import audio_bp
from services.cet4.routes import cet4_bp

# 设置日志
logger = setup_logger('main_app')


def create_app():
    """创建Flask应用"""
    app = Flask(__name__)
    app.config.from_object(Config)
    
    # 启用跨域 - 支持credentials
    CORS(app, 
         resources={r"/api/*": {"origins": "*"}},
         supports_credentials=True,
         allow_headers=["Content-Type", "Authorization"],
         methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"])
    
    # 注册所有服务蓝图
    register_blueprints(app)
    
    # 注册全局路由
    register_routes(app)
    
    logger.info("Flask应用初始化完成")
    return app


def register_blueprints(app):
    """注册所有服务蓝图"""
    # 音频克隆服务
    app.register_blueprint(audio_bp, url_prefix='/api/audio')
    
    # CET4 服务
    app.register_blueprint(cet4_bp, url_prefix='/api/cet4')
    
    # TODO: 高中英语服务（待实现）
    # app.register_blueprint(hs_dialogue_bp, url_prefix='/api/highschool/dialogue')
    # app.register_blueprint(hs_qa_bp, url_prefix='/api/highschool/qa')
    # app.register_blueprint(hs_teaching_bp, url_prefix='/api/highschool/teaching')
    
    logger.info("服务蓝图注册完成")


def register_routes(app):
    """注册全局路由"""
    
    @app.route('/')
    def index():
        """首页 - 服务状态"""
        return jsonify({
            'status': 'running',
            'message': 'ExaminationEn Python Services',
            'services': {
                'audio_clone': {
                    'url': '/api/audio',
                    'status': 'available',
                    'description': '音频克隆服务'
                },
                'cet4': {
                    'url': '/api/cet4',
                    'status': 'available',
                    'description': 'CET4试卷生成服务',
                    'endpoints': {
                        'generate_exam': '/api/cet4/generate-exam',
                        'health': '/api/cet4/health'
                    }
                },
                'highschool': {
                    'dialogue': {'status': 'pending', 'description': '高中智能对话（待实现）'},
                    'qa': {'status': 'pending', 'description': '高中错题答疑（待实现）'},
                    'teaching': {'status': 'pending', 'description': '高中AI教学（待实现）'}
                }
            }
        })
    
    @app.route('/health')
    def health():
        """健康检查"""
        return jsonify({'status': 'healthy', 'message': 'All services are running'})
    
    @app.errorhandler(404)
    def not_found(error):
        """404错误处理"""
        return jsonify({'error': 'Not Found', 'message': str(error)}), 404
    
    @app.errorhandler(500)
    def internal_error(error):
        """500错误处理"""
        logger.error(f"Internal Server Error: {error}")
        return jsonify({'error': 'Internal Server Error', 'message': str(error)}), 500


if __name__ == '__main__':
    app = create_app()
    port = ServicePorts.MAIN_APP
    logger.info(f"启动主应用服务，端口: {port}")
    print(f"\n{'='*60}")
    print(f"ExaminationEn Python Services 已启动")
    print(f"主服务地址: http://localhost:{port}")
    print(f"健康检查: http://localhost:{port}/health")
    print(f"{'='*60}\n")
    app.run(host='0.0.0.0', port=port, debug=True)
