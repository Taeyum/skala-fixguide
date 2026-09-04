<script setup>
/**
 * 랜딩 / 온보딩 (`/`, public) — ARGUS WRA
 * 다크 "관제 콘솔" 톤. 앱 본화면 진입 전 서비스 소개 + 로그인/가입.
 * 인증 사용자는 router beforeEnter에서 역할 홈으로 리다이렉트.
 */
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'
import { useAuthStore } from '@/stores/authStore'

const auth = useAuthStore()
const homePath = computed(() => auth.homeRoute())

const reduceMotion =
  typeof window !== 'undefined' &&
  window.matchMedia?.('(prefers-reduced-motion: reduce)').matches

/* ── 히어로 검증 콘솔 시뮬레이션 ─────────────────────── */
const agents = [
  { id: 'A1', label: '규격·호환 검증', note: '압력 등급 3000 psig · 실링 Kalrez 상향' },
  { id: 'A2', label: '적용 법령 식별', note: '산업안전보건법 §118 · KOSHA GUIDE E-92' },
  { id: 'A3', label: '안전서류 초안', note: 'LOTO 절차 · N2 퍼지 3회 · 잔류 독성 측정' },
]
const stage = ref(0) // 0..3 (3 = 전체 완료)
let simTimer = null

function stepSim() {
  stage.value = stage.value >= 3 ? 0 : stage.value + 1
}
function agentState(i) {
  if (stage.value > i) return 'done'
  if (stage.value === i) return 'run'
  return 'wait'
}

/* ── 카운터 (실제 시스템 수치) ──────────────────────── */
const stats = [
  { key: 'agents', target: 3, label: 'AI 검증 에이전트', unit: '개', sub: 'A1 · A2 · A3 병렬 실행' },
  { key: 'types', target: 5, label: '지원 부품 유형', unit: '종', sub: '밸브 · 피팅 · 레귤레이터 · 필터 · 기타' },
  { key: 'screens', target: 9, label: '연결된 업무 화면', unit: '개', sub: '요청 등록부터 승인 이력까지' },
]
const counts = reactive({ agents: 0, types: 0, screens: 0 })

function runCounters() {
  if (reduceMotion) {
    stats.forEach((s) => (counts[s.key] = s.target))
    return
  }
  const dur = 1100
  const t0 = performance.now()
  const tick = (now) => {
    const p = Math.min(1, (now - t0) / dur)
    const e = 1 - Math.pow(1 - p, 3)
    stats.forEach((s) => (counts[s.key] = Math.round(s.target * e)))
    if (p < 1) requestAnimationFrame(tick)
  }
  requestAnimationFrame(tick)
}

/* ── 인터랙티브 3단계 ──────────────────────────────── */
const flow = [
  {
    n: '01',
    title: '요청 등록',
    body: '설비·라인·물질·운전조건과 교체 부품을 구조화 입력합니다. 제품 유형을 고르면 필요한 스펙 칸만 나타납니다.',
    panel: 'form',
  },
  {
    n: '02',
    title: 'AI 검증',
    body: 'A1 규격·호환, A2 적용 법령, A3 안전서류 초안을 세 에이전트가 동시에 생성합니다. 진행 상황이 실시간으로 보입니다.',
    panel: 'console',
  },
  {
    n: '03',
    title: '검토 후 제출',
    body: 'AI 결과를 항목 단위로 직접 고치고 근거를 덧붙여 안전관리자에게 제출합니다. 반려되면 사유 옆에서 바로 수정합니다.',
    panel: 'review',
  },
]
const active = ref(1)
let flowTimer = null
function startFlowAuto() {
  if (reduceMotion) return
  stopFlowAuto()
  flowTimer = setInterval(() => {
    active.value = (active.value + 1) % flow.length
  }, 4200)
}
function stopFlowAuto() {
  clearInterval(flowTimer)
}
function pickFlow(i) {
  active.value = i
  startFlowAuto()
}

