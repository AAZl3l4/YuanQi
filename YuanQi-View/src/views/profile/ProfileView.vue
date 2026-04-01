<script setup>
import { ref, onMounted } from 'vue'
import { useUserStore } from '@/stores/user'
import { updateUserInfo } from '@/api/user'
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
  videoModel: '',
  password: '',
  confirmPassword: ''
})

const loading = ref(false)
const avatarLoading = ref(false)

const loadUserInfo = () => {
  if (userStore.user) {
    form.value = { ...userStore.user }
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
  // 如果填写了密码，验证两次密码是否一致
  if (form.value.password && form.value.password !== form.value.confirmPassword) {
    ElMessage.warning('两次密码不一致')
    return
  }
  
  loading.value = true
  try {
    // 构建提交数据
    const submitData = { ...form.value }
    // 如果没有填写密码，删除密码字段
    if (!submitData.password) {
      delete submitData.password
      delete submitData.confirmPassword
    } else {
      delete submitData.confirmPassword
    }
    
    const res = await updateUserInfo(submitData)
    if (res.code === 200) {
      // 清空密码字段
      form.value.password = ''
      form.value.confirmPassword = ''
      
      userStore.updateUser(submitData)
      ElMessage.success('保存成功')
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
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
        
        <el-divider content-position="left">修改密码</el-divider>
        
        <el-form-item label="新密码">
          <el-input v-model="form.password" type="password" placeholder="不修改请留空" show-password />
        </el-form-item>
        
        <el-form-item label="确认密码">
          <el-input v-model="form.confirmPassword" type="password" placeholder="不修改请留空" show-password />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" :loading="loading" @click="handleSave">
            保存修改
          </el-button>
        </el-form-item>
      </el-form>
    </div>
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
</style>
