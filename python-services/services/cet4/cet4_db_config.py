"""
CET4学情分析 - 数据库连接配置
"""
import pymysql
from dbutils.pooled_db import PooledDB

# MySQL 数据库配置（与 application.properties 保持一致）
DB_CONFIG = {
    'host': 'localhost',
    'port': 3306,
    'user': 'root',
    'password': '123456',
    'database': 'examinai',
    'charset': 'utf8mb4',
    'cursorclass': pymysql.cursors.DictCursor
}

# 创建连接池
pool = PooledDB(
    creator=pymysql,
    maxconnections=20,
    mincached=2,
    maxcached=5,
    maxshared=3,
    blocking=True,
    maxusage=None,
    setsession=[],
    ping=1,
    **DB_CONFIG
)


def get_connection():
    """获取数据库连接"""
    return pool.connection()


def execute_query(sql, params=None):
    """执行查询SQL，返回多条记录"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchall()
    finally:
        conn.close()


def execute_one(sql, params=None):
    """执行查询返回单条记录"""
    conn = get_connection()
    try:
        with conn.cursor() as cursor:
            cursor.execute(sql, params)
            return cursor.fetchone()
    finally:
        conn.close()
