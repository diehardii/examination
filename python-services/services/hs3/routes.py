# HS3 高考英语路由配置
# 参照 cet4/routes.py 结构

"""
HS3 高考英语 API 路由

该文件定义高考英语相关的所有 API 路由
"""

from flask import Blueprint

# 创建 HS3 蓝图
hs3_bp = Blueprint('hs3', __name__, url_prefix='/api/hs3')


# TODO: 添加路由
# @hd3_bp.route('/analyze', methods=['POST'])
# def analyze_paper():
#     pass

# @hd3_bp.route('/generate', methods=['POST'])
# def generate_paper():
#     pass

# @hd3_bp.route('/qa', methods=['POST'])
# def smart_qa():
#     pass
