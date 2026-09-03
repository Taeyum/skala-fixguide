<script setup>
/** WRA_S_01 · 승인 대기 큐 (/manage/requests) */
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { dashboardApi } from '@/api/dashboardApi'
import { requestApi } from '@/api/requestApi'
import { pick, unwrapList, unwrapOne } from '@/api/normalize'
import { useAsyncState } from '@/composables/useAsyncState'
import { STATUS, productTypeLabel } from '@/constants/workRequest'
import { fmtDateTime } from '@/utils/format'
import KpiCard from '@/components/common/KpiCard.vue'
import StateHandler from '@/components/common/StateHandler.vue'

const router = useRouter()

const summary = useAsyncState(() => dashboardApi.summary('safety').then((r) => unwrapOne(r.data)), {
  immediate: true,
})
const queue = useAsyncState(
  () => requestApi.list({ status: STATUS.PENDING }).then((r) => unwrapList(r.data)),
  { immediate: true, initial: [] },
)

const kpis = computed(() => {
  const s = summary.data.value ?? {}
  return [
    { label: '승인 대기', value: pick(s, 'pending', 'pendingCount') ?? 0, icon: 'pending_actions', tone: 'primary' },
    { label: '오늘 처리', value: pick(s, 'today_processed', 'todayProcessed') ?? 0, icon: 'task_alt' },
    { label: '이번 달 승인', value: pick(s, 'month_approved', 'monthApproved') ?? 0, icon: 'verified' },
    { label: '이번 달 거절', value: pick(s, 'month_rejected', 'monthRejected') ?? 0, icon: 'block', tone: 'error' },
  ]
})

const top5 = computed(() => {
  const raw = pick(summary.data.value ?? {}, 'reject_top5', 'rejectTop5') ?? []
  const max = Math.max(1, ...raw.map((r) => pick(r, 'count') ?? 0))
  return raw.map((r) => ({
    reason: pick(r, 'reason', 'label') ?? '-',
    count: pick(r, 'count') ?? 0,
    ratio: Math.round(((pick(r, 'count') ?? 0) / max) * 100),
  }))
})

const rows = computed(() => queue.data.value ?? [])

function goDetail(req) {
  router.push({ name: 'safety-detail', params: { id: pick(req, 'id') } })
}

function reload() {
  summary.execute()
  queue.execute()
}
</script>

<template>
  <div class="view">
    <div class="view__header">
      <div>
        <span class="view__eyebrow">안전 운영 허브</span>
        <h1 class="view__title">요청 관리 · 승인</h1>
        <p class="view__desc">제출된 부품 교체 요청을 검토하고 승인 또는 거절합니다.</p>
      </div>
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

    <div class="s1-grid">
      <div class="card">
        <div class="card__title">
          <span class="material-symbols-outlined">inbox</span>승인 대기 중인 요청
        </div>
        <StateHandler
          :loading="queue.loading.value"
          :error="queue.error.value"
          :empty="!rows.length"
          empty-text="승인 대기 중인 요청이 없습니다."
          @retry="reload"
        >
          <div class="table-scroll">
            <table class="data-table">
              <thead>
                <tr>
                  <th>설비 · 부품</th>
                  <th>제품 유형</th>
                  <th>요청자</th>
                  <th>제출일시</th>
                </tr>
              </thead>
              <tbody>
                <tr
                  v-for="req in rows"
                  :key="pick(req, 'id')"
                  class="is-clickable"
                  @click="goDetail(req)"
                >
                  <td>
                    {{ pick(req, 'equipment') ?? '-' }} ·
                    {{ pick(req, 'product_name', 'productName') ?? '-' }}
                  </td>
                  <td>{{ productTypeLabel(pick(req, 'product_type', 'productType')) }}</td>
                  <td>{{ pick(req, 'requester_name', 'requesterName') ?? pick(req, 'requester_id', 'requesterId') ?? '-' }}</td>
                  <td>{{ fmtDateTime(pick(req, 'submitted_at', 'submittedAt', 'created_at', 'createdAt')) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </StateHandler>
      </div>

      <div class="card">
        <div class="card__title"><span class="material-symbols-outlined">bar_chart</span>거절 사유 TOP 5</div>
        <StateHandler
          :loading="summary.loading.value"
          :error="summary.error.value"
          :empty="!top5.length"
          empty-text="거절 이력이 없습니다."
          @retry="reload"
        >
          <ul class="top5">
            <li v-for="(t, i) in top5" :key="i">
              <div class="top5__row">
                <span>{{ i + 1 }}. {{ t.reason }}</span>
                <span class="top5__count tabular-nums">{{ t.count }}건</span>
              </div>
              <div class="top5__bar"><span :style="{ width: t.ratio + '%' }" /></div>
            </li>
          </ul>
        </StateHandler>
      </div>
    </div>
  </div>
</template>

<style scoped>
.s1-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  align-items: start;
}

.top5 {
  list-style: none;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.top5__row {
  display: flex;
  justify-content: space-between;
  gap: 8px;
  font-size: 13px;
  margin-bottom: 6px;
}

.top5__count {
  color: var(--on-surface-variant);
}

.top5__bar {
  height: 8px;
  border-radius: 999px;
  background: var(--surface-container-high);
  overflow: hidden;
}

.top5__bar span {
  display: block;
  height: 100%;
  background: var(--error);
  border-radius: 999px;
}

@media (max-width: 960px) {
  .s1-grid {
    grid-template-columns: 1fr;
  }
}
</style>
