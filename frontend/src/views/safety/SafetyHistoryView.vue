<script setup>
/**
 * 안전관리자 처리 내역 (/manage/history)
 * 백엔드 무수정: GET /work-requests?status=APPROVED,REJECTED (안전관리자는 처리 완료 건 전체 조회).
 * 행을 펼치면 상세를 불러와 최신 결정·사유·결정자를 보여준다.
 */
import { computed, ref } from 'vue'
import { useRouter } from 'vue-router'
import { requestApi } from '@/api/requestApi'
import { pick, unwrapList, unwrapOne } from '@/api/normalize'
import { useAsyncState } from '@/composables/useAsyncState'
import { STATUS, productTypeLabel } from '@/constants/workRequest'
import KpiCard from '@/components/common/KpiCard.vue'
import StatusBadge from '@/components/common/StatusBadge.vue'
import StateHandler from '@/components/common/StateHandler.vue'

const router = useRouter()

// 전체를 받아온 뒤(안전관리자는 PENDING·APPROVED·REJECTED 만 조회됨) 처리 완료 건만 화면에 보여준다.
const list = useAsyncState(
  () => requestApi.list({ size: 100 }).then((r) => unwrapList(r.data)),
  { immediate: true, initial: [] },
)

const PROCESSED = [STATUS.APPROVED, STATUS.REJECTED]
const rows = computed(() =>
  (list.data.value ?? []).filter((r) => PROCESSED.includes(pick(r, 'status'))),
)

const TABS = [
  { key: 'ALL', label: '전체' },
  { key: STATUS.APPROVED, label: '승인' },
  { key: STATUS.REJECTED, label: '거절' },
]
const activeTab = ref('ALL')

function countFor(key) {
  if (key === 'ALL') return rows.value.length
  return rows.value.filter((r) => pick(r, 'status') === key).length
}

const filtered = computed(() =>
  activeTab.value === 'ALL'
    ? rows.value
    : rows.value.filter((r) => pick(r, 'status') === activeTab.value),
)

const kpis = computed(() => [
  { label: '처리 완료', value: rows.value.length, icon: 'fact_check' },
  { label: '승인', value: countFor(STATUS.APPROVED), icon: 'verified', tone: 'primary' },
  { label: '거절', value: countFor(STATUS.REJECTED), icon: 'block', tone: 'error' },
])

/** id → { decision, reason, decidedBy, decidedAt } */
const decisions = ref({})
const expanded = ref(null)

async function toggleDecision(req) {
  const id = pick(req, 'id')
  if (expanded.value === id) {
    expanded.value = null
    return
  }
  expanded.value = id
  if (decisions.value[id] !== undefined) return
  try {
    const { data } = await requestApi.get(id)
    const d = unwrapOne(data)
    const approvals = pick(d, 'approvals') ?? []
    const last = approvals[approvals.length - 1] ?? {}
    decisions.value = {
      ...decisions.value,
      [id]: {
        decision: pick(last, 'decision') ?? pick(req, 'status'),
        reason: pick(last, 'reason') ?? '',
        category: pick(last, 'reason_category', 'reasonCategory') ?? '',
        decidedBy: pick(last, 'decided_by', 'decidedBy') ?? '',
        decidedAt: (pick(last, 'decided_at', 'decidedAt') || '').replace('T', ' ').slice(0, 16),
      },
    }
  } catch {
    decisions.value = { ...decisions.value, [id]: { error: true } }
  }
}

function goDetail(req) {
  router.push({ name: 'safety-detail', params: { id: pick(req, 'id') } })
}
</script>

