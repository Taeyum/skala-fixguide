/**
 * 개발용 목(mock) 어댑터 — VITE_USE_MOCK=true 일 때 client.js 가 axios adapter 로 장착.
 * 백엔드 없이 C_00 → E_01~E_05 클릭 스루 확인용. 데이터는 메모리에만 존재(새로고침 시 초기화).
 */

const now = () => Date.now()
const iso = (d = new Date()) => d.toISOString()

// 샘플 사진(외부 요청 없이 렌더되는 인라인 SVG data URI)
const svgPhoto = (label, bg) =>
  `data:image/svg+xml;utf8,${encodeURIComponent(
    `<svg xmlns="http://www.w3.org/2000/svg" width="240" height="240"><rect width="240" height="240" fill="${bg}"/><circle cx="120" cy="110" r="46" fill="none" stroke="#ffffff" stroke-width="8"/><rect x="96" y="150" width="48" height="40" fill="#ffffff"/><text x="120" y="220" font-family="sans-serif" font-size="18" fill="#ffffff" text-anchor="middle">${label}</text></svg>`,
  )}`
const MOCK_PHOTO = svgPhoto('부품 사진 1', '#4d556b')
const MOCK_PHOTO2 = svgPhoto('부품 사진 2', '#515f74')

// ── 시드 데이터 ────────────────────────────────────────
let seq = 2000
const nextId = () => ++seq

const db = {
  requests: [
    mkRequest({ id: 1001, status: 'DRAFT', equipment: 'EQ-ETCH-04', product_name: 'VCR Diaphragm Valve', product_type: 'valve' }),
    mkRequest({ id: 1002, status: 'RUNNING', equipment: 'EQ-CVD-01', product_name: 'Gas Supply Fitting', product_type: 'fitting', run_id: 9001 }),
    mkRequest({ id: 1003, status: 'PENDING', equipment: 'EQ-PHOT-01', product_name: 'Dispense Nozzle Filter', product_type: 'filter', run_id: 9003, requester_name: '김엔지니어 (Fab 3)' }),
    mkRequest({ id: 1006, status: 'PENDING', equipment: 'EQ-ETCH-02', product_name: 'Electrostatic Chuck Fitting', product_type: 'fitting', run_id: 9006, requester_name: '박기술 (Fab 2)' }),
    mkRequest({ id: 1004, status: 'APPROVED', equipment: 'EQ-IMP-01', product_name: 'Beamline Regulator', product_type: 'regulator', run_id: 9004 }),
    mkRequest({
      id: 1005,
      status: 'REJECTED',
      equipment: 'EQ-CMP-01',
      product_name: 'Pad Conditioner Tube',
      product_type: 'fitting',
      run_id: 9005,
      reject_reason: '가스 누출 방지 인터록 검증 데이터가 누락되었습니다. 압력 테스트 결과를 첨부해 재제출해 주세요.',
    }),
  ],
  runs: {
    9001: { run_id: 9001, request_id: 1002, startedAt: now() },
    9003: { run_id: 9003, request_id: 1003, startedAt: now() - 60000 },
    9004: { run_id: 9004, request_id: 1004, startedAt: now() - 60000 },
    9005: { run_id: 9005, request_id: 1005, startedAt: now() - 60000 },
    9006: { run_id: 9006, request_id: 1006, startedAt: now() - 60000 },
  },
}

function mkRequest(over) {
  return {
    id: over.id,
    requester_id: 1,
    equipment: over.equipment ?? '',
    line: over.line ?? 'FAB-01 Line A1',
    substance: over.substance ?? 'WF6',
    operating_condition: over.operating_condition ?? '120°C, 3.5 bar',
    product_name: over.product_name ?? '',
    product_type: over.product_type ?? 'etc',
    spec_json: over.spec_json ?? {},
    symptom: over.symptom ?? '주기적 미세 누출 감지',
    site_memo: over.site_memo ?? '',
    engineer_note:
      over.engineer_note ??
      '표준 배치에서 높은 캐리어 가스 유량 시 미세 누출 경향이 있어 대체 등급으로 교체를 요청합니다. R&D 라인에서 500 사이클 사전 검증 완료.',
    requester_name: over.requester_name ?? '김엔지니어 (Fab 3)',
    status: over.status ?? 'DRAFT',
    approver_id: null,
    created_at: over.created_at ?? iso(new Date(now() - 86400000)),
    run_id: over.run_id ?? null,
    reject_reason: over.reject_reason ?? null,
  }
}

