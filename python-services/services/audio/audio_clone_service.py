"""
音频克隆服务 - 基于pyttsx3的离线语音合成
从原 audio_clone_server.py 迁移并模块化
"""
from flask import Blueprint, request, jsonify, send_file, Flask
from flask_cors import CORS
import uuid
from pathlib import Path
import warnings
import os
import platform
import ctypes
from ctypes import wintypes
import contextlib
import threading
import os as _os_for_pid

from common.logger import setup_logger
from common.response import success_response, error_response
from config import Config

warnings.filterwarnings('ignore')

# 设置日志
logger = setup_logger('audio_clone_service')

# 依赖标记
PYCAW_READY = False
try:
    from pycaw.pycaw import AudioUtilities, ISimpleAudioVolume
    from comtypes import CLSCTX_ALL  # noqa: F401
    PYCAW_READY = True
    logger.info("✓ 成功加载 pycaw，支持仅静音本进程")
except Exception as exc:
    logger.info(f"pycaw 不可用，静音将退回系统级: {exc}")

# Windows 主音量控制（仅用于静音生成时的临时关闭）
_WAVE_MAPPER_HANDLE = ctypes.c_void_p(0xFFFFFFFF)  # waveOut 默认设备 (WAVE_MAPPER)
_volume_lock = threading.Lock()


@contextlib.contextmanager
def _temporary_system_mute():
    """在 Windows 上暂时静音系统主音量，生成完毕恢复。"""
    if platform.system().lower() != 'windows':
        yield
        return

    try:
        winmm = ctypes.WinDLL('winmm')

        # 明确函数签名以避免 Python int 自动转换溢出
        HWAVEOUT = ctypes.c_void_p  # 使用通用 void* 句柄
        winmm.waveOutGetVolume.argtypes = [HWAVEOUT, ctypes.POINTER(wintypes.DWORD)]
        winmm.waveOutGetVolume.restype = wintypes.UINT
        winmm.waveOutSetVolume.argtypes = [HWAVEOUT, wintypes.DWORD]
        winmm.waveOutSetVolume.restype = wintypes.UINT

        current_volume = wintypes.DWORD()

        # 读取当前音量
        if winmm.waveOutGetVolume(_WAVE_MAPPER_HANDLE, ctypes.byref(current_volume)) != 0:
            yield
            return

        with _volume_lock:
            try:
                # 静音
                winmm.waveOutSetVolume(_WAVE_MAPPER_HANDLE, wintypes.DWORD(0))
                yield
            finally:
                # 恢复音量
                winmm.waveOutSetVolume(_WAVE_MAPPER_HANDLE, current_volume)
    except Exception as exc:
        logger.warning(f"暂时静音失败，跳过静音流程: {exc}")
        yield


@contextlib.contextmanager
def _temporary_session_mute():
    """优先静音当前进程会话；失败则回退到系统主音量。"""
    if platform.system().lower() != 'windows':
        yield
        return

    if PYCAW_READY:
        try:
            sessions = AudioUtilities.GetAllSessions()
            current_pid = _os_for_pid.getpid()
            for session in sessions:
                if not session.Process:
                    continue
                if session.Process.pid != current_pid:
                    continue

                ctl = session._ctl.QueryInterface(ISimpleAudioVolume)
                prev_mute = ctl.GetMute()
                prev_vol = ctl.GetMasterVolume()
                ctl.SetMute(1, None)
                try:
                    yield
                finally:
                    ctl.SetMute(prev_mute, None)
                    ctl.SetMasterVolume(prev_vol, None)
                return
        except Exception as exc:
            logger.warning(f"会话静音失败，改用系统静音: {exc}")

    # 回退到系统主音量静音
    with _temporary_system_mute():
        yield

# 检查依赖
try:
    import pyttsx3
    AUDIO_READY = True
    logger.info("✓ 成功加载 pyttsx3，支持离线朗读")
except ImportError as exc:
    AUDIO_READY = False
    pyttsx3 = None
    logger.error(f"✗ 缺少 pyttsx3: {exc}")

try:
    from pydub import AudioSegment
    PYDUB_READY = True
    logger.info("✓ 成功加载 pydub，支持音频合成")
except ImportError as exc:
    PYDUB_READY = False
    AudioSegment = None
    logger.error(f"✗ 缺少 pydub: {exc}")

# 创建蓝图
audio_bp = Blueprint('audio', __name__)

# 输出目录（直接使用outputs目录，不再添加audio子目录）
OUTPUT_FOLDER = Path(Config.OUTPUT_FOLDER)
OUTPUT_FOLDER.mkdir(exist_ok=True, parents=True)