<template>
  <div class="view">
    <div class="view__header">
      <div>
        <span class="view__eyebrow">안전 운영 허브</span>
        <h1 class="view__title">처리 내역</h1>
        <p class="view__desc">승인 또는 거절이 완료된 부품 교체 요청 이력입니다. 행을 펼치면 결정 사유를 볼 수 있습니다.</p>
      </div>
      <div class="hist-kpis">
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
        empty-text="처리된 요청이 없습니다."
        @retry="list.execute()"
      >
        <div class="table-scroll">
          <table class="data-table">
            <thead>
              <tr>
                <th>요청 ID / 설비 · 부품</th>
                <th>제품 유형</th>
                <th>요청자</th>
                <th>결정</th>
                <th>제출일</th>
                <th style="text-align: right">상세</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="req in filtered" :key="pick(req, 'id')">
                <tr>
                  <td>
                    <p class="hist-no">{{ pick(req, 'requestNo', 'request_no') ?? '-' }}</p>
                    <p class="hist-name">
                      {{ pick(req, 'equipment') ?? '-' }} · {{ pick(req, 'product_name', 'productName') ?? '-' }}
                    </p>
                  </td>
                  <td>{{ productTypeLabel(pick(req, 'product_type', 'productType')) }}</td>
                  <td>{{ pick(req, 'requester_name', 'requesterName') ?? '-' }}</td>
                  <td><StatusBadge :status="pick(req, 'status')" /></td>
                  <td>{{ (pick(req, 'submitted_at', 'submittedAt', 'created_at', 'createdAt') || '').replace('T', ' ').slice(0, 16) || '-' }}</td>
                  <td style="text-align: right">
                    <div class="row-actions">
                      <button class="btn btn--secondary btn--sm" @click="toggleDecision(req)">
                        {{ expanded === pick(req, 'id') ? '접기' : '결정 사유' }}
                      </button>
                      <button class="btn btn--primary btn--sm" @click="goDetail(req)">상세 보기</button>
                    </div>
                  </td>
                </tr>
                <tr v-if="expanded === pick(req, 'id')">
                  <td colspan="6">
                    <div class="hist-detail">
                      <template v-if="decisions[pick(req, 'id')] === undefined">불러오는 중…</template>
                      <template v-else-if="decisions[pick(req, 'id')].error">
                        결정 정보를 불러오지 못했습니다.
                      </template>
                      <template v-else>
                        <p>
                          <b>{{ decisions[pick(req, 'id')].decision === 'APPROVE' ? '승인' : '거절' }}</b>
                          <span v-if="decisions[pick(req, 'id')].decidedBy"> · {{ decisions[pick(req, 'id')].decidedBy }}</span>
                          <span v-if="decisions[pick(req, 'id')].decidedAt" class="hist-detail__at">
                            {{ decisions[pick(req, 'id')].decidedAt }}
                          </span>
                        </p>
                        <p v-if="decisions[pick(req, 'id')].category" class="hist-detail__cat">
                          분류: {{ decisions[pick(req, 'id')].category }}
                        </p>
                        <p v-if="decisions[pick(req, 'id')].reason" class="hist-detail__reason">
                          {{ decisions[pick(req, 'id')].reason }}
                        </p>
                        <p v-else class="hist-detail__reason hist-detail__reason--muted">사유 없음</p>
                      </template>
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
.hist-kpis {
  display: grid;
  grid-template-columns: repeat(3, minmax(120px, 1fr));
  gap: 12px;
  flex: 1;
  max-width: 480px;
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

.hist-no {
  font-size: 11px;
  color: var(--on-surface-variant);
}

.hist-name {
  font-weight: 500;
  margin-top: 2px;
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

.hist-detail {
  padding: 12px 14px;
  border-radius: var(--radius-sm);
  background: var(--surface-container-lowest);
  font-size: 13px;
  line-height: 1.6;
}

.hist-detail__at {
  color: var(--on-surface-variant);
  margin-left: 8px;
  font-size: 12px;
}

.hist-detail__cat {
  color: var(--on-surface-variant);
  font-size: 12px;
}

.hist-detail__reason {
  margin-top: 4px;
  white-space: pre-wrap;
}

.hist-detail__reason--muted {
  color: var(--outline);
}

@media (max-width: 900px) {
  .hist-kpis {
    grid-template-columns: repeat(3, 1fr);
    max-width: none;
    width: 100%;
  }
}
</style>
