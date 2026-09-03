<script setup>
/** WRA_E_02 · 부품 교체 요청 등록 (/requests/new) */
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { requestApi } from '@/api/requestApi'
import { agentApi } from '@/api/agentApi'
import { pick, unwrapOne } from '@/api/normalize'
import { apiErrorMessage } from '@/api/errorMessage'
import { PRODUCT_TYPES, STATUS } from '@/constants/workRequest'
import { useRequestDraftStore } from '@/stores/requestDraftStore'
import DynamicSpecFields from '@/components/domain/DynamicSpecFields.vue'
import PhotoThumbUploader from '@/components/domain/PhotoThumbUploader.vue'
import InlineError from '@/components/common/InlineError.vue'
import BaseButton from '@/components/common/BaseButton.vue'

const route = useRoute()
const router = useRouter()
const draftStore = useRequestDraftStore()

const form = reactive({
  equipment: '',
  line: '',
  substance: '',
  operating_condition: '',
  product_name: '',
  product_type: '',
  spec_json: {},
  symptom: '',
  site_memo: '',
})

const photos = ref([])
const draftId = ref(route.query.id ? String(route.query.id) : null)
const submitted = ref(false)
const errorMessage = ref('')
const savingDraft = ref(false)
const starting = ref(false)

const REQUIRED = ['equipment', 'line', 'substance', 'operating_condition', 'product_name', 'product_type']
const missing = ref([])

function validate() {
  missing.value = REQUIRED.filter((k) => !String(form[k] ?? '').trim())
  return missing.value.length === 0
}

function isInvalid(key) {
  return submitted.value && missing.value.includes(key)
}

function payload(status) {
  return { ...form, status }
}

async function persistDraft() {
  if (draftId.value) {
    await requestApi.update(draftId.value, payload(STATUS.DRAFT))
    return draftId.value
  }
  const { data } = await requestApi.create(payload(STATUS.DRAFT))
  draftId.value = String(pick(unwrapOne(data), 'id', 'workRequestId'))
  return draftId.value
}

async function uploadPhotos(id) {
  for (const file of photos.value) {
    await requestApi.uploadPhoto(id, file)
  }
  photos.value = []
}

async function onSaveDraft() {
  errorMessage.value = ''
  savingDraft.value = true
  try {
    const id = await persistDraft()
    await uploadPhotos(id)
    router.push({ name: 'my-requests' })
  } catch (err) {
    errorMessage.value = apiErrorMessage(err, '임시 저장에 실패했습니다.')
  } finally {
    savingDraft.value = false
  }
}

