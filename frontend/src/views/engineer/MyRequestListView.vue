<script setup>
/** WRA_E_05 · 내 요청 목록 / 마이페이지 (/my/requests) */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { requestApi } from '@/api/requestApi'
import { pick, unwrapList, unwrapOne } from '@/api/normalize'
import { useAsyncState } from '@/composables/useAsyncState'
import { STATUS, productTypeLabel } from '@/constants/workRequest'
import StatusBadge from '@/components/common/StatusBadge.vue'
import StateHandler from '@/components/common/StateHandler.vue'
import KpiCard from '@/components/common/KpiCard.vue'

const router = useRouter()

const TABS = [
  { key: 'ALL', label: '전체' },
  { key: STATUS.DRAFT, label: '작성 중' },
  { key: STATUS.RUNNING, label: '진행 중' },
  { key: STATUS.PENDING, label: '승인 대기' },
  { key: STATUS.REJECTED, label: '거절·보완' },
]
const activeTab = ref('ALL')

const list = useAsyncState(
  () => requestApi.list({ mine: true }).then((r) => unwrapList(r.data)),
  { immediate: true, initial: [] },
)

const rows = computed(() => list.data.value ?? [])

function countFor(key) {
  if (key === 'ALL') return rows.value.length
  return rows.value.filter((r) => pick(r, 'status') === key).length
}

const filtered = computed(() =>
  activeTab.value === 'ALL'
    ? rows.value
    : rows.value.filter((r) => pick(r, 'status') === activeTab.value),
)

/** 거절 사유 캐시: id → reason */
const reasons = ref({})
const expanded = ref(null)

async function toggleReason(req) {
  const id = pick(req, 'id')
  if (expanded.value === id) {
    expanded.value = null
    return
  }
  expanded.value = id
  if (reasons.value[id] !== undefined) return
  try {
    const { data } = await requestApi.get(id)
    const d = unwrapOne(data)
    const approvals = pick(d, 'approvals') ?? []
    const lastReject = [...approvals].reverse().find((a) => pick(a, 'decision') === 'REJECT')
    reasons.value = {
      ...reasons.value,
      [id]:
        pick(d, 'reject_reason', 'rejectReason') ??
        pick(lastReject, 'reason') ??
        '거절 사유가 등록되지 않았습니다.',
    }
  } catch {
    reasons.value = { ...reasons.value, [id]: '거절 사유를 불러오지 못했습니다.' }
  }
}

function rowTitle(req) {
  return `${pick(req, 'equipment') ?? '-'} · ${pick(req, 'product_name', 'productName') ?? '-'}`
}

function primaryAction(req) {
  const id = pick(req, 'id')
  const status = pick(req, 'status')
  if (status === STATUS.DRAFT) router.push({ name: 'request-create', query: { id } })
  else if (status === STATUS.RUNNING) router.push({ name: 'agent-run', params: { id } })
  else if (status === STATUS.REJECTED) router.push({ name: 'agent-result', params: { id } })
  else router.push({ name: 'agent-result', params: { id } })
}

function actionLabel(status) {
  if (status === STATUS.DRAFT) return '이어쓰기'
  if (status === STATUS.RUNNING) return '진행 보기'
  if (status === STATUS.REJECTED) return '수정 후 재제출'
  return '상세 보기'
}

const STATUS_ICON = {
  DRAFT: 'edit_note',
  RUNNING: 'sync',
  DONE: 'fact_check',
  PENDING: 'hourglass_top',
  APPROVED: 'check_circle',
  REJECTED: 'error',
}
function statusIcon(status) {
  return STATUS_ICON[status] ?? 'description'
}

const kpis = computed(() => {
  const by = (s) => rows.value.filter((r) => pick(r, 'status') === s).length
  return [
    { label: '전체 요청', value: rows.value.length, icon: 'assignment' },
    {
      label: '진행/대기',
      value: by(STATUS.RUNNING) + by(STATUS.DONE) + by(STATUS.PENDING),
      icon: 'sync',
      tone: 'primary',
    },
    { label: '승인 완료', value: by(STATUS.APPROVED), icon: 'verified' },
    { label: '거절·보완', value: by(STATUS.REJECTED), icon: 'block', tone: 'error' },
  ]
})
</script>

