<script setup>
/** WRA_S_02 · 요청 상세 · 승인/거절 (/manage/requests/:id) ★핵심 */
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { requestApi } from '@/api/requestApi'
import { approvalApi } from '@/api/approvalApi'
import { pick, unwrapList, unwrapOne } from '@/api/normalize'
import { apiErrorMessage } from '@/api/errorMessage'
import { STATUS, productTypeLabel } from '@/constants/workRequest'
import { fmtDate } from '@/utils/format'
import StatusBadge from '@/components/common/StatusBadge.vue'
import StateHandler from '@/components/common/StateHandler.vue'
import InlineError from '@/components/common/InlineError.vue'
import BaseButton from '@/components/common/BaseButton.vue'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()

const detail = ref(null)
const photos = ref([])
const loading = ref(true)
const loadError = ref(null)

const reason = ref('')
const reasonInvalid = ref(false)
const errorMessage = ref('')
const deciding = ref('')
const zoomPhoto = ref(null)

const reasonField = ref(null)

const info = computed(() => {
  const d = detail.value ?? {}
  return {
    status: pick(d, 'status'),
    requestNo: pick(d, 'requestNo', 'request_no') ?? '',
    equipment: pick(d, 'equipment') ?? '-',
    line: pick(d, 'line') ?? '-',
    substance: pick(d, 'substance') ?? '-',
    operating: pick(d, 'operating_condition', 'operatingCondition') ?? '-',
    productName: pick(d, 'product_name', 'productName') ?? '-',
    productType: productTypeLabel(pick(d, 'product_type', 'productType')),
    spec: pick(d, 'spec_json', 'specJson') ?? {},
    requester: pick(d, 'requester_name', 'requesterName') ?? pick(d, 'requester_id', 'requesterId') ?? '-',
    symptom: pick(d, 'symptom') ?? '',
    engineerNote: pick(d, 'engineer_note', 'engineerNote') ?? '',
    submittedAt: fmtDate(pick(d, 'submitted_at', 'submittedAt', 'created_at', 'createdAt')),
  }
})

const results = computed(() => {
  const raw = pick(detail.value ?? {}, 'agent_results', 'agentResults') ?? []
  return ['A1', 'A2', 'A3'].map((step) => {
    const block = raw.find((r) => (pick(r, 'step', 'code') ?? '').toUpperCase() === step)
    const content = pick(block ?? {}, 'content_json', 'contentJson') ?? {}
    const items = Array.isArray(content) ? content : (content.items ?? [])
    return {
      step,
      title: step === 'A1' ? '규격·호환' : step === 'A2' ? '적용 법령' : '안전서류',
      lines: items.map((it) => Object.values(it).filter(Boolean).join(' · ')),
    }
  })
})

const decided = computed(() =>
  [STATUS.APPROVED, STATUS.REJECTED].includes(info.value.status),
)

async function load() {
  loading.value = true
  loadError.value = null
  try {
    const { data } = await requestApi.get(props.id)
    detail.value = unwrapOne(data)
    try {
      const res = await requestApi.photos(props.id)
      photos.value = unwrapList(res.data)
    } catch {
      photos.value = []
    }
  } catch (err) {
    loadError.value = err
  } finally {
    loading.value = false
  }
}

async function approve() {
  errorMessage.value = ''
  deciding.value = 'APPROVE'
  try {
    await approvalApi.decide(props.id, 'APPROVE')
    router.push({ name: 'safety-manage' })
  } catch (err) {
    errorMessage.value = apiErrorMessage(err, '승인 처리에 실패했습니다.')
  } finally {
    deciding.value = ''
  }
}

const REJECT_REASON_MIN = 10

async function reject() {
  errorMessage.value = ''
  if (reason.value.trim().length < REJECT_REASON_MIN) {
    reasonInvalid.value = true
    errorMessage.value = `거절 사유는 ${REJECT_REASON_MIN}자 이상 입력해 주세요.`
    reasonField.value?.focus()
    return
  }
  deciding.value = 'REJECT'
  try {
    await approvalApi.decide(props.id, 'REJECT', reason.value.trim())
    router.push({ name: 'safety-manage' })
  } catch (err) {
    errorMessage.value = apiErrorMessage(err, '거절 처리에 실패했습니다.')
  } finally {
    deciding.value = ''
  }
}