/* ── 활동 피드 (예시) ─────────────────────────────── */
const feed = [
  { eq: 'EQ-ETCH-04', msg: 'VCR Diaphragm Valve · 규격 검증 통과', st: 'ok' },
  { eq: 'EQ-CVD-01', msg: 'Gas Supply Fitting · 법령 2건 식별', st: 'run' },
  { eq: 'EQ-PHOT-01', msg: 'Dispense Nozzle Filter · 승인 대기', st: 'pending' },
  { eq: 'EQ-IMP-01', msg: 'Beamline Regulator · 안전관리자 승인', st: 'ok' },
  { eq: 'EQ-CMP-01', msg: 'Pad Conditioner Tube · 보완 요청 반려', st: 'rejected' },
]

/* ── 앰비언트 캔버스 (도트 그리드 + 레이더 스윕) ────── */
const canvasEl = ref(null)
let raf = null
let ro = null

function initCanvas() {
  const cv = canvasEl.value
  if (!cv || reduceMotion) return
  const ctx = cv.getContext('2d')
  let w = 0
  let h = 0
  const dpr = Math.min(2, window.devicePixelRatio || 1)
  const gap = 34

  const resize = () => {
    w = cv.clientWidth
    h = cv.clientHeight
    cv.width = w * dpr
    cv.height = h * dpr
    ctx.setTransform(dpr, 0, 0, dpr, 0, 0)
  }
  resize()
  ro = new ResizeObserver(resize)
  ro.observe(cv)

  const cx = () => w * 0.72
  const cy = () => h * 0.32
  let ang = 0

  const draw = () => {
    ctx.clearRect(0, 0, w, h)
    const ox = cx()
    const oy = cy()
    // 레이더 스윕
    const grad = ctx.createConicGradient(ang, ox, oy)
    grad.addColorStop(0, 'rgba(37,99,235,0.10)')
    grad.addColorStop(0.08, 'rgba(37,99,235,0)')
    grad.addColorStop(1, 'rgba(37,99,235,0)')
    ctx.fillStyle = grad
    ctx.fillRect(0, 0, w, h)
    // 도트 그리드 — 스윕 근처만 밝게
    for (let x = gap; x < w; x += gap) {
      for (let y = gap; y < h; y += gap) {
        const d = Math.atan2(y - oy, x - ox)
        let da = ((d - ang) % (Math.PI * 2) + Math.PI * 2) % (Math.PI * 2)
        const near = da < 0.9 ? 1 - da / 0.9 : 0
        ctx.fillStyle = `rgba(37,99,235,${0.07 + near * 0.4})`
        ctx.fillRect(x, y, near > 0.4 ? 2 : 1.4, near > 0.4 ? 2 : 1.4)
      }
    }
    ang += 0.006
    raf = requestAnimationFrame(draw)
  }
  draw()
}

onMounted(() => {
  simTimer = setInterval(stepSim, reduceMotion ? 999999 : 2200)
  if (reduceMotion) stage.value = 3
  startFlowAuto()
  initCanvas()

  const io = new IntersectionObserver(
    (entries) => {
      entries.forEach((en) => {
        if (!en.isIntersecting) return
        en.target.classList.add('in')
        if (en.target.dataset.counters !== undefined) runCounters()
        io.unobserve(en.target)
      })
    },
    { threshold: 0.25 },
  )
  document.querySelectorAll('.reveal').forEach((el) => io.observe(el))
  // 옵저버가 안 돌아도 1.4s 후 전부 노출
  setTimeout(() => document.querySelectorAll('.reveal').forEach((el) => el.classList.add('in')), 1400)
})

onBeforeUnmount(() => {
  clearInterval(simTimer)
  stopFlowAuto()
  cancelAnimationFrame(raf)
  ro?.disconnect()
})
</script>