function sampleResults(requestId) {
  return [
    {
      id: requestId * 10 + 1,
      step: 'A1',
      content_json: {
        items: [
          { name: '압력 등급', status: '요구 3000 psig / 부품 3000 psig — 적합' },
          { name: '실링 재질', status: 'Viton → Kalrez 상향, 불소 세정 주기 대응' },
        ],
      },
    },
    {
      id: requestId * 10 + 2,
      step: 'A2',
      content_json: {
        items: [
          { code: '산업안전보건법 제118조', description: '유해·위험물질 취급 설비 안전조치' },
          { code: 'KOSHA GUIDE E-92-2020', description: '반도체 제조장비 안전지침' },
        ],
      },
    },
    {
      id: requestId * 10 + 3,
      step: 'A3',
      content_json: {
        items: [
          { content: '단계 1 — 메인 차단기 LOTO(잠금장치 ID-992) 확인' },
          { content: '단계 2 — 가스 라인 N2 퍼지 3회 후 잔류 독성 측정' },
        ],
      },
    },
  ]
}

function runSteps(run) {
  const elapsed = now() - run.startedAt
  const stage = elapsed < 3000 ? 0 : elapsed < 6000 ? 1 : elapsed < 9000 ? 2 : 3
  const mk = (i, text) => ({
    step: ['A1', 'A2', 'A3'][i],
    status: stage > i ? 'DONE' : stage === i ? 'RUNNING' : 'WAITING',
    result: stage > i ? { summary: text } : null,
  })
  return {
    overall_status: stage >= 3 ? 'DONE' : 'RUNNING',
    steps: [
      mk(0, '규격·호환 검토 완료 — 이상 없음'),
      mk(1, '적용 법령 2건 식별'),
      mk(2, '안전서류 초안 생성 완료'),
    ],
  }
}

// ── 라우팅 ────────────────────────────────────────────
function reply(status, data) {
  return { status, data }
}

