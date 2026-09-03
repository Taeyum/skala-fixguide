<script setup>
/** WRA_E_03 · AI 검증 진행 (/requests/:id/run) */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { requestApi } from '@/api/requestApi'
import { pick, unwrapOne } from '@/api/normalize'
import { AGENTS, STEP_STATUS, productTypeLabel } from '@/constants/workRequest'
import { useRequestDraftStore } from '@/stores/requestDraftStore'
import { useAgentPolling } from '@/composables/useAgentPolling'
import AgentStepCard from '@/components/domain/AgentStepCard.vue'
import StateHandler from '@/components/common/StateHandler.vue'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()
const draftStore = useRequestDraftStore()
const { steps, error: pollError, allDone, start } = useAgentPolling(2500)

const detail = ref(null)
const loading = ref(true)
const loadError = ref(null)

/** step 코드로 상태/결과 조회 */
function stepFor(code) {
  return (
    steps.value.find((s) => (pick(s, 'step', 'code') ?? '').toUpperCase() === code) ?? {
      status: STEP_STATUS.WAITING,
      result: null,
    }
  )
}

const summary = computed(() => {
  const d = detail.value ?? {}
  return {
    equipment: pick(d, 'equipment') ?? '-',
    line: pick(d, 'line') ?? '-',
    productName: pick(d, 'product_name', 'productName') ?? '-',
    productType: productTypeLabel(pick(d, 'product_type', 'productType')),
  }
})

async function load() {
  loading.value = true
  loadError.value = null
  try {
    const { data } = await requestApi.get(props.id)
    detail.value = unwrapOne(data)
    const runId =
      draftStore.runId ||
      pick(detail.value, 'run_id', 'runId', 'agent_run_id', 'agentRunId') ||
      pick(detail.value?.agent_runs?.[0], 'run_id', 'runId')
    if (runId) {
      draftStore.setContext({ requestId: props.id, runId })
      start(runId)
    } else {
      loadError.value = new Error('run id 없음')
    }
  } catch (err) {
    loadError.value = err
  } finally {
    loading.value = false
  }
}

function goToResult() {
  router.push({ name: 'agent-result', params: { id: props.id } })
}

onMounted(load)
</script>

<template>
  <div class="view">
    <div class="view__header">
      <div>
        <span class="view__eyebrow">WRA-E-03 · 실시간 폴링</span>
        <h1 class="view__title">AI 다중 에이전트 검증</h1>
        <p class="view__desc">약 2.5초 간격으로 자동 갱신됩니다. 3종 모두 완료되어야 결과를 확인할 수 있습니다.</p>
      </div>
    </div>

    <StateHandler :loading="loading" :error="loadError" @retry="load">
      <div class="card">
        <div class="card__title"><span class="material-symbols-outlined">description</span>요청 요약</div>
        <div class="summary-grid">
          <div><span>설비 / 라인</span><p>{{ summary.equipment }} / {{ summary.line }}</p></div>
          <div><span>제품명</span><p>{{ summary.productName }}</p></div>
          <div><span>제품 유형</span><p>{{ summary.productType }}</p></div>
        </div>
      </div>

      <div class="agent-list">
        <AgentStepCard
          v-for="a in AGENTS"
          :key="a.code"
          :agent="a"
          :status="stepFor(a.code).status"
          :result="stepFor(a.code).result"
        />
      </div>

      <p v-if="pollError" class="poll-warn">
        <span class="material-symbols-outlined">warning</span>
        상태 갱신에 일시적으로 실패했습니다. 재시도 중…
      </p>

      <div class="btn-row">
        <button class="btn btn--primary" :disabled="!allDone" @click="goToResult">
          <span class="material-symbols-outlined">task_alt</span> 결과 확인
        </button>
      </div>
    </StateHandler>
  </div>
</template>

<style scoped>
.summary-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px;
}

.summary-grid > div {
  background: var(--surface-container-low);
  padding: 12px;
  border-radius: var(--radius-sm);
}

.summary-grid span {
  font-size: 11px;
  color: var(--on-surface-variant);
}

.summary-grid p {
  font-size: 14px;
  font-weight: 500;
  color: var(--on-surface);
  margin-top: 4px;
}

.agent-list {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 16px;
}

.poll-warn {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: var(--st-running-fg);
}

.poll-warn .material-symbols-outlined {
  font-size: 16px;
}

@media (max-width: 900px) {
  .summary-grid,
  .agent-list {
    grid-template-columns: 1fr;
  }
}
</style>
