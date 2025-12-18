"""
CET4 AI辅导老师问答服务
负责管理学生与AI教师的对话，包括prompt管理、ChromaDB交互、Coze API调用（摘要）、DeepSeek API调用（问答）
"""
import json
import logging
import requests
from datetime import datetime
from typing import Dict, Any, List, Optional, Generator
import chromadb
from chromadb.config import Settings
import os
import openai
import sys
import random
import threading
from chromadb.errors import NotFoundError
from pathlib import Path

# 添加项目根目录到 Python 路径
sys.path.insert(0, str(Path(__file__).parent.parent.parent))
from common.logger import setup_logger

# 使用统一的日志配置
logger = setup_logger('cet4_qa_service')


class PromptBlock:
    """对话上下文内存块"""
    
    def __init__(self):
        self.metadata = {
            "user_id": None,
            "total_rounds": 0
        }
        self.background = {}
        self.summary = {
            "summary_context": "",
            "timestamp": ""
        }
        self.conversation = []
    
    def to_dict(self) -> Dict[str, Any]:
        """转换为字典"""
        return {
            "metadata": self.metadata,
            "background": self.background,
            "summary": self.summary,
            "conversation": self.conversation
        }
    
    def from_dict(self, data: Dict[str, Any]):
        """从字典加载"""
        self.metadata = data.get("metadata", self.metadata)
        self.background = data.get("background", {})
        self.summary = data.get("summary", self.summary)
        self.conversation = data.get("conversation", [])


