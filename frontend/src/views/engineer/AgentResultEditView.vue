<script setup>
/** WRA_E_04 · AI 결과 확인·수정 (/requests/:id/result) ★핵심 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { requestApi } from '@/api/requestApi'
import { agentApi } from '@/api/agentApi'
import { pick, unwrapOne } from '@/api/normalize'
import { STATUS } from '@/constants/workRequest'
import EditableItemList from '@/components/domain/EditableItemList.vue'
import ConfirmPanel from '@/components/domain/ConfirmPanel.vue'
import InlineError from '@/components/common/InlineError.vue'
import StateHandler from '@/components/common/StateHandler.vue'
import BaseButton from '@/components/common/BaseButton.vue'

const props = defineProps({ id: { type: String, required: true } })
const router = useRouter()

const loading = ref(true)
const loadError = ref(null)
const errorMessage = ref('')
const savingStep = ref('')
const submitting = ref(false)
const locked = ref(false)
const confirmed = ref(false)

/** step 코드 → { resultId, items } */
const blocks = reactive({
  A1: { resultId: null, items: [] },
  A2: { resultId: null, items: [] },
  A3: { resultId: null, items: [] },
})
const engineerNote = ref('')

// 백엔드 결과 스키마(API 명세서 5.13): A1·A2 는 items[{itemId,text}], A3 는 documents[{docId,type,name,content}]
const columns = {
  A1: [{ key: 'text', label: '규격·호환성 검토 항목', textarea: true }],
  A2: [{ key: 'text', label: '적용 법령·조문', textarea: true }],
  A3: [
    { key: 'name', label: '문서' },
    { key: 'content', label: '내용', textarea: true },
  ],
}

function toItems(contentJson) {
  if (!contentJson) return []
  if (Array.isArray(contentJson)) return contentJson
  return contentJson.items ?? contentJson.list ?? []
}

async function load() {
  loading.value = true
  loadError.value = null
  try {
    const { data } = await requestApi.get(props.id)
    const d = unwrapOne(data)
    engineerNote.value = pick(d, 'engineer_note', 'engineerNote') ?? ''
    locked.value = [STATUS.PENDING, STATUS.APPROVED, STATUS.REJECTED].includes(pick(d, 'status'))

    const results = pick(d, 'agent_results', 'agentResults') ?? []
    for (const r of results) {
      const step = (pick(r, 'step', 'code') ?? '').toUpperCase()
      if (blocks[step]) {
        blocks[step].resultId = pick(r, 'id')
        blocks[step].items = toItems(pick(r, 'content_json', 'contentJson'))
      }
    }
  } catch (err) {
    loadError.value = err
  } finally {
    loading.value = false
  }
}

async function saveBlock(step) {
  if (!blocks[step].resultId) {
    errorMessage.value = `${step} 결과 ID를 찾을 수 없어 저장할 수 없습니다.`
    return
  }
  errorMessage.value = ''
  savingStep.value = step
  try {
    const body =
      step === 'A3' ? { documents: blocks[step].items } : { items: blocks[step].items }
    await agentApi.updateResult(blocks[step].resultId, body)
  } catch {
    errorMessage.value = `${step} 결과 저장에 실패했습니다.`
  } finally {
    savingStep.value = ''
  }
}

async function saveNote() {
  errorMessage.value = ''
  try {
    await requestApi.update(props.id, { engineer_note: engineerNote.value })
  } catch {
    errorMessage.value = '엔지니어 설명 저장에 실패했습니다.'
  }
}

async function submit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    await saveNote()
    await requestApi.submitApproval(props.id)
    router.push({ name: 'my-requests' })
  } catch (err) {
    if (err.response?.status === 422) {
      errorMessage.value = '제출에 필요한 항목이 누락되었습니다. 결과를 확인해 주세요.'
    } else {
      errorMessage.value = '제출에 실패했습니다. 잠시 후 다시 시도해 주세요.'
    }
  } finally {
    submitting.value = false
  }
}

const canSubmit = computed(() => confirmed.value && !locked.value)

onMounted(load)
</script>

<template>
  <div class="view">
    <div class="view__header">
      <div>
        <span class="view__eyebrow">WRA-E-04</span>
        <h1 class="view__title">AI 결과 확인 · 수정</h1>
        <p class="view__desc">각 항목을 검토·수정하고 안전관리자에게 제출합니다.</p>
      </div>
    </div>

    <InlineError :message="errorMessage" />

    <StateHandler :loading="loading" :error="loadError" @retry="load">
      <div v-if="locked" class="locked-banner">
        <span class="material-symbols-outlined">lock</span>
        이미 제출된 요청입니다. 결과는 읽기 전용입니다.
      </div>

      <div v-for="step in ['A1', 'A2', 'A3']" :key="step" class="card">
        <div class="card__title">
          <span class="material-symbols-outlined">
            {{ step === 'A1' ? 'tune' : step === 'A2' ? 'gavel' : 'verified_user' }}
          </span>
          {{ step }} ·
          {{ step === 'A1' ? '규격·호환' : step === 'A2' ? '적용 법령' : '안전서류 초안' }}
        </div>

        <EditableItemList
          v-model="blocks[step].items"
          :columns="columns[step]"
          :add-label="step === 'A2' ? '법령 추가' : '항목 추가'"
          :disabled="locked"
        />

        <div v-if="!locked" class="btn-row" style="margin-top: 12px">
          <BaseButton
            variant="secondary"
            :loading="savingStep === step"
            @click="saveBlock(step)"
          >
            <span class="material-symbols-outlined">save</span> {{ step }} 저장
          </BaseButton>
        </div>
      </div>

      <div class="card">
        <div class="card__title">
          <span class="material-symbols-outlined">edit_note</span>엔지니어 설명 (안전관리자 전달)
        </div>
        <div class="form-field">
          <textarea
            v-model="engineerNote"
            class="form-control"
            rows="4"
            :disabled="locked"
            placeholder="AI 권장안과의 차이, 적용한 위험 완화 조치 등을 기재하세요."
          />
        </div>
      </div>

      <template v-if="!locked">
        <ConfirmPanel v-model="confirmed" />
        <div class="btn-row">
          <BaseButton variant="primary" :disabled="!canSubmit" :loading="submitting" @click="submit">
            <span class="material-symbols-outlined">send</span> 안전관리자에게 제출
          </BaseButton>
          <BaseButton variant="secondary" @click="saveNote">
            <span class="material-symbols-outlined">save</span> 설명 임시 저장
          </BaseButton>
        </div>
      </template>
    </StateHandler>
  </div>
</template>

<style scoped>
.locked-banner {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  background: var(--surface-container-high);
  color: var(--on-surface-variant);
  font-size: 13px;
}

.locked-banner .material-symbols-outlined {
  font-size: 18px;
}
</style>