async function onStart() {
  submitted.value = true
  errorMessage.value = ''
  if (!validate()) {
    errorMessage.value = '필수 항목을 모두 입력해 주세요.'
    return
  }
  starting.value = true
  try {
    const id = await persistDraft()
    await uploadPhotos(id)
    const { data } = await agentApi.startRun(id)
    const runId = pick(unwrapOne(data), 'run_id', 'runId')
    draftStore.setContext({ requestId: id, runId })
    router.push({ name: 'agent-run', params: { id } })
  } catch (err) {
    errorMessage.value = apiErrorMessage(err, 'AI 검증을 시작하지 못했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    starting.value = false
  }
}

onMounted(async () => {
  if (!draftId.value) return
  try {
    const { data } = await requestApi.get(draftId.value)
    const d = unwrapOne(data)
    Object.keys(form).forEach((k) => {
      const v = pick(d, k, k.replace(/_([a-z])/g, (_, c) => c.toUpperCase()))
      if (v !== undefined) form[k] = v
    })
    if (!form.spec_json || typeof form.spec_json !== 'object') form.spec_json = {}
  } catch {
    errorMessage.value = '기존 초안을 불러오지 못했습니다.'
  }
})
</script>

<template>
  <div class="e02">
    <!-- 상단 단계 인디케이터 -->
    <div class="e02-step">
      <div class="e02-step__lead">
        <div class="e02-step__badge">01</div>
        <div>
          <h1 class="e02-step__title">부품 교체 요청 등록</h1>
          <p class="e02-step__sub">WRA-E-02: 작업 요청 승인 및 안전 검증 프로토콜</p>
        </div>
      </div>
      <div class="e02-step__chips">
        <span class="e02-chip e02-chip--accent">AI 어시스턴트 활성</span>
        <span class="e02-chip">1단계 / 2단계</span>
      </div>
    </div>

    <InlineError :message="errorMessage" />

    <form class="e02-grid" @submit.prevent="onStart">
      <!-- 좌: 기본 정보 · 제품 정보 -->
      <div class="e02-col e02-col--main">
        <section class="e02-card">
          <header class="e02-card__head">
            <span class="material-symbols-outlined e02-card__icon">info</span>
            <h2>1. 요청 기본 정보</h2>
          </header>
          <div class="form-grid">
            <div class="form-field">
              <label for="equipment">설비 선택 <span class="req">*</span></label>
              <input
                id="equipment"
                v-model.trim="form.equipment"
                class="form-control"
                :class="{ 'is-invalid': isInvalid('equipment') }"
                placeholder="예: EQ-ETCH-04"
              />
            </div>
            <div class="form-field">
              <label for="line">라인 지정 <span class="req">*</span></label>
              <input
                id="line"
                v-model.trim="form.line"
                class="form-control"
                :class="{ 'is-invalid': isInvalid('line') }"
                placeholder="예: FAB-01 Line A1"
              />
            </div>
            <div class="form-field">
              <label for="substance">사용 물질 <span class="req">*</span></label>
              <input
                id="substance"
                v-model.trim="form.substance"
                class="form-control"
                :class="{ 'is-invalid': isInvalid('substance') }"
                placeholder="예: WF6 (독성/부식성)"
              />
            </div>
            <div class="form-field">
              <label for="op">운전 조건 <span class="req">*</span></label>
              <input
                id="op"
                v-model.trim="form.operating_condition"
                class="form-control"
                :class="{ 'is-invalid': isInvalid('operating_condition') }"
                placeholder="예: 120°C, 3.5 bar, Flow 50 sccm"
              />
            </div>
          </div>
        </section>

        <section class="e02-card">
          <header class="e02-card__head e02-card__head--between">
            <div class="e02-card__head-l">
              <span class="material-symbols-outlined e02-card__icon">settings_b_roll</span>
              <h2>2. 제품 정보 (AI 전송 핵심)</h2>
            </div>
            <span class="e02-hint">
              <span class="material-symbols-outlined">bolt</span> 실시간 AI 파싱
            </span>
          </header>
          <div class="form-grid">
            <div class="form-field">
              <label for="pname">제품명 / 모델명 <span class="req">*</span></label>
              <input
                id="pname"
                v-model.trim="form.product_name"
                class="form-control"
                :class="{ 'is-invalid': isInvalid('product_name') }"
                placeholder="예: Swagelok VCR Diaphragm Valve (SS-4H-VCR)"
              />
            </div>
            <div class="form-field">
              <label for="ptype">제품 유형 <span class="req">*</span></label>
              <select
                id="ptype"
                v-model="form.product_type"
                class="form-control"
                :class="{ 'is-invalid': isInvalid('product_type') }"
                @change="form.spec_json = {}"
              >
                <option value="">유형을 선택하세요</option>
                <option v-for="t in PRODUCT_TYPES" :key="t.value" :value="t.value">{{ t.label }}</option>
              </select>
            </div>
          </div>
          <DynamicSpecFields v-model="form.spec_json" :product-type="form.product_type" />
        </section>
      </div>

      <!-- 우: 사진 · 메모 · 액션 -->
      <div class="e02-col e02-col--side">
        <section class="e02-card">
          <header class="e02-card__head">
            <span class="material-symbols-outlined e02-card__icon">photo_camera</span>
            <h2>3. 제품 사진 업로드</h2>
          </header>
          <PhotoThumbUploader v-model:files="photos" />
        </section>

        <section class="e02-card">
          <header class="e02-card__head">
            <span class="material-symbols-outlined e02-card__icon">note_alt</span>
            <h2>4. 증상 및 현장 메모</h2>
          </header>
          <div class="form-field">
            <textarea
              id="symptom"
              v-model="form.symptom"
              class="form-control"
              rows="4"
              placeholder="교체 사유, 누출 여부, 오작동 증상 등을 상세히 기록하세요…"
            />
          </div>
        </section>

        <div class="e02-actions">
          <BaseButton type="submit" variant="primary" block class="e02-submit" :loading="starting">
            <span class="material-symbols-outlined">smart_toy</span> AI 검증 시작
          </BaseButton>
          <BaseButton type="button" variant="secondary" block :loading="savingDraft" @click="onSaveDraft">
            <span class="material-symbols-outlined">save</span> 임시 저장
          </BaseButton>
        </div>
      </div>
    </form>
  </div>
</template>

<style scoped>
.e02 {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 1180px;
}

/* 단계 인디케이터 */
.e02-step {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 20px 24px;
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
}

.e02-step__lead {
  display: flex;
  align-items: center;
  gap: 16px;
}

.e02-step__badge {
  width: 40px;
  height: 40px;
  flex-shrink: 0;
  border-radius: 50%;
  background: var(--primary);
  color: var(--on-primary);
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 17px;
  font-weight: 600;
}

.e02-step__title {
  font-size: 22px;
  font-weight: 600;
  color: var(--on-surface);
  letter-spacing: -0.005em;
}

.e02-step__sub {
  font-size: 12px;
  color: var(--on-surface-variant);
  margin-top: 2px;
}

.e02-step__chips {
  display: flex;
  align-items: center;
  gap: 8px;
}

.e02-chip {
  padding: 4px 12px;
  border-radius: 999px;
  font-size: 12px;
  font-weight: 500;
  background: var(--surface-container-highest);
  color: var(--on-surface-variant);
  white-space: nowrap;
}

.e02-chip--accent {
  background: var(--secondary-fixed);
  color: #0d1c2f;
}

/* 2열 그리드 */
.e02-grid {
  display: grid;
  grid-template-columns: 2fr 1fr;
  gap: 24px;
  align-items: start;
}

.e02-col {
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0;
}

/* 카드 (목업: bg-surface-container-low · rounded-xl · shadow-sm · p-8) */
.e02-card {
  background: var(--surface-container-low);
  border-radius: var(--radius-md);
  box-shadow: var(--shadow-sm);
  padding: 28px;
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.e02-card__head {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-bottom: 16px;
  border-bottom: 1px solid rgba(195, 198, 215, 0.35);
}

.e02-card__head--between {
  justify-content: space-between;
}

.e02-card__head-l {
  display: flex;
  align-items: center;
  gap: 12px;
}

.e02-card__head h2 {
  font-size: 17px;
  font-weight: 600;
  color: var(--on-surface);
}

.e02-card__icon {
  font-size: 22px;
  color: var(--primary);
  font-variation-settings: 'FILL' 1;
}

.e02-hint {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 12px;
  font-weight: 500;
  color: var(--primary);
}

.e02-hint .material-symbols-outlined {
  font-size: 16px;
}

/* 입력 필드 — 목업의 흰 배경 h-12 룩 */
.e02-card :deep(.form-control) {
  min-height: 46px;
  background: var(--surface-container-lowest);
}

.e02-card :deep(textarea.form-control) {
  min-height: 96px;
}

/* 우측 액션 */
.e02-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.e02-submit {
  height: 52px;
  font-size: 16px;
  font-weight: 600;
  box-shadow: 0 6px 16px -4px rgba(0, 74, 198, 0.35);
}

@media (max-width: 1024px) {
  .e02-grid {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 560px) {
  .e02-step {
    align-items: flex-start;
  }
}
</style>
