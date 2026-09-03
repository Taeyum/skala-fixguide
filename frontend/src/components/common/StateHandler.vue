<script setup>
/** Loading / Error / Empty 통합 래퍼 (buildver2 · 3. StateHandler) */
defineProps({
  loading: { type: Boolean, default: false },
  error: { type: [Object, Error, null], default: null },
  empty: { type: Boolean, default: false },
  emptyText: { type: String, default: '표시할 데이터가 없습니다.' },
})
const emit = defineEmits(['retry'])
</script>

<template>
  <div v-if="loading" class="state state--loading">
    <span class="material-symbols-outlined state__spin">progress_activity</span>
    <span>불러오는 중…</span>
  </div>

  <div v-else-if="error" class="state state--error">
    <span class="material-symbols-outlined">cloud_off</span>
    <p>데이터를 불러오지 못했습니다.</p>
    <button type="button" class="state__retry" @click="emit('retry')">다시 시도</button>
  </div>

  <div v-else-if="empty" class="state state--empty">
    <span class="material-symbols-outlined">inbox</span>
    <p>{{ emptyText }}</p>
  </div>

  <slot v-else />
</template>

<style scoped>
.state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  padding: 48px 24px;
  color: var(--on-surface-variant);
  font-size: 14px;
  text-align: center;
}

.state .material-symbols-outlined {
  font-size: 32px;
  color: var(--outline);
}

.state__spin {
  animation: sh-spin 0.9s linear infinite;
}

@keyframes sh-spin {
  to {
    transform: rotate(360deg);
  }
}

.state__retry {
  padding: 8px 16px;
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-sm);
  background: var(--surface-container-lowest);
  color: var(--primary);
  font-size: 13px;
}
</style>
