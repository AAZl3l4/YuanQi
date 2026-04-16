<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfo, changePassword, sendCode, getCaptcha } from '@/api/user'
import { uploadFile } from '@/api/file'
import { ElMessage } from 'element-plus'

const userStore = useUserStore()

const form = ref({
  username: '',
  avatar: '',
  apiKey: '',
  chatModel: '',
  chatVisionModel: '',
  imageModel: '',
  videoModel: ''
})

const loading = ref(false)
const avatarLoading = ref(false)

// 修改密码弹窗
const showPasswordDialog = ref(false)
const passwordLoading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)

// 图片验证码弹窗
const showCaptchaDialog = ref(false)
const captchaId = ref('')
const captchaImage = ref('')
const captchaAnswer = ref('')

const passwordForm = ref({
  email: '',
  password: '',
  confirmPassword: '',
  verifyCode: '',
  captchaId: '',
  captchaAnswer: ''
})

const loadUserInfo = () => {
  if (userStore.user) {
    form.value = { ...userStore.user }
    passwordForm.value.email = userStore.user.email
  }
}

const ALLOWED_IMAGE_TYPES = ['image/jpeg', 'image/png', 'image/jpg']

const handleAvatarUpload = async (file) => {
  if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
    ElMessage.warning('图片格式不支持，仅支持 jpg、jpeg、png 格式')
    return false
  }
  
  avatarLoading.value = true
  try {
    const res = await uploadFile(file)
    if (typeof res === 'string') {
      form.value.avatar = res
      ElMessage.success('上传成功')
    }
  } catch (error) {
    ElMessage.error('上传失败')
  } finally {
    avatarLoading.value = false
  }
  return false
}

