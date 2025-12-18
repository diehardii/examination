"""
Smart QA service: general tutoring with exam analysis background.
Follows CET4QuestionAnswerService flow but uses analysis doc and custom prompt.
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

logger = setup_logger('smart_qa_service')


class PromptBlock:
    """对话上下文内存块"""
    def __init__(self):
        self.metadata = {
            "user_id": None,
            "total_rounds": 0
        }
        self.background: Dict[str, Any] = {}
        self.summary = {
            "summary_context": "",
            "timestamp": ""
        }
        self.conversation: List[Dict[str, Any]] = []

    def to_dict(self) -> Dict[str, Any]:
        return {
            "metadata": self.metadata,
            "background": self.background,
            "summary": self.summary,
            "conversation": self.conversation
        }

    def from_dict(self, data: Dict[str, Any]):
        self.metadata = data.get("metadata", self.metadata)
        self.background = data.get("background", {})
        self.summary = data.get("summary", self.summary)
        self.conversation = data.get("conversation", [])


class SmartQuestionAnswerService:
    """智能问答服务，使用考试分析+历史摘要作为背景"""

    COZE_API_BASE = "https://api.coze.cn/v1/workflow/run"
    COZE_API_KEY = os.environ.get('COZE_API_KEY', 'pat_FsFpG2tf6nYuicX2OfAcYQ4cqy9gPtOH3RJeyohv1tt1xgKED53r9BsvsjFFZZJG')
    WORKFLOW_ID_SUMMARY = "7578730107335344147"

    DEEPSEEK_API_KEY = os.environ.get('DEEPSEEK_API_KEY', 'sk-508f4b7ff14d414fb806d0a2cb0b7b39')
    DEEPSEEK_BASE_URL = "https://api.deepseek.com"
    DEEPSEEK_MODEL = "deepseek-chat"

    CHROMA_PATH = os.path.join(os.path.dirname(__file__), '..', '..', '..', 'chroma')
    COLLECTION_NAME = "tutoring_content_en_cet4"
    EMBEDDING_DIMENSION = 384

    def __init__(self):
        logger.info("初始化 SmartQuestionAnswerService")
        self.prompt_blocks: Dict[str, PromptBlock] = {}
        try:
            self.chroma_client = chromadb.PersistentClient(
                path=self.CHROMA_PATH,
                settings=Settings(
                    anonymized_telemetry=False,
                    allow_reset=True
                )
            )
            self.collection = self._get_or_recreate_collection()
        except Exception as e:
            logger.error(f"Chroma 初始化失败: {e}")
            raise

        try:
            self.deepseek_client = openai.OpenAI(
                api_key=self.DEEPSEEK_API_KEY,
                base_url=self.DEEPSEEK_BASE_URL
            )
        except Exception as e:
            logger.error(f"DeepSeek 客户端初始化失败: {e}")
            raise

    def _get_cache_key(self, user_id: int, segment_id: Optional[str] = None) -> str:
        return f"{user_id}qa"

    def _get_document_by_id(self, doc_id: str) -> str:
        try:
            result = self.collection.get(ids=[doc_id], include=["documents"])
            if result and result.get('ids'):
                return result['documents'][0]
        except Exception as e:
            logger.warning(f"读取Chroma文档失败 id={doc_id}: {e}")
        return ""

    def initialize_prompt_block(self, user_id: int, segment_id: str = "analysis") -> PromptBlock:
        cache_key = self._get_cache_key(user_id, segment_id)
        if cache_key in self.prompt_blocks:
            return self.prompt_blocks[cache_key]

        prompt_block = PromptBlock()
        prompt_block.metadata = {"user_id": user_id, "total_rounds": 0}

        analysis_doc = self._get_document_by_id(f"{user_id}analysis")
        qa_history_doc = self._get_document_by_id(cache_key)

        prompt_block.background = {
            "subject": "smart_qa",
            "segment_id": segment_id,
            "analysis_document": analysis_doc,
            "qa_history_document": qa_history_doc
        }

        # 尝试从历史记录恢复摘要与对话
        loaded = False
        if qa_history_doc:
            try:
                parsed = json.loads(qa_history_doc)
                if isinstance(parsed, dict):
                    if 'summary' in parsed:
                        prompt_block.summary = parsed['summary']
                    if 'conversation' in parsed and parsed['conversation']:
                        conversations = parsed['conversation']
                        last_5 = conversations[-5:] if len(conversations) > 5 else conversations
                        prompt_block.conversation = [
                            {**conv, "round_id": idx + 1}
                            for idx, conv in enumerate(last_5)
                        ]
                        prompt_block.metadata["total_rounds"] = len(prompt_block.conversation)
                    loaded = True
            except Exception:
                # 非JSON内容，直接作为摘要上下文
                prompt_block.summary = {
                    "summary_context": qa_history_doc,
                    "timestamp": datetime.now().isoformat()
                }
                loaded = True

        if not loaded:
            prompt_block.summary = {
                "summary_context": "",
                "timestamp": datetime.now().isoformat()
            }

        self.prompt_blocks[cache_key] = prompt_block
        return prompt_block

    def _build_answer_prompt(
        self,
        background: Dict[str, Any],
        summary: Dict[str, Any],
        conversation: List[Dict[str, Any]],
        question: str
    ) -> str:
        analysis_text = background.get('analysis_document') or '暂无最近考试分析。'
        qa_history_text = background.get('qa_history_document') or ''

        summary_text = summary.get('summary_context', '') if summary else ''
        summary_section = qa_history_text or summary_text or '暂无历史摘要。'

        conversation_text = ""
        if conversation:
            conversation_text = "【近期对话】\n"
            for conv in conversation:
                conversation_text += f"第{conv.get('round_id', '?')}轮:\n"
                conversation_text += f"学生: {conv.get('content_of_user', '')}\n"
                conversation_text += f"老师: {conv.get('content_of_LLM', '')}\n\n"

        return f"""【考试分析】
{analysis_text}

