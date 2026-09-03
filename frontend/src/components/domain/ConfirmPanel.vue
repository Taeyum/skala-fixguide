<script setup>
/**
 * E_04 제출 전 확인 잠금 패널 (buildver2 · 5. WRA_E_04)
 * 체크박스가 체크되어야만 제출 버튼 활성화
 */
defineProps({
  modelValue: { type: Boolean, default: false },
})
defineEmits(['update:modelValue'])
</script>

<template>
  <label class="confirm">
    <input
      type="checkbox"
      :checked="modelValue"
      @change="$emit('update:modelValue', $event.target.checked)"
    />
    <span>
      AI 결과와 법령 항목을 검토했으며, <b>제출 후에는 수정할 수 없음</b>을 확인합니다.
    </span>
  </label>
</template>

<style scoped>
.confirm {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--secondary-fixed);
  color: var(--on-surface);
  font-size: 13px;
  line-height: 1.5;
}

.confirm input {
  margin-top: 2px;
  accent-color: var(--primary);
  width: 16px;
  height: 16px;
}
</style>
