"""
配置文件 - 统一管理所有服务的配置
"""
import os
from pathlib import Path

# 基础路径
BASE_DIR = Path(__file__).parent
UPLOAD_FOLDER = BASE_DIR / 'uploads'
OUTPUT_FOLDER = BASE_DIR / 'outputs'
LOG_FOLDER = BASE_DIR / 'logs'

# 确保目录存在
UPLOAD_FOLDER.mkdir(exist_ok=True)
OUTPUT_FOLDER.mkdir(exist_ok=True)
LOG_FOLDER.mkdir(exist_ok=True)

# Flask 配置
class Config:
    """Flask 应用配置"""
    SECRET_KEY = os.environ.get('SECRET_KEY', 'dev-secret-key-change-in-production')
    
    # 文件上传配置
    MAX_CONTENT_LENGTH = 16 * 1024 * 1024  # 16MB
    UPLOAD_FOLDER = str(UPLOAD_FOLDER)
    OUTPUT_FOLDER = str(OUTPUT_FOLDER)
    
    # 跨域配置
    CORS_ORIGINS = ["*"]
    
    # 日志配置
    LOG_LEVEL = os.environ.get('LOG_LEVEL', 'INFO')
    LOG_FILE = str(LOG_FOLDER / 'services.log')


# 服务端口配置
class ServicePorts:
    """各服务端口配置"""
    AUDIO_CLONE = 5001
    CET4_DIALOGUE = 5002
    CET4_QA = 5003
    CET4_TEACHING = 5004
    HS_DIALOGUE = 5005
    HS_QA = 5006
    HS_TEACHING = 5007
    MAIN_APP = 5000  # 主应用统一入口


# 外部服务配置
class ExternalServices:
    """外部服务配置"""
    # ChromaDB 配置
    CHROMA_HOST = os.environ.get('CHROMA_HOST', 'localhost')
    CHROMA_PORT = os.environ.get('CHROMA_PORT', '8000')
    
    # 后端服务配置
    BACKEND_HOST = os.environ.get('BACKEND_HOST', 'localhost')
    BACKEND_PORT = os.environ.get('BACKEND_PORT', '8080')
    
    # OpenAI/其他AI服务配置
    OPENAI_API_KEY = os.environ.get('OPENAI_API_KEY', '')
    OPENAI_API_BASE = os.environ.get('OPENAI_API_BASE', '')