class OfflineSpeaker:
    """封装 pyttsx3，提供离线英文朗读（男女声）"""

    def __init__(self):
        self.engine = None
        self.voice_map = {'female': None, 'male': None}
        self.default_voice = None
        self.voice_options = []
        if AUDIO_READY:
            self._init_engine()

    def _init_engine(self):
        try:
            self.engine = pyttsx3.init()
            rate = 165  # 默认语速
            self.engine.setProperty('rate', rate)
            self.engine.setProperty('volume', 1.0)

            voices = self.engine.getProperty('voices') or []
            self.voice_options = []
            female_keywords = ['female', 'zira', 'aria', 'eva', 'lily', 'zhiyu', 'susan']
            male_keywords = ['male', 'david', 'mark', 'ryan', 'guy', 'tony', 'sam', 'richard']

            for v in voices:
                languages = [str(lang) for lang in (getattr(v, 'languages', []) or [])]
                gender = str(getattr(v, 'gender', '') or '')
                info = {
                    'id': v.id,
                    'name': v.name,
                    'languages': languages,
                    'gender': gender
                }
                self.voice_options.append(info)

                descriptor = ' '.join([
                    (v.id or ''),
                    (v.name or ''),
                    gender,
                    ' '.join(languages)
                ]).lower()

                if not self.voice_map['female'] and any(key in descriptor for key in female_keywords):
                    self.voice_map['female'] = v.id
                if not self.voice_map['male'] and any(key in descriptor for key in male_keywords):
                    self.voice_map['male'] = v.id

            # 兜底：至少保证 male/female 指向不同语音
            if voices:
                self.default_voice = voices[0].id
                if not self.voice_map['female']:
                    self.voice_map['female'] = voices[0].id
                if not self.voice_map['male']:
                    alternative = next((v.id for v in voices if v.id != self.voice_map['female']), None)
                    self.voice_map['male'] = alternative or voices[0].id

            # 默认设置为女声
            target_voice = self.voice_map['female'] or self.default_voice
            if target_voice:
                self.engine.setProperty('voice', target_voice)

            logger.info("✓ TTS 引擎准备完成")
            logger.info(f"  female voice => {self.voice_map['female']}")
            logger.info(f"  male voice   => {self.voice_map['male']}")
        except Exception as exc:
            logger.error(f"✗ 初始化失败: {exc}")
            self.engine = None

    def speak(self, text: str, output_path: Path, voice: str = 'female') -> bool:
        if not self.engine:
            raise RuntimeError("pyttsx3 引擎不可用，请检查依赖")

        voice_id = self.voice_map.get(voice) or self.default_voice
        if voice_id:
            self.engine.setProperty('voice', voice_id)
        else:
            logger.warning(f"未找到 {voice} 音色，使用默认语音")
        with _temporary_session_mute():
            self.engine.save_to_file(text, str(output_path))
            self.engine.runAndWait()
        return True


# 初始化语音引擎
speaker = OfflineSpeaker()


@audio_bp.route('/synthesize-combined', methods=['POST'])
def synthesize_combined():
    """生成组合的听力音频"""
    try:
        data = request.get_json()
        segments = data.get('segments', [])
        
        if not segments:
            return error_response('请提供音频片段', 400)
        
        if not speaker.engine:
            return error_response('本地 TTS 引擎不可用', 500)
        
        if not PYDUB_READY:
            return error_response('pydub不可用，无法合并音频', 500)
        
        audio_id = uuid.uuid4().hex[:8]
        audio_files = []
        
        for i, segment in enumerate(segments):
            if segment.get('text') == 'pause':
                duration = segment.get('duration', 5000)
                silence_path = OUTPUT_FOLDER / f"{audio_id}_silence_{i}.wav"
                silence = AudioSegment.silent(duration=duration)
                silence.export(str(silence_path), format='wav')
                audio_files.append(str(silence_path))
                logger.info(f"[{i}] pause {duration}ms")
                continue
            
            text = segment.get('text', '').strip()
            voice = segment.get('voice', 'female')
            
            if not text:
                continue
            
            output_filename = f"{audio_id}_segment_{i}.wav"
            output_path = OUTPUT_FOLDER / output_filename
            
            success = speaker.speak(text, output_path, voice=voice)
            if success:
                audio_files.append(str(output_path))
                preview = text.replace('\n', ' ')[:60] + ('...' if len(text) > 60 else '')
                logger.info(f"[{i}] {voice}: '{preview}'")
        
        # 合并音频
        combined = AudioSegment.empty()
        for audio_file in audio_files:
            if os.path.exists(audio_file):
                audio = AudioSegment.from_wav(audio_file)
                combined += audio
        
        combined_filename = f"{audio_id}_combined.wav"
        combined_path = OUTPUT_FOLDER / combined_filename
        combined.export(str(combined_path), format='wav')
        
        return success_response({
            'audio_url': f"/api/audio/file/{combined_filename}",
            'message': f'已生成组合音频，包含 {len(audio_files)} 个片段'
        })
            
    except Exception as exc:
        logger.error(f"组合音频错误: {exc}", exc_info=True)
        return error_response(str(exc), 500)