function handle(config) {
  const method = (config.method || 'get').toLowerCase()
  const url = (config.url || '').split('?')[0]
  const body = parseBody(config.data)
  const q = config.params || {}

  // auth
  if (method === 'post' && url === '/auth/login') {
    const role = /safety|안전/i.test(body.email || '') ? 'safety' : 'engineer'
    return reply(200, {
      accessToken: `mock.${role}.${now()}`,
      user: { id: 1, name: role === 'safety' ? '이안전' : '김엔지니어', email: body.email, role },
    })
  }
  if (method === 'post' && url === '/auth/signup') return reply(201, {})

  // dashboard
  if (method === 'get' && url === '/dashboard/summary') {
    if (q.role === 'safety') {
      return reply(200, {
        pending: db.requests.filter((r) => r.status === 'PENDING').length,
        today_processed: 6,
        month_approved: 42,
        month_rejected: 8,
        reject_top5: [
          { reason: '위험성 평가 데이터 누락', count: 18 },
          { reason: '보호구(PPE) 체크리스트 미비', count: 11 },
          { reason: '교정 인증서 만료', count: 7 },
          { reason: '미승인 작업자 ID', count: 5 },
          { reason: '작업 일정 중복', count: 3 },
        ],
      })
    }
    return reply(200, { draft: 1, running: 1, pending: 1, approved: 1, rejected: 1 })
  }

  // work-requests 목록
  if (method === 'get' && url === '/work-requests') {
    let list = [...db.requests]
    if (q.status) list = list.filter((r) => r.status === q.status)
    return reply(200, list)
  }

  // work-requests 생성
  if (method === 'post' && url === '/work-requests') {
    const r = mkRequest({ ...body, id: nextId(), created_at: iso() })
    db.requests.unshift(r)
    return reply(201, { id: r.id })
  }

  // AI 검증 실행 — POST /agent-runs { workRequestId } (실제 백엔드 계약)
  if (method === 'post' && url === '/agent-runs') {
    const reqId = Number(body.workRequestId ?? body.work_request_id)
    const runId = nextId()
    db.runs[runId] = { run_id: runId, request_id: reqId, startedAt: now() }
    const req = db.requests.find((r) => r.id === reqId)
    if (req) {
      req.status = 'RUNNING'
      req.run_id = runId
    }
    return reply(202, {
      runId,
      run_id: runId,
      workRequestId: reqId,
      status: 'RUNNING',
      steps: [],
      pollIntervalMs: 2500,
    })
  }

  // /work-requests/:id ...
  const wr = url.match(/^\/work-requests\/(\d+)(\/[a-z-]+)?$/)
  if (wr) {
    const id = Number(wr[1])
    const sub = wr[2]
    const req = db.requests.find((r) => r.id === id)

    if (!sub && method === 'get') {
      if (!req) return reply(404, { message: 'not found' })
      return reply(200, {
        ...req,
        agent_results: sampleResults(id),
        approvals: req.reject_reason ? [{ decision: 'REJECT', reason: req.reject_reason }] : [],
      })
    }
    if (!sub && method === 'patch') {
      if (req) Object.assign(req, body)
      return reply(200, { ...(req || {}) })
    }
    if (sub === '/photos' && method === 'post') return reply(201, { id: nextId(), url: MOCK_PHOTO })
    if (sub === '/photos' && method === 'get') {
      return reply(200, [
        { id: id * 100 + 1, url: MOCK_PHOTO },
        { id: id * 100 + 2, url: MOCK_PHOTO2 },
      ])
    }
    if (sub === '/submit-approval' && method === 'patch') {
      if (req) req.status = 'PENDING'
      return reply(200, {})
    }
    if (sub === '/agent-runs' && method === 'post') {
      const runId = nextId()
      db.runs[runId] = { run_id: runId, request_id: id, startedAt: now() }
      if (req) {
        req.status = 'RUNNING'
        req.run_id = runId
      }
      return reply(202, { run_id: runId, overall_status: 'RUNNING' })
    }
    if (sub === '/approvals' && method === 'post') {
      if (req) {
        req.status = body.decision === 'REJECT' ? 'REJECTED' : 'APPROVED'
        if (body.decision === 'REJECT') req.reject_reason = body.reason
      }
      return reply(201, {})
    }
  }

  // agent-runs / agent-results
  const ar = url.match(/^\/agent-runs\/(\d+)$/)
  if (ar && method === 'get') {
    const run = db.runs[Number(ar[1])]
    if (!run) return reply(404, { message: 'no run' })
    return reply(200, { run_id: run.run_id, ...runSteps(run) })
  }
  if (url.match(/^\/agent-results\/\d+$/) && method === 'patch') return reply(200, {})

  return reply(404, { message: `mock: unhandled ${method.toUpperCase()} ${url}` })
}

function parseBody(data) {
  if (!data) return {}
  if (typeof data === 'string') {
    try {
      return JSON.parse(data)
    } catch {
      return {}
    }
  }
  return data // FormData 등
}

/** axios adapter */
export function mockAdapter(config) {
  const { status, data } = handle(config)
  const res = {
    data,
    status,
    statusText: status >= 400 ? 'Error' : 'OK',
    headers: {},
    config,
  }
  return new Promise((resolve, reject) => {
    setTimeout(() => {
      if (status >= 400) {
        const err = new Error(`Request failed with status code ${status}`)
        err.response = res
        err.config = config
        reject(err)
      } else {
        resolve(res)
      }
    }, 260)
  })
}
