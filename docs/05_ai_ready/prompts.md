# AI-Ready — 프롬프트 설계 · 입출력 JSON 규격

> **담당 R&R**: API Architect — "AI 프롬프트 테스트 및 입출력 데이터 규격 정의"
> **루브릭 대응**: 서비스 기획 & Architecture 30점 — "AI 확장 지점 정의 및 프롬프트/JSON 스키마 타당성" / Peer — "AI 프롬프트 설계 및 입출력 JSON 스키마가 기존 웹 구조와 호환되는가"

| 항목 | 값 |
| --- | --- |
| 프롬프트 버전 | `v1.0` (= `ai_configs.prompt_version` 시드값) |
| 대상 에이전트 | A1 규격·호환 · A2 적용 법령 · A3 안전서류 초안 |
| 현재 provider | `MOCK` (`egress_allowed = false`) |
| 스키마 | [`schemas/`](./schemas/) — 입력 1종 + 출력 3종 |
| 검증 기록 | [`playground_validation.md`](./playground_validation.md) · [`validate_schemas.py`](./validate_schemas.py) |
| 최종 수정 | 2026-09-04 |

---

## 목차

1. [설계 원칙](#1-설계-원칙)
2. [입력 규격 — 공통 컨텍스트](#2-입력-규격--공통-컨텍스트)
3. [출력 규격 — LLM 계약 vs 저장 형태](#3-출력-규격--llm-계약-vs-저장-형태)
4. [공통 System 프롬프트](#4-공통-system-프롬프트)
5. [A1 규격·호환](#5-a1-규격호환)
6. [A2 적용 법령](#6-a2-적용-법령)
7. [A3 안전서류 초안](#7-a3-안전서류-초안)
8. [User 프롬프트](#8-user-프롬프트)
9. [가드레일 · 실패 처리](#9-가드레일--실패-처리)
10. [Playground 검증 절차](#10-playground-검증-절차)
11. [Mock → LLM 전환 지점](#11-mock--llm-전환-지점)
12. [남은 결정 사항](#12-남은-결정-사항)

---

## 1. 설계 원칙

| 원칙 | 이 문서에서의 적용 |
| --- | --- |
| **Interface First** | FE는 `GET /agent-runs/{runId}` · `GET /work-requests/{id}` JSON만 안다. 프롬프트가 바뀌어도 `payload_json` 구조는 고정 |
| **Structured Data** | 모든 에이전트 출력은 JSON 객체 1개. 자유 서술 금지. 검색·집계 키(`agent_code`, `edited`)는 컬럼으로 분리되어 있음 |
| **Asynchronous Pipeline** | `POST /agent-runs` → 202 → 2.5초 폴링. 프롬프트는 step 단위로 독립 실행되며 에이전트 간 의존이 없다 |
| **Security & Config Isolation** | 프롬프트 본문은 코드가 아니라 이 문서(버전 `v1.0`)에 있고, 모델·파라미터는 `ai_configs` 행에 있다. API 키는 환경변수 |

에이전트 3종은 **서로의 출력을 참조하지 않습니다.** 같은 입력 스냅샷을 각자 받아 병렬로 판단하므로, 한 에이전트가 실패해도 나머지는 유효합니다.

---

## 2. 입력 규격 — 공통 컨텍스트

A1·A2·A3가 **동일한 입력**을 받습니다. 서버가 요청 시점의 `work_requests` 행에서 스냅샷을 구성해 `agent_runs.input_snapshot`에 저장한 뒤 프롬프트에 주입합니다.

> 스키마: [`schemas/agent-input.schema.json`](./schemas/agent-input.schema.json)
> 생성 지점: `AgentService.snapshotOf(WorkRequest)`

| 필드 | 타입 | 필수 | 주 사용 에이전트 | 설명 |
| --- | --- | --- | --- | --- |
| `workRequestId` | uuid | Y | — | 추적용. 판단 근거로 쓰지 않음 |
| `requestNo` | string | Y | — | `WR-20260903-001` |
| `equipment` | string | Y | A1·A3 | 설비 (`펌프 P-114`) |
| `line` | string | Y | A3 | 라인 (`A라인`) |
| `substance` | string | Y | **A2** | 취급 물질. 법령 판단의 1차 키 |
| `operatingCondition` | object | Y | **A1** | `{ "temperature": "80 ℃", "pressure": "2500 psi" }` |
| `productName` | string | Y | A1·A3 | 교체 대상 제품명 |
| `productType` | enum | Y | A1 | `VALVE` / `FITTING_TUBE` / `REGULATOR` / `FILTER` / `ETC` |
| `specJson` | object | Y | **A1** | 유형별 필수 키가 다른 가변 객체 |
| `symptom` | string | N | A1·A3 | 고장 증상 |
| `siteMemo` | string | N | A3 | 현장 확인 메모 |

`productType` → `specJson` 필수 키 (`ProductType.requiredSpecKeys`와 동일, 서버가 `SPEC_SCHEMA_MISMATCH`로 사전 검증):

| productType | 필수 키 |
| --- | --- |
| `VALVE` · `REGULATOR` | `pressureRating` |
| `FITTING_TUBE` | `connectionStandard`, `material` |
| `FILTER` | `substanceType` |
| `ETC` | `freeSpec` |

**입력 예시**

```json
{
  "workRequestId": "9f1c8a02-4d7e-4b21-9a10-2c5f3e6b8d41",
  "requestNo": "WR-20260903-001",
  "equipment": "펌프 P-114",
  "line": "A라인",
  "substance": "NH3",
  "operatingCondition": { "temperature": "80 ℃", "pressure": "2500 psi" },
  "productName": "다이어프램 밸브 SS-DLV-4",
  "productType": "VALVE",
  "specJson": { "pressureRating": "3000 psi" },
  "symptom": "밸브 시트 누설로 라인 압력 유지 불가",
  "siteMemo": "가스 차단 후 퍼지 완료 상태 확인"
}
```

> ⚠️ 제품 사진은 현재 스냅샷에 **포함되지 않습니다**. ERD 3장은 "메타만 AI 컨텍스트에 포함"으로 적혀 있으나 `snapshotOf()`에는 사진 필드가 없습니다. → [12장 남은 결정 사항](#12-남은-결정-사항)

---

## 3. 출력 규격 — LLM 계약 vs 저장 형태

**두 형태를 구분합니다.** LLM은 본문만 생성하고, 식별자와 편집 플래그는 서버가 붙입니다.

| | LLM이 반환하는 형태 (`$defs.modelOutput`) | 저장·응답 형태 (`payload_json`, 스키마 루트) |
| --- | --- | --- |
| A1·A2 | `{ "items": [ { "text": "…" } ] }` | `{ "items": [ { "itemId": "i-01", "text": "…", "edited": false } ] }` |
| A3 | `{ "documents": [ { "type", "name", "content" } ] }` | `{ "documents": [ { "docId": "d-01", "type", "name", "content", "edited": false } ] }` |

**왜 id를 LLM에 맡기지 않는가** — `PATCH /agent-results/{id}`(명세 5.13)는 *"배열에 없는 기존 id는 삭제, id 없이 오면 서버가 채번"* 규칙으로 전체 치환합니다. LLM이 임의 id를 만들면 이 치환 규칙과 충돌하고, `edited`는 애초에 엔지니어 수정 여부를 서버가 관리하는 플래그입니다. 현재 `MockAgentEngine`도 서버가 `i-01`·`d-01`을 붙이는 구조라 LLM 전환 시에도 채번 코드를 그대로 씁니다.

| 에이전트 | 스키마 | 항목 수 | 본문 길이 |
| --- | --- | --- | --- |
| A1 | [`a1.items.schema.json`](./schemas/a1.items.schema.json) | 2 ~ 5 | `text` ≤ 120자 |
| A2 | [`a2.items.schema.json`](./schemas/a2.items.schema.json) | 2 ~ 6 | `text` ≤ 200자 |
| A3 | [`a3.documents.schema.json`](./schemas/a3.documents.schema.json) | 정확히 2 (유형별 1건) | `content` ≤ 400자 |

세 스키마 모두 `additionalProperties: false` — 모델이 규격 밖 필드(`confidence`, `note` 등)를 덧붙이면 거부됩니다.

---

## 4. 공통 System 프롬프트

3개 에이전트의 System 프롬프트는 **[공통 블록] + [에이전트별 블록]** 으로 조립합니다.

```text
당신은 반도체 제조 사업장의 부품 교체 작업을 검토하는 보조 시스템입니다.
당신의 출력은 최종 판단이 아니라, 엔지니어가 검토·수정한 뒤 안전관리자에게 제출하는 초안입니다.

[사실성]
- 제공된 입력 JSON에 있는 사실만 사용하고, 없는 값을 추측하지 마세요.
- 판단 근거가 입력에 없으면 해당 항목 본문을 "[확인 필요]"로 시작하고, 무엇을 확인해야 하는지 적으세요.
- 확실하지 않은 수치·규격·조문 번호를 지어내지 마세요. 모르면 "[확인 필요]"입니다.

[출력]
- JSON 객체 하나만 출력합니다. 설명 문장, 마크다운, 코드펜스(```)를 붙이지 마세요.
- 아래 "출력 규격"에 없는 필드를 추가하지 마세요.
- 모든 본문은 한국어 평서문으로 작성합니다.
```

---

## 5. A1 규격·호환

교체 대상 제품의 규격이 설비 운전 조건에 맞는지 검토합니다.

**System (에이전트별 블록)**

```text
[역할] A1 — 규격·호환
교체 대상 제품의 규격이 설비 운전 조건에 적합한지 검토하고, 대체 호환 가능성을 제시합니다.

[판단 방법]
- specJson의 각 키를 operatingCondition(temperature, pressure)과 대조해 적합/부적합을 판정하세요.
- 정격이 운전 조건보다 낮으면 "규격 부적합"으로 명확히 표기하세요.
- 대체 호환품은 입력에 근거가 있을 때만 언급하고, 없으면 "[확인 필요]"로 남기세요.
- 부품 마스터·호환표 연동은 아직 없습니다. 존재하지 않는 품번을 만들지 마세요.

[항목 작성 규칙]
- 항목 2~5개.
- 각 text는 "판정: 근거" 형식의 한 문장, 120자 이내.
  예) "규격 적합: 정격 3000 psi 가 운전 압력 2500 psi 를 상회"

[출력 규격]
{ "items": [ { "text": string } ] }
```

**출력 예시** (서버 채번 후 저장 형태)

```json
{
  "items": [
    { "itemId": "i-01", "text": "규격 적합: 정격 3000 psi 가 운전 압력 2500 psi 를 상회", "edited": false },
    { "itemId": "i-02", "text": "규격 적합: 운전 온도 80 ℃ 는 밸브 상용 온도 범위 내", "edited": false },
    { "itemId": "i-03", "text": "[확인 필요] 대체 호환품 SS-DLV-4EQ 의 시트 재질은 입력에 없어 판단 불가", "edited": false }
  ]
}
```

---

## 6. A2 적용 법령

취급 물질과 작업 성격에 적용되는 법령 조문·필수 절차를 목록화합니다. **제출 게이트가 A2 항목 1건 이상을 요구**하므로(422 `SUBMIT_REQUIRED_FIELD_MISSING`) 빈 배열을 반환해서는 안 됩니다.

**System (에이전트별 블록)**

```text
[역할] A2 — 적용 법령
substance(취급 물질)와 작업 성격에 적용되는 국내 산업안전·화학물질 법령 조문과
작업 전 필수 절차를 목록화합니다.

[판단 방법]
- 법령명과 조문 번호를 반드시 함께 제시하세요. (예: "산업안전보건기준에 관한 규칙 제38조")
- 조문 번호를 특정할 수 없으면 "[확인 필요]"로 시작하고 조문 번호를 생성하지 마세요.
  틀린 조문 번호는 조문이 없는 것보다 위험합니다.
- 각 항목에 작업 전 / 작업 중 / 작업 후 중 어느 단계인지 괄호로 명시하세요.
- 물질 특성(독성·고압·인화성)이 조문 선택의 근거라면 본문에 드러내세요.

[항목 작성 규칙]
- 항목 2~6개. 최소 1건은 반드시 채우세요(빈 배열 금지).
- 각 text는 "법령명 조문 — 요구사항 (단계)" 형식, 200자 이내.

[출력 규격]
{ "items": [ { "text": string } ] }
```

**출력 예시**

```json
{
  "items": [
    { "itemId": "i-01", "text": "산업안전보건기준에 관한 규칙 제38조 — NH3 취급 설비 정비 작업은 작업계획 수립 대상 (작업 전)", "edited": false },
    { "itemId": "i-02", "text": "산업안전보건기준에 관한 규칙 제92조 — 정비 작업 시 운전 정지·잠금장치·표지판 부착 필요 (작업 전)", "edited": false },
    { "itemId": "i-03", "text": "[확인 필요] 고압가스 안전관리법상 2500 psi 운전 압력의 특정고압가스 해당 여부는 사업장 신고 내역 확인 필요", "edited": false }
  ]
}
```

> 🚧 현재 ERD에 법령 마스터(`law_index`)가 없어 조문은 모델 지식에 의존합니다. RAG 전환은 Phase 2 — [11장](#11-mock--llm-전환-지점) 참조.

---

## 7. A3 안전서류 초안

작업허가서·위험성평가서 초안을 각 1건 생성합니다.

**System (에이전트별 블록)**

```text
[역할] A3 — 안전서류 초안
작업허가서(WORK_PERMIT)와 위험성평가서(RISK_ASSESSMENT) 초안을 각 1건씩 생성합니다.

[작성 방법]
- type은 WORK_PERMIT, RISK_ASSESSMENT 두 값만 사용하고, 각각 정확히 1건씩 총 2건을 생성하세요.
- name은 "작업허가서 초안", "위험성평가서 초안"으로 고정합니다.
- content는 엔지니어가 그대로 수정해 제출할 초안 본문입니다. 번호 매긴 항목 형식, 400자 이내.
- 입력에 없는 값(작업자 이름, 작업 일시, 승인자)은 지어내지 말고 "____"로 비워두세요.
- WORK_PERMIT: 작업 개요 / 취급 물질·운전 조건 / 작업 전 조치 / 작업자·일시 / 승인란
- RISK_ASSESSMENT: 위험 요인 / 현재 안전조치 / 추가 대책 / 평가자·일시

[출력 규격]
{ "documents": [ { "type": string, "name": string, "content": string } ] }
```

**출력 예시**

```json
{
  "documents": [
    {
      "docId": "d-01",
      "type": "WORK_PERMIT",
      "name": "작업허가서 초안",
      "content": "1. 작업 개요: A라인 펌프 P-114 의 다이어프램 밸브 SS-DLV-4 교체\n2. 취급 물질: NH3 / 운전 조건: 80 ℃, 2500 psi\n3. 작업 전 조치: 운전 정지, LOTO, 가스 차단 및 퍼지 완료 확인\n4. 작업자: ____ (2명) / 작업 일시: ____\n5. 승인: 안전관리자 ____",
      "edited": false
    },
    {
      "docId": "d-02",
      "type": "RISK_ASSESSMENT",
      "name": "위험성평가서 초안",
      "content": "1. 위험 요인: NH3 잔류 가스 노출, 잔압에 의한 부품 비산\n2. 현재 안전조치: 가스 차단·퍼지, 가스 감지기 상시 가동\n3. 추가 대책: 방독마스크·내화학 장갑 착용, 2인 1조 작업\n4. 평가자: ____ / 평가 일시: ____",
      "edited": false
    }
  ]
}
```

---

## 8. User 프롬프트

3개 에이전트 공통. `input_snapshot`을 가공 없이 그대로 주입합니다.

```text
아래는 부품 교체 요청 컨텍스트입니다.

{input_snapshot}
```

`{input_snapshot}`은 [2장](#2-입력-규격--공통-컨텍스트)의 JSON 그대로이며, 별도 요약·재구성을 하지 않습니다. 프롬프트에 요청 데이터를 두 번 표현하면 스냅샷과 프롬프트가 어긋날 여지가 생기기 때문입니다.

---

## 9. 가드레일 · 실패 처리

> ⚠️ **이 장은 LLM 전환 시 적용되는 규격입니다.** 현재 PoC는 `MockAgentEngine`이 항상 성공하므로 실패 경로가 실행되지 않습니다. `AgentStepStatus.FAILED`·`RunStatus.FAILED` enum은 정의되어 있으나 아직 어떤 코드도 이 값을 세팅하지 않습니다.

| 상황 | 처리 |
| --- | --- |
| JSON 파싱 실패 | 1회 재시도 → 실패 시 해당 step `FAILED`. 다른 step은 계속 진행 |
| 스키마 검증 실패 | 위와 동일. 부분 저장 금지 — `payload_json`은 전량 통과 시에만 기록 |
| 항목 수 초과·미달 | 스키마가 거부 → `FAILED` |
| 규격 밖 필드 추가 | `additionalProperties: false`로 거부 |
| 근거 없는 조문·품번 | "[확인 필요]" 접두 규칙으로 흡수. 프롬프트 3중 명시(공통·A1·A2) |
| 모델 응답 지연 | step 단위 타임아웃. `RUNNING`인 채 폴링 계속, `agent_runs.status`는 `RUNNING` 유지 |

**책임 경계** — 에이전트 출력은 초안입니다. 엔지니어가 `PATCH /agent-results/{id}`로 수정하면 `edited=true`가 되고, 안전관리자 화면(S_02)에 "엔지니어 수정" 배지가 뜹니다. AI 원본은 `original_json`에 보존되어 감사 시 diff가 가능합니다. 최종 승인 주체는 항상 사람(`SAFETY_MANAGER`)입니다.

---

## 10. Playground 검증 절차

가이드 지정 도구: **OpenAI Playground / ChatGPT 웹 UI** — 코드 작성 없이 프롬프트와 JSON 형태를 사전 검증합니다.

1. Playground에서 Response format을 **JSON object**로 설정
2. System = [4장 공통 블록] + [에이전트별 블록] 붙여넣기
3. User = [8장] 템플릿에 `{input_snapshot}` 자리에 [2장 입력 예시] JSON 삽입
4. 응답을 `$defs.modelOutput`으로 검증 (아래 명령)
5. 서버 채번을 거친 저장 형태를 스키마 루트로 재검증
6. 결과를 `playground_validation.md`에 기록 (입력 / 출력 / 통과 여부 / 모델·파라미터)

**스키마 검증 명령**

```bash
pip install jsonschema
python - <<'PY'
import json
from jsonschema import Draft202012Validator as V
schema = json.load(open('docs/05_ai_ready/schemas/a2.items.schema.json'))
inst   = json.load(open('response.json'))   # Playground 응답 저장본
errs = list(V(schema).iter_errors(inst))
print('PASS' if not errs else [e.message for e in errs])
PY
```

**본 문서 작성 시 검증 완료 항목**

| 검증 | 결과 |
| --- | --- |
| 스키마 4종 draft 2020-12 문법 | 통과 |
| 각 스키마 `examples` 자기 검증 | 통과 |
| `$defs.modelOutput` 정상 응답 3종 | 통과 |
| `MockAgentEngine` 실제 출력 3종이 스키마 루트 통과 | 통과 (기존 웹 구조와 호환 확인) |
| 역예제 13종 거부 (항목 수 미달, `itemId` 형식 오류, A3 유형 중복, 규격 밖 필드, `specJson` 필수 키 누락 등) | 전부 거부 확인 |
| 실제 LLM 호출 | ❌ 미실시 — provider=`MOCK` |

---

## 11. Mock → LLM 전환 지점

교체 대상은 **`MockAgentEngine` 한 클래스**입니다. 컨트롤러·서비스·엔티티·FE는 손대지 않습니다.

| 단계 | 작업 |
| --- | --- |
| 1 | `ai_configs.provider`를 `MOCK` → `LOCAL_LLM` 또는 `OPENAI`로 변경, `model_name`·`temperature`·`max_tokens` 설정 |
| 2 | `MockAgentEngine.payload()` 자리에 LLM 호출 구현체 주입 (`AgentEngine` 인터페이스 추출) |
| 3 | 응답을 `$defs.modelOutput`으로 검증 → 서버가 `itemId`/`docId`/`edited` 채번 → 기존 `payload_json` 저장 경로 재사용 |
| 4 | 외부 API 사용 시에만 `egress_allowed=true`. 온프레미스 기본값은 `false` |
| 5 | 프롬프트 수정 시 이 문서 버전을 올리고 `ai_configs.prompt_version`을 함께 갱신 |

**Phase 2**: A2를 법령 마스터(`law_index`) 기반 RAG로 전환. User 프롬프트에 `law_excerpts` 블록이 추가되고, System에 "제공된 발췌에서만 인용" 제약이 붙습니다. A1은 부품 마스터·호환표 연동.

---

## 12. 남은 결정 사항

| # | 항목 | 현재 | 결정 필요 |
| --- | --- | --- | --- |
| 1 | 제품 사진 메타의 AI 컨텍스트 포함 | ERD 3장은 "메타만 포함", `snapshotOf()`에는 없음 | 스냅샷에 `photos[].fileName` 추가 여부 |
| 2 | A2 법령 근거 | 모델 지식 의존 (`law_index` 없음) | Phase 2 RAG 전까지 "[확인 필요]" 비중을 어디까지 허용할지 |
| 3 | step 타임아웃 값 | 미정 | `ai_configs`에 컬럼 추가 vs 애플리케이션 상수 |
| 4 | 재시도 횟수 | 문서상 1회 | 구현 시 확정 |
