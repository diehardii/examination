"""
CET4试卷解析服务（写作/阅读/翻译） - 使用DeepSeek API根据试卷内容生成结构化结果
"""
import json
import logging
from typing import Any, Dict, List

import openai

logger = logging.getLogger(__name__)


class ExamAnalysisService:
    """试卷解析服务类（去除听力部分）"""

    def __init__(self, api_key: str = "sk-508f4b7ff14d414fb806d0a2cb0b7b39"):
        """初始化DeepSeek客户端"""
        self.api_key = api_key
        self.client = openai.OpenAI(
            api_key=api_key,
            base_url="https://api.deepseek.com"
        )

    def analyze_exam(
        self,
        topics: List[str],
        input_file: str,
        model: str = "deepseek-reasoner"
    ) -> Dict[str, Any]:
        """调用DeepSeek解析试卷（写作/阅读/翻译）"""
        try:
            topics_text = json.dumps(topics, ensure_ascii=False) if isinstance(topics, list) else str(topics)
            logger.info(
                "开始调用DeepSeek进行试卷解析，模型: %s，topics长度: %s，input长度: %s",
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
            logger.info("DeepSeek解析完成，返回长度: %s", len(result))

            print("\n" + "=" * 80)
            print("🤖 DeepSeek 试卷解析输出:")
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
            "试卷解析大模型优化提示词（剔除Part II处理）\n\n"
            "你将获得两个输入，一个是文章的主题集合{{topics}}（由字符串组成的列表，例如 [\"Environmental Protection and Sustainable Development\", \"Climate Change and Carbon Neutrality\", \"Online Education vs. Traditional Classroom Learning\"]），另一个是试卷的内容{{inputFile}}。\n\n"
            "一、解析范围限定\n\n"
            "1. 从试卷内容{{inputFile}}提取\"Part I Writing\"\"Part III Reading Comprehension\"\"Part IV Translation\"模块下的所有内容。其中\"Part III Reading Comprehension\"包括Section A（选词填空）、Section B（段落匹配）、Section C（篇章阅读），需完整覆盖各题型的题干文本、题目内容及关联元素，不得遗漏关键信息。\n\n"
            "2. 若文档中存在题型编号、题目序号（如第36-45题、第46-50题等），需完整保留并对应到正确题型中。\n\n"
            "3. 特别说明：本提示词不处理\"Part II 听力\"相关内容，无需提取或解析该模块信息。\n\n"
            "二、各题型结构化要求\n\n"
            "（一）Part I Writing\n\n"
            "需包含以下核心字段，字段内容需与文档原文完全一致，不得篡改或缩写：\n\n"
            "- \"segment_id\"：固定值为\"1Writing1\"\n"
            "- \"topic\"：英语文章所对应的主题，优先从输入的主题集合中匹配最符合的，找不到则填\"unknown topic\"\n"
            "- \"question_type\"：固定值为\"Writing\"\n"
            "- \"prompt_requirement\"：固定值为\"一道写作题\"\n"
            "- \"passage\"：提取Part I Writing到Part II Listening Comprehension之间（或到文章末尾）的完整内容，若遇Part II相关标题仅作为提取终止标识，不包含其内容\n"
            "- \"reference_answer\"：提供写作题的参考范文或评分标准，从试卷中提取参考范文内容。若无明确范文，则基于题目要求生成符合评分标准的参考范文\n\n"
            "（二）Part III Reading Comprehension\n\n"
            "1. Section A（选词填空）\n\n"
            "需包含以下核心字段，字段内容需与文档原文完全一致，不得篡改或缩写：\n\n"
            "- \"segment_id\"：固定值为\"3BlankedCloze1\"\n"
            "- \"topic\"：英语文章所对应的主题，优先从输入的主题集合中匹配最符合的，找不到则填\"unknown topic\"\n"
            "- \"question_type\"：固定值为\"BlankedCloze\"\n"
            "- \"prompt_requirement\"：固定值为\"一篇含10个空白的短文，从15个备选单词（A-O）中选1个填入每个空白，不得重复使用单词\"\n"
            "- \"passage\"：文档中Section A对应的完整短文文本（需包含文中的空白位置标识，如\"26\"\"27\"等空白序号）\n"
            "- \"blank_count\"：固定值为10（代表短文含10个空白）\n"
            "- \"word_options\"：以数组形式列出15个备选单词，每个单词需包含\"letter\"（字母标识，如\"A\"\"B\"）和\"word\"（单词原文），格式示例:[{\"letter\":\"A\",\"word\":\"accepted\"},{\"letter\":\"B\",\"word\":\"audiences\"},...]\n"
            "- \"answers\"：以数组形式列出所有题目的正确答案，每个答案需包含\"question_number\"（题号）和\"answer\"（答案字母），格式示例:[{\"question_number\":\"26\",\"answer\":\"A\"},{\"question_number\":\"27\",\"answer\":\"B\"},...]\n\n"
            "2. Section B（段落匹配）\n\n"
            "需包含以下核心字段，字段内容需与文档原文完全一致，不得篡改或缩写：\n\n"
            "- \"segment_id\"：固定值为\"3Matching1\"\n"
            "- \"topic\"：英语文章所对应的主题，优先从输入的主题集合中匹配最符合的，找不到则填\"unknown topic\"\n"
            "- \"question_type\"：固定值为\"Matching\"\n"
            "- \"prompt_requirement\"：固定值为\"一篇含13个段落（A-M）的文章，10个陈述句（第36-45题），找出每个陈述句对应的段落来源，段落可重复选择\"\n"
            "- \"article\"：以数组形式列出文档中Section B对应的完整文章，每个段落需包含\"paragraph_mark\"（段落字母标识，如\"A\"\"B\"）和\"paragraph_content\"（段落完整文本），格式示例:[{\"paragraph_mark\":\"A\",\"paragraph_content\":\"Recently, a leading design federation...\"},{\"paragraph_mark\":\"B\",\"paragraph_content\":\"Chief executive John Kampfner...\"},...]\n"
            "- \"statement_count\"：固定值为10（代表10个信息陈述句）\n"
            "- \"statements\"：以数组形式列出10个信息陈述句，每个陈述句需包含\"question_number\"（题目序号，如\"36\"\"37\"）和\"statement_content\"（陈述句完整文本），格式示例:[{\"question_number\":\"36\",\"statement_content\":\"During the course of preparing for Design Ventura...\"},{\"question_number\":\"37\",\"statement_content\":\"A visit to the Design Museum shop...\"},...]\n"
            "- \"answers\"：以数组形式列出所有题目的正确答案，每个答案需包含\"question_number\"（题号）和\"answer\"（对应段落字母），格式示例:[{\"question_number\":\"36\",\"answer\":\"C\"},{\"question_number\":\"37\",\"answer\":\"D\"},...]\n\n"
            "3. Section C（篇章阅读）\n\n"
            "（1）Section C1（Passage One）\n\n"
            "需包含以下核心字段，字段内容需与文档原文完全一致，不得篡改或缩写：\n\n"
            "- \"segment_id\"：固定值为\"3ReadingPassage1\"\n"
            "- \"topic\"：Passage One对应的主题，优先从输入的主题集合中匹配最符合的，找不到则填\"unknown topic\"\n"
            "- \"question_type\"：固定值为\"ReadingPassage\"\n"
            "- \"prompt_requirement\"：固定值为\"1篇独立短文（Passage One），每篇对应5个问题（第46-50题）\"\n"
            "- \"passage_mark\"：固定值为\"Passage One\"\n"
            "- \"passage_content\"：短文完整文本\n"
            "- \"question_count\"：固定值为5（代表每篇短文对应5个问题）\n"
            "- \"questions\"：以数组形式列出该短文对应的5个问题，每个问题需包含\"question_number\"（题目序号，如\"46\"\"47\"）、\"question_content\"（问题完整文本）、\"options\"（以数组形式列出4个选项，每个选项含\"option_mark\"（选项标识，如\"A\"\"B\"）和\"option_content\"（选项完整文本）），格式示例:[{\"option_mark\":\"A\",\"option_content\":\"They are losing habitat due to...\"},{\"option_mark\":\"B\",\"option_content\":\"They have stopped seeking new mates...\"},...]\n"
            "- \"answers\"：以数组形式列出所有题目的正确答案，每个答案需包含\"question_number\"（题号）和\"answer\"（答案字母），格式示例:[{\"question_number\":\"46\",\"answer\":\"B\"},{\"question_number\":\"47\",\"answer\":\"A\"},...]\n\n"
            "（2）Section C2（Passage Two）\n\n"
            "需包含以下核心字段，字段内容需与文档原文完全一致，不得篡改或缩写：\n\n"
            "- \"segment_id\"：固定值为\"3ReadingPassage2\"\n"
            "- \"topic\"：Passage Two对应的主题，优先从输入的主题集合中匹配最符合的，找不到则填\"unknown topic\"\n"
            "- \"question_type\"：固定值为\"ReadingPassage\"\n"
            "- \"prompt_requirement\"：固定值为\"1篇独立短文（Passage Two），每篇对应5个问题（第51-55题）\"\n"
            "- \"passage_mark\"：固定值为\"Passage Two\"\n"
            "- \"passage_content\"：短文完整文本\n"
            "- \"question_count\"：固定值为5（代表每篇短文对应5个问题）\n"
            "- \"questions\"：以数组形式列出该短文对应的5个问题，每个问题需包含\"question_number\"（题目序号，如\"51\"\"52\"）、\"question_content\"（问题完整文本）、\"options\"（以数组形式列出4个选项，每个选项含\"option_mark\"（选项标识，如\"A\"\"B\"）和\"option_content\"（选项完整文本）），格式示例:[{\"option_mark\":\"A\",\"option_content\":\"They are losing habitat due to...\"},{\"option_mark\":\"B\",\"option_content\":\"They have stopped seeking new mates...\"},...]\n"
            "- \"answers\"：以数组形式列出所有题目的正确答案，每个答案需包含\"question_number\"（题号）和\"answer\"（答案字母），格式示例:[{\"question_number\":\"51\",\"answer\":\"C\"},{\"question_number\":\"52\",\"answer\":\"D\"},...]\n\n"
            "（三）Part IV Translation\n\n"
            "需包含以下核心字段，字段内容需与文档原文完全一致，不得篡改或缩写：\n\n"
            "- \"segment_id\"：固定值为\"4Translation1\"\n"
            "- \"topic\"：中文文章所对应的主题，优先从输入的主题集合中匹配最符合的，找不到则填\"unknown topic\"\n"
            "- \"question_type\"：固定值为\"Translation\"\n"
            "- \"prompt_requirement\"：固定值为\"一道翻译题\"\n"
            "- \"passage\"：提取Part IV Translation到文本末尾的完整内容\n"
            "- \"reference_answer\"：提供翻译题的参考译文，从试卷中提取参考译文内容。若无明确参考译文，则基于原文生成符合评分标准的参考译文\n\n"
            "三、输出格式要求\n\n"
            "1. JSON格式规范：整体采用JSON格式，确保语法正确（无多余逗号、引号匹配、字段名统一使用双引号），结构清晰，层级分明。顶层键名依次为\"writing\"、\"reading_comprehension\"、\"translation\"，分别对应三个处理模块。\n\n"
            "2. 原文还原要求：所有文本内容需严格还原文档原文，包括标点符号、大小写、特殊标识（如英文括号、连接符等），不得进行任何改写、简化或补充。\n\n"
            "3. 缺失内容标注：若文档中存在与上述字段对应的内容缺失（如个别选项文本不全），需在对应字段值中标注\"[文档中未提及此内容]\"，不得随意编造信息。\n\n"
            "4. 模块取舍规则：若某一题型（或模块）在文档中无对应内容，不输出该题型（或模块）的任何信息；若某个Part的部分核心内容缺失，不输出该Part的任何信息。\n\n"
            "5. 字符替换规则：遇到\"_+数字\"（如_34）或\"_+空格+数字\"组合，替换为\"_+数字+_\"（如_34_）；已为\"_+数字+_\"格式的无需修改。\n\n"
            "6. 冗余内容剔除：一行中前后带空格的单独数字（类似页号）需剔除；类似\"025 年 6 月四级真题 (第一套)\"的试卷标识文本需剔除。\n\n"
            "7. 答案处理规则：\n   - 客观题（选词填空、段落匹配、篇章阅读）： 请您阅读题目提供答案\n   - 主观题（写作、翻译）：提供参考范文或参考译文\n\n"
            "四、示例参考（完整结构片段）\n\n"
            "{\n  \"writing\": {\n    \"segment_id\": \"1writing\",\n    \"topic\": \"Online Education vs. Traditional Classroom Learning\",\n    \"question_type\": \"写作\",\n    \"prompt_requirement\": \"一道写作题\",\n    \"passage\": \"Directions: For this part, you are allowed 30 minutes to write an essay comparing online education and traditional classroom learning. You should write at least 120 words but no more than 180 words.\",\n    \"reference_answer\": \"With the rapid development of technology, online education has become increasingly popular. Both online education and traditional classroom learning have their unique advantages. Online education offers flexibility and convenience, allowing students to learn at their own pace and schedule. It also provides access to a wide range of resources from anywhere in the world. On the other hand, traditional classroom learning facilitates face-to-face interaction between teachers and students, which enhances immediate feedback and collaborative learning. It also creates a structured environment that helps students stay focused. In my opinion, the best approach is to combine the strengths of both methods to create a more effective learning experience.\"\n  },\n  \"reading_comprehension\": {\n    \"section_a\": {\n      \"segment_id\": \"3BlankedCloze1\",\n      \"topic\": \"Environmental Protection and Sustainable Development\",\n      \"question_type\": \"选词填空\",\n      \"prompt_requirement\": \"一篇含10个空白的短文，从15个备选单词（A-O）中选1个填入每个空白，不得重复使用单词\",\n      \"passage\": \"Sustainable development has become a global focus. Many countries are taking measures to protect the environment. _26_ the past few years, great progress has been made. _27_...\",\n      \"blank_count\": 10,\n      \"word_options\": [\n        {\"letter\": \"A\", \"word\": \"Over\"},\n        {\"letter\": \"B\", \"word\": \"However\"}\n      ],\n      \"answers\": [\n        {\"question_number\": \"26\", \"answer\": \"A\"},\n        {\"question_number\": \"27\", \"answer\": \"B\"}\n      ]\n    },\n    \"section_c1\": {\n      \"segment_id\": \"3ReadingPassage1\",\n      \"topic\": \"Climate Change and Carbon Neutrality\",\n      \"question_type\": \"篇章阅读\",\n      \"prompt_requirement\": \"1篇独立短文（Passage One），每篇对应5个问题（第46-50题）\",\n      \"passage_mark\": \"Passage One\",\n      \"passage_content\": \"Climate change is one of the most serious challenges facing humanity. Carbon neutrality has been put forward as a key solution to this problem. Many countries have announced their carbon neutrality goals...\",\n      \"question_count\": 5,\n      \"questions\": [{\"question_number\": \"46\", \"question_content\": \"What is the key solution to climate change mentioned in the passage?\", \"options\": [{\"option_mark\": \"A\", \"option_content\": \"Reducing the use of fossil fuels\"}]}],\n      \"answers\": [\n        {\"question_number\": \"46\", \"answer\": \"B\"},\n        {\"question_number\": \"47\", \"answer\": \"A\"}\n      ]\n    }\n  },\n  \"translation\": {\n    \"segment_id\": \"4translation1\",\n    \"topic\": \"Environmental Protection and Sustainable Development\",\n    \"question_type\": \"翻译\",\n    \"prompt_requirement\": \"一道翻译题\",\n    \"passage\": \"Directions: For this part, you are allowed 30 minutes to translate a passage from Chinese into English. The passage is as follows: 可持续发展是一种注重长远发展的经济增长模式，强调在满足当代人需求的同时，不损害后代人满足其自身需求的能力。近年来，中国在可持续发展领域取得了显著成就，受到国际社会的广泛认可。\",\n    \"reference_answer\": \"Sustainable development is an economic growth model that focuses on long-term development. It emphasizes meeting the needs of the present generation without compromising the ability of future generations to meet their own needs. In recent years, China has achieved remarkable accomplishments in the field of sustainable development, which has been widely recognized by the international community.\"\n  }\n}\n"
            "提示词结束：\n"
            "输入参数为{{topics}}（由字符串组成的列表，例如 [\"Environmental Protection and Sustainable Development\", \"Climate Change and Carbon Neutrality\", \"Online Education vs. Traditional Classroom Learning\"]），另一个是试卷的内容{{inputFile}}。\n"
            "这两个参数要拼接到提示词中，这两个参数是整个py文件的入参"
        )
        return base_prompt + f"\n\n【本次输入】\n- topics: {topics_text}\n- inputFile: {input_file}\n"

    def _build_user_message(self, topics_text: str, input_file: str) -> str:
        """构建用户消息，显式包含topics与input"""
        return (
            "请按照系统提示词要求解析试卷并输出JSON。\n\n"
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


# 创建服务实例
exam_analysis_service = ExamAnalysisService()
