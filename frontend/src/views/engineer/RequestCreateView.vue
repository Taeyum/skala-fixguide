<script setup>
/** WRA_E_02 · 부품 교체 요청 등록 (/requests/new) */
import { onMounted, reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { requestApi } from '@/api/requestApi'
import { agentApi } from '@/api/agentApi'
import { pick, unwrapOne } from '@/api/normalize'
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
  } catch {
    errorMessage.value = '임시 저장에 실패했습니다.'
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
  } catch {
    errorMessage.value = 'AI 검증을 시작하지 못했습니다. 잠시 후 다시 시도해 주세요.'
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
  <div class="view">
    <div class="view__header">
      <div>
        <span class="view__eyebrow">WRA-E-02</span>
        <h1 class="view__title">부품 교체 요청 등록</h1>
        <p class="view__desc">입력값 전체가 AI 검증(A1·A2·A3)에 전달됩니다.</p>
      </div>
    </div>

    <InlineError :message="errorMessage" />

    <form class="view" @submit.prevent="onStart">
      <!-- 1. 기본 정보 -->
      <div class="card">
        <div class="card__title"><span class="material-symbols-outlined">info</span>1. 요청 기본 정보</div>
        <div class="form-grid">
          <div class="form-field">
            <label for="equipment">설비 <span class="req">*</span></label>
            <input
              id="equipment"
              v-model.trim="form.equipment"
              class="form-control"
              :class="{ 'is-invalid': isInvalid('equipment') }"
              placeholder="예: EQ-ETCH-04"
            />
          </div>
          <div class="form-field">
            <label for="line">라인 <span class="req">*</span></label>
            <input
              id="line"
              v-model.trim="form.line"
              class="form-control"
              :class="{ 'is-invalid': isInvalid('line') }"
              placeholder="예: FAB-01 Line A1"
            />
          </div>
          <div class="form-field">
            <label for="substance">물질 <span class="req">*</span></label>
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
              placeholder="예: 120°C, 3.5 bar, 50 sccm"
            />
          </div>
        </div>
      </div>

      <!-- 2. 제품 정보 + 동적 스펙 -->
      <div class="card">
        <div class="card__title">
          <span class="material-symbols-outlined">category</span>2. 제품 정보 (AI 전송 핵심)
        </div>
        <div class="form-grid" style="margin-bottom: 16px">
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
      </div>

      <!-- 3. 사진 -->
      <div class="card">
        <div class="card__title">
          <span class="material-symbols-outlined">photo_camera</span>3. 제품 사진 업로드
        </div>
        <PhotoThumbUploader v-model:files="photos" />
      </div>

      <!-- 4. 증상 / 메모 -->
      <div class="card">
        <div class="card__title"><span class="material-symbols-outlined">note_alt</span>4. 증상 및 현장 메모</div>
        <div class="form-field" style="margin-bottom: 12px">
          <label for="symptom">증상</label>
          <textarea id="symptom" v-model="form.symptom" class="form-control" placeholder="누출 여부, 오작동 증상 등" />
        </div>
        <div class="form-field">
          <label for="memo">현장 확인 메모</label>
          <textarea id="memo" v-model="form.site_memo" class="form-control" placeholder="현장 점검 내용" />
        </div>
      </div>

      <div class="btn-row">
        <BaseButton type="submit" variant="primary" :loading="starting">
          <span class="material-symbols-outlined">smart_toy</span> AI 검증 시작
        </BaseButton>
        <BaseButton type="button" variant="secondary" :loading="savingDraft" @click="onSaveDraft">
          <span class="material-symbols-outlined">save</span> 임시 저장
        </BaseButton>
      </div>
    </form>
  </div>
</template>
