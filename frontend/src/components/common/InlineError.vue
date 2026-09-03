<script setup>
/** 폼 상단 인라인 경고 박스 (401/409/유효성 메시지) */
defineProps({
  message: { type: String, default: '' },
})
</script>

<template>
  <Transition name="inline-error">
    <div v-if="message" class="inline-error" role="alert">
      <span class="material-symbols-outlined">error</span>
      <span>{{ message }}</span>
    </div>
  </Transition>
</template>

<style scoped>
.inline-error {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  background: var(--error-container);
  color: var(--on-error-container);
  font-size: 13px;
}

.inline-error .material-symbols-outlined {
  font-size: 18px;
}

.inline-error-enter-active,
.inline-error-leave-active {
  transition:
    opacity 0.15s,
    transform 0.15s;
}

.inline-error-enter-from,
.inline-error-leave-to {
  opacity: 0;
  transform: translateY(-4px);
}
</style>
