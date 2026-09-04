# AI-Ready — 프롬프트 · JSON 스키마 검증 기록

> 대상: [`prompts.md`](./prompts.md) `v1.0` · [`schemas/`](./schemas/) 4종
> 검증일: 2026-09-04

---

## 1. 검증 범위

| 구분 | 실시 여부 | 비고 |
| --- | --- | --- |
| 스키마 정적 검증 (문법 · examples · 역예제) | ✅ 실시 | 아래 2장 |
| 기존 구현과의 호환 검증 (`MockAgentEngine` 출력) | ✅ 실시 | 아래 3장 |
| 실제 LLM 호출 (OpenAI Playground) | ❌ 미실시 | `ai_configs.provider = MOCK`, `egress_allowed = false`. 절차는 4장 |

현재 PoC는 provider가 `MOCK`이라 외부 모델을 호출하지 않습니다. 프롬프트는 **설계 산출물**로 확정하고, 스키마는 **기계 검증**까지 마친 상태입니다.

---

## 2. 스키마 정적 검증

도구: Python `jsonschema` 4.26.0 (Draft 2020-12)

### 2.1 통과해야 하는 케이스

| 케이스 | 결과 |
| --- | --- |
| `agent-input.schema.json` 문법 유효 | PASS |
| `a1.items.schema.json` 문법 유효 | PASS |
| `a2.items.schema.json` 문법 유효 | PASS |
| `a3.documents.schema.json` 문법 유효 | PASS |
| 각 스키마 `examples` 자기 검증 (4건) | PASS |
| A1 `$defs.modelOutput` 정상 응답 | PASS |
| A2 `$defs.modelOutput` 정상 응답 | PASS |
| A3 `$defs.modelOutput` 정상 응답 | PASS |
| 입력 `FILTER` + `substanceType` 조합 | PASS |

### 2.2 거부되어야 하는 케이스 (역예제)

| 케이스 | 기대 | 실제 거부 사유 |
| --- | --- | --- |
| A1 항목 1건 (min 2 미달) | 거부 | `[] is too short` |
| A1 `itemId` 형식 오류 (`item-1`) | 거부 | `'item-1' does not match '^i-[0-9]{2}$'` |
| A1 규격 밖 필드 `model_note` 주입 | 거부 | `Additional properties are not allowed ('model_note' was unexpected)` |
| A2 `itemId` 누락 | 거부 | `'itemId' is a required property` |
| A2 `text` 200자 초과 | 거부 | `is too long` |
| A3 문서 1건만 | 거부 | `is too short` |
| A3 `WORK_PERMIT` 2건 (유형 중복) | 거부 | `Too many items match the given schema (expected at most 1)` |
| A3 허용되지 않는 `type` (`LOTO`) | 거부 | `is not one of ['WORK_PERMIT', 'RISK_ASSESSMENT']` |
| 입력 `FILTER`인데 `substanceType` 없음 | 거부 | `'substanceType' is a required property` |
| 입력 `FITTING_TUBE` `material` 누락 | 거부 | `'material' is a required property` |
| 입력 `requestNo` 형식 오류 (`WR-1`) | 거부 | `does not match '^WR-[0-9]{8}-[0-9]{3}$'` |
| 입력 `productType` 미허용 값 (`PUMP`) | 거부 | `is not one of ['VALVE', 'FITTING_TUBE', 'REGULATOR', 'FILTER', 'ETC']` |

**전 항목 기대대로 동작.** 스키마가 "형태만 맞으면 통과"가 아니라 실제로 잘못된 응답을 걸러냅니다.

---

## 3. 기존 구현과의 호환 검증

루브릭 Peer 항목 *"입출력 JSON 스키마가 기존 웹 구조와 호환되는가"* 에 대응합니다.
현재 동작 중인 `MockAgentEngine`이 생성하는 `payload_json`을 **그대로** 스키마 루트에 넣어 검증했습니다.

| 검증 대상 | 결과 |
| --- | --- |
| `MockAgentEngine.payload(A1, wr)` 출력 → `a1.items.schema.json` | PASS |
| `MockAgentEngine.payload(A2, wr)` 출력 → `a2.items.schema.json` | PASS |
| `MockAgentEngine.payload(A3, wr)` 출력 → `a3.documents.schema.json` | PASS |

> 📌 검증 중 발견해 반영한 사항: A1 초안은 항목 최소 3건이었으나 현행 Mock이 2건을 생성하므로 `minItems`를 **2**로 맞췄습니다. 스키마를 이상적인 값이 아니라 **돌아가는 구현에 맞춘** 것입니다.

입력 측은 `AgentService.snapshotOf(WorkRequest)`가 만드는 11개 필드를 `agent-input.schema.json`으로 정의했으며, `productType` → `specJson` 필수 키 규칙은 `ProductType.requiredSpecKeys`와 1:1로 일치합니다.

---

## 4. Playground 검증 절차 (LLM 전환 시 수행)

1. OpenAI Playground 접속 → Model 선택 → **Response format: JSON object**
2. System 메시지에 [`prompts.md` 4장 공통 블록] + [5·6·7장 중 해당 에이전트 블록] 붙여넣기
3. User 메시지에 [8장 템플릿]의 `{input_snapshot}` 자리에 [2장 입력 예시] JSON 삽입
4. 응답을 `response.json`으로 저장 후 아래 명령으로 검증

```bash
pip install jsonschema
python - <<'PY'
import json
from jsonschema import Draft202012Validator as V
s = json.load(open('docs/05_ai_ready/schemas/a2.items.schema.json'))
model_out = {**s['$defs']['modelOutput'], '$defs': s['$defs']}   # LLM 직접 출력 검증
inst = json.load(open('response.json'))
errs = list(V(model_out).iter_errors(inst))
print('PASS' if not errs else [e.message for e in errs])
PY
```

5. 아래 표에 결과 기록

| 에이전트 | 모델 | temperature | 입력 | 스키마 통과 | 비고 |
| --- | --- | --- | --- | --- | --- |
| A1 | — | — | — | — | 미실시 |
| A2 | — | — | — | — | 미실시 |
| A3 | — | — | — | — | 미실시 |

6. 통과 시 `ai_configs`의 `model_name` · `temperature` · `max_tokens`를 검증에 쓴 값으로 갱신하고, `prompt_version`을 `prompts.md` 버전과 맞춥니다.

---

## 5. 재현 방법

이 문서 2·3장의 검증은 다음으로 재현합니다.

```bash
python3 -m venv .venv && .venv/bin/pip install jsonschema
.venv/bin/python docs/05_ai_ready/validate_schemas.py
```

전 항목 PASS 시 종료 코드 0, 하나라도 어긋나면 1을 반환합니다.
스키마나 `MockAgentEngine` 출력 구조를 바꾸면 이 스크립트가 먼저 깨지도록 되어 있습니다.
