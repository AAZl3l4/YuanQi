<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { storeToRefs } from 'pinia'
import { usePreviewStore } from '@/stores/preview'
import { ElMessage } from 'element-plus'

const previewStore = usePreviewStore()
const { visible, htmlContent } = storeToRefs(previewStore)

const isPickingElement = ref(false)
const iframeRef = ref(null)

const isResizing = ref(false)
const panelWidth = ref(0) // 0 means use default CSS (50%)

const startResize = (e) => {
  isResizing.value = true
  if (panelWidth.value === 0) {
    const rect = document.querySelector('.preview-panel').getBoundingClientRect()
    panelWidth.value = rect.width
  }
  
  const startX = e.clientX
  const startWidth = panelWidth.value

  const handleMouseMove = (mouseEvent) => {
    const deltaX = startX - mouseEvent.clientX
    let newWidth = startWidth + deltaX
    // Min width 320px, Max width screen - 100px
    newWidth = Math.max(320, Math.min(newWidth, window.innerWidth - 100))
    panelWidth.value = newWidth
  }

  const handleMouseUp = () => {
    isResizing.value = false
    document.removeEventListener('mousemove', handleMouseMove)
    document.removeEventListener('mouseup', handleMouseUp)
  }

  document.addEventListener('mousemove', handleMouseMove)
  document.addEventListener('mouseup', handleMouseUp)
}

// 注入到 iframe 内的“元素审查”高亮脚本
const inspectorScript = `
  <style>
    .inspector-highlight {
        outline: 2px solid #1677ff !important;
        background: rgba(22, 119, 255, 0.1) !important;
        cursor: crosshair !important;
        transition: outline 0.1s ease, background 0.1s ease;
    }
  </style>
  <script>
    (function() {
      if (window.__inspector_injected) return;
      window.__inspector_injected = true;
      
      let currentEl = null;
      let picking = false;
      
      const mouseOverHandler = (e) => {
        if(!picking) return;
        e.stopPropagation();
        if (currentEl) currentEl.classList.remove('inspector-highlight');
        currentEl = e.target;
        currentEl.classList.add('inspector-highlight');
      };
      
      const mouseOutHandler = (e) => {
        if(!picking) return;
        if (currentEl) {
          currentEl.classList.remove('inspector-highlight');
          currentEl = null;
        }
      };
      
      const clickHandler = (e) => {
        if(!picking) return;
        e.preventDefault();
        e.stopPropagation();
        if (currentEl) {
          currentEl.classList.remove('inspector-highlight');
          const elementHtml = currentEl.outerHTML;
          window.parent.postMessage({ type: 'ELEMENT_PICKED', html: elementHtml }, '*');
        }
      };

      window.addEventListener('message', (e) => {
        if(e.data && e.data.type === 'START_PICKING') {
           picking = true;
        }
        if(e.data && e.data.type === 'STOP_PICKING') {
           picking = false;
           if (currentEl) currentEl.classList.remove('inspector-highlight');
        }
      });
      
      document.addEventListener('mouseover', mouseOverHandler, true);
      document.addEventListener('mouseout', mouseOutHandler, true);
      document.addEventListener('click', clickHandler, true);
    })();
  <\/script>
`

const processedHtmlContent = computed(() => {
  if (!htmlContent.value) return ''
  return htmlContent.value + inspectorScript
})

const handleMessage = (e) => {
  if (e.data && e.data.type === 'ELEMENT_PICKED') {
    previewStore.setPickedElement(e.data.html)
    ElMessage.success('元素选取成功！已提取至会话框')
    // 选取完成后，保持面板打开，但退出选取模式
    togglePicking(false)
  }
}

onMounted(() => {
  window.addEventListener('message', handleMessage)
})

onUnmounted(() => {
  window.removeEventListener('message', handleMessage)
})

const togglePicking = (forceState) => {
  if (typeof forceState === 'boolean') {
    isPickingElement.value = forceState
  } else {
    isPickingElement.value = !isPickingElement.value
  }
  
  if (iframeRef.value && iframeRef.value.contentWindow) {
    iframeRef.value.contentWindow.postMessage(
      { type: isPickingElement.value ? 'START_PICKING' : 'STOP_PICKING' },
      '*'
    )
  }
}

const onIframeLoad = () => {
  if (iframeRef.value && iframeRef.value.contentWindow) {
    iframeRef.value.contentWindow.postMessage(
      { type: isPickingElement.value ? 'START_PICKING' : 'STOP_PICKING' },
      '*'
    )
  }
}

const handleClose = () => {
  previewStore.hidePreview()
  isPickingElement.value = false
}
</script>