class CET4QuestionAnswerService:
    """CET4问答服务类"""
    
    # Coze API 配置（仅用于摘要）
    COZE_API_BASE = "https://api.coze.cn/v1/workflow/run"
    COZE_API_KEY = os.environ.get('COZE_API_KEY', 'pat_FsFpG2tf6nYuicX2OfAcYQ4cqy9gPtOH3RJeyohv1tt1xgKED53r9BsvsjFFZZJG')
    
    # 工作流ID（仅摘要使用Coze）
    WORKFLOW_ID_SUMMARY = "7578730107335344147"  # 摘要压缩的工作流
    
    # DeepSeek API 配置（用于问答）
    DEEPSEEK_API_KEY = os.environ.get('DEEPSEEK_API_KEY', 'sk-508f4b7ff14d414fb806d0a2cb0b7b39')
    DEEPSEEK_BASE_URL = "https://api.deepseek.com"
    DEEPSEEK_MODEL = "deepseek-chat"  # 使用 deepseek-chat 进行流式对话
    
    # ChromaDB配置
    CHROMA_PATH = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'chroma')
    COLLECTION_NAME = "tutoring_content_en_cet4"
    EMBEDDING_DIMENSION = 384  # 与 Java 端保持一致，避免维度不匹配
    
    def __init__(self):
        """初始化服务"""
        logger.info("=" * 60)
        logger.info("开始初始化 CET4QuestionAnswerService")
        logger.info("=" * 60)
        
        # 初始化ChromaDB客户端
        try:
            logger.info(f"正在初始化 ChromaDB，路径: {self.CHROMA_PATH}")
            self.chroma_client = chromadb.PersistentClient(
                path=self.CHROMA_PATH,
                settings=Settings(
                    anonymized_telemetry=False,
                    allow_reset=True
                )
            )
            # 获取或创建集合；若遇到 HNSW/索引损坏错误，尝试删除后重建
            self.collection = self._get_or_recreate_collection()
            logger.info(f"✅ ChromaDB initialized: {self.CHROMA_PATH}")
        except Exception as e:
            logger.error(f"❌ Failed to initialize ChromaDB: {e}")
            logger.exception("ChromaDB 初始化失败详情:")
            raise
        
        # 初始化DeepSeek客户端
        try:
            logger.info(f"正在初始化 DeepSeek 客户端")
            logger.info(f"  Base URL: {self.DEEPSEEK_BASE_URL}")
            logger.info(f"  Model: {self.DEEPSEEK_MODEL}")
            self.deepseek_client = openai.OpenAI(
                api_key=self.DEEPSEEK_API_KEY,
                base_url=self.DEEPSEEK_BASE_URL
            )
            logger.info(f"✅ DeepSeek client initialized")
        except Exception as e:
            logger.error(f"❌ Failed to initialize DeepSeek client: {e}")
            logger.exception("DeepSeek 客户端初始化失败详情:")
            raise
        
        # 内存中的PromptBlock缓存 {user_id_segment_id: PromptBlock}
        self.prompt_blocks: Dict[str, PromptBlock] = {}
        
        logger.info("=" * 60)
        logger.info("✅ CET4QuestionAnswerService 初始化完成")
        logger.info("=" * 60)
    
    def _get_cache_key(self, user_id: int, segment_id: str) -> str:
        """
        生成缓存键
        注意：一个用户只保留一个对话历史摘要，所以只使用 user_id
        segment_id 参数保留用于兼容性，但不用于生成键
        """
        return str(user_id)
    
    def initialize_prompt_block(
        self,
        user_id: int,
        segment_id: str,
        question_type: str,
        document: str,
        user_answers: List[Dict[str, Any]]
    ) -> PromptBlock:
        """
        初始化PromptBlock
        
        Args:
            user_id: 用户ID
            segment_id: 题目片段ID
            question_type: 题型
            document: ChromaDB中的document JSON字符串
            user_answers: 用户答案列表
        
        Returns:
            初始化的PromptBlock
        """
        cache_key = self._get_cache_key(user_id, segment_id)
        
        # 检查缓存
        if cache_key in self.prompt_blocks:
            logger.info(f"Found cached PromptBlock for user {user_id}")
            logger.info(f"Current segment: {segment_id}, 继续累积对话")
            return self.prompt_blocks[cache_key]
        
        logger.info(f"Initializing new PromptBlock for user {user_id}")
        logger.info(f"Current segment: {segment_id}")
        logger.info(f"说明: 用户的所有题目对话将累积在同一个历史记录中")
        
        prompt_block = PromptBlock()
        
        # 1. 初始化metadata
        prompt_block.metadata = {
            "user_id": user_id,
            "total_rounds": 0
        }
        
        # 2. 初始化background
        prompt_block.background = {
            "question_type": question_type,
            "subject": "cet4",
            "segment_id": segment_id,
            "Document": document,
            "user_answers": user_answers
        }
        
        # 3. 从ChromaDB加载历史摘要和对话
        try:
            # 查询历史记录
            results = self.collection.get(
                ids=[cache_key],
                include=["documents", "metadatas"]
            )
            
            if results and results['ids']:
                # 找到历史记录
                doc = json.loads(results['documents'][0])
                
                # 加载摘要
                if 'summary' in doc:
                    prompt_block.summary = doc['summary']
                else:
                    prompt_block.summary = {
                        "summary_context": "",
                        "timestamp": datetime.now().isoformat()
                    }
                
                # 加载最近5轮对话
                if 'conversation' in doc and doc['conversation']:
                    conversations = doc['conversation']
                    # 取最后5轮
                    last_5_conversations = conversations[-5:] if len(conversations) > 5 else conversations
                    # 重新编号为1-5
                    prompt_block.conversation = [
                        {
                            **conv,
                            "round_id": idx + 1
                        }
                        for idx, conv in enumerate(last_5_conversations)
                    ]
                    # 更新total_rounds
                    prompt_block.metadata["total_rounds"] = len(prompt_block.conversation)
                    logger.info(f"Loaded {len(prompt_block.conversation)} conversation rounds from ChromaDB")
            else:
                # 没有历史记录，使用空摘要
                prompt_block.summary = {
                    "summary_context": "",
                    "timestamp": datetime.now().isoformat()
                }
                logger.info("No history found in ChromaDB, starting fresh")
        
        except Exception as e:
            logger.error(f"Error loading from ChromaDB: {e}")
            # 出错时使用空摘要
            prompt_block.summary = {
                "summary_context": "",
                "timestamp": datetime.now().isoformat()
            }
        
        # 缓存
        self.prompt_blocks[cache_key] = prompt_block
        
        return prompt_block
    
    def _build_answer_prompt(
        self,
        background: Dict[str, Any],
        summary: Dict[str, Any],
        conversation: List[Dict[str, Any]],
        question: str
    ) -> str:
        """
        构建DeepSeek问答提示词
        
        Args:
            background: 背景信息
            summary: 摘要信息
            conversation: 对话历史
            question: 学生问题
        
        Returns:
            完整的提示词
        """
        # 格式化背景信息
        background_text = f"""【当前错题信息】
题型：{background.get('question_type', 'N/A')}
科目：{background.get('subject', 'N/A')}
题目ID：{background.get('segment_id', 'N/A')}
题目内容：{background.get('Document', 'N/A')}
学生答案：{json.dumps(background.get('user_answers', []), ensure_ascii=False)}
"""
        
        # 格式化摘要信息
        summary_text = ""
        if summary and summary.get('summary_context'):
            summary_text = f"""【学生个人知识档案】
{summary.get('summary_context', '暂无历史学习档案')}
"""
        
        # 格式化对话历史
        conversation_text = ""
        if conversation:
            conversation_text = "【近期对话上下文】\n"
            for conv in conversation:
                conversation_text += f"第{conv.get('round_id', '?')}轮:\n"
                conversation_text += f"学生: {conv.get('content_of_user', '')}\n"
                conversation_text += f"老师: {conv.get('content_of_LLM', '')}\n\n"
        
        # 组装完整提示词
        full_prompt = f"""{background_text}

{conversation_text}

{summary_text}

【学生当前的提问】
{question}"""
        
        return full_prompt
    
    def call_deepseek_answer_stream(
        self,
        background: Dict[str, Any],
        summary: Dict[str, Any],
        conversation: List[Dict[str, Any]],
        question: str
    ) -> Generator[str, None, None]:
        """
        调用DeepSeek API进行流式回答
        
        Args:
            background: 背景信息
            summary: 摘要信息
            conversation: 对话历史
            question: 学生问题
        
        Yields:
            AI回答的文本片段
        """
        try:
            logger.info(f"=== DeepSeek Answer Stream 调用参数 ===")
            logger.info(f"Model: {self.DEEPSEEK_MODEL}")
            logger.info(f"API Base URL: {self.DEEPSEEK_BASE_URL}")
            logger.info(f"Question: {question[:100]}...")
            
            # 打印PromptBlock详细内容
            logger.info(f"\n=== PromptBlock 内容 ===")
            logger.info(f"Background: {json.dumps(background, ensure_ascii=False, indent=2)}")
            logger.info(f"Summary: {json.dumps(summary, ensure_ascii=False, indent=2)}")
            logger.info(f"Conversation (共{len(conversation)}轮): {json.dumps(conversation, ensure_ascii=False, indent=2)}")
            
            # 构建提示词
            user_prompt = self._build_answer_prompt(background, summary, conversation, question)
            
            logger.info(f"\n=== 构建的完整提示词 ===")
            logger.info(user_prompt[:500] + "..." if len(user_prompt) > 500 else user_prompt)
            
            # 系统提示词
            system_prompt = """你是一位经验丰富、耐心细致的四级/六级英语辅导老师，专门帮助学生分析错题。请严格按照下面的信息来回答问题：

🎯 原则
1. 精准定位，紧扣材料：所有分析必须基于提供的三个信息源，尤其是当前错题。不引入外部超纲知识或假设。
2. 联系历史，因人施教：在分析错误时，要主动关联【学生个人知识档案】中记录的该学生的高频失分点或已掌握优势。例如："你之前在处理长对话转折时把握得很好，但这次在新闻听力的主旨题上又出现了类似问题…"。
3. 引导发现，授人以渔：不直接给答案。通过拆解问题、回顾知识档案中的技巧、或对比过往错题，引导学生找到解题思路。多用"你觉得…？"、"我们之前用什么方法处理过类似问题？"。
4. 结构清晰，分步拆解：按照"确认与理解 → 关联档案，诊断根源 → 引导式破题 → 总结与强化记忆 → 鼓励与衔接"的逻辑组织回答内容。
5. 积极鼓励，正向反馈：对学生的进步（如在档案中已改善的弱点）要明确指出并表扬，建立信心。

📝 回答要求（重要）
回答时需要包含以下内容，但【严禁输出任何标题、编号、分隔符】，直接以自然连贯的语言输出：
- 首先复述错题关键信息，确保双方理解一致
- 然后结合学生档案分析错误原因
- 接着通过提问引导学生找到解题方法
- 随后提炼核心教训和方法
- 最后给予正向反馈和鼓励

❌ 禁止输出：
- 任何加粗标题（如 **确认与理解**、**诊断根源** 等）
- 任何编号（如 1.、2.、3. 或 一、二、三）
- 任何分隔符（如 ---、===、###）

✅ 正确示例：
"我看到你在这道长对话题中选择了B选项。让我们一起来看看这道题..."

请用自然流畅的对话方式输出，让回答像一位老师在面对面交流，而不是在填写结构化的表格。"""
            
            logger.info(f"\n=== 开始调用DeepSeek Stream API ===")
            
            # 调用DeepSeek流式API（使用 openai 库）
            response = self.deepseek_client.chat.completions.create(
                model=self.DEEPSEEK_MODEL,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_prompt}
                ],
                stream=True,
                temperature=0.7,
                max_tokens=2000
            )
            
            logger.info("DeepSeek Stream API 调用成功，开始接收流式响应")
            
            # 流式返回内容
            chunk_count = 0
            total_content_length = 0
            for chunk in response:
                if chunk.choices[0].delta.content:
                    content = chunk.choices[0].delta.content
                    chunk_count += 1
                    total_content_length += len(content)
                    
                    # 打印第一个和每100个chunk的信息
                    if chunk_count == 1 or chunk_count % 100 == 0:
                        logger.info(f"[DeepSeek] 收到第 {chunk_count} 个chunk，内容长度: {len(content)}, 累计: {total_content_length}")
                    
                    yield content
            
            logger.info(f"DeepSeek Stream API 响应完成，共 {chunk_count} 个chunks，总长度 {total_content_length} 字符")
        
        except Exception as e:
            logger.error(f"Failed to call DeepSeek stream API: {e}")
            logger.exception("详细错误堆栈:")
            raise
    
    def call_coze_summary_workflow(
        self,
        summary_before: Dict[str, Any],
        conversations: List[Dict[str, Any]]
    ) -> str:
        """
        调用Coze摘要工作流
        
        Args:
            summary_before: 之前的摘要
            conversations: 需要压缩的对话
        
        Returns:
            新的摘要内容
        """
        try:
            logger.info(f"\n=== Coze Summary Workflow 调用参数 ===")
            logger.info(f"Workflow ID: {self.WORKFLOW_ID_SUMMARY}")
            logger.info(f"API Base URL: {self.COZE_API_BASE}")
            logger.info(f"API Token: {self.COZE_API_KEY[:20]}...{self.COZE_API_KEY[-10:]}")
            
            # 打印摘要前的内容
            logger.info(f"\n=== Summary 输入内容 ===")
            logger.info(f"Summary Before: {json.dumps(summary_before, ensure_ascii=False, indent=2)}")
            logger.info(f"Conversations to compress (共{len(conversations)}轮): {json.dumps(conversations, ensure_ascii=False, indent=2)}")
            
            headers = {
                "Authorization": f"Bearer {self.COZE_API_KEY}",
                "Content-Type": "application/json"
            }
            
            payload = {
                "workflow_id": self.WORKFLOW_ID_SUMMARY,
                "parameters": {
                    "summary_before": json.dumps(summary_before, ensure_ascii=False),
                    "conversations": json.dumps(conversations, ensure_ascii=False)
                }
            }
            
            logger.info(f"\n=== 开始调用Coze Summary API ===")
            
            response = requests.post(
                self.COZE_API_BASE,
                headers=headers,
                json=payload,
                timeout=60
            )
            
            response.raise_for_status()
            result = response.json()
            
            logger.info(f"Summary Response Status: {response.status_code}")
            logger.info(f"Summary Result code: {result.get('code')}")
            
            # 提取输出
            if result.get('code') == 0 and result.get('data'):
                data = result['data']
                
                # data可能是字符串(需要解析)或已经是dict
                if isinstance(data, str):
                    logger.info("Summary data is string, parsing JSON...")
                    data = json.loads(data)
                
                output = data.get('output', '')
                logger.info(f"\n=== Coze Summary Workflow 响应成功 ===")
                logger.info(f"Summary Output length: {len(output)} 字符")
                logger.info(f"Summary Output content: {output}")  # 打印完整摘要内容
                logger.info(f"Summary Usage: {result.get('usage', {})}")
                logger.info(f"Summary Execute ID: {result.get('execute_id', 'N/A')}")
                return output
            else:
                error_msg = result.get('msg', 'Unknown error')
                logger.error(f"Coze API error: {error_msg}")
                raise Exception(f"Coze API error: {error_msg}")
        
        except Exception as e:
            logger.error(f"Failed to call Coze summary workflow: {e}")
            raise
    
    def process_question(
        self,
        user_id: int,
        segment_id: str,
        question_type: str,
        document: str,
        user_answers: List[Dict[str, Any]],
        question: str
    ) -> Dict[str, Any]:
        """
        处理学生提问（非流式，完整返回）
        
        Args:
            user_id: 用户ID
            segment_id: 题目片段ID
            question_type: 题型
            document: 题目document
            user_answers: 用户答案
            question: 学生问题
        
        Returns:
            包含AI回答的响应
        """
        try:
            logger.info(f"\n{'='*60}")
            logger.info(f"=== process_question 开始 ===")
            logger.info(f"{'='*60}")
            logger.info(f"User ID: {user_id}")
            logger.info(f"Segment ID: {segment_id}")
            logger.info(f"Question Type: {question_type}")
            logger.info(f"Question: {question}")
            
            # 1. 初始化或获取PromptBlock
            logger.info(f"\n{'='*60}")
            logger.info("步骤1: 初始化PromptBlock")
            logger.info(f"{'='*60}")
            prompt_block = self.initialize_prompt_block(
                user_id, segment_id, question_type, document, user_answers
            )
            logger.info(f"PromptBlock初始化完成")
            
            # 2. 调用DeepSeek获取完整回答
            logger.info(f"\n{'='*60}")
            logger.info("步骤2: 调用DeepSeek API获取回答")
            logger.info(f"{'='*60}")
            
            # 收集完整答案
            full_answer = ""
            for chunk in self.call_deepseek_answer_stream(
                background=prompt_block.background,
                summary=prompt_block.summary,
                conversation=prompt_block.conversation,
                question=question
            ):
                full_answer += chunk
            
            logger.info(f"DeepSeek 完整回答长度: {len(full_answer)} 字符")
            
            # 3. 更新对话历史
            prompt_block.metadata["total_rounds"] += 1
            round_id = prompt_block.metadata["total_rounds"]
            
            logger.info(f"\n{'='*60}")
            logger.info("步骤3: 更新对话历史")
            logger.info(f"{'='*60}")
            
            new_round = {
                "round_id": round_id,
                "content_of_user": question,
                "content_of_LLM": full_answer,
                "timestamp": datetime.now().isoformat()
            }
            
            prompt_block.conversation.append(new_round)
            logger.info(f"新增第 {round_id} 轮对话")
            
            # 4. 检查是否需要压缩（超过10轮）
            logger.info(f"\n{'='*60}")
            logger.info(f"步骤4: 检查是否需要压缩 (当前轮次: {prompt_block.metadata['total_rounds']})")
            logger.info(f"{'='*60}")
            
            if prompt_block.metadata["total_rounds"] >= 10:
                logger.info(f"⚠️ 对话达到/超过10轮，开始压缩前5轮...")
                self._run_async(self._compress_conversation, prompt_block)
                logger.info(f"✅ 已异步触发压缩，当前对话轮数（压缩后将更新）: {len(prompt_block.conversation)}")
            else:
                logger.info(f"对话未超过10轮，无需压缩")
            
            # 5. 返回结果
            logger.info(f"\n{'='*60}")
            logger.info(f"=== process_question 完成 ===")
            logger.info(f"{'='*60}")
            logger.info(f"✅ 成功返回答案，轮次: {round_id}/{prompt_block.metadata['total_rounds']}")
            
            return {
                "success": True,
                "answer": full_answer,
                "round_id": round_id,
                "total_rounds": prompt_block.metadata["total_rounds"]
            }
        
        except Exception as e:
            logger.error(f"Error processing question: {e}")
            logger.exception("详细错误堆栈:")
            return {
                "success": False,
                "error": str(e)
            }

    def process_question_stream(
        self,
        user_id: int,
        segment_id: str,
        question_type: str,
        document: str,
        user_answers: List[Dict[str, Any]],
        question: str
    ) -> Generator[Dict[str, Any], None, None]:
        """
        处理学生提问（流式）
        
        Args:
            user_id: 用户ID
            segment_id: 题目片段ID
            question_type: 题型
            document: 题目document
            user_answers: 用户答案
            question: 学生问题
        
        Yields:
            包含流式响应的字典
        """
        try:
            logger.info(f"\n{'='*60}")
            logger.info(f"=== process_question_stream 开始 ===")
            logger.info(f"{'='*60}")
            logger.info(f"User ID: {user_id}")
            logger.info(f"Segment ID: {segment_id}")
            logger.info(f"Question Type: {question_type}")
            logger.info(f"Question: {question}")
            
            # 1. 初始化或获取PromptBlock
            logger.info(f"\n{'='*60}")
            logger.info("步骤1: 初始化PromptBlock")
            logger.info(f"{'='*60}")
            prompt_block = self.initialize_prompt_block(
                user_id, segment_id, question_type, document, user_answers
            )
            logger.info(f"PromptBlock初始化完成")
            logger.info(f"\n=== 完整 PromptBlock 内容 ===")
            logger.info(f"Metadata: {json.dumps(prompt_block.metadata, ensure_ascii=False, indent=2)}")
            logger.info(f"Background: {json.dumps(prompt_block.background, ensure_ascii=False, indent=2)}")
            logger.info(f"Summary: {json.dumps(prompt_block.summary, ensure_ascii=False, indent=2)}")
            logger.info(f"Conversation History (共{len(prompt_block.conversation)}轮):")
            for idx, conv in enumerate(prompt_block.conversation, 1):
                logger.info(f"  轮次{idx}: {json.dumps(conv, ensure_ascii=False, indent=4)}")
            
            # 2. 调用DeepSeek获取流式回答
            logger.info(f"\n{'='*60}")
            logger.info("步骤2: 调用DeepSeek API获取流式回答")
            logger.info(f"{'='*60}")
            
            # 发送开始事件
            yield {"type": "start", "message": "正在思考..."}
            
            # 收集完整答案用于保存
            full_answer = ""
            
            # 流式返回答案
            chunk_count = 0
            for chunk in self.call_deepseek_answer_stream(
                background=prompt_block.background,
                summary=prompt_block.summary,
                conversation=prompt_block.conversation,
                question=question
            ):
                chunk_count += 1
                full_answer += chunk
                if chunk_count % 10 == 1:  # 每10个chunk打印一次日志
                    logger.info(f"收到第 {chunk_count} 个chunk，当前总长度: {len(full_answer)}")
                yield {"type": "chunk", "content": chunk}
            
            logger.info(f"共收到 {chunk_count} 个chunks")
            
            logger.info(f"DeepSeek 完整回答长度: {len(full_answer)} 字符")
            
            # 3. 更新对话历史
            prompt_block.metadata["total_rounds"] += 1
            round_id = prompt_block.metadata["total_rounds"]
            
            logger.info(f"\n{'='*60}")
            logger.info("步骤3: 更新对话历史")
            logger.info(f"{'='*60}")
            
            new_round = {
                "round_id": round_id,
                "content_of_user": question,
                "content_of_LLM": full_answer,
                "timestamp": datetime.now().isoformat()
            }
            
            prompt_block.conversation.append(new_round)
            logger.info(f"新增第 {round_id} 轮对话")
            logger.info(f"新对话内容: {json.dumps(new_round, ensure_ascii=False, indent=2)}")
            
            # 4. 检查是否需要压缩（超过10轮）
            logger.info(f"\n{'='*60}")
            logger.info(f"步骤4: 检查是否需要压缩 (当前轮次: {prompt_block.metadata['total_rounds']})")
            logger.info(f"{'='*60}")
            
            if prompt_block.metadata["total_rounds"] >= 10:
                logger.info(f"⚠️ 对话达到/超过10轮，开始压缩前5轮...")
                self._run_async(self._compress_conversation, prompt_block)
                logger.info(f"✅ 已异步触发压缩，当前对话轮数（压缩后将更新）: {len(prompt_block.conversation)}")
            else:
                logger.info(f"对话未超过10轮，无需压缩")
            
            # 5. 发送完成事件
            logger.info(f"\n{'='*60}")
            logger.info(f"=== process_question_stream 完成 ===")
            logger.info(f"{'='*60}")
            logger.info(f"✅ 成功返回答案，轮次: {round_id}/{prompt_block.metadata['total_rounds']}")
            
            yield {
                "type": "done",
                "round_id": round_id,
                "total_rounds": prompt_block.metadata["total_rounds"]
            }
        
        except Exception as e:
            logger.error(f"Error processing question stream: {e}")
            logger.exception("详细错误堆栈:")
            yield {"type": "error", "message": str(e)}
    
    def _compress_conversation(self, prompt_block: PromptBlock):
        """压缩对话历史（当超过10轮时）"""
        try:
            logger.info(f"\n{'='*60}")
            logger.info("=== 开始压缩对话 ===")
            logger.info(f"{'='*60}")
            
            # 打印调用摘要前的完整 prompt_block 内容
            logger.info(f"\n=== 调用摘要前的 PromptBlock 完整内容 ===")
            logger.info(f"Metadata: {json.dumps(prompt_block.metadata, ensure_ascii=False, indent=2)}")
            logger.info(f"Background: {json.dumps(prompt_block.background, ensure_ascii=False, indent=2)}")
            logger.info(f"当前Summary: {json.dumps(prompt_block.summary, ensure_ascii=False, indent=2)}")
            logger.info(f"当前Conversation (共{len(prompt_block.conversation)}轮): {json.dumps(prompt_block.conversation, ensure_ascii=False, indent=2)}")
            
            # 取出前5轮对话
            conversations_to_compress = prompt_block.conversation[:5]
            logger.info(f"\n待压缩的对话 (前5轮): {json.dumps(conversations_to_compress, ensure_ascii=False, indent=2)}")
            
            # 调用摘要工作流
            logger.info(f"\n🔄 开始调用摘要工作流...")
            new_summary_context = self.call_coze_summary_workflow(
                summary_before=prompt_block.summary,
                conversations=conversations_to_compress
            )
            
            # 更新摘要
            old_summary = prompt_block.summary.copy()
            prompt_block.summary = {
                "summary_context": new_summary_context,
                "timestamp": datetime.now().isoformat()
            }
            
            logger.info(f"\n=== ✅ 摘要压缩完成 ===")
            logger.info(f"📋 旧摘要: {json.dumps(old_summary, ensure_ascii=False, indent=2)}")
            logger.info(f"✨ 新摘要: {json.dumps(prompt_block.summary, ensure_ascii=False, indent=2)}")
            
            # 删除前5轮，保留后5轮
            remaining_conversations = prompt_block.conversation[5:]
            logger.info(f"\n保留的对话 (后{len(remaining_conversations)}轮)")
            
            # 重新编号为1-5
            prompt_block.conversation = [
                {
                    **conv,
                    "round_id": idx + 1
                }
                for idx, conv in enumerate(remaining_conversations)
            ]
            
            # 重置总轮数
            prompt_block.metadata["total_rounds"] = len(prompt_block.conversation)
            
            logger.info(f"\n✅ 对话压缩完成")
            logger.info(f"压缩后对话轮数: {len(prompt_block.conversation)}")
            logger.info(f"压缩后的对话: {json.dumps(prompt_block.conversation, ensure_ascii=False, indent=2)}")
        
        except Exception as e:
            logger.error(f"Error compressing conversation: {e}")
            raise
    
    def save_to_chromadb(self, user_id: int, segment_id: str):
        """
        保存PromptBlock到ChromaDB
        
        Args:
            user_id: 用户ID
            segment_id: 片段ID
        """
        try:
            cache_key = self._get_cache_key(user_id, segment_id)
            
            logger.info(f"\n{'='*60}")
            logger.info(f"=== 开始保存会话到ChromaDB ===")
            logger.info(f"{'='*60}")
            logger.info(f"User ID: {user_id}")
            logger.info(f"Segment ID: {segment_id} (仅用于记录，不影响存储)")
            logger.info(f"Storage Key (仅用户ID): {cache_key}")
            logger.info(f"说明: 一个用户只保留一个对话历史摘要")
            logger.info(f"当前缓存的所有会话: {list(self.prompt_blocks.keys())}")
            
            if cache_key not in self.prompt_blocks:
                logger.warning(f"⚠️ 缓存中未找到 PromptBlock: {cache_key}")
                logger.warning(f"可能原因：1) 没有进行过对话 2) 会话已经被保存过了")
                return
            
            prompt_block = self.prompt_blocks[cache_key]
            logger.info(f"✅ 找到 PromptBlock，对话轮数: {len(prompt_block.conversation)}")
            
            # 如果有未压缩的对话，先做最终摘要
            if len(prompt_block.conversation) > 0:
                logger.info(f"\n{'='*60}")
                logger.info("=== 保存前执行最终摘要 ===")
                logger.info(f"{'='*60}")
                
                # 打印调用摘要前的完整 prompt_block 内容
                logger.info(f"\n=== 调用最终摘要前的 PromptBlock 完整内容 ===")
                logger.info(f"Metadata: {json.dumps(prompt_block.metadata, ensure_ascii=False, indent=2)}")
                logger.info(f"Background: {json.dumps(prompt_block.background, ensure_ascii=False, indent=2)}")
                logger.info(f"当前Summary: {json.dumps(prompt_block.summary, ensure_ascii=False, indent=2)}")
                logger.info(f"当前Conversation (共{len(prompt_block.conversation)}轮): {json.dumps(prompt_block.conversation, ensure_ascii=False, indent=2)}")
                
                logger.info(f"\n🔄 开始调用最终摘要工作流...")
                new_summary_context = self.call_coze_summary_workflow(
                    summary_before=prompt_block.summary,
                    conversations=prompt_block.conversation
                )
                
                old_summary = prompt_block.summary.copy()
                prompt_block.summary = {
                    "summary_context": new_summary_context,
                    "timestamp": datetime.now().isoformat()
                }
                
                logger.info(f"\n=== ✅ 最终摘要完成 ===")
                logger.info(f"📋 旧摘要: {json.dumps(old_summary, ensure_ascii=False, indent=2)}")
                logger.info(f"✨ 新摘要: {json.dumps(prompt_block.summary, ensure_ascii=False, indent=2)}")
            
            # 准备存储数据
            document_data = {
                "summary": prompt_block.summary,
                "conversation": prompt_block.conversation
            }
            
            metadata = {
                "user_id": str(user_id),
                "timestamp": datetime.now().isoformat(),
                "subject": "cet4",
                "last_segment_id": segment_id  # 记录最后一次对话的片段ID
            }
            
            # 存储到ChromaDB
            # 注意：ChromaDB 需要 embeddings，这里提供一个占位向量
            # 因为我们只是存储对话历史，不需要向量搜索功能
            # ID 只使用 user_id，一个用户只保留一个对话历史摘要
            collection = self._ensure_collection()
            collection.upsert(
                ids=[cache_key],  # cache_key = str(user_id)
                documents=[json.dumps(document_data, ensure_ascii=False)],
                metadatas=[metadata],
                embeddings=[self._generate_deterministic_embedding(cache_key)]
            )
            
            logger.info(f"✅ Saved PromptBlock to ChromaDB")
            logger.info(f"   Storage ID (用户ID): {cache_key}")
            logger.info(f"   Last Segment ID: {segment_id}")
            logger.info(f"   Document size: {len(json.dumps(document_data, ensure_ascii=False))} bytes")
            logger.info(f"   Conversation rounds: {len(prompt_block.conversation)}")
            logger.info(f"   说明: 该用户的所有题目对话都累积在此记录中")
            
            # 清除缓存
            del self.prompt_blocks[cache_key]
        
        except Exception as e:
            logger.error(f"Error saving to ChromaDB: {e}")
            raise
    
    def end_session(self, user_id: int, segment_id: str):
        """
        结束对话会话，保存到ChromaDB
        
        Args:
            user_id: 用户ID
            segment_id: 片段ID
        """
        self.save_to_chromadb_async(user_id, segment_id)

    def save_to_chromadb_async(self, user_id: int, segment_id: str):
        """异步保存，前端无需等待"""
        self._run_async(self.save_to_chromadb, user_id, segment_id)

    def _generate_deterministic_embedding(self, seed: str) -> List[float]:
        """生成与 Java 端一致的固定长度占位 embedding，避免维度不匹配"""
        rng = random.Random(seed or 0)
        return [rng.random() for _ in range(self.EMBEDDING_DIMENSION)]

    def _run_async(self, target, *args, **kwargs):
        """启动后台线程运行长耗时任务（如摘要、保存）"""
        def _wrapper():
            try:
                target(*args, **kwargs)
            except Exception as e:
                logger.error(f"后台任务执行失败: {e}", exc_info=True)
        threading.Thread(target=_wrapper, daemon=True).start()

    def _get_or_recreate_collection(self):
        """尽量复原损坏的集合（HNSW/索引缺失时删除重建）"""
        metadata = {
            "description": "CET4 tutoring conversation history",
            "hnsw:space": "l2"
        }
        try:
            return self.chroma_client.get_or_create_collection(
                name=self.COLLECTION_NAME,
                metadata=metadata
            )
        except Exception as e:
            logger.warning(f"⚠️ 获取集合失败，尝试删除重建: {e}")
            try:
                self.chroma_client.delete_collection(name=self.COLLECTION_NAME)
            except Exception as del_err:
                logger.warning(f"删除损坏集合失败（可忽略）: {del_err}")
            return self.chroma_client.get_or_create_collection(
                name=self.COLLECTION_NAME,
                metadata=metadata
            )

    def _ensure_collection(self):
        """确保 collection 句柄有效；若被外部删除则重建并更新 self.collection"""
        try:
            # 触发一次轻量操作，若句柄失效会抛 NotFound
            self.collection.count()
            return self.collection
        except NotFoundError:
            logger.warning("检测到集合句柄失效，尝试重新获取/重建")
        except Exception as e:
            logger.warning(f"集合句柄检查异常，将尝试重建: {e}")
        self.collection = self._get_or_recreate_collection()
        return self.collection


# 创建服务实例
logger.info("🚀 正在创建 CET4QuestionAnswerService 实例...")
qa_service = CET4QuestionAnswerService()
logger.info("🎉 qa_service 实例创建成功！")

