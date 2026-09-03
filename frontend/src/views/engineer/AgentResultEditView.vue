<script setup>
/** WRA_E_04 · AI 결과 확인·수정 (/requests/:id/result) ★핵심 */
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { requestApi } from '@/api/requestApi'
import { agentApi } from '@/api/agentApi'
import { pick, unwrapOne } from '@/api/normalize'
import { apiErrorMessage } from '@/api/errorMessage'
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
const meta = reactive({ requestNo: '', equipment: '', line: '' })

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
    locked.value = [STATUS.PENDING, STATUS.APPROVED].includes(pick(d, 'status'))
    meta.requestNo = pick(d, 'requestNo', 'request_no', 'requestId') ?? ''
    meta.equipment = pick(d, 'equipment') ?? ''
    meta.line = pick(d, 'line') ?? ''


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
  } catch (err) {
    errorMessage.value = apiErrorMessage(err, `${step} 결과 저장에 실패했습니다.`)
  } finally {
    savingStep.value = ''
  }
}

async function saveNote() {
  errorMessage.value = ''
  try {
    await requestApi.update(props.id, { engineer_note: engineerNote.value })
  } catch (err) {
    errorMessage.value = apiErrorMessage(err, '엔지니어 설명 저장에 실패했습니다.')
  }
}

async function submit() {
  errorMessage.value = ''
  submitting.value = true
  try {
    // 엔지니어 설명은 제출 요청 본문에 함께 실어 보낸다 (없으면 서버가 422)
    await requestApi.submitApproval(props.id, engineerNote.value.trim())
    router.push({ name: 'my-requests' })
  } catch (err) {
    errorMessage.value =
      err.response?.status === 422
        ? apiErrorMessage(err, '제출에 필요한 항목이 누락되었습니다. 결과를 확인해 주세요.')
        : apiErrorMessage(err, '제출에 실패했습니다. 잠시 후 다시 시도해 주세요.')
  } finally {
    submitting.value = false
  }
}

const canSubmit = computed(
  () => confirmed.value && !locked.value && engineerNote.value.trim().length > 0,
)

onMounted(load)
</script>

<template>
  <div class="e04">
    <InlineError :message="errorMessage" />

    <StateHandler :loading="loading" :error="loadError" @retry="load">
      <!-- 헤더 바 -->
      <div class="e04-bar">
        <div class="e04-bar__lead">
          <span class="e04-bar__mark"><span class="material-symbols-outlined">smart_toy</span></span>
          <div>
            <h1 class="e04-bar__title">
              {{ meta.requestNo || 'AI 결과' }} AI 검토 및 편집
              <span v-if="locked" class="e04-bar__badge">읽기 전용</span>
              <span v-else class="e04-bar__badge e04-bar__badge--draft">초안</span>
            </h1>
            <p class="e04-bar__sub">
              {{ [meta.equipment, meta.line].filter(Boolean).join(' · ') || '검토 항목을 확인하고 안전관리자에게 제출합니다.' }}
            </p>
          </div>
        </div>
        <div v-if="!locked" class="e04-bar__actions">
          <BaseButton variant="secondary" @click="saveNote">
            <span class="material-symbols-outlined">save</span> 임시 저장
          </BaseButton>
          <BaseButton variant="primary" :disabled="!canSubmit" :loading="submitting" @click="submit">
            <span class="material-symbols-outlined">send</span> 안전관리자에게 제출
          </BaseButton>
        </div>
      </div>

      <div v-if="locked" class="locked-banner">
        <span class="material-symbols-outlined">lock</span>
        이미 제출된 요청입니다. 결과는 읽기 전용입니다.
      </div>

      <div class="e04-grid">
        <!-- 좌: A1 · A2 -->
        <div class="e04-col">
          <section v-for="step in ['A1', 'A2']" :key="step" class="card">
            <div class="card__title">
              <span class="e04-tag">{{ step }}</span>
              <span class="material-symbols-outlined">{{ step === 'A1' ? 'tune' : 'gavel' }}</span>
              {{ step === 'A1' ? '규격 및 호환성' : '적용 규정' }}
            </div>
            <EditableItemList
              v-model="blocks[step].items"
              :columns="columns[step]"
              :add-label="step === 'A2' ? '법령 추가' : '항목 추가'"
              :disabled="locked"
            />
            <div v-if="!locked" class="btn-row" style="margin-top: 14px">
              <BaseButton variant="secondary" :loading="savingStep === step" @click="saveBlock(step)">
                <span class="material-symbols-outlined">save</span> {{ step }} 저장
              </BaseButton>
            </div>
          </section>
        </div>

        <!-- 우: A3 · 엔지니어 노트 -->
        <div class="e04-col">
          <section class="card">
            <div class="card__title">
              <span class="e04-tag">A3</span>
              <span class="material-symbols-outlined">verified_user</span>
              안전 문서 초안
            </div>
            <EditableItemList
              v-model="blocks.A3.items"
              :columns="columns.A3"
              add-label="문서 추가"
              :disabled="locked"
            />
            <div v-if="!locked" class="btn-row" style="margin-top: 14px">
              <BaseButton variant="secondary" :loading="savingStep === 'A3'" @click="saveBlock('A3')">
                <span class="material-symbols-outlined">save</span> A3 저장
              </BaseButton>
            </div>
          </section>

          <section class="card">
            <div class="card__title">
              <span class="material-symbols-outlined">edit_note</span>엔지니어 노트 및 사유
              <span class="req">제출 시 필수</span>
            </div>
            <div class="form-field">
              <label>수정 배경</label>
              <textarea
                v-model="engineerNote"
                class="form-control"
                rows="5"
                :disabled="locked"
                placeholder="AI 권장안과의 차이, 적용한 위험 완화 조치 등을 기재하세요."
              />
            </div>
            <template v-if="!locked">
              <ConfirmPanel v-model="confirmed" />
              <p class="e04-note-info">
                <span class="material-symbols-outlined">info</span>
                제출하면 편집 내용이 잠기고 안전관리자 검토 대기열로 이동합니다.
              </p>
            </template>
          </section>
        </div>
      </div>
    </StateHandler>
  </div>