@audio_bp.route('/file/<filename>', methods=['GET'])
def get_audio(filename):
    """获取生成的音频文件"""
    file_path = OUTPUT_FOLDER / filename
    if file_path.exists():
        return send_file(str(file_path), mimetype='audio/wav')
    return error_response('文件不存在', 404)


@audio_bp.route('/merge-audios', methods=['POST'])
def merge_audios():
    """合并多个已生成的音频文件"""
    try:
        data = request.get_json()
        audio_files = data.get('audio_files', [])
        
        # 详细的调试日志
        logger.info(f"=== MERGE AUDIOS REQUEST ===")
        logger.info(f"收到的原始数据: {data}")
        logger.info(f"audio_files 数量: {len(audio_files)}")
        logger.info(f"audio_files 内容: {audio_files}")
        
        if not audio_files:
            return error_response('请提供音频文件路径', 400)
        
        if not PYDUB_READY:
            return error_response('pydub不可用，无法合并音频', 500)
        
        combined = AudioSegment.empty()
        valid_files_count = 0
        
        for idx, file_path in enumerate(audio_files):
            # 处理各种可能的路径格式
            # Java传来的可能是: /api/audio/file/xxx.wav, /file/xxx.wav, audio/xxx.wav, xxx.wav
            logger.info(f"[{idx}] 处理音频文件路径: '{file_path}' (type: {type(file_path).__name__}, len: {len(file_path) if file_path else 0})")
            
            # 移除开头的斜杠
            if file_path.startswith('/'):
                file_path = file_path[1:]
            
            # 移除 api/audio/file/ 前缀
            if file_path.startswith('api/audio/file/'):
                file_path = file_path[15:]
            elif file_path.startswith('file/'):
                file_path = file_path[5:]
            elif file_path.startswith('audio/file/'):
                file_path = file_path[11:]
            elif file_path.startswith('audio/'):
                file_path = file_path[6:]
            
            logger.info(f"处理后的文件名: {file_path}")
            
            full_path = OUTPUT_FOLDER / file_path
            logger.info(f"完整路径: {full_path}")
            
            if full_path.exists() and full_path.is_file():
                audio = AudioSegment.from_wav(str(full_path))
                combined += audio
                valid_files_count += 1
                logger.info(f"成功加载音频: {file_path}, 时长: {len(audio)}ms")
            else:
                logger.warning(f"音频文件不存在或不是文件: {full_path}")
        
        if len(combined) == 0:
            logger.error(f"没有有效的音频文件可合并，请求的文件: {audio_files}")
            return error_response('没有有效的音频文件可合并', 500)
        
        merged_id = uuid.uuid4().hex[:8]
        merged_filename = f"merged_{merged_id}.wav"
        merged_path = OUTPUT_FOLDER / merged_filename
        combined.export(str(merged_path), format='wav')
        
        logger.info(f"成功合并音频: {merged_filename}, 总时长: {len(combined)}ms, 合并了 {valid_files_count} 个文件")
        
        return success_response({
            'audio_url': f"/api/audio/file/{merged_filename}",
            'message': f'已合并 {valid_files_count} 个音频文件',
            'duration_ms': len(combined)
        })
        
    except Exception as exc:
        logger.error(f"合并音频错误: {exc}", exc_info=True)
        return error_response(str(exc), 500)


def create_audio_app():
    """创建独立的音频服务应用（用于单独启动）"""
    app = Flask(__name__)
    app.config.from_object(Config)
    CORS(app, 
         resources={r"/*": {"origins": "*"}},
         supports_credentials=True,
         allow_headers=["Content-Type", "Authorization"],
         methods=["GET", "POST", "PUT", "DELETE", "OPTIONS"])
    app.register_blueprint(audio_bp)
    
    @app.route('/')
    def index():
        return success_response({
            'service': 'Audio Clone Service',
            'status': 'running',
            'endpoints': [
                '/synthesize-combined', 
                '/file/<filename>', 
                '/merge-audios'
            ]
        })
    
    return app


if __name__ == '__main__':
    from config import ServicePorts
    app = create_audio_app()
    port = ServicePorts.AUDIO_CLONE
    logger.info(f"启动音频克隆服务，端口: {port}")
    print(f"\n音频克隆服务启动 - http://localhost:{port}\n")
    app.run(host='0.0.0.0', port=port, debug=True)
