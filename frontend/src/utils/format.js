/**
 * 날짜 표시 유틸. 백엔드는 ISO 8601(KST 오프셋)로 내려주므로 문자열을 자르지 않고
 * Date 로 파싱해 브라우저 로컬 시간대로 표시한다.
 */
const pad = (n) => String(n).padStart(2, '0')

export function fmtDate(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '-'
  return `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`
}

export function fmtDateTime(value) {
  if (!value) return '-'
  const d = new Date(value)
  if (Number.isNaN(d.getTime())) return '-'
  return `${fmtDate(d)} ${pad(d.getHours())}:${pad(d.getMinutes())}`
}