onMounted(load)
</script>

<template>
  <div class="view">
    <div class="s2-head">
      <div class="s2-head__l">
        <button class="s2-back" @click="router.push({ name: 'safety-manage' })">
          <span class="material-symbols-outlined">arrow_back</span>
        </button>
        <div>
          <div class="s2-head__tags">
            <span v-if="info.requestNo" class="s2-head__no">{{ info.requestNo }}</span>
            <StatusBadge v-if="info.status" :status="info.status" />
          </div>
          <h1 class="view__title">부품 교체 요청 상세</h1>
          <p class="view__desc">AI 결과와 엔지니어 설명을 검토한 뒤 승인 또는 거절하세요.</p>
        </div>
      </div>
      <div v-if="info.requester && info.requester !== '-'" class="s2-head__requester">
        <span class="s2-head__rlabel">제출자</span>
        <span class="s2-head__rname">{{ info.requester }}</span>
        <span class="s2-head__avatar">{{ info.requester.slice(0, 1) }}</span>
      </div>
    </div>

    <StateHandler :loading="loading" :error="loadError" @retry="load">
      <div class="s2-grid">
        <!-- 좌: 상세 -->
        <div class="s2-main">
          <div class="card">
            <div class="card__title">
              <span class="material-symbols-outlined">description</span>요청 정보
              <StatusBadge v-if="info.status" :status="info.status" style="margin-left: auto" />
            </div>
            <div class="kv">
              <div><span>설비 / 라인</span><p>{{ info.equipment }} / {{ info.line }}</p></div>
              <div><span>물질 / 운전 조건</span><p>{{ info.substance }} / {{ info.operating }}</p></div>
              <div><span>제품명 / 유형</span><p>{{ info.productName }} / {{ info.productType }}</p></div>
              <div><span>요청자</span><p>{{ info.requester }}</p></div>
              <div><span>제출일</span><p>{{ info.submittedAt || '-' }}</p></div>
              <div v-if="info.symptom"><span>증상</span><p>{{ info.symptom }}</p></div>
            </div>
          </div>

          <div v-if="photos.length" class="card">
            <div class="card__title"><span class="material-symbols-outlined">photo_camera</span>제품 사진</div>
            <div class="s2-photos">
              <button
                v-for="p in photos"
                :key="pick(p, 'id')"
                class="s2-photo"
                @click="zoomPhoto = pick(p, 'url')"
              >
                <img :src="pick(p, 'url')" alt="제품 사진" />
              </button>
            </div>
          </div>

          <div class="card">
            <div class="card__title">
              <span class="material-symbols-outlined">auto_awesome</span>AI 검증 결과 (읽기 전용)
            </div>
            <div class="s2-results">
              <div v-for="r in results" :key="r.step" class="s2-result">
                <p class="s2-result__h">{{ r.step }} · {{ r.title }}</p>
                <ul v-if="r.lines.length">
                  <li v-for="(l, i) in r.lines" :key="i">{{ l }}</li>
                </ul>
                <p v-else class="s2-result__empty">결과 없음</p>
              </div>
            </div>
          </div>

          <div class="card">
            <div class="card__title"><span class="material-symbols-outlined">notes</span>엔지니어 설명</div>
            <p class="s2-note">{{ info.engineerNote || '작성된 설명이 없습니다.' }}</p>
          </div>
        </div>

        <!-- 우: 승인 패널 -->
        <aside class="s2-panel">
          <div class="card">
            <div class="card__title"><span class="material-symbols-outlined">verified_user</span>안전 승인</div>

            <InlineError :message="errorMessage" />

            <div v-if="decided" class="s2-decided">
              <StatusBadge :status="info.status" />
              이미 처리된 요청입니다.
            </div>

            <template v-else>
              <div class="form-field" style="margin-bottom: 14px">
                <label for="reason">거절 사유 <span class="req">거절 시 필수 · 10자 이상</span></label>
                <textarea
                  id="reason"
                  ref="reasonField"
                  v-model="reason"
                  class="form-control"
                  :class="{ 'is-invalid': reasonInvalid }"
                  rows="4"
                  placeholder="거절하는 경우 안전·규정 준수 사유를 10자 이상 입력하세요."
                  @input="reasonInvalid = false"
                />
                <p class="reason-count" :class="{ 'is-short': reason.trim().length > 0 && reason.trim().length < 10 }">
                  {{ reason.trim().length }} / 10자
                </p>
              </div>

              <div class="btn-row" style="flex-direction: column">
                <BaseButton
                  variant="primary"
                  block
                  :loading="deciding === 'APPROVE'"
                  :disabled="!!deciding"
                  @click="approve"
                >
                  <span class="material-symbols-outlined">check</span> 요청 승인
                </BaseButton>
                <BaseButton
                  variant="danger-outline"
                  block
                  :loading="deciding === 'REJECT'"
                  :disabled="!!deciding"
                  @click="reject"
                >
                  <span class="material-symbols-outlined">close</span> 요청 거절
                </BaseButton>
              </div>
            </template>
          </div>
        </aside>
      </div>
    </StateHandler>

    <div v-if="zoomPhoto" class="s2-zoom" @click="zoomPhoto = null">
      <img :src="zoomPhoto" alt="제품 사진 원본" />
    </div>
  </div>
