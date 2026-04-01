import { defineStore } from 'pinia'
import { ref } from 'vue'

export const usePreviewStore = defineStore('preview', () => {
  const visible = ref(false)
  const htmlContent = ref('')
  const pickedElement = ref(null) // 存储被选中的网页元素对象
  
  const showPreview = (html) => {
    htmlContent.value = html
    visible.value = true
  }
  
  const hidePreview = () => {
    visible.value = false
  }

  const setPickedElement = (element) => {
    pickedElement.value = element
  }
  
  const clearPickedElement = () => {
    pickedElement.value = null
  }
  
  return {
    visible,
    htmlContent,
    pickedElement,
    showPreview,
    hidePreview,
    setPickedElement,
    clearPickedElement
  }
})
