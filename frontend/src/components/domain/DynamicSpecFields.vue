<script setup>
/**
 * 제품 유형별 동적 스펙 인풋 (WRA_SCREENS_v2 · 1.4)
 * v-model 은 spec_json 오브젝트
 */
import { computed } from 'vue'
import { specFieldsFor } from '@/constants/workRequest'

const props = defineProps({
  productType: { type: String, default: '' },
  modelValue: { type: Object, default: () => ({}) },
})
const emit = defineEmits(['update:modelValue'])

const fields = computed(() => specFieldsFor(props.productType))

function setField(key, value) {
  emit('update:modelValue', { ...props.modelValue, [key]: value })
}
</script>

<template>
  <div v-if="fields.length" class="spec-fields">
    <div class="spec-fields__head">
      <span class="material-symbols-outlined">tune</span>
      유형별 상세 스펙
    </div>
    <div class="form-grid">
      <div v-for="f in fields" :key="f.key" class="form-field">
        <label :for="`spec-${f.key}`">{{ f.label }}</label>
        <textarea
          v-if="f.textarea"
          :id="`spec-${f.key}`"
          class="form-control"
          :placeholder="f.placeholder"
          :value="modelValue[f.key] || ''"
          @input="setField(f.key, $event.target.value)"
        />
        <input
          v-else
          :id="`spec-${f.key}`"
          class="form-control"
          type="text"
          :placeholder="f.placeholder"
          :value="modelValue[f.key] || ''"
          @input="setField(f.key, $event.target.value)"
        />
      </div>
    </div>
  </div>
</template>

<style scoped>
.spec-fields {
  padding: 16px;
  background: var(--surface-container);
  border-radius: var(--radius-md);
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.spec-fields__head {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  font-weight: 600;
  color: var(--on-surface-variant);
}

.spec-fields__head .material-symbols-outlined {
  font-size: 16px;
}
</style>