<template>
  <div class="lp">
    <canvas ref="canvasEl" class="lp__canvas" aria-hidden="true" />

    <header class="lp__nav">
      <a class="brand" href="#top">
        <span class="brand__mark"><i /><i /><i /></span>
        ARGUS <b>WRA</b>
      </a>
      <nav class="lp__nav-links">
        <a href="#flow">워크플로우</a>
        <a href="#roles">역할</a>
        <template v-if="auth.isAuthenticated">
          <RouterLink :to="homePath" class="btn btn--ghost">
            <span class="material-symbols-outlined">dashboard</span> 대시보드로
          </RouterLink>
        </template>
        <template v-else>
          <RouterLink to="/login" class="lp__login">로그인</RouterLink>
          <RouterLink to="/signup" class="btn btn--ghost">신규 사원 등록</RouterLink>
        </template>
      </nav>
    </header>

    <!-- ── HERO ─────────────────────────────────────── -->
    <section id="top" class="hero">
      <div class="hero__copy">
        <span class="eyebrow"><span class="dot" /> Work Request &amp; Approval System</span>
        <h1 class="hero__title">
          부품 교체 안전 근거,<br />
          <span class="hero__accent">빠뜨림 없이 한 번에.</span>
        </h1>
        <p class="hero__lead">
          엔지니어가 교체 정보를 입력하면 AI 에이전트 셋이 규격·법령·안전서류 초안을 정리합니다.
          검토·수정해 안전관리자에게 넘기고, 승인까지 하나의 흐름으로 이어집니다.
        </p>
        <div class="hero__cta">
          <RouterLink v-if="auth.isAuthenticated" :to="homePath" class="btn btn--primary">
            대시보드로 이동 <span class="material-symbols-outlined">arrow_forward</span>
          </RouterLink>
          <template v-else>
            <RouterLink to="/login" class="btn btn--primary">
              시작하기 <span class="material-symbols-outlined">arrow_forward</span>
            </RouterLink>
            <RouterLink to="/signup" class="btn btn--outline">신규 사원 등록 요청</RouterLink>
          </template>
        </div>
        <ul class="hero__feed" aria-label="예시 활동 피드">
          <li v-for="f in feed" :key="f.eq" :class="['feed', 'feed--' + f.st]">
            <span class="feed__eq">{{ f.eq }}</span>
            <span class="feed__msg">{{ f.msg }}</span>
          </li>
        </ul>
      </div>

      <!-- 검증 콘솔 목업 -->
      <div class="console" role="img" aria-label="AI 검증 콘솔 미리보기">
        <div class="console__bar">
          <span class="console__dots"><i /><i /><i /></span>
          <span class="console__path">/requests/1042/run</span>
          <span class="console__live" :class="{ 'is-done': stage === 3 }">
            {{ stage === 3 ? 'DONE' : 'RUNNING' }}
          </span>
        </div>
        <div class="console__body">
          <div class="console__progress">
            <span :style="{ width: (stage / 3) * 100 + '%' }" />
          </div>
          <article
            v-for="(a, i) in agents"
            :key="a.id"
            :class="['ag', 'ag--' + agentState(i)]"
          >
            <span class="ag__id">{{ a.id }}</span>
            <div class="ag__main">
              <div class="ag__label">
                {{ a.label }}
                <span class="ag__status">
                  {{ agentState(i) === 'done' ? '완료' : agentState(i) === 'run' ? '분석 중…' : '대기' }}
                </span>
              </div>
              <div class="ag__note">{{ a.note }}</div>
            </div>
            <span class="material-symbols-outlined ag__tick">
              {{ agentState(i) === 'done' ? 'check_circle' : agentState(i) === 'run' ? 'progress_activity' : 'schedule' }}
            </span>
          </article>
        </div>
      </div>
    </section>

    <!-- ── STATS ────────────────────────────────────── -->
    <section class="stats reveal" data-counters>
      <div v-for="s in stats" :key="s.key" class="stat">
        <div class="stat__num tabular-nums">
          {{ counts[s.key] }}<span class="stat__unit">{{ s.unit }}</span>
        </div>
        <div class="stat__label">{{ s.label }}</div>
        <div class="stat__sub">{{ s.sub }}</div>
      </div>
    </section>

    <!-- ── FLOW ─────────────────────────────────────── -->
    <section id="flow" class="flow reveal">
      <h2 class="sec-title">요청 등록에서 승인까지, 세 단계</h2>
      <div class="flow__grid" @mouseleave="startFlowAuto">
        <ol class="flow__steps">
          <li
            v-for="(f, i) in flow"
            :key="f.n"
            :class="['fstep', { 'is-active': active === i }]"
            @mouseenter="pickFlow(i)"
            @click="pickFlow(i)"
          >
            <span class="fstep__n">{{ f.n }}</span>
            <div>
              <h3 class="fstep__title">{{ f.title }}</h3>
              <p class="fstep__body">{{ f.body }}</p>
            </div>
          </li>
        </ol>

        <div class="preview" :data-panel="flow[active].panel">
          <div class="preview__bar">
            <span class="console__dots"><i /><i /><i /></span>
            <span class="console__path">{{ flow[active].title }}</span>
          </div>

          <!-- 01 요청 폼 -->
          <div v-if="flow[active].panel === 'form'" class="pv-form">
            <div class="pv-row"><label>설비</label><span>EQ-ETCH-04</span></div>
            <div class="pv-row"><label>물질 / 운전조건</label><span>WF6 · 120°C, 3.5 bar</span></div>
            <div class="pv-row"><label>제품 유형</label><span class="pv-sel">밸브 ▾</span></div>
            <div class="pv-row pv-row--spec"><label>압력 등급</label><span>3000 psig</span></div>
            <button class="pv-cta">AI 검증 시작</button>
          </div>

          <!-- 02 콘솔 (히어로와 동일 시뮬레이션 재사용) -->
          <div v-else-if="flow[active].panel === 'console'" class="pv-console">
            <div
              v-for="(a, i) in agents"
              :key="a.id"
              :class="['pv-ag', 'ag--' + agentState(i)]"
            >
              <span>{{ a.id }}</span>
              <span class="pv-ag__l">{{ a.label }}</span>
              <span class="pv-ag__s">{{
                agentState(i) === 'done' ? '완료' : agentState(i) === 'run' ? '분석 중' : '대기'
              }}</span>
            </div>
          </div>

          <!-- 03 결과 검토 -->
          <div v-else class="pv-review">
            <div class="pv-card">
              <span class="pv-tag">A2 · 적용 법령</span>
              <p>산업안전보건법 제118조 <button class="pv-x">✕</button></p>
              <p>KOSHA GUIDE E-92-2020 <span class="pv-edit">✎</span></p>
              <button class="pv-add">＋ 법령 추가</button>
            </div>
            <button class="pv-cta">안전관리자에게 제출</button>
          </div>
        </div>
      </div>
    </section>

    <!-- ── ROLES ────────────────────────────────────── -->
    <section id="roles" class="roles reveal">
      <h2 class="sec-title">역할을 골라 들어가세요</h2>
      <div class="roles__grid">
        <div class="role">
          <span class="material-symbols-outlined role__icon">engineering</span>
          <h3>설비 엔지니어</h3>
          <p>교체 요청을 등록하고 AI 검증 결과를 수정해 제출합니다.</p>
          <RouterLink to="/login" class="btn btn--outline btn--sm">엔지니어로 시작</RouterLink>
        </div>
        <div class="role">
          <span class="material-symbols-outlined role__icon">verified_user</span>
          <h3>안전관리자</h3>
          <p>제출된 요청의 근거를 확인하고 승인 또는 반려합니다.</p>
          <RouterLink to="/login" class="btn btn--outline btn--sm">안전관리자로 시작</RouterLink>
        </div>
      </div>
    </section>

    <!-- ── CTA ──────────────────────────────────────── -->
    <section class="cta reveal">
      <h2>지금 근거 패키지를 만들어 보세요</h2>
      <p>반려 왕복 없이, 예정된 작업일에 교체를 시작합니다.</p>
      <div class="hero__cta">
        <RouterLink v-if="auth.isAuthenticated" :to="homePath" class="btn btn--primary">
          대시보드로 이동 <span class="material-symbols-outlined">arrow_forward</span>
        </RouterLink>
        <template v-else>
          <RouterLink to="/login" class="btn btn--primary">
            시작하기 <span class="material-symbols-outlined">arrow_forward</span>
          </RouterLink>
          <RouterLink to="/signup" class="btn btn--outline btn--on-light">신규 사원 등록</RouterLink>
        </template>
      </div>
    </section>

    <footer class="lp__foot">
      <span>ARGUS Semiconductor Fab · 부품 교체 요청·승인 시스템</span>
      <RouterLink to="/login">로그인</RouterLink>
    </footer>
  </div>
