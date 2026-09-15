#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
用户相关 API 接口
"""

import json
from typing import List
from fastapi import APIRouter, HTTPException, Depends
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy import select

from ..core.auth import get_current_user
from ..core.database import get_db
from ..models.user import User
from ..schemas.common import ResponseBase
from ..schemas.task import PassengerInfo
from ..services.login_service import LoginService, LoginSession
from ..services.order_service import OrderService

router = APIRouter(prefix="/users", tags=["用户"])


_SESSION_EXPIRED_HINTS = (
    "登录已过期",
    "用户未登录",
    "未登录",
    "解析失败(非JSON)",
    "请重新登录",
)


def _session_cookies_from_user(user: User) -> dict:
    """从用户会话数据中提取 cookies。"""
    session_data = json.loads(user.session_data)
    # 兼容处理：如果是新格式（包含 cookies 键），取 cookies；否则假设整个对象就是 cookies 字典
    if "cookies" in session_data and isinstance(session_data["cookies"], dict):
        return session_data["cookies"]
    return session_data


def _to_passenger_info_list(passengers) -> List[PassengerInfo]:
    data = []
    for p in passengers:
        data.append(PassengerInfo(
            passenger_name=p.passenger_name,
            passenger_id_no=p.passenger_id_no,
            passenger_id_type_code=p.passenger_id_type_code,
            passenger_type=p.passenger_type,
            mobile_no=p.mobile_no
        ))
    return data


async def _auto_refresh_user_session(user: User, db: AsyncSession) -> bool:
    """会话过期时静默续期（会话文件缺失则从数据库备份恢复），成功写回并提交。"""
    login_service = LoginService(str(user.id))
    try:
        # 会话文件缺失/为空时，从数据库备份恢复会话再续期
        if not getattr(login_service.session, "cookies", None) and user.session_data:
            try:
                data = json.loads(user.session_data)
                login_service.session = LoginSession.from_dict(data)
                login_service._save_session()
            except Exception:
                pass

        ok, _ = await login_service.refresh_session()
        if ok:
            user.session_data = json.dumps(login_service.session.to_dict())
            user.is_logged_in = True
            user.login_time = login_service.session.login_time
            await db.commit()
        return ok
    finally:
        await login_service.close()


async def _load_user_passengers_from_session(user: User, db: AsyncSession) -> ResponseBase[List[PassengerInfo]]:
    """从用户会话加载乘车人列表；会话过期时自动续期并重试一次。"""
    if not user.session_data:
        raise HTTPException(status_code=400, detail="用户未登录 12306")

    order_service = OrderService(_session_cookies_from_user(user))

    try:
        success, passengers, msg = await order_service.query_passengers()
        await order_service.close()

        if success:
            return ResponseBase(success=True, data=_to_passenger_info_list(passengers))

        # 疑似登录过期：先自动续期，成功后用新会话重试一次
        if any(hint in msg for hint in _SESSION_EXPIRED_HINTS):
            if await _auto_refresh_user_session(user, db):
                order_service = OrderService(_session_cookies_from_user(user))
                try:
                    success, passengers, msg = await order_service.query_passengers()
                finally:
                    await order_service.close()
                if success:
                    return ResponseBase(success=True, data=_to_passenger_info_list(passengers))
            return ResponseBase(success=False, message="登录已过期，自动续期失败，请重新登录 12306", data=[])

        return ResponseBase(success=False, message=msg, data=[])

    except Exception as e:
        await order_service.close()
        return ResponseBase(success=False, message=str(e), data=[])


@router.get("/me/passengers", response_model=ResponseBase[List[PassengerInfo]])
async def get_my_passengers(
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db),
):
    """获取当前登录用户乘车人列表"""
    return await _load_user_passengers_from_session(current_user, db)

@router.get("/{user_id}/passengers", response_model=ResponseBase[List[PassengerInfo]])
async def get_user_passengers(
    user_id: int,
    current_user: User = Depends(get_current_user),
    db: AsyncSession = Depends(get_db)
):
    """获取指定用户乘车人列表（兼容接口，仅允许查询本人）"""
    if user_id != current_user.id:
        raise HTTPException(status_code=403, detail="无权访问该用户数据")

    stmt = select(User).where(User.id == user_id, User.is_active == True)
    result = await db.execute(stmt)
    user = result.scalar_one_or_none()

    if not user:
        raise HTTPException(status_code=404, detail="用户不存在")

    return await _load_user_passengers_from_session(user, db)
