<script setup>
/** 통계 카드 · tabular-nums 로 CLS 방지 (buildver2 · 6.3) */
defineProps({
  label: { type: String, required: true },
  value: { type: [Number, String], default: 0 },
  unit: { type: String, default: '' },
  icon: { type: String, default: '' },
  tone: { type: String, default: 'default' }, // default | primary | error
  loading: { type: Boolean, default: false },
})
</script>

<template>
  <div class="kpi" :class="`kpi--${tone}`">
    <div class="kpi__head">
      <span class="kpi__label">{{ label }}</span>
      <span v-if="icon" class="material-symbols-outlined kpi__icon">{{ icon }}</span>
    </div>
    <div class="kpi__body">
      <span class="kpi__value tabular-nums">{{ loading ? '–' : value }}</span>
      <span v-if="unit" class="kpi__unit">{{ unit }}</span>
    </div>
  </div>
</template>

<style scoped>
.kpi {
  padding: 20px;
  border-radius: var(--radius-lg);
  background: var(--surface-container-lowest);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.kpi__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.kpi__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--on-surface-variant);
}

.kpi__icon {
  font-size: 22px;
  color: var(--outline);
}

.kpi--primary .kpi__icon {
  color: var(--primary);
}
.kpi--error .kpi__icon {
  color: var(--error);
}

.kpi__body {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.kpi__value {
  font-size: 30px;
  font-weight: 600;
  color: var(--on-surface);
}

.kpi--primary .kpi__value {
  color: var(--primary);
}
.kpi--error .kpi__value {
  color: var(--error);
}

.kpi__unit {
  font-size: 12px;
  color: var(--on-surface-variant);
}
</style>