【历史摘要】
{summary_section}

{conversation_text}
【学生当前的提问】
{question}"""

    def call_deepseek_answer_stream(
        self,
        background: Dict[str, Any],
        summary: Dict[str, Any],
        conversation: List[Dict[str, Any]],
        question: str
    ) -> Generator[str, None, None]:
        system_prompt = """### **提示词：**

**角色与任务**
你是一位资深英语教育专家，。你的专长是覆盖从**小学到大学英语六级（CET-6）**的全阶段英语学习辅导。你的核心任务是为学生提供关于**语法、词汇、阅读、听力、写作、口语**等所有技能的个性化答疑、策略指导和资源推荐。你的风格应**亲切、耐心、鼓励性强**，同时提供**具体、可操作**的建议。

**核心原则**
1.  **精准定位**：在回答前，务必先**判断或询问**学生的**当前学段/水平**（如：小学三年级、高中备考、大学四级等）和**具体问题上下文**。
2.  **结构化解答**：回答问题采用清晰结构，例如：“问题分析 -> 核心要点 -> 解决步骤/练习方法 -> 推荐资源（可选）”。
3.  **授人以渔**：不仅要解答具体题目，更要传授**学习方法、考试技巧和思维过程**。例如，讲解如何分析长难句、如何规划写作提纲、如何精听。
4.  **积极鼓励**：始终以建设性态度回应，肯定学生的提问，激发学习动力。

**能力范围与应答框架**

**1. 语法问题**
*   **方式**：用简洁易懂的语言解释规则，对比易错点，提供经典例句。
*   **举例**：当被问到“现在完成时和一般过去时的区别”时，除了讲概念，要给出典型时间状语对比（for/since vs. yesterday）和场景例句。

**2. 阅读问题**
*   **方式**：区分题型（主旨、细节、推理、词义猜测）。教授略读、扫读、精读技巧。讲解如何分析文章结构。
*   **举例**：对于“总是做错推理题”，可以指导“寻找文中隐含线索，避免过度主观臆断”的方法。

**3. 听力问题**
*   **方式**：区分精听与泛听。提供听前预测、抓关键词、记笔记的方法。针对不同口音和语速给出建议。
*   **举例**：对于“听力太快跟不上”，建议从“听写VOA慢速英语”开始，逐步过渡到常速。

**4. 写作问题**
*   **方式**：从句子润色到篇章结构（如议论文三段式、图表作文描述）。提供常用句型、逻辑连接词和思路拓展方法。
*   **举例**：批改句子时，先指出问题（如主谓不一致），给出修改版，再解释原因。

**5. 词汇与口语**
*   **方式**：推荐记忆法（词根词缀、语境记忆）。提供情景对话练习、发音技巧和流利度提升策略。

**6. 考试策略**
*   **方式**：针对**小升初、中考、高考、大学四六级、考研英语**等，提供备考规划、时间分配、题型专项突破和真题利用方法。

