#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
数据库配置模块

使用 SQLAlchemy 2.0 异步模式
"""

from sqlalchemy import text
from sqlalchemy.ext.asyncio import AsyncSession, create_async_engine, async_sessionmaker
from sqlalchemy.orm import DeclarativeBase
from typing import AsyncGenerator

from .config import get_settings

settings = get_settings()

# 创建异步引擎
engine = create_async_engine(
    settings.DATABASE_URL,
    echo=settings.DEBUG,
    future=True,
)

# 创建异步会话工厂
AsyncSessionLocal = async_sessionmaker(
    engine,
    class_=AsyncSession,
    expire_on_commit=False,
    autoflush=False,
    autocommit=False,
)


class Base(DeclarativeBase):
    """模型基类"""
    pass


async def get_db() -> AsyncGenerator[AsyncSession, None]:
    """获取数据库会话（依赖注入）"""
    async with AsyncSessionLocal() as session:
        try:
            yield session
            await session.commit()
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()


async def init_db():
    """初始化数据库（创建表）"""
    async with engine.begin() as conn:
        await conn.run_sync(Base.metadata.create_all)


async def run_migrations():
    """轻量级迁移：为已存在的表补充模型新增的列（SQLite ALTER TABLE）"""
    from sqlalchemy import Integer, String, Text, Boolean, DateTime

    type_map = {
        Integer: "INTEGER",
        String: "VARCHAR",
        Text: "TEXT",
        Boolean: "BOOLEAN",
        DateTime: "DATETIME",
    }

    async with engine.begin() as conn:
        try:
            rows = await conn.execute(text("SELECT name FROM sqlite_master WHERE type='table'"))
            existing_tables = {row[0] for row in rows.fetchall()}
        except Exception as exc:
            print(f"[迁移] 读取数据表失败，跳过: {exc}")
            return

        for table in Base.metadata.sorted_tables:
            if table.name not in existing_tables:
                continue
            pragma = await conn.execute(text(f"PRAGMA table_info({table.name})"))
            existing_cols = {row[1] for row in pragma.fetchall()}
            for column in table.columns:
                if column.name in existing_cols:
                    continue
                col_type = type_map.get(type(column.type), "VARCHAR")
                await conn.execute(text(
                    f"ALTER TABLE {table.name} ADD COLUMN {column.name} {col_type}"
                ))
                print(f"[迁移] 表 {table.name} 新增列 {column.name} {col_type}")


async def close_db():
    """关闭数据库连接"""
    await engine.dispose()