const handleSave = async () => {
  loading.value = true
  try {
    const res = await updateUserInfo(form.value)
    if (res.code === 200) {
      userStore.updateUser(form.value)
      ElMessage.success('保存成功')
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 获取图片验证码
const refreshCaptcha = async () => {
  try {
    const res = await getCaptcha()
    if (res.code === 200) {
      captchaId.value = res.data.captchaId
      captchaImage.value = res.data.captchaImage
    }
  } catch (error) {
    console.error(error)
  }
}

// 打开修改密码弹窗
const openPasswordDialog = () => {
  passwordForm.value = {
    email: userStore.user?.email || '',
    password: '',
    confirmPassword: '',
    verifyCode: '',
    captchaId: '',
    captchaAnswer: ''
  }
  showPasswordDialog.value = true
}

// 打开图片验证码弹窗
const openCaptchaDialog = () => {
  captchaAnswer.value = ''
  refreshCaptcha()
  showCaptchaDialog.value = true
}

// 确认图片验证码并发送邮件验证码
const confirmCaptchaAndSendCode = async () => {
  if (!captchaAnswer.value) {
    ElMessage.warning('请输入验证码')
    return
  }
  
  codeLoading.value = true
  try {
    const res = await sendCode({
      email: passwordForm.value.email,
      captchaId: captchaId.value,
      captchaAnswer: captchaAnswer.value
    })
    if (res.code === 200) {
      ElMessage.success('验证码发送中，请稍候')
      showCaptchaDialog.value = false
      // 立即开始倒计时
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    }
  } catch (error) {
    refreshCaptcha()
    captchaAnswer.value = ''
    console.error(error)
  } finally {
    codeLoading.value = false
  }
}

// 修改密码
const handleChangePassword = async () => {
  if (!passwordForm.value.password) {
    ElMessage.warning('请输入新密码')
    return
  }
  if (passwordForm.value.password !== passwordForm.value.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  if (!passwordForm.value.verifyCode) {
    ElMessage.warning('请输入邮箱验证码')
    return
  }
  
  passwordLoading.value = true
  try {
    const res = await changePassword({
      password: passwordForm.value.password,
      verifyCode: passwordForm.value.verifyCode
    })
    if (res.code === 200) {
      ElMessage.success('密码修改成功')
      showPasswordDialog.value = false
    }
  } catch (error) {
    console.error(error)
  } finally {
    passwordLoading.value = false
  }
}

onMounted(() => {
  loadUserInfo()
})
</script>

<template>
  <div class="profile-view page-container">
    <h2 class="page-title">个人中心</h2>
    
    <div class="profile-card card">
      <div class="avatar-section">
        <el-avatar :size="100" :src="form.avatar" class="avatar">
          {{ form.username?.charAt(0) }}
        </el-avatar>
        <el-upload
          :show-file-list="false"
          :before-upload="handleAvatarUpload"
          accept=".jpg,.jpeg,.png"
        >
          <el-button :loading="avatarLoading" size="small">更换头像</el-button>
        </el-upload>
      </div>
      
      <el-form :model="form" label-width="100px" class="profile-form">
        <el-form-item label="用户名">
          <el-input v-model="form.username" placeholder="请输入用户名" />
        </el-form-item>
        
        <el-form-item label="邮箱">
          <el-input :value="userStore.user?.email" disabled />
        </el-form-item>
        
        <el-divider content-position="left">模型配置</el-divider>
        
        <el-form-item label="API Key">
          <el-input v-model="form.apiKey" placeholder="智谱AI API Key" show-password />
        </el-form-item>
        
        <el-form-item label="聊天模型">
          <el-input v-model="form.chatModel" placeholder="如：glm-4.7-flash" />
        </el-form-item>
        
        <el-form-item label="视觉模型">
          <el-input v-model="form.chatVisionModel" placeholder="如：glm-4v-flash" />
        </el-form-item>
        
        <el-form-item label="生图模型">
          <el-input v-model="form.imageModel" placeholder="如：cogview-3-flash" />
        </el-form-item>
        
        <el-form-item label="生视频模型">
          <el-input v-model="form.videoModel" placeholder="如：cogvideox-flash" />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSave">
            保存修改
          </el-button>
          <el-button @click="openPasswordDialog">
            修改密码
          </el-button>
        </el-form-item>
      </el-form>
    </div>
    
    <!-- 修改密码弹窗 -->
    <el-dialog
      v-model="showPasswordDialog"
      title="修改密码"
      width="420px"
      :close-on-click-modal="false"
    >
      <el-form :model="passwordForm" label-width="100px">
        <el-form-item label="邮箱">
          <el-input v-model="passwordForm.email" disabled />
        </el-form-item>
        
        <el-form-item label="邮箱验证码">
          <div class="code-input">
            <el-input
              v-model="passwordForm.verifyCode"
              placeholder="请输入验证码"
              maxlength="6"
            />
            <el-button
              type="primary"
              :loading="codeLoading"
              :disabled="countdown > 0"
              @click="openCaptchaDialog"
            >
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </el-button>
          </div>
        </el-form-item>
        
        <el-form-item label="新密码">
          <el-input
            v-model="passwordForm.password"
            type="password"
            placeholder="请输入新密码"
            show-password
          />
        </el-form-item>
        
        <el-form-item label="确认密码">
          <el-input
            v-model="passwordForm.confirmPassword"
            type="password"
            placeholder="请确认新密码"
            show-password
          />
        </el-form-item>
      </el-form>
      
      <template #footer>
        <el-button @click="showPasswordDialog = false">取消</el-button>
        <el-button type="primary" :loading="passwordLoading" @click="handleChangePassword">
          确认修改
        </el-button>
      </template>
    </el-dialog>
    
    <!-- 图片验证码弹窗 -->
    <el-dialog
      v-model="showCaptchaDialog"
      title="安全验证"
      width="360px"
      :close-on-click-modal="false"
      align-center
      class="captcha-dialog"
    >
      <div class="captcha-content">
        <div class="captcha-image-wrapper" @click="refreshCaptcha">
          <img v-if="captchaImage" :src="captchaImage" alt="验证码" class="captcha-img" />
          <span v-else class="captcha-loading">加载中...</span>
        </div>
        <p class="captcha-tip">请计算图中算式的结果</p>
        <el-input
          v-model="captchaAnswer"
          placeholder="请输入计算结果"
          size="large"
          maxlength="10"
          @keyup.enter="confirmCaptchaAndSendCode"
        />
      </div>
      <template #footer>
        <div class="dialog-footer">
          <el-button @click="showCaptchaDialog = false">取消</el-button>
          <el-button type="primary" :loading="codeLoading" @click="confirmCaptchaAndSendCode">
            确认
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.profile-view {
  max-width: 800px;
}

.profile-card {
  padding: var(--spacing-xl);
}

.avatar-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
  margin-bottom: var(--spacing-xl);
}

.avatar {
  background: var(--color-primary);
  color: var(--color-white);
  font-size: var(--font-size-2xl);
}

.profile-form {
  max-width: 500px;
}

.code-input {
  display: flex;
  gap: var(--spacing-sm);
  width: 100%;
}

.code-input .el-input {
  flex: 1;
}

.captcha-dialog .captcha-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: var(--spacing-md);
}

.captcha-image-wrapper {
  width: 200px;
  height: 60px;
  border: 1px solid var(--color-border);
  border-radius: var(--radius-md);
  overflow: hidden;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f5f5;
  transition: all 0.3s ease;
}

.captcha-image-wrapper:hover {
  border-color: var(--color-primary);
}

.captcha-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.captcha-loading {
  font-size: 14px;
  color: var(--color-text-muted);
}

.captcha-tip {
  font-size: 13px;
  color: var(--color-text-muted);
  margin: 0;
}

.captcha-dialog .el-input {
  width: 200px;
}

.dialog-footer {
  display: flex;
  justify-content: center;
  gap: var(--spacing-md);
}
</style>
