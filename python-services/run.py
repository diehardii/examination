"""
服务启动脚本 - 支持启动单个服务或所有服务
Usage:
    python run.py               # 启动所有服务（主应用）
    python run.py audio         # 仅启动音频克隆服务
    python run.py cet4-dialogue # 仅启动CET4对话服务
    python run.py cet4-qa       # 仅启动CET4答疑服务
    python run.py cet4-teaching # 仅启动CET4教学服务
    python run.py hs-dialogue   # 仅启动高中对话服务
    python run.py hs-qa         # 仅启动高中答疑服务
    python run.py hs-teaching   # 仅启动高中教学服务
"""
import sys
from pathlib import Path

# 添加项目根目录到路径
sys.path.insert(0, str(Path(__file__).parent))

from config import ServicePorts
from common.logger import setup_logger

logger = setup_logger('run')


def run_main_app():
    """运行主应用（所有服务）"""
    from app import create_app
    app = create_app()
    port = ServicePorts.MAIN_APP
    logger.info(f"启动主应用，端口: {port}")
    app.run(host='0.0.0.0', port=port, debug=True)


def run_audio_service():
    """运行音频克隆服务"""
    from services.audio.audio_clone_service import create_audio_app
    app = create_audio_app()
    port = ServicePorts.AUDIO_CLONE
    logger.info(f"启动音频克隆服务，端口: {port}")
    app.run(host='0.0.0.0', port=port, debug=True)


# TODO: 以下服务待实现
# def run_cet4_dialogue():
#     """运行CET4对话服务"""
#     pass

# def run_cet4_qa():
#     """运行CET4答疑服务"""
#     pass

# def run_cet4_teaching():
#     """运行CET4教学服务"""
#     pass

# def run_hs_dialogue():
#     """运行高中对话服务"""
#     pass

# def run_hs_qa():
#     """运行高中答疑服务"""
#     pass

# def run_hs_teaching():
#     """运行高中教学服务"""
#     pass


SERVICE_MAP = {
    'main': run_main_app,
    'audio': run_audio_service,
    # TODO: 其他服务待实现
    # 'cet4-dialogue': run_cet4_dialogue,
    # 'cet4-qa': run_cet4_qa,
    # 'cet4-teaching': run_cet4_teaching,
    # 'hs-dialogue': run_hs_dialogue,
    # 'hs-qa': run_hs_qa,
    # 'hs-teaching': run_hs_teaching,
}


def print_usage():
    """打印使用说明"""
    print("\n" + "="*60)
    print("ExaminationEn Python Services - 启动脚本")
    print("="*60)
    print("\n使用方法:")
    print("  python run.py               # 启动所有服务（主应用，端口5000）")
    print("  python run.py audio         # 仅启动音频克隆服务（端口5001）")
    print("\n待实现的服务:")
    print("  - CET4对话服务 (cet4-dialogue)")
    print("  - CET4答疑服务 (cet4-qa)")
    print("  - CET4教学服务 (cet4-teaching)")
    print("  - 高中对话服务 (hs-dialogue)")
    print("  - 高中答疑服务 (hs-qa)")
    print("  - 高中教学服务 (hs-teaching)")
    print("\n" + "="*60 + "\n")


if __name__ == '__main__':
    if len(sys.argv) > 1:
        service = sys.argv[1].lower()
        if service in SERVICE_MAP:
            SERVICE_MAP[service]()
        elif service in ['help', '-h', '--help']:
            print_usage()
        else:
            print(f"❌ 未知服务: {service}")
            print_usage()
            sys.exit(1)
    else:
        # 默认启动主应用
        run_main_app()