</template>

<style scoped>
.e04 {
  display: flex;
  flex-direction: column;
  gap: 24px;
  max-width: 1180px;
}

/* 헤더 바 */
.e04-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  flex-wrap: wrap;
  padding: 18px 22px;
  background: var(--surface-container-low);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-sm);
}

.e04-bar__lead {
  display: flex;
  align-items: center;
  gap: 14px;
}

.e04-bar__mark {
  width: 42px;
  height: 42px;
  flex-shrink: 0;
  border-radius: var(--radius-md);
  background: var(--secondary-fixed);
  color: var(--primary);
  display: flex;
  align-items: center;
  justify-content: center;
}

.e04-bar__title {
  display: flex;
  align-items: center;
  gap: 10px;
  font-size: 20px;
  font-weight: 600;
  color: var(--on-surface);
}

.e04-bar__badge {
  padding: 2px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 500;
  background: var(--surface-container-highest);
  color: var(--on-surface-variant);
}

.e04-bar__badge--draft {
  background: var(--secondary-fixed);
  color: var(--on-secondary-fixed);
}

.e04-bar__sub {
  font-size: 12px;
  color: var(--on-surface-variant);
  margin-top: 2px;
}

.e04-bar__actions {
  display: flex;
  gap: 10px;
}

/* 2열 */
.e04-grid {
  display: grid;
  grid-template-columns: 1.35fr 1fr;
  gap: 24px;
  align-items: start;
}

.e04-col {
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0;
}

.e04-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: var(--radius-sm);
  background: var(--primary-container);
  color: #fff;
  font-size: 12px;
  font-weight: 600;
}

.card__title .e04-tag + .material-symbols-outlined {
  font-size: 20px;
}

.e04-note-info {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 14px;
  padding: 12px 14px;
  border-radius: var(--radius-md);
  background: var(--primary-fixed);
  color: var(--primary-hover);
  font-size: 12px;
  line-height: 1.5;
}

.e04-note-info .material-symbols-outlined {
  font-size: 16px;
}

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

@media (max-width: 1024px) {
  .e04-grid {
    grid-template-columns: 1fr;
  }
}
</style>
