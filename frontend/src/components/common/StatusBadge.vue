<script setup>
/** 5대 상태 뱃지 (Dot + Label) — WRA_SCREENS_v2 · 1.2 */
import { computed } from 'vue'
import { STATUS_META } from '@/constants/workRequest'

const props = defineProps({
  status: { type: String, required: true },
})

const meta = computed(() => STATUS_META[props.status] ?? { label: props.status, tone: 'draft' })
</script>

<template>
  <span class="status-badge" :class="`status-badge--${meta.tone}`">
    <span class="status-badge__dot" />
    {{ meta.label }}
  </span>
</template>

<style scoped>
.status-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  white-space: nowrap;
}

.status-badge__dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: currentColor;
}

.status-badge--draft {
  background: var(--st-draft-bg);
  color: var(--st-draft-fg);
}
.status-badge--running {
  background: var(--st-running-bg);
  color: var(--st-running-fg);
}
.status-badge--pending {
  background: var(--st-pending-bg);
  color: var(--st-pending-fg);
}
.status-badge--approved {
  background: var(--st-approved-bg);
  color: var(--st-approved-fg);
}
.status-badge--rejected {
  background: var(--st-rejected-bg);
  color: var(--st-rejected-fg);
}
</style>
