"""
CET4听力解析服务 - 使用DeepSeek API根据试卷内容生成结构化听力结果
"""
import json
import logging
from typing import Any, Dict, List

import openai

logger = logging.getLogger(__name__)


class ExamAnalysisListeningService:
    """听力部分解析服务类"""

    def __init__(self, api_key: str = "sk-508f4b7ff14d414fb806d0a2cb0b7b39"):
        """初始化DeepSeek客户端"""
        self.api_key = api_key
        self.client = openai.OpenAI(
            api_key=api_key,
            base_url="https://api.deepseek.com"
        )

    def analyze_listening(
        self,
        topics: List[str],
        input_file: str,
        model: str = "deepseek-reasoner"
    ) -> Dict[str, Any]:
        """调用DeepSeek解析听力试卷"""
        try:
            topics_text = json.dumps(topics, ensure_ascii=False) if isinstance(topics, list) else str(topics)
            logger.info(
                "开始调用DeepSeek进行听力解析，模型: %s，topics长度: %s，input长度: %s",
                model,
                len(topics),
                len(input_file),
            )

            system_prompt = self._build_system_prompt(topics_text, input_file)
            user_message = self._build_user_message(topics_text, input_file)

            response = self.client.chat.completions.create(
                model=model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_message},
                ],
                max_tokens=20000,
                temperature=0.3,
                top_p=0.9,
            )

            result = response.choices[0].message.content
            logger.info("DeepSeek听力解析完成，返回长度: %s", len(result))

            print("\n" + "=" * 80)
            print("🤖 DeepSeek 听力解析输出:")
            print("=" * 80)
            print(result)
            print("=" * 80 + "\n")

            json_content = self._extract_and_validate_json(result)
            return {"success": True, "data": json_content, "rawOutput": result, "message": "解析成功"}

        except openai.APIError as e:
            logger.error("DeepSeek API错误: %s", e)
            return {"success": False, "data": None, "message": f"API调用失败: {e}"}
        except Exception as e:
            logger.error("解析异常: %s", e, exc_info=True)
            return {"success": False, "data": None, "message": f"解析失败: {e}"}

    def _build_system_prompt(self, topics_text: str, input_file: str) -> str:
        """构建系统提示词，拼接用户入参"""
        base_prompt = (
            "我将为您优化提示词，移除所有关于question_prompt字段的要求。以下是更新后的完整提示词：\n\n"
            "---\n\n"
            "你将获得两个输入，一个是文章的主题集合{{topics}}是一个由字符串组成的列表，例如 `[\"Environmental Protection and Sustainable Development\", \"Climate Change and Carbon Neutrality\", \"Online Education vs. Traditional Classroom Learning\"]`，另一个是试卷的内容{{inputFile}}。\n\n"
            "请将{{inputFile}}所有内容，按以下要求转化为完整JSON结构化格式：\n\n"
            "1. 顶层包含\"units\"数组（存储所有题型单元）。\n"
            "2. 每个单元为\"units\"数组中的对象，包含6个固定字段：\n"
            "   - \"unit_type\"：题型标识（如\"News Report 1\"\"Conversation 2\"\"Passage 3\"）；\n"
            "   - \"segment_id\"：按固定规则生成的唯一标识（规则：part II 第一篇news report为2NewsReport1、第二篇为2NewsReport2、第三篇为2NewsReport3；第一篇conversation为2Conversation1、第二篇为2Conversation2；第一篇passage为2ListeningPassage1、第二篇为2ListeningPssage2、第三篇为2ListeningPassage3）；\n"
            "   - \"topic\"：本单元英语文章所对应的主题，优先从输入的主题集合中匹配最符合的，找不到则填\"unknown topic\"；\n\n"
            "   - \"listening_content\"：该单元听力朗读原文（仅保留原文段落，不含题目、选项）；\n"
            "   - \"question_and_options\"：数组类型，存储该单元所有题目，每题含\"question_number\"（题号）、\"question_content\"（题干）字段和\"options\"数组（选项按A、B、C、D顺序排列，每个选项包含\"option_mark\"和\"option_content\"，完全保留原文表述）；\n"
            "   - \"answers\"：数组类型，存储该单元所有题目的正确答案，每个答案包含\"question_number\"（题号）和\"answer\"（答案字母）。\n"
            "3. 按原文顺序呈现单元（News Report 1-3 → Conversation 1-2 → Passage 1-3），内容无遗漏、无修改，JSON格式规范无语法错误。\n"
            "4. 如果某个单元缺失，则完全跳过该单元继续生成下一个单元。\n\n"
            "**答案提取要求：**\n"
            "- 请阅读试卷的题目给出您的答案，每道题都有原文\n"
            "- 客观题：答案为字母（A/B/C/D）\n"
            "- 答案格式：每个单元的\"answers\"字段为数组，包含该单元所有题目的您提供的答案\n\n"
            "**输出格式要求：**\n"
            "1. JSON格式规范：确保语法正确（无多余逗号、引号匹配、字段名统一使用双引号），结构清晰，层级分明。\n"
            "2. 原文还原要求：所有文本内容需严格还原文档原文，包括标点符号、大小写、特殊标识，不得进行任何改写、简化或补充。\n"
            "3. 缺失内容标注：若文档中存在内容缺失，需在对应字段值中标注\"[文档中未提及此内容]\"，不得随意编造信息。\n"
            "4. 字符处理规则：遇到特殊字符组合需按规则处理，冗余内容（如页号、试卷标识）需剔除。\n"
            "5、对于所有这种两个单词之间有一个短杠的，如：\n"
            "data - entry，full - time ；全部把短杠去掉，将前后两个单词连起来作为一个单词\n\n"
            "**完整示例：**\n"
            "```json\n"
            "{\n"
            "  \"units\": [\n"
            "    {\n"
            "      \"unit_type\": \"News Report 1\",\n"
            "      \"segment_id\": \"2NewsReport1\",\n"
            "      \"topic\": \"Environmental Protection\",\n"
            "      \"directions\": \"Directions: In this section, you will hear three news reports. At the end of each news report, you will hear two or three questions. Both the news report and the questions will be spoken only once. After you hear a question, you must choose the best answer from the four choices marked A, B, C and D. Then mark the corresponding letter on Answer Sheet 1 with a single line through the centre.\",\n"
            "      \"listening_content\": \"A severe storm hit the coastal city yesterday, causing widespread power outages and flooding in low-lying areas. Emergency services have been working throughout the night to restore electricity and clear debris from roads...\",\n"
            "      \"question_and_options\": [\n"
            "        {\n"
            "          \"question_number\": \"1\",\n"
            "          \"question_content\": \"What is the main topic of this news report?\",\n"
            "          \"options\": [\n"
            "            {\"option_mark\": \"A\", \"option_content\": \"A political meeting\"},\n"
            "            {\"option_mark\": \"B\", \"option_content\": \"A severe storm\"},\n"
            "            {\"option_mark\": \"C\", \"option_content\": \"An economic crisis\"},\n"
            "            {\"option_mark\": \"D\", \"option_content\": \"A cultural festival\"}\n"
            "          ]\n"
            "        },\n"
            "        {\n"
            "          \"question_number\": \"2\", \n"
            "          \"question_content\": \"What have emergency services been doing?\",\n"
            "          \"options\": [\n"
            "            {\"option_mark\": \"A\", \"option_content\": \"Organizing evacuations\"},\n"
            "            {\"option_mark\": \"B\", \"option_content\": \"Restoring electricity and clearing debris\"},\n"
            "            {\"option_mark\": \"C\", \"option_content\": \"Building new shelters\"},\n"
            "            {\"option_mark\": \"D\", \"option_content\": \"Distributing food supplies\"}\n"
            "          ]\n"
            "        }\n"
            "      ],\n"
            "      \"answers\": [\n"
            "        {\"question_number\": \"1\", \"answer\": \"B\"},\n"
            "        {\"question_number\": \"2\", \"answer\": \"B\"}\n"
            "      ]\n"
            "    },\n"
            "    {\n"
            "      \"unit_type\": \"Conversation 1\",\n"
            "      \"segment_id\": \"2Conversation1\",\n"
            "      \"topic\": \"Campus Life\",\n"
            "      \"directions\": \"Directions: In this section, you will hear two long conversations. At the end of each conversation, you will hear four questions. Both the conversation and the questions will be spoken only once. After you hear a question, you must choose the best answer from the four choices marked A, B, C and D. Then mark the corresponding letter on Answer Sheet 1 with a single line through the centre.\",\n"
            "      \"listening_content\": \"M: Hey, Sarah! Have you decided which elective courses you're taking next semester? W: Not yet, I'm still considering my options. There are so many interesting courses to choose from...\",\n"
            "      \"question_and_options\": [\n"
            "        {\n"
            "          \"question_number\": \"3\",\n"
            "          \"question_content\": \"What are the speakers mainly discussing?\",\n"
            "          \"options\": [\n"
            "            {\"option_mark\": \"A\", \"option_content\": \"Summer vacation plans\"},\n"
            "            {\"option_mark\": \"B\", \"option_content\": \"Elective courses for next semester\"},\n"
            "            {\"option_mark\": \"C\", \"option_content\": \"Part-time job opportunities\"},\n"
            "            {\"option_mark\": \"D\", \"option_content\": \"Dormitory arrangements\"}\n"
            "          ]\n"
            "        }\n"
            "      ],\n"
            "      \"answers\": [\n"
            "        {\"question_number\": \"3\", \"answer\": \"B\"}\n"
            "      ]\n"
            "    }\n"
            "  ]\n"
            "}\n"
            "```\n"
            "提示词结束\n"
        )
        return base_prompt + f"\n\n【本次输入】\n- topics: {topics_text}\n- inputFile: {input_file}\n"

    def _build_user_message(self, topics_text: str, input_file: str) -> str:
        """构建用户消息，显式包含topics与input"""
        return (
            "请按照系统提示词要求解析听力试卷并输出JSON。\n\n"
            f"[topics]\n{topics_text}\n\n"
            f"[input]\n{input_file}"
        )

    def _extract_and_validate_json(self, result: str) -> str:
        """提取并验证JSON内容"""
        try:
            if "```json" in result:
                json_content = result.split("```json")[1].split("```")[0].strip()
                logger.info("检测到JSON代码块，已提取")
            elif "```" in result:
                json_content = result.split("```")[1].split("```")[0].strip()
                logger.info("检测到代码块，已提取")
            else:
                json_content = result.strip()

            json.loads(json_content)
            logger.info("✓ JSON格式验证成功")
            return json_content
        except Exception as e:
            logger.error("JSON提取或验证失败: %s", e)
            return result


# 创建服务实例（兼容路由引用名）
exam_analysis_listening_service = ExamAnalysisListeningService()
listening_analysis_service = exam_analysis_listening_service
