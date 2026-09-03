<script setup>
/** 공용 버튼 · variant: primary | secondary | danger-outline (스타일은 app-view.css 전역 .btn) */
defineProps({
  variant: { type: String, default: 'primary' },
  type: { type: String, default: 'button' },
  loading: { type: Boolean, default: false },
  disabled: { type: Boolean, default: false },
  block: { type: Boolean, default: false },
})
</script>

<template>
  <button
    :type="type"
    :disabled="disabled || loading"
    :class="['btn', `btn--${variant}`, { 'btn--block': block }]"
  >
    <span v-if="loading" class="material-symbols-outlined btn__spinner">progress_activity</span>
    <slot v-else />
  </button>
</template>
