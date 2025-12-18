"""
模拟考试记录生成器
功能：
1. 扫描exam_paper_en和users表
2. 在user_exam_paper_en表中为所有用户建立与所有试卷的关系
3. 为每个学生在每张试卷模拟10次考试记录
"""

import mysql.connector
import chromadb
from chromadb.config import Settings
import random
import json
from datetime import datetime, timedelta
from typing import Dict, List, Any, Optional
import os

# 数据库配置 - 参照 application.properties
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'examinai',
    'charset': 'utf8mb4'
}

# ChromaDB配置
CHROMA_PATH = os.path.join(os.path.dirname(__file__), '..', 'chroma')
COLLECTION_NAME = 'eng_exam_papers_cet4'

# 试卷结构和分数配置
EXAM_STRUCTURE = {
    '1Writing1': {'number_of_questions': 1, 'score_per_question': 106.5, 'segment_total_score': 106.5, 'question_type': 'Writing', 'skip_answer': True},
    '2NewsReport1': {'number_of_questions': 2, 'score_per_question': 7.1, 'segment_total_score': 14.2, 'question_type': 'NewsReport', 'options': ['A', 'B', 'C', 'D']},
    '2NewsReport2': {'number_of_questions': 2, 'score_per_question': 7.1, 'segment_total_score': 14.2, 'question_type': 'NewsReport', 'options': ['A', 'B', 'C', 'D']},
    '2NewsReport3': {'number_of_questions': 3, 'score_per_question': 7.1, 'segment_total_score': 21.3, 'question_type': 'NewsReport', 'options': ['A', 'B', 'C', 'D']},
    '2Conversation1': {'number_of_questions': 4, 'score_per_question': 7.1, 'segment_total_score': 28.4, 'question_type': 'Conversation', 'options': ['A', 'B', 'C', 'D']},
    '2Conversation2': {'number_of_questions': 4, 'score_per_question': 7.1, 'segment_total_score': 28.4, 'question_type': 'Conversation', 'options': ['A', 'B', 'C', 'D']},
    '2ListeningPassage1': {'number_of_questions': 3, 'score_per_question': 14.2, 'segment_total_score': 42.6, 'question_type': 'ListeningPassage', 'options': ['A', 'B', 'C', 'D']},
    '2ListeningPassage2': {'number_of_questions': 3, 'score_per_question': 14.2, 'segment_total_score': 42.6, 'question_type': 'ListeningPassage', 'options': ['A', 'B', 'C', 'D']},
    '2ListeningPassage3': {'number_of_questions': 4, 'score_per_question': 14.2, 'segment_total_score': 56.8, 'question_type': 'ListeningPassage', 'options': ['A', 'B', 'C', 'D']},
    '3BlankedCloze1': {'number_of_questions': 10, 'score_per_question': 3.55, 'segment_total_score': 35.5, 'question_type': 'BlankedCloze', 'options': ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O']},
    '3Matching1': {'number_of_questions': 10, 'score_per_question': 7.1, 'segment_total_score': 71, 'question_type': 'Matching', 'options': ['A', 'B', 'C', 'D', 'E', 'F', 'G', 'H', 'I', 'J', 'K', 'L', 'M', 'N', 'O']},
    '3ReadingPassage1': {'number_of_questions': 5, 'score_per_question': 14.2, 'segment_total_score': 71, 'question_type': 'ReadingPassage', 'options': ['A', 'B', 'C', 'D']},
    '3ReadingPassage2': {'number_of_questions': 5, 'score_per_question': 14.2, 'segment_total_score': 71, 'question_type': 'ReadingPassage', 'options': ['A', 'B', 'C', 'D']},
    '4Translation1': {'number_of_questions': 1, 'score_per_question': 106.5, 'segment_total_score': 106.5, 'question_type': 'Translation', 'skip_answer': True},
}

class ExamSimulator:
    def __init__(self):
        self.db_conn = None
        self.chroma_client = None
        self.collection = None
        self.exam_papers_data = {}  # 缓存试卷数据 {exam_paper_id: {segment_id: data}}
        
    def connect_db(self):
        """连接MySQL数据库"""
        print("正在连接MySQL数据库...")
        self.db_conn = mysql.connector.connect(**DB_CONFIG)
        print("✅ MySQL数据库连接成功")
        
    def connect_chroma(self):
        """连接ChromaDB"""
        print(f"正在连接ChromaDB，路径: {CHROMA_PATH}")
        self.chroma_client = chromadb.PersistentClient(
            path=CHROMA_PATH,
            settings=Settings(
                anonymized_telemetry=False,
                allow_reset=True
            )
        )
        self.collection = self.chroma_client.get_or_create_collection(name=COLLECTION_NAME)
        print(f"✅ ChromaDB连接成功，集合: {COLLECTION_NAME}")
        
    def get_all_exam_papers(self) -> List[Dict]:
        """获取所有试卷"""
        cursor = self.db_conn.cursor(dictionary=True)
        cursor.execute("SELECT id, exam_paper_en_name, exam_paper_en_subject FROM exam_paper_en")
        papers = cursor.fetchall()
        cursor.close()
        print(f"📄 找到 {len(papers)} 份试卷")
        return papers
    
    def get_all_students(self) -> List[Dict]:
        """获取所有学生（role_id=2）"""
        cursor = self.db_conn.cursor(dictionary=True)
        cursor.execute("SELECT id, username, real_name FROM users WHERE role_id = 2")
        students = cursor.fetchall()
        cursor.close()
        print(f"👨‍🎓 找到 {len(students)} 名学生")
        return students
    
    def create_user_exam_paper_relations(self, students: List[Dict], papers: List[Dict]):
        """为所有用户建立与所有试卷的关系"""
        print("\n📌 正在建立用户-试卷关系...")
        cursor = self.db_conn.cursor()
        
        # 先检查已存在的关系
        cursor.execute("SELECT user_id, exam_paper_en_id FROM user_exam_paper_en")
        existing = set((row[0], row[1]) for row in cursor.fetchall())
        
        inserted = 0
        for student in students:
            for paper in papers:
                if (student['id'], paper['id']) not in existing:
                    cursor.execute(
                        "INSERT INTO user_exam_paper_en (user_id, exam_paper_en_id) VALUES (%s, %s)",
                        (student['id'], paper['id'])
                    )
                    inserted += 1
        
        self.db_conn.commit()
        cursor.close()
        print(f"✅ 新建立 {inserted} 条用户-试卷关系")
        
    def load_exam_paper_from_chroma(self, exam_paper_id: int) -> Dict[str, Any]:
        """从ChromaDB加载试卷数据"""
        if exam_paper_id in self.exam_papers_data:
            return self.exam_papers_data[exam_paper_id]
        
        # 查询该试卷的所有数据
        results = self.collection.get(
            where={"exam_paper_en_id": exam_paper_id},
            include=["documents", "metadatas"]
        )
        
        paper_data = {}
        if results and results['documents']:
            for i, doc in enumerate(results['documents']):
                metadata = results['metadatas'][i] if results['metadatas'] else {}
                segment_id = metadata.get('segment_id', '')
                if segment_id:
                    paper_data[segment_id] = {
                        'document': doc,
                        'metadata': metadata
                    }
        
        self.exam_papers_data[exam_paper_id] = paper_data
        return paper_data
    
    def extract_correct_answers(self, document: str, segment_id: str) -> Dict[int, str]:
        """从试卷文档中提取正确答案"""
        answers = {}
        try:
            data = json.loads(document)
            
            # 尝试从不同的结构中提取答案
            questions = None
            if 'questions' in data:
                questions = data['questions']
            elif 'answers' in data:
                # 有些结构直接有answers字段
                ans_data = data['answers']
                if isinstance(ans_data, dict):
                    for q_num, ans in ans_data.items():
                        answers[int(q_num)] = str(ans)
                    return answers
                elif isinstance(ans_data, list):
                    for i, ans in enumerate(ans_data, 1):
                        answers[i] = str(ans)
                    return answers
            
            if questions and isinstance(questions, list):
                for q in questions:
                    q_num = q.get('question_number') or q.get('number') or q.get('blank_number')
                    correct = q.get('correct_answer') or q.get('answer') or q.get('correct')
                    if q_num is not None and correct:
                        answers[int(q_num)] = str(correct)
            
            # 处理选词填空等特殊结构
            if 'blanks' in data:
                for blank in data['blanks']:
                    b_num = blank.get('blank_number') or blank.get('number')
                    correct = blank.get('correct_answer') or blank.get('answer')
                    if b_num is not None and correct:
                        answers[int(b_num)] = str(correct)
                        
        except json.JSONDecodeError:
            pass
        except Exception as e:
            print(f"  ⚠️ 提取答案时出错 ({segment_id}): {e}")
            
        return answers
    
    def generate_user_answer(self, correct_answer: str, options: List[str], correct_rate: float = 0.6) -> str:
        """生成用户答案，有一定概率答对"""
        if random.random() < correct_rate:
            return correct_answer
        else:
            # 随机选择一个错误答案
            wrong_options = [opt for opt in options if opt != correct_answer]
            if wrong_options:
                return random.choice(wrong_options)
            return random.choice(options)
    
    def simulate_single_test(self, user_id: int, exam_paper_id: int, paper_data: Dict, test_time: datetime) -> Optional[int]:
        """模拟一次考试，返回test_en_id"""
        cursor = self.db_conn.cursor()
        
        total_score = 0.0
        total_correct = 0
        segment_records = []  # 段落记录
        detail_records = []   # 详细题目记录
        
        # 对于每个段落
        for segment_id, config in EXAM_STRUCTURE.items():
            question_type = config['question_type']
            num_questions = config['number_of_questions']
            score_per_question = config['score_per_question']
            segment_total = config['segment_total_score']
            skip_answer = config.get('skip_answer', False)
            options = config.get('options', ['A', 'B', 'C', 'D'])
            
            # 获取该段落的正确答案
            correct_answers = {}
            if segment_id in paper_data:
                correct_answers = self.extract_correct_answers(
                    paper_data[segment_id]['document'], 
                    segment_id
                )
            
            # 如果没有找到答案，使用默认的选项
            if not correct_answers and not skip_answer:
                for i in range(1, num_questions + 1):
                    correct_answers[i] = random.choice(options)
            
            segment_correct = 0
            segment_score = 0.0
            
            if skip_answer:
                # 写作和翻译，随机给分（60-100分之间的比例）
                score_ratio = random.uniform(0.5, 0.95)
                segment_score = segment_total * score_ratio
            else:
                # 选择题，模拟答题
                for q_num in range(1, num_questions + 1):
                    correct_answer = correct_answers.get(q_num, random.choice(options))
                    user_answer = self.generate_user_answer(correct_answer, options, random.uniform(0.4, 0.8))
                    
                    is_correct = (user_answer == correct_answer)
                    if is_correct:
                        segment_correct += 1
                        segment_score += score_per_question
                    
                    # 记录详细答题
                    detail_records.append({
                        'correct_answer': correct_answer,
                        'questions_en_number': q_num,
                        'segment_id': segment_id,
                        'questions_type': question_type,
                        'user_answer': user_answer
                    })
            
            total_score += segment_score
            total_correct += segment_correct
            
            # 记录段落统计
            correct_percent = (segment_correct / num_questions * 100) if num_questions > 0 else 0
            if skip_answer:
                correct_percent = (segment_score / segment_total * 100) if segment_total > 0 else 0
                
            segment_records.append({
                'segment_id': segment_id,
                'question_type': question_type,
                'score': round(segment_score, 2),
                'correct_answers_number': segment_correct,
                'number_of_questions': num_questions,
                'correct_answers_percent': round(correct_percent, 2)
            })
        
        # 插入主记录
        cursor.execute(
            """INSERT INTO user_test_record_en 
               (correct_number, test_en_score, test_en_time, exam_paper_en_id, user_id)
               VALUES (%s, %s, %s, %s, %s)""",
            (total_correct, round(total_score, 2), test_time, exam_paper_id, user_id)
        )
        test_en_id = cursor.lastrowid
        
        # 插入段落记录
        for seg in segment_records:
            cursor.execute(
                """INSERT INTO user_test_record_segment_en
                   (segment_id, question_type, score, correct_answers_number, 
                    number_of_questions, correct_answers_percent, test_en_id)
                   VALUES (%s, %s, %s, %s, %s, %s, %s)""",
                (seg['segment_id'], seg['question_type'], seg['score'],
                 seg['correct_answers_number'], seg['number_of_questions'],
                 seg['correct_answers_percent'], test_en_id)
            )
        
        # 插入详细记录
        for detail in detail_records:
            cursor.execute(
                """INSERT INTO user_test_record_detail_en
                   (correct_answer, questions_en_number, segment_id, 
                    questions_type, user_answer, test_en_id)
                   VALUES (%s, %s, %s, %s, %s, %s)""",
                (detail['correct_answer'], detail['questions_en_number'],
                 detail['segment_id'], detail['questions_type'],
                 detail['user_answer'], test_en_id)
            )
        
        cursor.close()
        return test_en_id
    
    def simulate_all_tests(self, students: List[Dict], papers: List[Dict], tests_per_paper: int = 10):
        """为所有学生模拟所有试卷的考试"""
        print(f"\n🎯 开始模拟考试，每个学生每张试卷 {tests_per_paper} 次...")
        
        total_students = len(students)
        total_papers = len(papers)
        total_tests = total_students * total_papers * tests_per_paper
        completed = 0
        
        base_time = datetime.now() - timedelta(days=90)  # 从90天前开始
        
        for s_idx, student in enumerate(students):
            print(f"\n👤 学生 {s_idx + 1}/{total_students}: {student['username']} (ID: {student['id']})")
            
            for p_idx, paper in enumerate(papers):
                print(f"  📝 试卷 {p_idx + 1}/{total_papers}: {paper['exam_paper_en_name']}")
                
                # 加载试卷数据
                paper_data = self.load_exam_paper_from_chroma(paper['id'])
                
                for test_num in range(tests_per_paper):
                    # 生成随机考试时间
                    test_time = base_time + timedelta(
                        days=random.randint(0, 90),
                        hours=random.randint(8, 20),
                        minutes=random.randint(0, 59)
                    )
                    
                    test_id = self.simulate_single_test(
                        student['id'], 
                        paper['id'], 
                        paper_data, 
                        test_time
                    )
                    
                    completed += 1
                    if completed % 100 == 0:
                        self.db_conn.commit()
                        print(f"    ⏳ 进度: {completed}/{total_tests} ({completed*100//total_tests}%)")
        
        self.db_conn.commit()
        print(f"\n✅ 模拟完成！共生成 {completed} 条考试记录")
    
    def run(self):
        """运行模拟器"""
        try:
            print("=" * 60)
            print("考试记录模拟生成器")
            print("=" * 60)
            
            # 连接数据库
            self.connect_db()
            self.connect_chroma()
            
            # 获取数据
            papers = self.get_all_exam_papers()
            students = self.get_all_students()
            
            if not papers:
                print("❌ 没有找到试卷，请先添加试卷")
                return
            if not students:
                print("❌ 没有找到学生，请先添加学生用户")
                return
            
            # 建立用户-试卷关系
            self.create_user_exam_paper_relations(students, papers)
            
            # 模拟考试
            self.simulate_all_tests(students, papers, tests_per_paper=10)
            
            print("\n" + "=" * 60)
            print("✅ 所有操作完成！")
            print("=" * 60)
            
        except Exception as e:
            print(f"\n❌ 发生错误: {e}")
            import traceback
            traceback.print_exc()
        finally:
            if self.db_conn:
                self.db_conn.close()
                print("数据库连接已关闭")


if __name__ == "__main__":
    simulator = ExamSimulator()
    simulator.run()
