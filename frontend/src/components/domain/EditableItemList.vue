<script setup>
/**
 * E_04 AI 결과 인라인 편집 리스트 (buildver2 · ResultBlockEditor / LawListEditor)
 * v-model = 항목 배열. 각 항목은 { ...cols } 형태.
 * columns: [{ key, label, textarea? }]
 */
const props = defineProps({
  modelValue: { type: Array, default: () => [] },
  columns: { type: Array, required: true },
  addLabel: { type: String, default: '항목 추가' },
  disabled: { type: Boolean, default: false },
})
const emit = defineEmits(['update:modelValue'])

function emitList(next) {
  emit('update:modelValue', next)
}

function setCell(index, key, value) {
  const next = props.modelValue.map((row, i) => (i === index ? { ...row, [key]: value } : row))
  emitList(next)
}

function addRow() {
  const blank = Object.fromEntries(props.columns.map((c) => [c.key, '']))
  emitList([...props.modelValue, blank])
}

function removeRow(index) {
  emitList(props.modelValue.filter((_, i) => i !== index))
}
</script>

<template>
  <div class="eil">
    <div v-for="(row, i) in modelValue" :key="i" class="eil__row">
      <div class="eil__cells">
        <div v-for="col in columns" :key="col.key" class="eil__cell">
          <label>{{ col.label }}</label>
          <textarea
            v-if="col.textarea"
            class="form-control"
            rows="2"
            :disabled="disabled"
            :value="row[col.key] || ''"
            @input="setCell(i, col.key, $event.target.value)"
          />
          <input
            v-else
            class="form-control"
            type="text"
            :disabled="disabled"
            :value="row[col.key] || ''"
            @input="setCell(i, col.key, $event.target.value)"
          />
        </div>
      </div>
      <button
        type="button"
        class="eil__remove"
        :disabled="disabled"
        title="삭제"
        @click="removeRow(i)"
      >
        <span class="material-symbols-outlined">delete</span>
      </button>
    </div>

    <button type="button" class="eil__add" :disabled="disabled" @click="addRow">
      <span class="material-symbols-outlined">add</span> {{ addLabel }}
    </button>
  </div>
</template>

<style scoped>
.eil {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.eil__row {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  padding: 12px;
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
}

.eil__cells {
  flex: 1;
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.eil__cells:has(.eil__cell:only-child) {
  grid-template-columns: 1fr;
}

.eil__cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.eil__cell label {
  font-size: 11px;
  color: var(--on-surface-variant);
}

.eil__remove {
  border: none;
  background: transparent;
  color: var(--on-surface-variant);
  padding: 6px;
  border-radius: var(--radius-sm);
  margin-top: 18px;
}

.eil__remove:hover:not(:disabled) {
  color: var(--error);
  background: var(--error-container);
}

.eil__remove .material-symbols-outlined {
  font-size: 18px;
}

.eil__add {
  align-self: flex-start;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 8px 12px;
  border: 1px dashed var(--outline-variant);
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--primary);
  font-size: 13px;
}

.eil__add:disabled {
  opacity: 0.5;
}

.eil__add .material-symbols-outlined {
  font-size: 16px;
}

@media (max-width: 640px) {
  .eil__cells {
    grid-template-columns: 1fr;
  }
}
</style>