</template>

<style scoped>
/* ── 랜딩 전용 라이트 블루 팔레트 (앱 톤과 조화, 관제 콘솔 모티프 유지) ── */
.lp {
  --bg: #f4f7fc;
  --bg-raised: #e9f1fc;
  --panel: #ffffff;
  --ink: #0f2544;
  --ink-dim: #57688a;
  --line: rgba(23, 63, 125, 0.14);
  --tint: rgba(37, 99, 235, 0.05);
  --accent: #2563eb;
  --scan: #0ea5e9;
  --ok: #16a34a;
  --warn: #d97706;
  --rej: #dc2626;

  position: relative;
  min-height: 100vh;
  background: var(--bg);
  color: var(--ink);
  font-family: 'IBM Plex Sans', -apple-system, BlinkMacSystemFont, sans-serif;
  overflow: hidden;
}

.lp__canvas {
  position: absolute;
  inset: 0;
  width: 100%;
  height: 720px;
  pointer-events: none;
  opacity: 0.6;
}

.mono,
.eyebrow,
.console__path,
.feed__eq,
.stat__unit,
.fstep__n,
.pv-tag,
.console__live {
  font-family: 'IBM Plex Mono', ui-monospace, monospace;
}

/* ── Nav ─────────────────────────────────────────── */
.lp__nav {
  position: relative;
  z-index: 3;
  max-width: 1120px;
  margin: 0 auto;
  padding: 22px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}
