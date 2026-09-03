<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import AuthLayout from '@/layouts/AuthLayout.vue'
import AppLayout from '@/layouts/AppLayout.vue'

const route = useRoute()
const layout = computed(() => {
  if (route.meta.layout === 'auth') return AuthLayout
  if (route.meta.layout === 'app') return AppLayout
  return 'div'
})
</script>

<template>
  <component :is="layout">
    <RouterView />
  </component>
</template>
