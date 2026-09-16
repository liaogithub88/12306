<template>
  <div class="login-page">
    <el-card class="login-card">
      <template #header>
        <div class="card-header">
          <div>
            <div class="title">12306 登录</div>
            <div class="subtitle">登录后即可创建抢票任务</div>
          </div>
          <el-tabs v-model="loginMode" class="login-tabs">
            <el-tab-pane label="账号密码登录" name="password" />
            <el-tab-pane label="扫码登录" name="qr" />
          </el-tabs>
        </div>
      </template>

      <!-- ==================== 账号密码登录 ==================== -->
      <div v-if="loginMode === 'password'" class="pwd-container">
        <el-form v-if="pwdStep === 'form'" label-position="top" @submit.prevent>
          <el-form-item label="12306 账号">
            <el-input
              v-model="pwdForm.username"
              placeholder="请输入 12306 账号"
              clearable
              autocomplete="username"
            />
          </el-form-item>
          <el-form-item label="密码">
            <el-input
              v-model="pwdForm.password"
              type="password"
              show-password
              placeholder="请输入 12306 密码"
              autocomplete="current-password"
              @keyup.enter="handlePasswordLogin"
            />
          </el-form-item>
          <el-button type="primary" style="width: 100%" :loading="pwdLoading" @click="handlePasswordLogin">
            登 录
          </el-button>
          <p class="form-tips">登录成功后会话自动续期，一段时间不操作也无需重新登录</p>
        </el-form>

        <el-form v-else-if="pwdStep === 'sms'" label-position="top" @submit.prevent>
          <el-alert
            type="warning"
            :closable="false"
            show-icon
            title="12306 要求短信验证"
            :description="pwdMessage"
            class="sms-alert"
          />
          <el-form-item label="证件号后 4 位（用于接收验证码）">
            <div class="sms-row">
              <el-input
                v-model="pwdForm.castNum"
                maxlength="4"
                placeholder="请输入证件号后 4 位"
                @keyup.enter="handleSendSms"
              />
              <el-button
                :loading="smsSending"
                :disabled="smsCountdown > 0"
                @click="handleSendSms"
              >
                {{ smsCountdown > 0 ? smsCountdown + 's' : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          <el-form-item label="短信验证码">
            <el-input
              v-model="pwdForm.smsCode"
              placeholder="请输入短信验证码"
              @keyup.enter="handleSmsSubmit"
            />
          </el-form-item>
          <el-button type="primary" style="width: 100%" :loading="pwdLoading" @click="handleSmsSubmit">
            确认登录
          </el-button>
          <el-button text type="primary" style="width: 100%; margin-top: 4px" @click="backToForm">
            返回重新输入
          </el-button>
        </el-form>

        <el-result v-else-if="pwdStep === 'success'" icon="success" title="登录成功">
          <template #sub-title>
            欢迎，{{ loginUsername }}
          </template>
          <template #extra>
            <el-button type="primary" @click="router.replace('/')">进入系统</el-button>
          </template>
        </el-result>
      </div>

      <!-- ==================== 扫码登录 ==================== -->
      <div v-if="loginMode === 'qr'" class="qrcode-container">
        <template v-if="loginState === 'idle'">
          <el-empty description="准备开始扫码登录">
            <template #image>
              <el-icon size="80" color="#909399"><Iphone /></el-icon>
            </template>
            <el-button type="primary" @click="startLogin">获取二维码</el-button>
          </el-empty>
        </template>

        <template v-else-if="loginState === 'loading'">
          <el-icon class="is-loading" size="60" color="#409EFF">
            <Loading />
          </el-icon>
          <p>{{ statusMessage }}</p>
        </template>

        <template v-else-if="loginState === 'qrcode'">
          <div class="qrcode-wrapper">
            <img :src="'data:image/png;base64,' + qrcodeImage" alt="登录二维码" />
            <div v-if="qrcodeExpired" class="qrcode-expired" @click="refreshQRCode">
              <el-icon size="40"><RefreshRight /></el-icon>
              <span>二维码已过期，点击刷新</span>
            </div>
          </div>
          <p>{{ statusMessage }}</p>
          <p class="tips">请使用 12306 APP 扫描并确认登录</p>
        </template>

        <template v-else-if="loginState === 'success'">
          <el-result icon="success" title="登录成功">
            <template #sub-title>
              欢迎，{{ loginUsername }}
            </template>
            <template #extra>
              <el-button type="primary" @click="router.replace('/')">进入系统</el-button>
            </template>
          </el-result>
        </template>

        <template v-else-if="loginState === 'error'">
          <el-result icon="error" title="登录失败">
            <template #sub-title>
              {{ statusMessage }}
            </template>
            <template #extra>
              <el-button type="primary" @click="refreshQRCode">重试</el-button>
            </template>
          </el-result>
        </template>
      </div>
    </el-card>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '../stores/user'
import api from '../api'

const router = useRouter()
const userStore = useUserStore()

// ==================== 模式切换 ====================
const loginMode = ref('password') // password / qr

// ==================== 账号密码登录状态 ====================
const pwdStep = ref('form') // form / sms / success
const pwdLoading = ref(false)
const smsSending = ref(false)
const smsCountdown = ref(0)
const pwdForm = ref({ username: '', password: '', castNum: '', smsCode: '' })
const pwdMessage = ref('')
const loginUsername = ref('')
let smsTimer = null

// ==================== 扫码登录状态 ====================
const loginState = ref('idle') // idle, loading, qrcode, success, error
const statusMessage = ref('')
const qrcodeImage = ref('')
const challengeId = ref('')
const qrcodeExpired = ref(false)
let pollTimer = null

onMounted(async () => {
  if (userStore.isLoggedIn && userStore.currentUser) {
    router.replace('/')
    return
  }
})

onUnmounted(() => {
  stopPolling()
  if (smsTimer) {
    clearInterval(smsTimer)
    smsTimer = null
  }
})

// 切换到扫码 tab 时自动获取二维码
watch(loginMode, (mode) => {
  if (mode === 'qr' && loginState.value === 'idle') {
    startLogin()
  }
})

// ==================== 账号密码登录 ====================

const handlePasswordLogin = async () => {
  if (!pwdForm.value.username || !pwdForm.value.password) {
    ElMessage.warning('请输入账号和密码')
    return
  }
  pwdLoading.value = true
  try {
    const res = await api.passwordLogin({
      username: pwdForm.value.username.trim(),
      password: pwdForm.value.password
    })
    const data = res.data

    if (res.success && data?.status === 'success' && data.auth) {
      userStore.setAuthSession(data.auth)
      loginUsername.value = data.auth.user?.railway_username || data.auth.user?.username || '用户'
      pwdStep.value = 'success'
      setTimeout(() => {
        router.replace('/')
      }, 600)
    } else if (data?.verification_type === 'sms') {
      pwdStep.value = 'sms'
      pwdMessage.value = data.message || '请输入证件号后 4 位获取短信验证码'
    } else if (data?.verification_type === 'slide') {
      ElMessage.warning(data.message || '12306 要求滑块验证，请使用扫码登录')
      loginMode.value = 'qr'
    } else {
      ElMessage.error(data?.message || res.message || '登录失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    pwdLoading.value = false
  }
}

const handleSendSms = async () => {
  if (!pwdForm.value.castNum || pwdForm.value.castNum.length !== 4) {
    ElMessage.warning('请输入证件号后 4 位')
    return
  }
  smsSending.value = true
  try {
    const res = await api.sendPasswordSmsCode({
      username: pwdForm.value.username.trim(),
      cast_num: pwdForm.value.castNum
    })
    if (res.success) {
      ElMessage.success('验证码已发送，请注意查收短信')
      smsCountdown.value = 60
      if (smsTimer) clearInterval(smsTimer)
      smsTimer = setInterval(() => {
        smsCountdown.value -= 1
        if (smsCountdown.value <= 0) {
          clearInterval(smsTimer)
          smsTimer = null
        }
      }, 1000)
    } else {
      ElMessage.error(res.message || '验证码发送失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '验证码发送失败')
  } finally {
    smsSending.value = false
  }
}

const handleSmsSubmit = async () => {
  if (!pwdForm.value.smsCode) {
    ElMessage.warning('请输入短信验证码')
    return
  }
  pwdLoading.value = true
  try {
    const res = await api.submitPasswordLogin({
      username: pwdForm.value.username.trim(),
      password: pwdForm.value.password,
      verification: { type: 'sms', sms_code: pwdForm.value.smsCode.trim() }
    })
    const data = res.data

    if (res.success && data?.status === 'success' && data.auth) {
      userStore.setAuthSession(data.auth)
      loginUsername.value = data.auth.user?.railway_username || data.auth.user?.username || '用户'
      pwdStep.value = 'success'
      setTimeout(() => {
        router.replace('/')
      }, 600)
    } else {
      ElMessage.error(data?.message || '登录失败')
    }
  } catch (error) {
    ElMessage.error(error.message || '登录失败')
  } finally {
    pwdLoading.value = false
  }
}

const backToForm = () => {
  pwdStep.value = 'form'
  pwdForm.value.castNum = ''
  pwdForm.value.smsCode = ''
}

// ==================== 扫码登录 ====================

const stopPolling = () => {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = null
  }
}

const startLogin = async () => {
  stopPolling()
  loginState.value = 'loading'
  statusMessage.value = '正在获取二维码...'
  qrcodeExpired.value = false
  challengeId.value = ''

  try {
    const res = await api.createLoginQRCode()
    if (!res.success || !res.data) {
      loginState.value = 'error'
      statusMessage.value = res.message || '获取二维码失败'
      return
    }

    challengeId.value = res.data.challenge_id
    qrcodeImage.value = res.data.image_base64
    loginState.value = 'qrcode'
    statusMessage.value = '等待扫码...'
    startPolling()
  } catch (error) {
    loginState.value = 'error'
    statusMessage.value = error.message
  }
}

const startPolling = () => {
  stopPolling()

  pollTimer = setInterval(async () => {
    if (!challengeId.value) {
      stopPolling()
      return
    }

    try {
      const res = await api.checkLoginQRCodeStatus(challengeId.value)
      if (!res.success || !res.data) {
        return
      }

      const data = res.data
      statusMessage.value = data.message

      if (data.status === 1) {
        statusMessage.value = '已扫码，请在手机上确认...'
      } else if (data.status === 2 && data.is_success && data.auth?.access_token) {
        stopPolling()
        userStore.setAuthSession(data.auth)
        loginUsername.value = data.auth.user?.railway_username || data.auth.user?.username || '用户'
        loginState.value = 'success'
        setTimeout(() => {
          router.replace('/')
        }, 600)
      } else if (data.status === 3) {
        stopPolling()
        qrcodeExpired.value = true
        loginState.value = 'qrcode'
        statusMessage.value = '二维码已过期'
      } else if (data.status === 5) {
        stopPolling()
        loginState.value = 'error'
      }
    } catch (error) {
      stopPolling()
      loginState.value = 'error'
      statusMessage.value = error.message || '轮询登录状态失败'
    }
  }, 2000)
}

const refreshQRCode = async () => {
  await startLogin()
}
</script>

<style scoped>
.login-page {
  padding: 0;
  max-width: 720px;
  margin: 0 auto;
}

.login-card {
  min-height: 520px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 12px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #303133;
}

.subtitle {
  margin-top: 6px;
  font-size: 13px;
  color: #909399;
}

.login-tabs {
  --el-tabs-header-margin-bottom: 0;
}

.pwd-container {
  min-height: 420px;
  padding: 32px 48px 24px;
  display: flex;
  flex-direction: column;
  justify-content: center;
}

.form-tips {
  margin-top: 12px;
  font-size: 12px;
  color: #909399;
  text-align: center;
}

.sms-alert {
  margin-bottom: 16px;
}

.sms-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.sms-row .el-input {
  flex: 1;
}

.qrcode-container {
  min-height: 420px;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.qrcode-wrapper {
  position: relative;
  width: 220px;
  height: 220px;
  margin-bottom: 16px;
}

.qrcode-wrapper img {
  width: 100%;
  height: 100%;
}

.qrcode-expired {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.72);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  color: white;
  cursor: pointer;
  gap: 8px;
}

.tips {
  color: #909399;
  font-size: 14px;
}
</style>
