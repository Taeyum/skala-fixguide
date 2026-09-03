<script setup>
/** WRA_E_01 · 엔지니어 메인 (/home) */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardApi } from '@/api/dashboardApi'
import { requestApi } from '@/api/requestApi'
import { unwrapList, unwrapOne, pick } from '@/api/normalize'
import { useAsyncState } from '@/composables/useAsyncState'
import { STATUS, productTypeLabel } from '@/constants/workRequest'
import { fmtDate } from '@/utils/format'
import KpiCard from '@/components/common/KpiCard.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import StateHandler from '@/components/common/StateHandler.vue'

const router = useRouter()

const summary = useAsyncState(() => dashboardApi.summary('engineer').then((r) => unwrapOne(r.data)), {
  immediate: true,
})
const requests = useAsyncState(
  () => requestApi.list({ mine: true }).then((r) => unwrapList(r.data)),
  { immediate: true, initial: [] },
)

const kpis = computed(() => {
  const s = summary.data.value ?? {}
  return [
    { label: '작성 중', value: pick(s, 'draft', 'draftCount') ?? 0, icon: 'edit_note' },
    { label: '진행 중', value: pick(s, 'running', 'runningCount') ?? 0, icon: 'sync', tone: 'primary' },
    { label: '승인 대기', value: pick(s, 'pending', 'pendingCount') ?? 0, icon: 'hourglass_top' },
    {
      label: '반려·보완',
      value: pick(s, 'rejected', 'rejectedCount') ?? 0,
      icon: 'error',
      tone: 'error',
    },
  ]
})

const recent = computed(() => (requests.data.value ?? []).slice(0, 8))

function rowTitle(req) {
  return `${pick(req, 'equipment') ?? '-'} · ${pick(req, 'product_name', 'productName') ?? '-'}`
}

function goToRequest(req) {
  const id = pick(req, 'id')
  const status = pick(req, 'status')
  if (status === STATUS.RUNNING) router.push({ name: 'agent-run', params: { id } })
  else if (status === STATUS.DRAFT) router.push({ name: 'request-create', query: { id } })
  else router.push({ name: 'my-requests' })
}

function reload() {
  summary.execute()
  requests.execute()
}
</script>

<template>
  <div class="view">
    <div class="view__header">
      <div>
        <span class="view__eyebrow">Fab 안전 포털</span>
        <h1 class="view__title">엔지니어 대시보드</h1>
        <p class="view__desc">내 부품 교체 요청 현황을 확인하고 새 요청을 시작하세요.</p>
      </div>
      <button class="btn btn--primary" @click="router.push({ name: 'request-create' })">
        <span class="material-symbols-outlined">add_circle</span>
        신규 교체 요청
      </button>
    </div>

    <div class="kpi-grid">
      <KpiCard
        v-for="k in kpis"
        :key="k.label"
        :label="k.label"
        :value="k.value"
        :icon="k.icon"
        :tone="k.tone || 'default'"
        :loading="summary.loading.value"
      />
    </div>

    <div class="card">
      <div class="card__title"><span class="material-symbols-outlined">assignment</span>최근 내 요청</div>

      <StateHandler
        :loading="requests.loading.value"
        :error="requests.error.value"
        :empty="!recent.length"
        empty-text="아직 등록한 요청이 없습니다. 새 교체 요청을 시작하세요."
        @retry="reload"
      >
        <div class="table-scroll">
          <table class="data-table">
            <thead>
              <tr>
                <th>설비 · 부품</th>
                <th>제품 유형</th>
                <th>상태</th>
                <th>제출일</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="req in recent"
                :key="pick(req, 'id')"
                class="is-clickable"
                @click="goToRequest(req)"
              >
                <td>{{ rowTitle(req) }}</td>
                <td>{{ productTypeLabel(pick(req, 'product_type', 'productType')) }}</td>
                <td><StatusBadge :status="pick(req, 'status')" /></td>
                <td>{{ fmtDate(pick(req, 'submitted_at', 'submittedAt', 'created_at', 'createdAt')) }}</td>
              </tr>
            </tbody>
          </table>
        </div>
      </StateHandler>
    </div>
  </div>
</template>