</template>

<style scoped>
.s2-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
}

.s2-head__l {
  display: flex;
  gap: 14px;
}

.s2-head__tags {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 6px;
}

.s2-head__no {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 600;
  background: var(--secondary-fixed);
  color: var(--on-primary-fixed-variant, #003ea8);
}

.s2-head__requester {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
}

.s2-head__rlabel {
  color: var(--on-surface-variant);
  font-size: 11px;
}

.s2-head__rname {
  font-weight: 500;
}

.s2-head__avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: var(--primary);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 13px;
  font-weight: 600;
}

.s2-back {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: var(--radius-sm);
  background: var(--surface-container);
  color: var(--on-surface-variant);
  padding: 0;
  flex-shrink: 0;
}

.s2-back .material-symbols-outlined {
  font-size: 18px;
}

.s2-grid {
  display: grid;
  grid-template-columns: 1fr 340px;
  gap: 24px;
  align-items: start;
}

.s2-main {
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0;
}

.s2-panel {
  position: sticky;
  top: 80px;
}

.kv {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 12px;
}

.kv > div {
  background: var(--surface-container-lowest);
  padding: 10px 12px;
  border-radius: var(--radius-sm);
}

.kv span {
  font-size: 11px;
  color: var(--on-surface-variant);
}

.kv p {
  font-size: 13px;
  font-weight: 500;
  margin-top: 3px;
}

.s2-photos {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.s2-photo {
  width: 96px;
  height: 96px;
  padding: 0;
  border: none;
  border-radius: var(--radius-sm);
  overflow: hidden;
  cursor: pointer;
}

.s2-photo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.s2-results {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.s2-result {
  background: var(--surface-container-lowest);
  padding: 12px 14px;
  border-radius: var(--radius-sm);
}

.s2-result__h {
  font-size: 13px;
  font-weight: 600;
  margin-bottom: 6px;
}

.s2-result ul {
  margin: 0;
  padding-left: 18px;
  font-size: 13px;
  color: var(--on-surface-variant);
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.s2-result__empty {
  font-size: 12px;
  color: var(--outline);
}

.s2-note {
  font-size: 13px;
  color: var(--on-surface-variant);
  background: var(--surface-container-lowest);
  padding: 14px;
  border-radius: var(--radius-sm);
  white-space: pre-wrap;
  line-height: 1.6;
}

.s2-decided {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: var(--on-surface-variant);
}

.form-field .req {
  color: var(--error);
  font-weight: 400;
}

.reason-count {
  margin-top: 4px;
  font-size: 11px;
  color: var(--on-surface-variant);
  text-align: right;
}

.reason-count.is-short {
  color: var(--error);
}

.s2-zoom {
  position: fixed;
  inset: 0;
  background: rgba(3, 20, 39, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px;
  z-index: 100;
  cursor: zoom-out;
}

.s2-zoom img {
  max-width: 90vw;
  max-height: 90vh;
  border-radius: var(--radius-md);
}

@media (max-width: 960px) {
  .s2-grid {
    grid-template-columns: 1fr;
  }
  .s2-panel {
    position: static;
  }
  .kv {
    grid-template-columns: 1fr;
  }
}
</style>
