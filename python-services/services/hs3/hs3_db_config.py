# HS3 高考英语数据库配置
# 参照 cet4/cet4_db_config.py 结构

"""
HS3 高考英语数据库配置

定义高考英语相关的数据库连接和集合配置
"""

# ChromaDB 集合名称
HS3_EXAM_COLLECTION = "exam_papers_en_hs3"
HS3_TUTORING_COLLECTION = "tutoring_content_en_hs3"
HS3_KNOWLEDGE_COLLECTION = "knowledge_base_en_hs3"

# 数据库配置
HS3_DB_CONFIG = {
    "collection_prefix": "hs3_",
    "embedding_dimension": 1024,
    "distance_metric": "cosine"
}