.brand {
  display: flex;
  align-items: center;
  gap: 10px;
  color: var(--ink);
  font-size: 15px;
  letter-spacing: 0.14em;
  text-decoration: none;
}
.brand b {
  color: var(--accent);
  font-weight: 700;
}
.brand__mark {
  display: flex;
  gap: 3px;
  align-items: flex-end;
  height: 16px;
}
.brand__mark i {
  width: 3px;
  background: var(--accent);
  border-radius: 1px;
  animation: eq 1.4s ease-in-out infinite;
}
.brand__mark i:nth-child(1) { height: 60%; animation-delay: 0s; }
.brand__mark i:nth-child(2) { height: 100%; animation-delay: 0.2s; }
.brand__mark i:nth-child(3) { height: 40%; animation-delay: 0.4s; }
@keyframes eq { 0%, 100% { transform: scaleY(0.5); } 50% { transform: scaleY(1); } }

.lp__nav-links {
  display: flex;
  align-items: center;
  gap: 22px;
  font-size: 13px;
}
.lp__nav-links a {
  color: var(--ink-dim);
  text-decoration: none;
  transition: color 0.15s;
}
.lp__nav-links a:hover { color: var(--ink); }
.lp__login { font-weight: 600; }

/* ── Buttons ─────────────────────────────────────── */
.btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 22px;
  border-radius: 10px;
  font-size: 14px;
  font-weight: 600;
  text-decoration: none;
  border: 1px solid transparent;
  transition: transform 0.15s ease, background 0.15s, border-color 0.15s;
}
.btn .material-symbols-outlined { font-size: 19px; }
.btn--primary {
  background: var(--accent);
  color: #fff;
  box-shadow: 0 10px 26px -10px rgba(37, 99, 235, 0.45);
}
.btn--primary:hover { transform: translateY(-2px); background: #2f6fe0; }
.btn--outline {
  border-color: var(--line);
  color: var(--ink);
}
.btn--outline:hover { border-color: var(--accent); transform: translateY(-2px); }
.btn--outline.btn--on-light { border-color: rgba(0, 0, 0, 0.18); color: #0d1e33; }
.btn--ghost {
  padding: 9px 16px;
  border-color: var(--line);
  color: var(--ink-dim);
  font-size: 13px;
}
.btn--ghost:hover { color: var(--ink); border-color: var(--accent); }
.btn--sm { padding: 9px 16px; font-size: 13px; }

/* ── Hero ────────────────────────────────────────── */
.hero {
  position: relative;
  z-index: 2;
  max-width: 1120px;
  margin: 0 auto;
  padding: 40px 24px 96px;
  display: grid;
  grid-template-columns: 1.05fr 0.95fr;
  gap: 56px;
  align-items: center;
}
.hero__copy { animation: rise 0.7s ease both; }
.eyebrow {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  letter-spacing: 0.16em;
  text-transform: uppercase;
  color: var(--scan);
}
.eyebrow .dot,
.feed__eq::before {
  width: 7px;
  height: 7px;
  border-radius: 50%;
  background: var(--scan);
  box-shadow: 0 0 0 0 rgba(56, 189, 248, 0.6);
  animation: pulse 2s infinite;
}
@keyframes pulse {
  0% { box-shadow: 0 0 0 0 rgba(56, 189, 248, 0.5); }
  70% { box-shadow: 0 0 0 8px rgba(56, 189, 248, 0); }
  100% { box-shadow: 0 0 0 0 rgba(56, 189, 248, 0); }
}
.hero__title {
  margin: 18px 0 0;
  font-size: clamp(30px, 4.4vw, 48px);
  line-height: 1.16;
  font-weight: 700;
  letter-spacing: -0.02em;
  text-wrap: balance;
}
.hero__accent {
  background: linear-gradient(90deg, var(--accent), var(--scan));
  -webkit-background-clip: text;
  background-clip: text;
  color: transparent;
}
.hero__lead {
  margin-top: 20px;
  max-width: 46ch;
  font-size: 15px;
  line-height: 1.75;
  color: var(--ink-dim);
}
.hero__cta {
  margin-top: 30px;
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}
.hero__feed {
  margin-top: 34px;
  list-style: none;
  border-top: 1px solid var(--line);
  max-width: 440px;
}
.feed {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 9px 2px;
  border-bottom: 1px solid var(--line);
  font-size: 12px;
  color: var(--ink-dim);
}
.feed__eq {
  display: inline-flex;
  align-items: center;
  gap: 7px;
  color: var(--ink);
  font-size: 11px;
  flex-shrink: 0;
  width: 96px;
}
.feed__eq::before { content: ''; display: inline-block; animation: none; }
.feed--ok .feed__eq::before { background: var(--ok); }
.feed--run .feed__eq::before { background: var(--scan); animation: pulse 2s infinite; }
.feed--pending .feed__eq::before { background: var(--warn); }
.feed--rejected .feed__eq::before { background: var(--rej); }
.feed__msg { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }

/* ── Console mockup ──────────────────────────────── */
.console {
  position: relative;
  z-index: 2;
  background: var(--panel);
  border: 1px solid var(--line);
  border-radius: 16px;
  box-shadow: 0 30px 70px -28px rgba(15, 37, 68, 0.18);
  overflow: hidden;
  animation: rise 0.7s ease 0.1s both;
}
.console__bar,
.preview__bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-bottom: 1px solid var(--line);
  background: var(--tint);
}
.console__dots { display: flex; gap: 6px; }
.console__dots i {
  width: 9px;
  height: 9px;
  border-radius: 50%;
  background: var(--line);
}
.console__path {
  font-size: 12px;
  color: var(--ink-dim);
}
.console__live {
  margin-left: auto;
  font-size: 10px;
  letter-spacing: 0.1em;
  padding: 3px 9px;
  border-radius: 999px;
  background: rgba(2, 132, 199, 0.12);
  color: #0369a1;
}
.console__live.is-done { background: rgba(22, 163, 74, 0.14); color: #15803d; }
.console__body { padding: 18px 16px 20px; }
.console__progress {
  height: 4px;
  border-radius: 3px;
  background: var(--line);
  overflow: hidden;
  margin-bottom: 16px;
}
.console__progress span {
  display: block;
  height: 100%;
  background: linear-gradient(90deg, var(--accent), var(--scan));
  transition: width 0.6s ease;
}
.ag {
  display: flex;
  gap: 13px;
  align-items: center;
  padding: 13px;
  border: 1px solid var(--line);
  border-radius: 11px;
  margin-top: 10px;
  transition: border-color 0.3s, background 0.3s, opacity 0.3s;
}
.ag--wait { opacity: 0.5; }
.ag--run { border-color: var(--scan); background: rgba(14, 165, 233, 0.09); }
.ag--done { border-color: rgba(22, 163, 74, 0.4); }
.ag__id {
  font-family: 'IBM Plex Mono', monospace;
  font-size: 12px;
  font-weight: 600;
  color: var(--scan);
  width: 22px;
}
.ag__main { flex: 1; min-width: 0; }
.ag__label {
  font-size: 13px;
  font-weight: 600;
  display: flex;
  gap: 8px;
  align-items: baseline;
}
.ag__status { font-size: 11px; font-weight: 400; color: var(--ink-dim); }
.ag__note {
  font-size: 11px;
  color: var(--ink-dim);
  margin-top: 3px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.ag__tick { font-size: 20px; color: var(--ink-dim); }
.ag--done .ag__tick { color: var(--ok); }
.ag--run .ag__tick { color: var(--scan); animation: spin 1s linear infinite; }
@keyframes spin { to { transform: rotate(360deg); } }

/* ── Stats ───────────────────────────────────────── */
.stats {
  position: relative;
  z-index: 2;
  max-width: 1120px;
  margin: 0 auto;
  padding: 8px 24px 72px;
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 24px;
}
.stat {
  padding: 24px;
  border: 1px solid var(--line);
  border-radius: 14px;
  background: linear-gradient(180deg, var(--tint), transparent);
}
.stat__num {
  font-size: 40px;
  font-weight: 700;
  letter-spacing: -0.02em;
  line-height: 1;
}
.stat__unit { font-size: 15px; color: var(--ink-dim); margin-left: 4px; }
.stat__label { margin-top: 10px; font-size: 14px; font-weight: 600; }
.stat__sub { margin-top: 4px; font-size: 12px; color: var(--ink-dim); }

/* ── Section shared ──────────────────────────────── */
.sec-title {
  font-size: clamp(22px, 3vw, 30px);
  font-weight: 700;
  letter-spacing: -0.02em;
  text-wrap: balance;
  margin-bottom: 36px;
}
.flow,
.roles,
.cta {
  position: relative;
  z-index: 2;
  max-width: 1120px;
  margin: 0 auto;
  padding: 72px 24px;
}

/* ── Flow ────────────────────────────────────────── */
.flow__grid {
  display: grid;
  grid-template-columns: 0.9fr 1.1fr;
  gap: 40px;
  align-items: start;
}
.flow__steps { list-style: none; display: flex; flex-direction: column; gap: 8px; }
.fstep {
  display: flex;
  gap: 16px;
  padding: 20px;
  border-radius: 12px;
  border: 1px solid transparent;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}
.fstep:hover { background: var(--tint); }
.fstep.is-active {
  background: var(--bg-raised);
  border-color: var(--line);
}
.fstep__n {
  font-size: 13px;
  font-weight: 600;
  color: var(--ink-dim);
  padding-top: 2px;
}
.fstep.is-active .fstep__n { color: var(--scan); }
.fstep__title { font-size: 16px; font-weight: 600; }
.fstep__body {
  margin-top: 6px;
  font-size: 13px;
  line-height: 1.65;
  color: var(--ink-dim);
  max-height: 0;
  overflow: hidden;
  transition: max-height 0.3s ease, margin 0.3s;
}
.fstep.is-active .fstep__body { max-height: 120px; }

.preview {
  border: 1px solid var(--line);
  border-radius: 16px;
  background: var(--panel);
  overflow: hidden;
  min-height: 320px;
  box-shadow: 0 24px 56px -28px rgba(15, 37, 68, 0.16);
}
.pv-form,
.pv-console,
.pv-review { padding: 22px; display: flex; flex-direction: column; gap: 12px; animation: rise 0.4s ease both; }
.pv-row {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 14px;
  border: 1px solid var(--line);
  border-radius: 9px;
  font-size: 13px;
}
.pv-row label { color: var(--ink-dim); }
.pv-row--spec { border-color: var(--scan); background: rgba(14, 165, 233, 0.09); }
.pv-sel { color: var(--scan); }
.pv-cta {
  margin-top: 6px;
  align-self: flex-end;
  padding: 11px 20px;
  border: none;
  border-radius: 9px;
  background: var(--accent);
  color: #fff;
  font-size: 13px;
  font-weight: 600;
}
.pv-ag {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px;
  border: 1px solid var(--line);
  border-radius: 10px;
  font-size: 13px;
}
.pv-ag > span:first-child { font-family: 'IBM Plex Mono', monospace; color: var(--scan); font-weight: 600; }
.pv-ag__l { flex: 1; font-weight: 600; }
.pv-ag__s { font-size: 11px; color: var(--ink-dim); }
.pv-card {
  border: 1px solid var(--line);
  border-radius: 11px;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
}
.pv-tag { font-size: 11px; color: var(--scan); }
.pv-card p { display: flex; justify-content: space-between; color: var(--ink-dim); }
.pv-x, .pv-add {
  border: 1px solid var(--line);
  background: transparent;
  color: var(--ink-dim);
  border-radius: 6px;
  font-size: 11px;
  padding: 2px 7px;
}
.pv-add { align-self: flex-start; margin-top: 4px; }
.pv-edit { color: var(--scan); }

/* ── Roles ───────────────────────────────────────── */
.roles__grid { display: grid; grid-template-columns: 1fr 1fr; gap: 22px; }
.role {
  padding: 30px 26px;
  border: 1px solid var(--line);
  border-radius: 16px;
  background: linear-gradient(180deg, var(--tint), transparent);
  transition: border-color 0.2s, transform 0.2s;
}
.role:hover { border-color: var(--accent); transform: translateY(-3px); }
.role__icon {
  font-size: 30px;
  color: var(--scan);
  font-variation-settings: 'FILL' 1;
}
.role h3 { margin: 14px 0 8px; font-size: 17px; }
.role p { font-size: 13px; line-height: 1.6; color: var(--ink-dim); margin-bottom: 18px; }

/* ── CTA ─────────────────────────────────────────── */
.cta {
  text-align: center;
  margin-top: 24px;
  margin-bottom: 24px;
  padding: 64px 24px;
  border-radius: 22px;
  background: linear-gradient(135deg, #1e3a8a, #2563eb);
  color: #fff;
  max-width: 1072px;
  box-shadow: 0 30px 70px -30px rgba(37, 99, 235, 0.5);
}
.cta h2 { font-size: clamp(22px, 3vw, 30px); font-weight: 700; letter-spacing: -0.02em; }
.cta p { margin: 12px 0 26px; color: rgba(255, 255, 255, 0.78); font-size: 14px; }
.cta .hero__cta { justify-content: center; }
.cta .btn--primary { background: #fff; color: #1e3a8a; box-shadow: none; }
.cta .btn--primary:hover { background: #eaf1fc; }
.cta .btn--outline.btn--on-light { border-color: rgba(255, 255, 255, 0.4); color: #fff; }
.cta .btn--outline.btn--on-light:hover { border-color: #fff; }

/* ── Foot ────────────────────────────────────────── */
.lp__foot {
  position: relative;
  z-index: 2;
  border-top: 1px solid var(--line);
  max-width: 1120px;
  margin: 0 auto;
  padding: 26px 24px;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: var(--ink-dim);
}
.lp__foot a { color: var(--ink-dim); }

/* ── Reveal / motion ─────────────────────────────── */
@keyframes rise { from { opacity: 0; transform: translateY(18px); } to { opacity: 1; transform: none; } }
.reveal { opacity: 1; }
@media (prefers-reduced-motion: no-preference) {
  .reveal { opacity: 0; transform: translateY(22px); transition: opacity 0.7s ease, transform 0.7s ease; }
  .reveal.in { opacity: 1; transform: none; }
}
@media (prefers-reduced-motion: reduce) {
  .brand__mark i, .ag--run .ag__tick, .eyebrow .dot, .feed--run .feed__eq::before { animation: none; }
  .hero__copy, .console, .pv-form, .pv-console, .pv-review { animation: none; }
}

/* ── Responsive ──────────────────────────────────── */
@media (max-width: 900px) {
  .hero { grid-template-columns: 1fr; gap: 40px; padding-bottom: 64px; }
  .lp__canvas { height: 100%; }
  .stats { grid-template-columns: 1fr; }
  .flow__grid { grid-template-columns: 1fr; }
  .roles__grid { grid-template-columns: 1fr; }
  .lp__nav-links a[href="#flow"], .lp__nav-links a[href="#roles"] { display: none; }
}
</style>
