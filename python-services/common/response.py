"""
统一响应格式
"""
from flask import jsonify


def success_response(data=None, message="success", code=200):
    """
    成功响应
    
    Args:
        data: 响应数据
        message: 响应消息
        code: HTTP状态码
    
    Returns:
        JSON响应
    """
    response = {
        'code': code,
        'message': message,
        'success': True
    }
    if data is not None:
        response['data'] = data
    return jsonify(response), code


def error_response(message="error", code=400, error_type=None):
    """
    错误响应
    
    Args:
        message: 错误消息
        code: HTTP状态码
        error_type: 错误类型
    
    Returns:
        JSON响应
    """
    response = {
        'code': code,
        'message': message,
        'success': False
    }
    if error_type:
        response['error_type'] = error_type
    return jsonify(response), code
