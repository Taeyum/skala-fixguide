import { ref, shallowRef } from 'vue'

/**
 * 비동기 요청의 loading / error / data 상태 추적.
 * @param {Function} fn - Promise 를 반환하는 실행 함수
 * @param {{ immediate?: boolean, initial?: any }} options
 */
export function useAsyncState(fn, options = {}) {
  const { immediate = false, initial = null } = options

  const data = shallowRef(initial)
  const error = ref(null)
  const loading = ref(false)

  async function execute(...args) {
    loading.value = true
    error.value = null
    try {
      const result = await fn(...args)
      data.value = result
      return result
    } catch (err) {
      error.value = err
      throw err
    } finally {
      loading.value = false
    }
  }

  if (immediate) {
    execute()
  }

  return { data, error, loading, execute }
}
