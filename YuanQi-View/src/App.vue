<script setup>
import { provide } from 'vue'
import PreviewPanel from '@/components/common/PreviewPanel.vue'
import { usePreviewStore } from '@/stores/preview'

const previewStore = usePreviewStore()
provide('previewStore', previewStore)
</script>

<template>
  <router-view />
  <PreviewPanel />
</template>

<style>
</style>
