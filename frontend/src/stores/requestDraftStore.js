import { defineStore } from 'pinia'
import { ref } from 'vue'

/**
 * E_02 → E_03 → E_04 화면 전이 시 작성 컨텍스트 유실 방지 (WRA_SCREENS_v2 · 5. 전이 맵)
 */
export const useRequestDraftStore = defineStore('requestDraft', () => {
  /** 현재 진행 중인 요청 id */
  const requestId = ref(null)
  /** 현재 agent run id (E_03 폴링용) */
  const runId = ref(null)
  /** E_02 에서 입력한 폼 스냅샷 (뒤로가기 대비) */
  const formSnapshot = ref(null)

  function setContext({ requestId: rid, runId: rrid } = {}) {
    if (rid !== undefined) requestId.value = rid
    if (rrid !== undefined) runId.value = rrid
  }

  function saveForm(snapshot) {
    formSnapshot.value = snapshot
  }

  function reset() {
    requestId.value = null
    runId.value = null
    formSnapshot.value = null
  }

  return { requestId, runId, formSnapshot, setContext, saveForm, reset }
})
