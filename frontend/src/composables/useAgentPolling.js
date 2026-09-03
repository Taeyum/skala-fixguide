import { computed, onUnmounted, ref } from 'vue'
import { agentApi } from '@/api/agentApi'
import { STEP_STATUS } from '@/constants/workRequest'

/**
 * E_03 · AI 에이전트 폴링 (WRA_SCREENS_v2 · 5. E_03)
 * - 2~3초 간격 GET /agent-runs/{runId}
 * - 모든 스텝 DONE 시 즉시 폴링 중단
 * - onUnmounted 에서 타이머 해제 보장 (리소스 누수 방지)
 */
export function useAgentPolling(intervalMs = 2500) {
  const steps = ref([])
  const overallStatus = ref(null)
  const error = ref(null)
  const polling = ref(false)

  let timer = null
  let currentRunId = null

  const serverAllDone = ref(false)
  const allDone = computed(
    () =>
      serverAllDone.value ||
      (steps.value.length > 0 && steps.value.every((s) => s.status === STEP_STATUS.DONE)),
  )

  function stop() {
    polling.value = false
    if (timer) {
      clearInterval(timer)
      timer = null
    }
  }

  async function tick() {
    try {
      const { data } = await agentApi.getRun(currentRunId)
      steps.value = data.steps ?? data.agent_steps ?? []
      overallStatus.value = data.overall_status ?? data.overallStatus ?? null
      serverAllDone.value = data.allDone ?? false
      error.value = null
      if (allDone.value) {
        stop()
      }
    } catch (err) {
      error.value = err
    }
  }

  function start(runId) {
    stop()
    currentRunId = runId
    serverAllDone.value = false
    polling.value = true
    tick()
    timer = setInterval(tick, intervalMs)
  }

  onUnmounted(stop)

  return { steps, overallStatus, error, polling, allDone, start, stop, refresh: tick }
}
