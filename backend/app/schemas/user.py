#!/usr/bin/env python
# -*- coding: utf-8 -*-
"""
用户相关的 Pydantic 模式
"""

from datetime import datetime
from typing import Optional
from pydantic import BaseModel, Field


class UserBase(BaseModel):
    """用户基础模式"""
    username: str = Field(..., min_length=1, max_length=100)


class UserCreate(UserBase):
    """创建用户"""
    pass


class UserUpdate(BaseModel):
    """更新用户"""
    username: Optional[str] = Field(None, min_length=1, max_length=100)
    is_active: Optional[bool] = None


class UserResponse(UserBase):
    """用户响应"""
    id: int
    railway_username: Optional[str] = None
    is_logged_in: bool
    is_active: bool
    login_time: Optional[datetime] = None
    created_at: datetime
    
    class Config:
        from_attributes = True


class LoginStatusResponse(BaseModel):
    """登录状态响应"""
    is_logged_in: bool
    username: Optional[str] = None
    railway_username: Optional[str] = None
    login_time: Optional[datetime] = None
    expire_time: Optional[datetime] = None


class AuthSessionResponse(BaseModel):
    """认证会话信息"""
    access_token: str
    token_type: str = "bearer"
    expires_in: int
    user: UserResponse


class QRCodeResponse(BaseModel):
    """二维码响应"""
    uuid: str
    image_base64: str  # Base64 编码的图片


class LoginQRCodeResponse(QRCodeResponse):
    """扫码登录挑战二维码响应"""
    challenge_id: str


class QRCodeStatusResponse(BaseModel):
    """二维码状态响应"""
    status: int  # 0-等待, 1-已扫码, 2-确认登录, 3-过期
    message: str
    is_success: bool = False
    auth: Optional[AuthSessionResponse] = None


class PasswordLoginRequest(BaseModel):
    """账号密码登录请求"""
    username: str = Field(..., min_length=1, max_length=100, description="12306 账号")
    password: str = Field(..., min_length=1, max_length=200, description="12306 密码")


class PasswordLoginSmsRequest(BaseModel):
    """发送登录短信验证码请求"""
    username: str = Field(..., min_length=1, max_length=100, description="12306 账号")
    cast_num: str = Field(..., min_length=1, max_length=20, description="证件号后 4 位（用于接收验证码）")


class PasswordLoginSubmitRequest(BaseModel):
    """提交密码登录请求（含验证信息）"""
    username: str = Field(..., min_length=1, max_length=100, description="12306 账号")
    password: str = Field(..., min_length=1, max_length=200, description="12306 密码")
    verification: dict = Field(default_factory=dict, description="验证信息，如 {'type': 'sms', 'sms_code': '1234'}")


class PasswordLoginStepResponse(BaseModel):
    """密码登录分步响应"""
    status: str  # success / needs_verification / error
    message: str
    auth: Optional[AuthSessionResponse] = None
    verification_type: Optional[str] = None
    available_verifications: Optional[list] = None
    slide_token: Optional[str] = None
