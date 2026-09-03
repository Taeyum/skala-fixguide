<script setup>
/**
 * 역할 선택 세그먼트 토글 (C_01).
 * options: [{ value, label, icon? }]
 * 접근성: 각 버튼 aria-pressed
 */
defineProps({
  modelValue: { type: String, default: null },
  options: { type: Array, required: true },
})
const emit = defineEmits(['update:modelValue'])
</script>

<template>
  <div class="segmented" role="group">
    <button
      v-for="opt in options"
      :key="opt.value"
      type="button"
      class="segmented__item"
      :class="{ 'segmented__item--active': modelValue === opt.value }"
      :aria-pressed="modelValue === opt.value"
      @click="emit('update:modelValue', opt.value)"
    >
      <span v-if="opt.icon" class="material-symbols-outlined">{{ opt.icon }}</span>
      {{ opt.label }}
    </button>
  </div>
</template>

<style scoped>
.segmented {
  display: grid;
  grid-auto-flow: column;
  grid-auto-columns: 1fr;
  gap: 4px;
  padding: 4px;
  background: var(--surface-container);
  border-radius: var(--radius-md);
}

.segmented__item {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  padding: 10px 12px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--on-surface-variant);
  font-size: 13px;
  font-weight: 500;
  transition:
    background-color 0.15s,
    color 0.15s;
}

.segmented__item .material-symbols-outlined {
  font-size: 18px;
}

.segmented__item--active {
  background: var(--surface-container-lowest);
  color: var(--primary);
  box-shadow: var(--shadow-sm);
}
</style>
