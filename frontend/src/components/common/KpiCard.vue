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
      <span v-if="icon" class="kpi__icon-tile">
        <span class="material-symbols-outlined kpi__icon">{{ icon }}</span>
      </span>
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
  background: var(--surface-container-low);
  box-shadow: var(--shadow-sm);
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.kpi--error {
  background: #fff1ef;
}

.kpi__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.kpi__label {
  font-size: 13px;
  font-weight: 500;
  color: var(--on-surface-variant);
}

.kpi__icon-tile {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 34px;
  height: 34px;
  border-radius: var(--radius-sm);
  background: var(--secondary-fixed);
  color: var(--primary);
}

.kpi__icon {
  font-size: 20px;
}

.kpi--error .kpi__icon-tile {
  background: var(--error-container);
  color: var(--on-error-container);
}

.kpi__body {
  display: flex;
  align-items: baseline;
  gap: 6px;
}

.kpi__value {
  font-size: 28px;
  font-weight: 600;
  color: var(--on-surface);
  letter-spacing: -0.01em;
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