**互动流程**
1.  **欢迎与询问**：首先热情问候，主动询问学生的学段、具体问题和目标（例如：“你好！很高兴为你提供英语学习帮助。可以告诉我你现在是几年级，或者正在准备什么考试吗？具体遇到了什么问题呢？”）。
2.  **分析与解答**：根据学生信息，提供量身定制的建议。
3.  **检查与深化**：在解答后，可以问“我这样解释清楚吗？”或“你需要我就某个点再展开一下吗？”，以确保沟通有效。
4.  **鼓励与总结**：结束时，简要总结要点，并给予鼓励。

**重要提示**
*   如果问题信息不足，请**主动、有策略地提问**以获取关键信息（如水平、薄弱点、考试类型）。
*   避免给出模糊、笼统的建议（如“多听多读”）。要具体（如“建议每天精听一篇2-3分钟的BBC新闻，并按以下步骤进行：第一遍盲听抓主旨，第二遍逐句听写，第三遍对照原文查漏”）。
*   可以虚拟推荐一些广受认可的经典资源（如《新概念英语》、剑桥雅思真题、TED演讲、可可英语等APP），但说明“仅供参考，请根据自身情况选择”。

**你的开场白**
“你好！我是你的英语学习伙伴LinguaGuide，陪伴你从小学到六级的每一步。无论你是被一个语法点困扰，还是想系统提升阅读速度，或者需要备考规划，我都在这里为你提供详细指导。请告诉我你的情况吧！（例如：我是一名高二学生，完形填空总是错很多，该怎么办？）”
"""
        try:
            user_prompt = self._build_answer_prompt(background, summary, conversation, question)
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

            for chunk in response:
                if chunk.choices[0].delta.content:
                    yield chunk.choices[0].delta.content
        except Exception as e:
            logger.error(f"调用DeepSeek失败: {e}")
            raise

    def call_coze_summary_workflow(self, summary_before: Dict[str, Any], conversations: List[Dict[str, Any]]) -> str:
        try:
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
            response = requests.post(self.COZE_API_BASE, headers=headers, json=payload, timeout=60)
            response.raise_for_status()
            result = response.json()
            if result.get('code') == 0 and result.get('data'):
                data = result['data']
                if isinstance(data, str):
                    data = json.loads(data)
                return data.get('output', '')
            raise Exception(result.get('msg', 'Coze summary error'))
        except Exception as e:
            logger.error(f"调用摘要工作流失败: {e}")
            raise

    def process_question_stream(self, user_id: int, question: str, segment_id: str = "analysis") -> Generator[Dict[str, Any], None, None]:
        try:
            prompt_block = self.initialize_prompt_block(user_id, segment_id)
            yield {"type": "start", "message": "正在思考..."}

            full_answer = ""
            for chunk in self.call_deepseek_answer_stream(
                background=prompt_block.background,
                summary=prompt_block.summary,
                conversation=prompt_block.conversation,
                question=question
            ):
                full_answer += chunk
                yield {"type": "chunk", "content": chunk}

            prompt_block.metadata["total_rounds"] += 1
            round_id = prompt_block.metadata["total_rounds"]
            prompt_block.conversation.append({
                "round_id": round_id,
                "content_of_user": question,
                "content_of_LLM": full_answer,
                "timestamp": datetime.now().isoformat()
            })

            if prompt_block.metadata["total_rounds"] >= 10:
                self._run_async(self._compress_conversation, prompt_block)

            yield {"type": "done", "round_id": round_id, "total_rounds": prompt_block.metadata["total_rounds"]}
        except Exception as e:
            logger.error(f"处理智能问答失败: {e}")
            yield {"type": "error", "message": str(e)}

    def get_background_documents(self, user_id: int, segment_id: str = "analysis") -> Dict[str, str]:
        analysis_doc = self._get_document_by_id(f"{user_id}analysis")
        qa_doc = self._get_document_by_id(self._get_cache_key(user_id, segment_id))
        return {
            "analysis_document": analysis_doc,
            "qa_document": qa_doc
        }

    def _compress_conversation(self, prompt_block: PromptBlock):
        try:
            conversations_to_compress = prompt_block.conversation[:5]
            logger.info(
                "[SmartQA] Start Coze summary (compress) user=%s rounds=%s",
                prompt_block.metadata.get("user_id"),
                len(conversations_to_compress)
            )
            new_summary_context = self.call_coze_summary_workflow(
                summary_before=prompt_block.summary,
                conversations=conversations_to_compress
            )
            logger.info(
                "[SmartQA] Coze summary done (compress) user=%s summary_chars=%s",
                prompt_block.metadata.get("user_id"),
                len(new_summary_context)
            )
            prompt_block.summary = {
                "summary_context": new_summary_context,
                "timestamp": datetime.now().isoformat()
            }
            remaining = prompt_block.conversation[5:]
            prompt_block.conversation = [
                {**conv, "round_id": idx + 1}
                for idx, conv in enumerate(remaining)
            ]
            prompt_block.metadata["total_rounds"] = len(prompt_block.conversation)
        except Exception as e:
            logger.error(f"压缩对话失败: {e}")
            raise

    def save_to_chromadb(self, user_id: int, segment_id: str = "analysis"):
        cache_key = self._get_cache_key(user_id, segment_id)
        if cache_key not in self.prompt_blocks:
            return
        prompt_block = self.prompt_blocks[cache_key]

        if len(prompt_block.conversation) > 0:
            try:
                logger.info(
                    "[SmartQA] Start Coze summary before save user=%s segment=%s rounds=%s",
                    user_id,
                    segment_id,
                    len(prompt_block.conversation)
                )
                new_summary_context = self.call_coze_summary_workflow(
                    summary_before=prompt_block.summary,
                    conversations=prompt_block.conversation
                )
                logger.info(
                    "[SmartQA] Coze summary done before save user=%s segment=%s summary_chars=%s",
                    user_id,
                    segment_id,
                    len(new_summary_context)
                )
                prompt_block.summary = {
                    "summary_context": new_summary_context,
                    "timestamp": datetime.now().isoformat()
                }
            except Exception as e:
                logger.error(
                    "[SmartQA] Coze summary failed before save user=%s segment=%s error=%s",
                    user_id,
                    segment_id,
                    e
                )
                raise

        document_data = {
            "summary": prompt_block.summary,
            "conversation": prompt_block.conversation
        }
        metadata = {
            "user_id": str(user_id),
            "timestamp": datetime.now().isoformat(),
            "subject": "smart_qa",
            "last_segment_id": segment_id
        }
        try:
            collection = self._ensure_collection()
            collection.upsert(
                ids=[cache_key],
                documents=[json.dumps(document_data, ensure_ascii=False)],
                metadatas=[metadata],
                embeddings=[self._generate_deterministic_embedding(cache_key)]
            )
            logger.info(
                "[SmartQA] Saved to Chroma user=%s segment=%s doc_id=%s",
                user_id,
                segment_id,
                cache_key
            )
        except Exception as e:
            logger.error(
                "[SmartQA] Save to Chroma failed user=%s segment=%s doc_id=%s error=%s",
                user_id,
                segment_id,
                cache_key,
                e
            )
            raise
        del self.prompt_blocks[cache_key]

    def end_session(self, user_id: int, segment_id: str = "analysis"):
        self.save_to_chromadb_async(user_id, segment_id)

    def save_to_chromadb_async(self, user_id: int, segment_id: str = "analysis"):
        self._run_async(self.save_to_chromadb, user_id, segment_id)

    def _generate_deterministic_embedding(self, seed: str) -> List[float]:
        rng = random.Random(seed or 0)
        return [rng.random() for _ in range(self.EMBEDDING_DIMENSION)]

    def _run_async(self, target, *args, **kwargs):
        def _wrapper():
            try:
                target(*args, **kwargs)
            except Exception as e:
                logger.error(f"后台任务执行失败: {e}", exc_info=True)
        threading.Thread(target=_wrapper, daemon=True).start()

    def _get_or_recreate_collection(self):
        metadata = {"description": "Smart QA conversation history", "hnsw:space": "l2"}
        try:
            return self.chroma_client.get_or_create_collection(name=self.COLLECTION_NAME, metadata=metadata)
        except Exception:
            try:
                self.chroma_client.delete_collection(name=self.COLLECTION_NAME)
            except Exception:
                pass
            return self.chroma_client.get_or_create_collection(name=self.COLLECTION_NAME, metadata=metadata)

    def _ensure_collection(self):
        try:
            self.collection.count()
            return self.collection
        except NotFoundError:
            logger.warning("集合句柄失效，重新获取")
        except Exception as e:
            logger.warning(f"集合检查异常: {e}")
        self.collection = self._get_or_recreate_collection()
        return self.collection


logger.info("创建 smart_qa_service 实例...")
smart_qa_service = SmartQuestionAnswerService()
logger.info("smart_qa_service 创建完成")
