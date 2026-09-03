<script setup>
/** A1/A2/A3 진행 카드 (buildver2 · AgentStepCard / 목업 E_03) */
import { computed } from 'vue'
import { STEP_STATUS } from '@/constants/workRequest'

const props = defineProps({
  agent: { type: Object, required: true }, // { code, title, desc, icon }
  status: { type: String, default: STEP_STATUS.WAITING },
  result: { type: [Object, String, null], default: null },
})

const meta = computed(() => {
  switch (props.status) {
    case STEP_STATUS.DONE:
      return { label: '완료', tone: 'done', icon: 'check_circle' }
    case STEP_STATUS.RUNNING:
      return { label: '실행 중', tone: 'running', icon: 'progress_activity' }
    default:
      return { label: '대기', tone: 'waiting', icon: 'schedule' }
  }
})

const resultText = computed(() => {
  if (!props.result) return null
  if (typeof props.result === 'string') return props.result
  return props.result.summary ?? JSON.stringify(props.result)
})
</script>

<template>
  <div class="agent-card" :class="`agent-card--${meta.tone}`">
    <div class="agent-card__head">
      <div class="agent-card__id">
        <span class="material-symbols-outlined">{{ agent.icon }}</span>
        <div>
          <p class="agent-card__code">에이전트 {{ agent.code }}</p>
          <p class="agent-card__title">{{ agent.title }}</p>
        </div>
      </div>
      <span class="agent-card__badge">
        <span
          class="material-symbols-outlined"
          :class="{ spin: status === 'RUNNING' }"
        >{{ meta.icon }}</span>
        {{ meta.label }}
      </span>
    </div>

    <p class="agent-card__desc">{{ agent.desc }}</p>

    <div v-if="resultText" class="agent-card__result">{{ resultText }}</div>
    <div v-else-if="status === 'RUNNING'" class="agent-card__result agent-card__result--muted">
      분석 진행 중…
    </div>
  </div>
</template>

<style scoped>
.agent-card {
  background: var(--surface-container-lowest);
  border: 1px solid var(--outline-variant);
  border-radius: var(--radius-lg);
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.agent-card--running {
  border-color: var(--primary);
}

.agent-card__head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.agent-card__id {
  display: flex;
  gap: 10px;
}

.agent-card__id > .material-symbols-outlined {
  font-size: 22px;
  color: var(--primary);
}

.agent-card__code {
  font-size: 11px;
  color: var(--on-surface-variant);
}

.agent-card__title {
  font-size: 14px;
  font-weight: 600;
  color: var(--on-surface);
}

.agent-card__badge {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 9px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
  background: var(--st-draft-bg);
  color: var(--st-draft-fg);
  white-space: nowrap;
}

.agent-card--done .agent-card__badge {
  background: var(--st-approved-bg);
  color: var(--st-approved-fg);
}

.agent-card--running .agent-card__badge {
  background: var(--st-running-bg);
  color: var(--st-running-fg);
}

.agent-card__badge .material-symbols-outlined {
  font-size: 13px;
}

.agent-card__desc {
  font-size: 12px;
  color: var(--on-surface-variant);
}

.agent-card__result {
  font-size: 13px;
  color: var(--on-surface);
  background: var(--surface-container-low);
  padding: 10px 12px;
  border-radius: var(--radius-sm);
  white-space: pre-wrap;
}

.agent-card__result--muted {
  color: var(--on-surface-variant);
}

.spin {
  animation: agent-spin 0.9s linear infinite;
}

@keyframes agent-spin {
  to {
    transform: rotate(360deg);
  }
}
</style>