<template>
  <Transition name="slide">
    <div 
      v-if="visible" 
      class="preview-panel"
      :class="{ 'is-resizing': isResizing }"
      :style="panelWidth ? { width: panelWidth + 'px' } : {}"
    >
      <!-- 左侧调整宽度的把手 -->
      <div class="resize-handle" @mousedown.prevent="startResize">
        <div class="resize-indicator"></div>
      </div>
      
      <div class="preview-header">
        <div class="header-left">
          <span class="header-title">网页预览</span>
        </div>
        <div class="header-actions">
          <el-button 
            :type="isPickingElement ? 'primary' : 'default'" 
            :plain="!isPickingElement"
            size="small" 
            class="pick-btn"
            @click="togglePicking"
          >
            <el-icon><Pointer /></el-icon>
            {{ isPickingElement ? '正在选取...' : '选取元素' }}
          </el-button>
          
          <div class="divider"></div>

          <el-button text class="close-btn" @click="handleClose">
            <el-icon :size="18"><Close /></el-icon>
          </el-button>
        </div>
      </div>
      <div class="preview-body">
        <iframe
          ref="iframeRef"
          :srcdoc="processedHtmlContent"
          class="preview-iframe"
          sandbox="allow-scripts allow-same-origin"
          @load="onIframeLoad"
        />
        <!-- 取消提示遮罩层以防干扰点击 -->
        <Transition name="fade">
          <div v-if="isPickingElement" class="picking-status-bar">
            <span>🖱️ 鼠标悬停可高亮块，点击即可截取网页元素</span>
          </div>
        </Transition>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.preview-panel {
  position: fixed;
  top: calc(var(--header-height) + 16px);
  right: 16px;
  width: 50%;
  height: calc(100vh - var(--header-height) - 32px);
  background: var(--color-white);
  border: 1px solid var(--color-border-light);
  border-radius: 12px;
  box-shadow: 0 10px 40px -10px rgba(0,0,0,0.1), 0 1px 3px rgba(0,0,0,0.05); /* 更柔和、更大的阴影 */
  z-index: 100;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  transition: box-shadow 0.3s ease;
}

/* 关闭拖拽宽度时的 css 动作，以免影响拖拽时的顺滑 */
.preview-panel:not(.is-resizing) {
  transition: box-shadow 0.3s ease, width 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.preview-panel.is-resizing {
  box-shadow: 0 15px 50px -10px rgba(0,0,0,0.15), 0 2px 8px rgba(0,0,0,0.08); /* 拖拽时阴影加深浮起感更强 */
}

/* 拖动把手 */
.resize-handle {
  position: absolute;
  top: 0;
  bottom: 0;
  left: -4px;
  width: 10px;
  cursor: ew-resize;
  z-index: 200;
  display: flex;
  align-items: center;
  justify-content: center;
}

.resize-indicator {
  width: 4px;
  height: 32px;
  border-radius: 4px;
  background-color: transparent;
  transition: background-color 0.2s;
}

.resize-handle:hover .resize-indicator,
.preview-panel.is-resizing .resize-indicator {
  background-color: var(--color-primary-light-3, #a0cfff); /* 可替换为您主题的颜色 */
}

.preview-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border-light);
  background: rgba(255, 255, 255, 0.85); /* 模拟极简悬浮头的效果 */
  backdrop-filter: blur(10px);
}

.header-left {
  display: flex;
  align-items: center;
  gap: 8px;
}

.header-title {
  font-size: 14px;
  font-weight: 500;
  color: var(--color-text-primary);
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 12px;
}

.pick-btn {
  display: flex;
  border-radius: 6px;
  align-items: center;
  gap: 4px;
}

.divider {
  width: 1px;
  height: 16px;
  background-color: var(--color-border-light);
}

.close-btn {
  padding: 4px;
  margin-left: 0;
  color: var(--color-text-secondary);
}

.close-btn:hover {
  color: var(--color-danger);
  background-color: var(--color-danger-light-9);
}

.preview-body {
  position: relative;
  flex: 1;
  overflow: hidden;
  background: #fcfcfc;
}

.preview-iframe {
  width: 100%;
  height: 100%;
  border: none;
}

/* 拖拽面板时，取消 iframe 的指针事件捕捉，防止拖拽卡顿或焦点丢失 */
.preview-panel.is-resizing .preview-iframe {
  pointer-events: none;
}

/* 提示条状态 */
.picking-status-bar {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  background: rgba(0, 0, 0, 0.75);
  backdrop-filter: blur(5px);
  padding: 8px 16px;
  border-radius: 20px;
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  pointer-events: none; /* 让鼠标穿透提示栏点击下方的iframe元素 */
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  display: flex;
  align-items: center;
  gap: 6px;
}

/* 过渡动画 */
.slide-enter-active,
.slide-leave-active {
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
}

.slide-enter-from,
.slide-leave-to {
  transform: translateX(100%) scale(0.98); /* 带缩放的滑入 */
  opacity: 0;
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