<template>
  <div class="view">
    <div class="view__header">
      <div>
        <span class="view__eyebrow">My Workspace</span>
        <h1 class="view__title">내 요청 목록</h1>
        <p class="view__desc">제출한 요청의 상태를 확인하고, 반려 건은 사유 확인 후 재제출할 수 있습니다.</p>
      </div>
      <div class="e05-kpis">
        <KpiCard
          v-for="k in kpis"
          :key="k.label"
          :label="k.label"
          :value="k.value"
          :icon="k.icon"
          :tone="k.tone || 'default'"
          :loading="list.loading.value"
        />
      </div>
    </div>

    <div class="tabs">
      <button
        v-for="t in TABS"
        :key="t.key"
        class="tabs__btn"
        :class="{ 'tabs__btn--active': activeTab === t.key }"
        role="tab"
        :aria-selected="activeTab === t.key"
        @click="activeTab = t.key"
      >
        {{ t.label }} <span class="tabs__count">{{ countFor(t.key) }}</span>
      </button>
    </div>

    <div class="card">
      <StateHandler
        :loading="list.loading.value"
        :error="list.error.value"
        :empty="!filtered.length"
        empty-text="해당 상태의 요청이 없습니다."
        @retry="list.execute()"
      >
        <div class="table-scroll">
          <table class="data-table">
            <thead>
              <tr>
                <th>설비 · 부품</th>
                <th>제품 유형</th>
                <th>상태</th>
                <th>제출일</th>
                <th style="text-align: right">액션</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="req in filtered" :key="pick(req, 'id')">
                <tr>
                  <td>
                    <div class="e05-cell">
                      <span class="e05-cell__icon" :class="`e05-cell__icon--${pick(req, 'status')}`">
                        <span class="material-symbols-outlined">{{ statusIcon(pick(req, 'status')) }}</span>
                      </span>
                      <span>{{ rowTitle(req) }}</span>
                    </div>
                  </td>
                  <td>{{ productTypeLabel(pick(req, 'product_type', 'productType')) }}</td>
                  <td><StatusBadge :status="pick(req, 'status')" /></td>
                  <td>{{ (pick(req, 'created_at', 'createdAt') || '').slice(0, 10) || '-' }}</td>
                  <td style="text-align: right">
                    <div class="row-actions">
                      <button
                        v-if="pick(req, 'status') === STATUS.REJECTED"
                        class="btn btn--danger-outline btn--sm"
                        @click="toggleReason(req)"
                      >
                        거절 사유
                      </button>
                      <button class="btn btn--primary btn--sm" @click="primaryAction(req)">
                        {{ actionLabel(pick(req, 'status')) }}
                      </button>
                    </div>
                  </td>
                </tr>
                <tr v-if="expanded === pick(req, 'id')">
                  <td colspan="5">
                    <div class="reject-reason">
                      <span class="material-symbols-outlined">warning</span>
                      {{ reasons[pick(req, 'id')] ?? '불러오는 중…' }}
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </StateHandler>
    </div>
  </div>
</template>

<style scoped>
.e05-kpis {
  display: grid;
  grid-template-columns: repeat(4, minmax(120px, 1fr));
  gap: 12px;
  flex: 1;
  max-width: 620px;
}

.e05-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.e05-cell__icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  border-radius: var(--radius-sm);
  background: var(--secondary-fixed);
  color: var(--primary);
}

.e05-cell__icon .material-symbols-outlined {
  font-size: 18px;
}

.e05-cell__icon--REJECTED {
  background: var(--error-container);
  color: var(--on-error-container);
}

.e05-cell__icon--APPROVED {
  background: var(--st-approved-bg);
  color: var(--st-approved-fg);
}

@media (max-width: 900px) {
  .e05-kpis {
    grid-template-columns: repeat(2, 1fr);
    max-width: none;
    width: 100%;
  }
}

.tabs {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  padding: 6px;
  background: var(--surface-container);
  border-radius: var(--radius-md);
  align-self: flex-start;
}

.tabs__btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: none;
  border-radius: var(--radius-sm);
  background: transparent;
  color: var(--on-surface-variant);
  font-size: 13px;
  font-weight: 500;
}

.tabs__btn--active {
  background: var(--primary-container);
  color: #fff;
}

.tabs__count {
  font-size: 11px;
  padding: 1px 6px;
  border-radius: 999px;
  background: rgba(0, 0, 0, 0.08);
}

.tabs__btn--active .tabs__count {
  background: rgba(255, 255, 255, 0.25);
}

.row-actions {
  display: inline-flex;
  gap: 8px;
  justify-content: flex-end;
}

.btn--sm {
  padding: 6px 12px;
  font-size: 12px;
}

.reject-reason {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: var(--error-container);
  color: var(--on-error-container);
  font-size: 13px;
}

.reject-reason .material-symbols-outlined {
  font-size: 18px;
}
</style>
