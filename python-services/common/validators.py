"""
通用验证器
"""
from functools import wraps
from flask import request
from common.response import error_response


def validate_required_fields(data, required_fields):
    """
    验证字典数据中的必需字段
    
    Args:
        data: 待验证的字典数据
        required_fields: 必需字段列表
    
    Returns:
        错误消息字符串，如果验证通过则返回None
    """
    if not data:
        return "请求数据为空"
    
    # 修复：允许空数组/空列表，只检查字段是否存在
    missing_fields = [field for field in required_fields if field not in data or data[field] is None]
    
    if missing_fields:
        return f"缺少必需字段: {', '.join(missing_fields)}"
    
    return None


def validate_json(*required_fields):
    """
    验证JSON请求数据
    
    Args:
        *required_fields: 必需的字段名
    
    Returns:
        装饰器函数
    """
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            if not request.is_json:
                return error_response("请求必须是JSON格式", 400)
            
            data = request.get_json()
            error = validate_required_fields(data, required_fields)
            
            if error:
                return error_response(error, 400)
            
            return f(*args, **kwargs)
        return decorated_function
    return decorator


def validate_file(allowed_extensions=None):
    """
    验证文件上传
    
    Args:
        allowed_extensions: 允许的文件扩展名列表
    
    Returns:
        装饰器函数
    """
    def decorator(f):
        @wraps(f)
        def decorated_function(*args, **kwargs):
            if 'file' not in request.files:
                return error_response("未找到文件", 400)
            
            file = request.files['file']
            if file.filename == '':
                return error_response("未选择文件", 400)
            
            if allowed_extensions:
                ext = file.filename.rsplit('.', 1)[1].lower() if '.' in file.filename else ''
                if ext not in allowed_extensions:
                    return error_response(
                        f"不支持的文件类型，仅支持: {', '.join(allowed_extensions)}", 
                        400
                    )
            
            return f(*args, **kwargs)
        return decorated_function
    return decorator
