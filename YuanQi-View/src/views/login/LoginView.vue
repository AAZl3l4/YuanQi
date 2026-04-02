<script setup>
import { ref, reactive, onMounted, onUnmounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { login, sendCode } from '@/api/user'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const formRef = ref()
const loading = ref(false)
const codeLoading = ref(false)
const countdown = ref(0)
const showInfoDialog = ref(false)
const showQrFullscreen = ref(false)

const form = reactive({
  email: '',
  password: '',
  verifyCode: ''
})

const rules = {
  email: [
    { required: true, message: '请输入邮箱', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱格式', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  verifyCode: [
    { required: true, message: '请输入验证码', trigger: 'blur' },
    { len: 6, message: '验证码为6位', trigger: 'blur' }
  ]
}

const handleSendCode = async () => {
  if (!form.email) {
    ElMessage.warning('请先输入邮箱')
    return
  }
  
  codeLoading.value = true
  try {
    const res = await sendCode(form.email)
    if (res.code === 200) {
      ElMessage.success('验证码已发送')
      countdown.value = 60
      const timer = setInterval(() => {
        countdown.value--
        if (countdown.value <= 0) {
          clearInterval(timer)
        }
      }, 1000)
    }
  } catch (error) {
    console.error(error)
  } finally {
    codeLoading.value = false
  }
}

const handleLogin = async () => {
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return
  
  loading.value = true
  try {
    const res = await login(form)
    if (res.code === 200) {
      ElMessage.success('登录成功')
      await userStore.fetchUserInfo()
      router.push('/chat')
    }
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 粒子动画
const particles = ref([])
const canvasRef = ref(null)
let animationId = null

const createParticles = () => {
  const particlesArray = []
  for (let i = 0; i < 50; i++) {
    particlesArray.push({
      x: Math.random() * window.innerWidth,
      y: Math.random() * window.innerHeight,
      size: Math.random() * 3 + 1,
      speedX: (Math.random() - 0.5) * 0.5,
      speedY: (Math.random() - 0.5) * 0.5,
      opacity: Math.random() * 0.5 + 0.2
    })
  }
  return particlesArray
}

const animateParticles = () => {
  const canvas = canvasRef.value
  if (!canvas) return
  
  const ctx = canvas.getContext('2d')
  canvas.width = window.innerWidth
  canvas.height = window.innerHeight
  
  const draw = () => {
    ctx.clearRect(0, 0, canvas.width, canvas.height)
    
    particles.value.forEach((particle, index) => {
      particle.x += particle.speedX
      particle.y += particle.speedY
      
      if (particle.x < 0) particle.x = canvas.width
      if (particle.x > canvas.width) particle.x = 0
      if (particle.y < 0) particle.y = canvas.height
      if (particle.y > canvas.height) particle.y = 0
      
      ctx.beginPath()
      ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2)
      ctx.fillStyle = `rgba(201, 169, 98, ${particle.opacity})`
      ctx.fill()
      
      particles.value.slice(index + 1).forEach(otherParticle => {
        const dx = particle.x - otherParticle.x
        const dy = particle.y - otherParticle.y
        const distance = Math.sqrt(dx * dx + dy * dy)
        
        if (distance < 150) {
          ctx.beginPath()
          ctx.moveTo(particle.x, particle.y)
          ctx.lineTo(otherParticle.x, otherParticle.y)
          ctx.strokeStyle = `rgba(201, 169, 98, ${0.1 * (1 - distance / 150)})`
          ctx.stroke()
        }
      })
    })
    
    animationId = requestAnimationFrame(draw)
  }
  
  draw()
}

// 文字显示效果
const showText = ref(false)

onMounted(() => {
  particles.value = createParticles()
  animateParticles()
  
  setTimeout(() => {
    showText.value = true
  }, 300)
  
  // 显示说明弹窗
  setTimeout(() => {
    showInfoDialog.value = true
  }, 800)
  
  const lines = document.querySelectorAll('.poem-line')
  lines.forEach((line, index) => {
    line.style.opacity = '0'
    line.style.transform = 'translateX(-20px)'
    setTimeout(() => {
      line.style.transition = 'all 0.6s ease'
      line.style.opacity = '1'
      line.style.transform = 'translateX(0)'
    }, 600 + index * 200)
  })
})

onUnmounted(() => {
  if (animationId) {
    cancelAnimationFrame(animationId)
  }
})
</script>

<template>
  <div class="login-page">
    <!-- 粒子画布 -->
    <canvas ref="canvasRef" class="particle-canvas"></canvas>
    
    <!-- 左侧艺术字区域 -->
    <div class="brand-section">
      <div class="brand-content" :class="{ show: showText }">
        <div class="brand-title">
          <span class="title-char">元</span>
          <span class="title-char">启</span>
        </div>
        
        <div class="brand-poem">
          <p class="poem-line indent">元，始也。启，开也。</p>
          <p class="poem-line">于混沌初开之际，执代码为斧，劈智能之蒙昧。</p>
          <p class="poem-line">非巨构之塔，乃一人之庐</p>
          <p class="poem-line">Spring为梁，AI为火，亲手锻打每一行。</p>
          <p class="poem-line">流式如溪，RAG如仓，Agent如仆，皆从此出。</p>
          <p class="poem-line">练手即问道，小成亦星辰。</p>
          <p class="poem-line">此平台不立，何以启未来之元？</p>
        </div>
        
        <div class="brand-divider"></div>
        
        <div class="brand-footer">
          <span class="footer-text">以代码为笔，书写智能新篇</span>
        </div>
      </div>
      
      <!-- 装饰元素 -->
      <div class="decoration">
        <div class="glow glow-1"></div>
        <div class="glow glow-2"></div>
        <div class="float-element float-1"></div>
        <div class="float-element float-2"></div>
        <div class="float-element float-3"></div>
      </div>
    </div>
    
    <!-- 右侧登录表单区域 -->
    <div class="form-section">
      <div class="form-container" :class="{ show: showText }">
        <div class="form-header">
          <div class="logo-wrapper">
            <img src="/logo.png" alt="元启" class="logo" />
            <div class="logo-glow"></div>
          </div>
          <h1 class="title">欢迎回来</h1>
          <p class="subtitle">登录您的账号，继续探索</p>
        </div>
        
        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          class="login-form"
          @submit.prevent="handleLogin"
        >
          <el-form-item prop="email">
            <el-input
              v-model="form.email"
              placeholder="请输入邮箱"
              prefix-icon="Message"
              size="large"
            />
          </el-form-item>
          
          <el-form-item prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              prefix-icon="Lock"
              size="large"
              show-password
            />
          </el-form-item>
          
          <el-form-item prop="verifyCode">
            <div class="code-input">
              <el-input
                v-model="form.verifyCode"
                placeholder="请输入验证码"
                prefix-icon="Key"
                size="large"
                maxlength="6"
              />
              <el-button
                type="primary"
                size="large"
                :loading="codeLoading"
                :disabled="countdown > 0"
                @click="handleSendCode"
              >
                {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
              </el-button>
            </div>
          </el-form-item>
          
          <el-form-item>
            <el-button
              type="primary"
              size="large"
              class="login-btn"
              :loading="loading"
              @click="handleLogin"
            >
              <span class="btn-text">登录</span>
              <span class="btn-shine"></span>
            </el-button>
          </el-form-item>
          
          <div class="form-footer">
            <span>还没有账号？</span>
            <router-link to="/register">立即注册</router-link>
          </div>
        </el-form>
      </div>
    </div>
    
    <!-- 说明弹窗 -->
    <el-dialog
      v-model="showInfoDialog"
      title=""
      width="560px"
      class="info-dialog"
      :close-on-click-modal="true"
      :close-on-press-escape="true"
      align-center
    >
      <div class="dialog-content">
        <div class="dialog-header">
          <div class="header-icon">
            <el-icon :size="32"><InfoFilled /></el-icon>
          </div>
          <h3 class="header-title">使用须知</h3>
        </div>
        
        <div class="info-list">
          <div class="info-item highlight">
            <div class="info-icon script">
              <el-icon><Document /></el-icon>
            </div>
            <div class="info-text">
              <strong>QQ脚本用户使用流程</strong>
              <span>登录账户 → 创建中转配置(或使用公开的) → 创建平台APIKEY → 在脚本中配置APIKEY</span>
            </div>
          </div>
          
          <div class="info-item">
            <div class="info-icon free">
              <el-icon><Present /></el-icon>
            </div>
            <div class="info-text">
              <strong>本平台完全免费</strong>
              <span>所有功能无需付费即可使用</span>
            </div>
          </div>
          
          <div class="info-item">
            <div class="info-icon contact">
              <el-icon><Service /></el-icon>
            </div>
            <div class="info-text">
              <strong>联系方式</strong>
              <span>邮箱：AAZl3l4@111.com / QQ：AAZl3l4</span>
              <a href="http://qm.qq.com/cgi-bin/qm/qr?_wv=1027&k=lYK91yPoerwu2pXIWYAxra4o43ScIGAR&authKey=xLvt4Ryo%2BW%2FPSb1GlR4xzsWi5fnk7Cv5iHK%2Fb4qInI0mKLM611HrOrEQvtED7k3l&noverify=0&group_code=883640898" target="_blank">加入QQ交流群</a>
            </div>
          </div>
        </div>
        
        <div class="sponsor-section">
          <div class="sponsor-title">
            <el-icon><Coffee /></el-icon>
            <span>支持开发者</span>
          </div>
          <p class="sponsor-desc">您的支持是我持续维护的动力 ❤️</p>
          <div class="qr-wrapper" @click="showQrFullscreen = true">
            <img src="/ma.png" alt="打赏二维码" class="qr-code" />
            <span class="qr-label">点击放大查看</span>
          </div>
        </div>
      </div>
    </el-dialog>
    
    <!-- 二维码全屏弹窗 -->
    <el-dialog
      v-model="showQrFullscreen"
      title=""
      width="400px"
      class="qr-dialog"
      :close-on-click-modal="true"
      align-center
    >
      <div class="qr-fullscreen-content">
        <img src="/ma.png" alt="打赏二维码" class="qr-fullscreen" />
        <p class="qr-tip">感谢支持 ❤️</p>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: flex;
  background: linear-gradient(135deg, #0a0a0f 0%, #1a1a2e 50%, #0f0f1a 100%);
  position: relative;
  overflow: hidden;
}

/* 粒子画布 */
.particle-canvas {
  position: absolute;
  inset: 0;
  pointer-events: none;
  z-index: 1;
}

/* 左侧品牌区域 */
.brand-section {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  padding: var(--spacing-2xl);
  z-index: 2;
}

.brand-content {
  position: relative;
  z-index: 2;
  max-width: 560px;
  opacity: 0;
  transform: translateY(30px);
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1);
}

.brand-content.show {
  opacity: 1;
  transform: translateY(0);
}

.brand-title {
  display: flex;
  gap: var(--spacing-xl);
  margin-bottom: var(--spacing-2xl);
}

.title-char {
  font-size: 140px;
  font-weight: 700;
  line-height: 1;
  background: linear-gradient(135deg, #C9A962 0%, #E8D5A3 30%, #C9A962 50%, #E8D5A3 70%, #C9A962 100%);
  background-size: 200% 200%;
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  animation: shimmer 3s ease-in-out infinite, float 4s ease-in-out infinite;
  text-shadow: 0 0 80px rgba(201, 169, 98, 0.4);
  position: relative;
}

.title-char::after {
  content: '';
  position: absolute;
  inset: -20px;
  background: radial-gradient(circle, rgba(201, 169, 98, 0.15) 0%, transparent 70%);
  z-index: -1;
  animation: pulse-glow 2s ease-in-out infinite;
}

.title-char:last-child {
  animation-delay: 0.3s;
}

@keyframes shimmer {
  0%, 100% { background-position: 0% 50%; }
  50% { background-position: 100% 50%; }
}

@keyframes float {
  0%, 100% { transform: translateY(0) rotate(0deg); }
  25% { transform: translateY(-8px) rotate(1deg); }
  75% { transform: translateY(-4px) rotate(-1deg); }
}

@keyframes pulse-glow {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.1); }
}

.brand-poem {
  margin-bottom: var(--spacing-2xl);
}

.poem-line {
  font-size: 18px;
  line-height: 2.4;
  color: rgba(255, 255, 255, 0.8);
  letter-spacing: 3px;
  margin: 0;
  position: relative;
  padding-left: 0;
  transition: all 0.3s ease;
}

.poem-line:hover {
  color: #E8D5A3;
  letter-spacing: 5px;
}

.poem-line.indent {
  padding-left: 2em;
  font-size: 22px;
  color: #C9A962;
  font-weight: 500;
  margin-bottom: var(--spacing-md);
  text-shadow: 0 0 20px rgba(201, 169, 98, 0.3);
}

.brand-divider {
  width: 100px;
  height: 2px;
  background: linear-gradient(90deg, transparent, #C9A962, transparent);
  margin-bottom: var(--spacing-xl);
  animation: expand-width 1.5s ease-out forwards;
}

@keyframes expand-width {
  from { width: 0; }
  to { width: 100px; }
}

.brand-footer {
  margin-top: var(--spacing-lg);
}

.footer-text {
  font-size: 14px;
  color: rgba(255, 255, 255, 0.4);
  letter-spacing: 6px;
  animation: fade-in 1s ease-out 1.5s forwards;
  opacity: 0;
}

@keyframes fade-in {
  to { opacity: 1; }
}

/* 装饰元素 */
.decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
  overflow: hidden;
}

.glow {
  position: absolute;
  border-radius: 50%;
  filter: blur(80px);
  opacity: 0.3;
}

.glow-1 {
  width: 400px;
  height: 400px;
  background: radial-gradient(circle, #C9A962 0%, transparent 70%);
  top: -100px;
  left: -100px;
  animation: float-glow 8s ease-in-out infinite;
}

.glow-2 {
  width: 300px;
  height: 300px;
  background: radial-gradient(circle, #E8D5A3 0%, transparent 70%);
  bottom: -50px;
  right: 20%;
  animation: float-glow 10s ease-in-out infinite reverse;
}

@keyframes float-glow {
  0%, 100% { transform: translate(0, 0) scale(1); }
  33% { transform: translate(30px, -30px) scale(1.1); }
  66% { transform: translate(-20px, 20px) scale(0.9); }
}

.float-element {
  position: absolute;
  border: 1px solid rgba(201, 169, 98, 0.2);
  border-radius: 50%;
}

.float-1 {
  width: 60px;
  height: 60px;
  top: 20%;
  left: 10%;
  animation: orbit 20s linear infinite;
}

.float-2 {
  width: 40px;
  height: 40px;
  top: 60%;
  right: 15%;
  animation: orbit 15s linear infinite reverse;
}

.float-3 {
  width: 80px;
  height: 80px;
  bottom: 20%;
  left: 20%;
  animation: orbit 25s linear infinite;
}

@keyframes orbit {
  from { transform: rotate(0deg) translateX(20px) rotate(0deg); }
  to { transform: rotate(360deg) translateX(20px) rotate(-360deg); }
}

/* 右侧表单区域 */
.form-section {
  width: 500px;
  min-width: 500px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(255, 255, 255, 0.98);
  padding: var(--spacing-2xl);
  position: relative;
  z-index: 2;
}

.form-section::before {
  content: '';
  position: absolute;
  left: 0;
  top: 0;
  bottom: 0;
  width: 1px;
  background: linear-gradient(180deg, transparent, rgba(201, 169, 98, 0.3), transparent);
}

.form-container {
  width: 100%;
  max-width: 380px;
  opacity: 0;
  transform: translateX(30px);
  transition: all 0.8s cubic-bezier(0.4, 0, 0.2, 1) 0.3s;
}

.form-container.show {
  opacity: 1;
  transform: translateX(0);
}

.form-header {
  text-align: center;
  margin-bottom: var(--spacing-2xl);
}

.logo-wrapper {
  position: relative;
  display: inline-block;
  margin-bottom: var(--spacing-lg);
}

.logo {
  width: 80px;
  height: 80px;
  border-radius: var(--radius-lg);
  position: relative;
  z-index: 1;
  transition: transform 0.3s ease;
}

.logo:hover {
  transform: scale(1.05) rotate(5deg);
}

.logo-glow {
  position: absolute;
  inset: -10px;
  background: radial-gradient(circle, rgba(201, 169, 98, 0.3) 0%, transparent 70%);
  border-radius: var(--radius-xl);
  animation: logo-pulse 2s ease-in-out infinite;
}

@keyframes logo-pulse {
  0%, 100% { opacity: 0.5; transform: scale(1); }
  50% { opacity: 1; transform: scale(1.1); }
}

.title {
  font-size: 32px;
  font-weight: 600;
  color: var(--color-text);
  margin-bottom: var(--spacing-xs);
  background: linear-gradient(135deg, var(--color-text) 0%, var(--color-primary-dark) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: var(--spacing-md);
}

.login-form :deep(.el-input__wrapper) {
  transition: all 0.3s ease;
}

.login-form :deep(.el-input__wrapper:focus-within) {
  box-shadow: 0 0 0 2px rgba(201, 169, 98, 0.2);
}

.code-input {
  display: flex;
  gap: var(--spacing-sm);
  width: 100%;
}

.code-input .el-input {
  flex: 1;
}

.login-btn {
  width: 100%;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  border: none;
  height: 52px;
  font-size: var(--font-size-lg);
  margin-top: var(--spacing-sm);
  position: relative;
  overflow: hidden;
  transition: all 0.3s ease;
}

.login-btn:hover {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(201, 169, 98, 0.4);
}

.login-btn:active {
  transform: translateY(0);
}

.btn-shine {
  position: absolute;
  top: 0;
  left: -100%;
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, rgba(255, 255, 255, 0.3), transparent);
  animation: shine 3s ease-in-out infinite;
}

@keyframes shine {
  0%, 100% { left: -100%; }
  50% { left: 100%; }
}

.form-footer {
  text-align: center;
  font-size: var(--font-size-sm);
  color: var(--color-text-muted);
  margin-top: var(--spacing-md);
}

.form-footer a {
  color: var(--color-primary);
  margin-left: var(--spacing-xs);
  text-decoration: none;
  font-weight: 500;
  position: relative;
}

.form-footer a::after {
  content: '';
  position: absolute;
  bottom: -2px;
  left: 0;
  width: 0;
  height: 2px;
  background: var(--color-primary);
  transition: width 0.3s ease;
}

.form-footer a:hover::after {
  width: 100%;
}

/* 弹窗样式 */
.info-dialog :deep(.el-dialog) {
  background: #ffffff;
  border-radius: 16px;
  border: 1px solid rgba(201, 169, 98, 0.3);
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.3);
}

.info-dialog :deep(.el-dialog__header) {
  display: none;
}

.info-dialog :deep(.el-dialog__body) {
  padding: 0;
}

.dialog-content {
  padding: 32px;
}

.dialog-header {
  text-align: center;
  margin-bottom: 28px;
}

.header-icon {
  width: 64px;
  height: 64px;
  background: linear-gradient(135deg, #C9A962 0%, #E8D5A3 100%);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 0 auto 16px;
  color: #1a1a2e;
}

.header-title {
  font-size: 24px;
  font-weight: 600;
  color: #C9A962;
  margin: 0;
}

.info-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-bottom: 28px;
}

.info-item {
  display: flex;
  align-items: flex-start;
  gap: 14px;
  padding: 14px;
  background: #f8f9fa;
  border-radius: 12px;
  border: 1px solid rgba(201, 169, 98, 0.15);
  transition: all 0.3s ease;
}

.info-item:hover {
  background: #f0f1f2;
  border-color: rgba(201, 169, 98, 0.3);
}

.info-item.highlight {
  background: rgba(201, 169, 98, 0.1);
  border-color: rgba(201, 169, 98, 0.4);
}

.info-item.highlight:hover {
  background: rgba(201, 169, 98, 0.15);
}

.info-icon {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  font-size: 18px;
}

.info-icon.script {
  background: rgba(201, 169, 98, 0.2);
  color: #C9A962;
}

.info-icon.free {
  background: rgba(103, 194, 58, 0.15);
  color: #67C23A;
}

.info-icon.contact {
  background: rgba(230, 162, 60, 0.15);
  color: #E6A23C;
}

.info-text {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.info-text strong {
  color: #1a1a2e;
  font-size: 16px;
  font-weight: 600;
}

.info-text span {
  color: #4a4a4a;
  font-size: 14px;
  line-height: 1.6;
}

.sponsor-section {
  text-align: center;
  padding-top: 20px;
  border-top: 1px solid rgba(201, 169, 98, 0.2);
}

.sponsor-title {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #C9A962;
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 8px;
}

.sponsor-desc {
  color: #666666;
  font-size: 14px;
  margin: 0 0 16px;
}

.qr-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 12px;
  border-radius: 12px;
  transition: all 0.3s ease;
}

.qr-wrapper:hover {
  background: rgba(201, 169, 98, 0.1);
}

.qr-code {
  width: 180px;
  height: 180px;
  border-radius: 12px;
  border: 3px solid rgba(201, 169, 98, 0.4);
  padding: 10px;
  background: #fff;
  transition: all 0.3s ease;
}

.qr-wrapper:hover .qr-code {
  border-color: #C9A962;
  transform: scale(1.02);
}

.qr-label {
  color: #888888;
  font-size: 13px;
}

/* 二维码全屏弹窗 */
.qr-dialog :deep(.el-dialog) {
  background: linear-gradient(135deg, #1a1a2e 0%, #0f0f1a 100%);
  border-radius: 20px;
  border: 2px solid rgba(201, 169, 98, 0.3);
  box-shadow: 0 30px 80px rgba(0, 0, 0, 0.6);
}

.qr-dialog :deep(.el-dialog__header) {
  display: none;
}

.qr-dialog :deep(.el-dialog__body) {
  padding: 40px;
}

.qr-fullscreen-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 20px;
}

.qr-fullscreen {
  width: 320px;
  height: 320px;
  border-radius: 16px;
  border: 4px solid rgba(201, 169, 98, 0.5);
  padding: 12px;
  background: #fff;
}

.qr-tip {
  color: #E8D5A3;
  font-size: 18px;
  font-weight: 500;
  margin: 0;
}

/* 响应式 */
@media (max-width: 1024px) {
  .brand-section {
    display: none;
  }
  
  .form-section {
    width: 100%;
    min-width: auto;
  }
}

@media (max-width: 480px) {
  .form-section {
    padding: var(--spacing-lg);
  }
  
  .form-container {
    max-width: 100%;
  }
  
  .dialog-content {
    padding: 20px;
  }
  
  .qr-code {
    width: 120px;
    height: 120px;
  }
}
</style>
