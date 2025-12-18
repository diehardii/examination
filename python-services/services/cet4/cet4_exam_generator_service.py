"""
CET4试卷生成服务 - 使用DeepSeek API生成试卷题目
"""
import openai
import json
import logging
from typing import Optional, Dict, Any

logger = logging.getLogger(__name__)


class ExamGeneratorService:
    """试卷生成服务类"""
    
    def __init__(self, api_key: str = "sk-508f4b7ff14d414fb806d0a2cb0b7b39"):
        """
        初始化试卷生成服务
        
        Args:
            api_key: DeepSeek API密钥
        """
        self.api_key = api_key
        self.client = openai.OpenAI(
            api_key=api_key,
            base_url="https://api.deepseek.com"
        )
    
    def preprocess_exam_sample(
        self,
        input_exam_paper_samp: str,
        segment_id_self: str,
        model: str = "deepseek-reasoner"
    ) -> Dict[str, Any]:
        """
        预处理试卷样例，规范化格式
        
        Args:
            input_exam_paper_samp: 输入的试卷样例
            segment_id_self: 新的segment_id
            model: 使用的模型
            
        Returns:
            处理结果
        """
        try:
            logger.info(f"开始预处理试卷样例，segment_id: {segment_id_self}")
            
            system_prompt = f"""你是一个专业的文本格式处理助手。请对输入的JSON文本进行以下规范化处理：

1. **移除unit_type中的数字**：将所有 "unit_type" 属性值中的数字去掉
   - 例如："News Report 3" → "News Report"
   - 例如："Passage 2" → "Passage"
   - 例如："Conversation 1" → "Conversation"

2. **重新编号question_number**：将所有 "question_number" 属性的值按出现顺序重新编号为 1, 2, 3, 4...

3. **替换segment_id**：将所有 "segment_id" 属性的值替换为 "{segment_id_self}"

4. **保持其他格式不变**：除上述三点外，保持JSON的所有其他格式、结构、内容完全不变

**重要提示**：
- 只输出处理后的JSON文本，不要添加任何说明或标记
- 确保输出是有效的JSON格式
- 保持原有的缩进和格式风格
"""
            
            user_message = f"""请处理以下JSON文本：

{input_exam_paper_samp}

按照以下规则处理：
1. unit_type 值中的数字全部去掉
2. question_number 按顺序重新编号为 1, 2, 3, 4...
3. segment_id 全部替换为 "{segment_id_self}"

只输出处理后的JSON，不要添加任何其他内容。"""
            
            logger.info(f"调用DeepSeek API进行预处理，模型: {model}")
            
            response = self.client.chat.completions.create(
                model=model,
                messages=[
                    {"role": "system", "content": system_prompt},
                    {"role": "user", "content": user_message}
                ],
                max_tokens=20000,
                temperature=0.1
            )
            
            result = response.choices[0].message.content
            logger.info(f"预处理完成，结果长度: {len(result)} 字符")
            
            # 打印预处理结果
            print("\n" + "="*80)
            print("🔧 预处理结果:")
            print("="*80)
            print(result)
            print("="*80 + "\n")
            
            # 提取JSON
            processed_content = result.strip()
            if "```json" in processed_content:
                processed_content = processed_content.split("```json")[1].split("```")[0].strip()
            elif "```" in processed_content:
                processed_content = processed_content.split("```")[1].split("```")[0].strip()
            
            # 验证JSON格式
            try:
                json.loads(processed_content)
                logger.info("✓ 预处理结果JSON格式验证成功")
            except json.JSONDecodeError as e:
                logger.error(f"✗ 预处理结果JSON格式错误: {e}")
            
            return {
                "success": True,
                "data": processed_content,
                "message": "预处理成功"
            }
            
        except Exception as e:
            logger.error(f"预处理失败: {e}", exc_info=True)
            return {
                "success": False,
                "data": None,
                "message": f"预处理失败: {str(e)}"
            }
    
    def generate_exam_question(
        self,
        input_exam_paper_samp: str,
        exam_topic: str,
        model: str = "deepseek-reasoner",
        exam_paper_en_source: str = None,
        segment_id_self: str = None
    ) -> Dict[str, Any]:
        """
        调用DeepSeek API生成试卷题目
        
        Args:
            input_exam_paper_samp: 输入的试卷样例（JSON格式字符串）
            exam_topic: 考试主题
            model: 指定模型，"deepseek-chat"或"deepseek-reasoner"
            exam_paper_en_source: 试卷来源，如果是"AIfromself"或"AIfromWrongBank"需要预处理
            segment_id_self: 自定义的segment_id
            
        Returns:
            包含生成结果的字典，格式：
            {
                "success": bool,
                "data": str,  # 生成的JSON字符串
                "message": str
            }
        """
        try:
            # 如果来源是AI生成或错题库，先进行预处理
            if exam_paper_en_source in ["AIfromself", "AIfromWrongBank"] and segment_id_self:
                logger.info(f"检测到需要预处理，来源: {exam_paper_en_source}, segment_id: {segment_id_self}")
                
                preprocess_result = self.preprocess_exam_sample(
                    input_exam_paper_samp=input_exam_paper_samp,
                    segment_id_self=segment_id_self,
                    model="deepseek-reasoner"
                )
                
                if preprocess_result['success']:
                    # 使用预处理后的结果
                    input_exam_paper_samp = preprocess_result['data']
                    logger.info("✓ 预处理完成，使用处理后的样例生成试卷")
                else:
                    logger.warning(f"⚠ 预处理失败，使用原始样例: {preprocess_result['message']}")
            else:
                logger.info("无需预处理，直接生成试卷")
            
            # 继续原有的生成流程
            # 截断超长输入
            if len(input_exam_paper_samp) > 20000:
                input_exam_paper_samp = input_exam_paper_samp[:20000]
                logger.warning(f"试卷样例已截断至20000字符")
            
            if len(exam_topic) > 100:
                exam_topic = exam_topic[:100]
                logger.warning(f"考试主题已截断至100字符")
            
            logger.info(f"输入参数 - 考试主题: {exam_topic}, 试卷样例长度: {len(input_exam_paper_samp)}字符")
            
            # 构建完整的提示词
            system_prompt = self._build_system_prompt(exam_topic)
            user_message = self._build_user_message(exam_topic, input_exam_paper_samp)
            
            logger.info(f"正在调用DeepSeek API，使用模型: {model}...")
            
            # 调用API
            response = self.client.chat.completions.create(
                model=model,
                messages=[
                    {
                        "role": "system",
                        "content": system_prompt
                    },
                    {
                        "role": "user",
                        "content": user_message
                    }
                ],
                max_tokens=20000,
                temperature=0.3,
                top_p=0.9
            )
            
            result = response.choices[0].message.content
            logger.info(f"API调用成功，返回结果长度: {len(result)} 字符")
            
            # 打印DeepSeek原始输出到控制台
            print("\n" + "="*80)
            print("🤖 DeepSeek API 原始输出:")
            print("="*80)
            print(result)
            print("="*80 + "\n")
            
            # 处理和验证结果
            json_content = self._extract_and_validate_json(result, exam_topic)
            
            return {
                "success": True,
                "data": json_content,
                "rawOutput": result,  # 添加原始输出
                "message": "试卷生成成功"
            }
            
        except openai.APIError as e:
            logger.error(f"DeepSeek API错误: {e}")
            return {
                "success": False,
                "data": None,
                "message": f"API调用失败: {str(e)}"
            }
        except Exception as e:
            logger.error(f"生成试卷异常: {e}", exc_info=True)
            return {
                "success": False,
                "data": None,
                "message": f"生成失败: {str(e)}"
            }
    
    def _build_system_prompt(self, exam_topic: str) -> str:
        """构建系统提示词"""
        return f"""你是一个专业的试卷生成助手。请根据用户提供的题目主题{{examTopic}} 和 题目样例{{inputExamPaperSamp}}，生成一道**全新题目**。

#### **一、核心任务**
你是一个专业的试卷生成助手。请根据用户提供的题目主题"{exam_topic}"和题目样例，生成一道**全新题目**。具体要求：

1. **题型与难度**：新题目必须与样例在题型、难度、长度、词汇量、结构上保持一致。
2. **题目数量**：严格按照输入样例的题目数量生成。
3. **标识符严格一致**：**所有题号、段落标记、单元类型、segment_id等标识符必须与输入样例完全一致**。
4. **JSON字符串转义要求**：**所有字符串内容中的双引号必须正确转义为 `\\"`，这是保证JSON可解析的最关键要求**。
5. **主题替换**：所有与主题相关的内容替换为用户指定的 "{exam_topic}"。
6. **格式严格一致**：输出的JSON必须与样例的字段名称、嵌套结构、标点符号完全一致。

---

#### **二、通用输出规则**
- ✅ **必须做**：
  1. 将 `topic` 字段的值替换为 "{exam_topic}"。
  2. **严格保持以下标识符与输入样例完全一致**：
     - 所有题号（question_number）
     - 所有选项标记（option_mark）
     - 单元类型（unit_type）
     - 段落标记（paragraph_mark、passage_mark）
     - 分段标识（segment_id）
     - 单词选项字母（letter）
     - 问题数量字段值（question_count、blank_count、statement_count等）
  3. **严格处理字符串转义**：
     - **所有字符串中的双引号必须转义为 `\\"`**（例如：`"flow"` 应写为 `\\"flow\\"`）
     - 反斜杠必须转义为 `\\\\`
     - 这是防止JSON解析失败的最关键步骤
  4. 严格按照输入样例的题目数量生成对应数量的题目。
  5. 重新生成与 "{exam_topic}" 相关的所有内容（文章、问题、选项、答案等）。
  6. 保持其他非主题相关字段与样例完全一致。
  7. 输出**完整的、语法正确的**JSON对象。
- ❌ **禁止做**：
  1. 修改JSON结构（增删字段、改变嵌套）。
  2. 更改题目数量或任何标识符。
  3. 在JSON外部添加任何额外文本。
  4. 使用不一致的引号或标点。
  5. **使用未转义的双引号**（这是最常见的JSON解析错误原因）。

---

#### **三、标识符处理关键原则**
1. **绝对一致原则**：所有标识符必须**逐字逐字符**与输入样例保持一致。
2. **数量对应原则**：生成的问题数量 = 输入样例中的问题数量。
3. **格式继承原则**：完全继承输入样例的所有格式特征。
4. **转义关键原则**：**所有字符串内容中的双引号必须转义为 `\\"`**。

---

#### **四、JSON完整性检查清单**
在输出前，请确认：
1. ✅ **字符串转义检查（最重要）**：所有字符串中的双引号已正确转义为 `\\"`
2. ✅ 所有大括号 `{{}}` 和中括号 `[]` 已正确闭合
3. ✅ 所有字符串已用双引号包裹
4. ✅ 字段之间用逗号分隔，但最后一个字段后无逗号
5. ✅ **所有标识符与输入样例完全一致**
6. ✅ 题目数量与输入样例完全一致
7. ✅ 所有数组长度匹配对应数量字段的值
8. ✅ `topic` 字段已正确替换为 "{exam_topic}"

---

#### **五、处理流程**
1. **分析输入**：仔细读取输入样例，记录所有标识符、题目数量和数组结构。
2. **生成内容**：根据 "{exam_topic}" 生成相关内容。
3. **转义处理**：检查所有生成的文本内容，将 `"` 替换为 `\\"`。
4. **构建JSON**：使用输入样例的标识符和结构，填入生成的内容。
5. **严格检查**：对照检查清单验证JSON，**特别关注转义情况**。
6. **最终验证**：确认输出的JSON可以被标准JSON解析器正确解析。

---

#### **六、最终指令**
请严格遵循以上格式和规则，根据用户提供的考试主题"{exam_topic}"和试卷样例生成题目。

**特别注意（按重要性排序）**：
1. **所有字符串中的双引号必须正确转义**（`"` → `\\"`）- 这是保证JSON可解析的最关键要求
2. 题目数量必须与输入样例完全一致
3. 所有题号、标记、标识符必须与输入样例完全一致
4. 只输出JSON对象，不要添加任何额外说明

**转义提醒**：在生成任何文本内容时，如果文本中需要包含引号，请务必使用 `\\"` 而不是 `"`。这是JSON格式的基本要求。"""
    
    def _build_user_message(self, exam_topic: str, input_exam_paper_samp: str) -> str:
        """构建用户消息"""
        return f"""考试主题：{exam_topic}

试卷样例（JSON格式）：
{input_exam_paper_samp}

请根据以上考试主题和试卷样例，生成一道全新题目。
**请严格按照以下要求输出：**
1. 只输出一个完整的、语法正确的JSON对象
2. 不要输出任何额外的文本、解释或标记
3. 确保所有标识符与输入样例完全一致
4. **特别注意：所有字符串中的双引号必须正确转义为 `\\"`**

输出格式必须是有效的JSON，可以直接被解析。"""
    
    def _extract_and_validate_json(self, result: str, exam_topic: str) -> str:
        """提取并验证JSON"""
        try:
            # 如果结果中包含代码块标记，提取JSON部分
            if "```json" in result:
                json_content = result.split("```json")[1].split("```")[0].strip()
                logger.info("检测到JSON代码块，已提取")
            elif "```" in result:
                json_content = result.split("```")[1].split("```")[0].strip()
                logger.info("检测到代码块，已提取")
            else:
                json_content = result.strip()
            
            # 解析JSON以验证格式
            parsed_json = json.loads(json_content)
            logger.info("✓ JSON格式验证成功")
            
            # 检查topic字段
            topic_found, topic_msg = self._check_topic_in_json(parsed_json, exam_topic)
            if topic_found:
                logger.info(f"✓ {topic_msg}")
            else:
                logger.warning(f"⚠ {topic_msg}")
            
            return json_content
            
        except json.JSONDecodeError as e:
            logger.error(f"✗ JSON解析错误: {e}")
            logger.error(f"错误位置: 第{e.lineno}行, 第{e.colno}列")
            
            # 尝试修复常见的JSON错误
            logger.info("尝试修复JSON格式...")
            try:
                fixed_json = result
                # 确保以{开头，以}结尾
                if not fixed_json.strip().startswith('{'):
                    start = fixed_json.find('{')
                    if start != -1:
                        fixed_json = fixed_json[start:]
                
                if not fixed_json.strip().endswith('}'):
                    end = fixed_json.rfind('}')
                    if end != -1:
                        fixed_json = fixed_json[:end+1]
                
                # 尝试解析修复后的JSON
                parsed = json.loads(fixed_json)
                logger.info("✓ JSON修复成功")
                return fixed_json
            except:
                logger.error("✗ JSON修复失败，返回原始结果")
                return result
        except Exception as e:
            logger.error(f"✗ 其他错误: {e}")
            return result
    
    def _check_topic_in_json(self, obj: Any, topic: str) -> tuple:
        """递归检查JSON对象中是否包含topic字段"""
        if isinstance(obj, dict):
            if "topic" in obj:
                if obj["topic"] == topic:
                    return True, "topic字段正确"
                else:
                    return False, f"topic字段值不匹配: {obj['topic']} != {topic}"
            for value in obj.values():
                found, msg = self._check_topic_in_json(value, topic)
                if found:
                    return found, msg
        elif isinstance(obj, list):
            for item in obj:
                found, msg = self._check_topic_in_json(item, topic)
                if found:
                    return found, msg
        return False, "未找到topic字段"


# 创建服务实例
exam_generator_service = ExamGeneratorService()
