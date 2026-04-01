<script setup>
import { useRouter } from 'vue-router'

const router = useRouter()

const goHome = () => {
  router.push('/')
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="not-found">
    <div class="content">
      <div class="error-code">404</div>
      <div class="error-title">页面走丢了</div>
      <div class="quote">
        "少年一贯快马扬帆，道阻且长不转弯。"
      </div>
      <div class="actions">
        <el-button type="primary" @click="goHome">
          <el-icon><HomeFilled /></el-icon>
          返回首页
        </el-button>
        <el-button @click="goBack">
          <el-icon><Back /></el-icon>
          返回上页
        </el-button>
      </div>
    </div>
    <div class="decoration">
      <div class="circle circle-1"></div>
      <div class="circle circle-2"></div>
      <div class="circle circle-3"></div>
    </div>
  </div>
</template>

<style scoped>
.not-found {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, var(--color-bg) 0%, var(--color-bg-secondary) 100%);
  position: relative;
  overflow: hidden;
}

.content {
  text-align: center;
  z-index: 1;
  padding: var(--spacing-xl);
}

.error-code {
  font-size: 160px;
  font-weight: 700;
  line-height: 1;
  background: linear-gradient(135deg, var(--color-primary) 0%, var(--color-primary-dark) 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
  margin-bottom: var(--spacing-md);
  text-shadow: 0 20px 40px rgba(0, 0, 0, 0.1);
}

.error-title {
  font-size: var(--font-size-2xl);
  color: var(--color-text);
  margin-bottom: var(--spacing-lg);
  font-weight: 500;
}

.quote {
  font-size: var(--font-size-lg);
  color: var(--color-text-secondary);
  margin-bottom: var(--spacing-xl);
  font-style: italic;
  position: relative;
  padding: 0 var(--spacing-xl);
}

.quote::before,
.quote::after {
  content: '';
  position: absolute;
  width: 30px;
  height: 2px;
  background: var(--color-primary-light);
  top: 50%;
}

.quote::before {
  left: 0;
}

.quote::after {
  right: 0;
}

.actions {
  display: flex;
  gap: var(--spacing-md);
  justify-content: center;
}

.decoration {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.circle {
  position: absolute;
  border-radius: 50%;
  opacity: 0.1;
}

.circle-1 {
  width: 400px;
  height: 400px;
  background: var(--color-primary);
  top: -100px;
  right: -100px;
}

.circle-2 {
  width: 300px;
  height: 300px;
  background: var(--color-primary-dark);
  bottom: -50px;
  left: -50px;
}

.circle-3 {
  width: 200px;
  height: 200px;
  background: var(--color-primary);
  top: 50%;
  left: 10%;
  transform: translateY(-50%);
}

@media (max-width: 768px) {
  .error-code {
    font-size: 100px;
  }
  
  .error-title {
    font-size: var(--font-size-xl);
  }
  
  .quote {
    font-size: var(--font-size-base);
    padding: 0 var(--spacing-md);
  }
  
  .actions {
    flex-direction: column;
    align-items: center;
  }
  
  .actions .el-button {
    width: 100%;
    max-width: 200px;
  }
}
</style>
