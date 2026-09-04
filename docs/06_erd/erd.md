# FixGuide 데이터 모델 정의서

기준: API 명세서 v1.0 (REQ-F-0001) · WRA 화면정의서 v2.0 · 백엔드 엔티티 (`com.skala.fixguide.**.entity`)
테이블 8개, enum 7종.

이 폴더의 파일:
- `erd.md` — 이 문서. 엔티티·속성·관계·정규화 근거 (정본)
- `erd.png` — dbdiagram.io 내보내기
- `fixguide_erd.dbml` — dbdiagram.io 소스
- `schema_postgres.sql` — Hibernate가 생성한 DDL 참고본. 실제 스키마는 JPA `ddl-auto`가 만든다

관련 문서: 상태 전이는 `docs/07_api/README.md` 3장, API별 테이블 쓰기 흐름은 `docs/04_architecture/data_flow.md`

## 설계 원칙

| 원칙 | 적용 |
|---|---|
| 대리키 PK | 전 테이블 UUID v4. 업무 식별자(`users.email`, `work_requests.request_no`, `agent_steps/agent_results (run_id, agent_code)`)는 UNIQUE. 번호·코드 체계가 바뀌어도 FK 무결성 유지, 조인은 대리키로 |
| 사실 / 추론 / 행동 분리 | 입력(`work_requests`) · AI 산출(`agent_runs`, `agent_steps`, `agent_results`) · 사람의 결정(`approvals`)을 테이블로 분리. 위 층이 아래 층을 덮어쓰지 않음 |
| append-only 이력 | `agent_runs`(재실행), `approvals`(재제출 후 재결정)는 갱신하지 않고 행을 추가. 최신 1건을 화면에 노출 |
| jsonb는 구조가 가변인 곳에만 | `operating_condition`, `spec_json`(유형별 키), `payload_json`(에이전트별 구조). 조회·집계 키(`status`, `agent_code`, `reason_category`, `edited`)는 컬럼 |
| 상태는 enum | 값이 고정이고 코드에서 분기하는 것(`status`, `agent_code`, `decision`)은 룩업 테이블이 아니라 PostgreSQL enum |

## 상태머신

`work_requests.status`의 전이 규칙(트리거 API·조건·오류 코드)은 **API 명세서 3장**(`docs/07_api/README.md`)이 정본이다. 여기서는 컬럼 정의만 다루고 중복 기술하지 않는다.

| 전이 | 트리거 | 조건 · 오류 |
|---|---|---|
| — → DRAFT | `POST /work-requests` `draft=true` | 필수 검증 생략 |
| — / DRAFT → AI_RUNNING | `POST /work-requests` `draft=false` 후 `POST /agent-runs` | 필수값 누락 400, 진행 중 run 존재 409 |
| AI_RUNNING → AI_DONE | 마지막 step DONE (`allDone: true`) | 서버가 전환 |
| AI_DONE / REJECTED → PENDING | `PATCH /work-requests/{id}/submit-approval` | 3종 결과 존재 · `engineer_note` · A2 1건 이상, 아니면 422 |
| PENDING → APPROVED / REJECTED | `POST /approvals` | SAFETY_MANAGER만(403), PENDING 아니면 409, REJECT 사유 없으면 400 |
| PENDING · APPROVED | 수정 불가 | `PATCH /work-requests` 409 `IMMUTABLE_STATUS`, `PATCH /agent-results` 409 `RESULT_LOCKED` |

---

## 1. users — 계정

API 1·2·3 (signup / login / me). 역할 하나로 진입 화면(E_01 / S_01)과 API 권한이 갈린다.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | uuid | PK | |
| name | varchar(20) | NOT NULL | 2~20자 |
| email | varchar(255) | UNIQUE, NOT NULL | 로그인 ID. 중복 시 409 `EMAIL_ALREADY_EXISTS` |
| password_hash | varchar(255) | NOT NULL | bcrypt. 평문·`passwordConfirm`은 저장하지 않음 |
| role | user_role | NOT NULL | `ENGINEER` / `SAFETY_MANAGER` |
| created_at | timestamptz | NOT NULL | JPA Auditing |
| updated_at | timestamptz | NOT NULL | JPA Auditing |

권한 규칙 (명세 1장): ENGINEER는 `requester_id = 본인`인 요청만, SAFETY_MANAGER는 `PENDING` 이상 전체.

---

## 2. work_requests — 부품 교체 요청

API 5·6·7·8·14. 핵심 엔티티이자 상태머신의 주체.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | uuid | PK | 응답의 `workRequestId`. URL·FK에 사용 |
| request_no | varchar(20) | UNIQUE (NOT NULL은 서비스 보장) | 업무 번호 `WR-20260903-001`. 화면 표시·검색용 자연키. 생성 시 서버 채번 (일자 + 일련번호). 응답 `requestNo` |
| requester_id | uuid | FK → users, NOT NULL | 요청자. `?mine=true` 필터 키 |
| equipment | varchar(100) | draft=false 시 필수 | 설비 (`펌프 P-114`) |
| line | varchar(100) | draft=false 시 필수 | 라인 (`A라인`) |
| substance | varchar(100) | draft=false 시 필수 | 취급 물질. A2 입력 |
| operating_condition | jsonb | draft=false 시 필수 | `{ "temperature": "80 ℃", "pressure": "2500 psi" }` |
| product_name | varchar(200) | draft=false 시 필수 | 제품명. AI 전송 핵심 값 |
| product_type | product_type | draft=false 시 필수 | 5종 enum |
| spec_json | jsonb | draft=false 시 필수 | 유형별 필수 키 서버 검증. 불일치 400 `SPEC_SCHEMA_MISMATCH` |
| symptom | text | | 고장 증상 |
| site_memo | text | | 현장 확인 메모 |
| engineer_note | text | submit 시 필수 | E_04 엔지니어 설명. 안전관리자에게 전달. 없으면 422 |
| status | work_request_status | NOT NULL, default DRAFT | 위 상태머신 |
| created_at | timestamptz | NOT NULL | |
| updated_at | timestamptz | NOT NULL | PATCH 시 갱신 |
| submitted_at | timestamptz | | PENDING 전환 시각. 재제출 시 갱신. 목록 기본 정렬 키 |

인덱스: `request_no` UNIQUE (코드 생성). `(requester_id, status)` E_01·E_05 / `(status, submitted_at)` S_01 은 권장 인덱스로 코드(JPA)에는 아직 없음.

대리키와 자연키 분리: `id`는 URL과 FK, `request_no`는 사람이 보는 번호. 번호 체계를 바꿔도 FK가 안 흔들리고, 번호로 검색하면 UNIQUE 인덱스를 탄다.

`draft=true`로 만든 행은 대부분 컬럼이 NULL일 수 있으므로 DB NOT NULL 대신 서비스 계층에서 `draft=false` 조건부 검증.

`spec_json` 필수 키 (명세 2.3):

| product_type | 필수 키 |
|---|---|
| VALVE | `pressureRating` |
| FITTING_TUBE | `connectionStandard`, `material` |
| REGULATOR | `pressureRating` |
| FILTER | `substanceType` |
| ETC | `freeSpec` |

---

## 3. work_request_photos — 제품 사진

API 9·10. 현장 사진이 아니라 교체 대상 제품 사진. 메타만 AI 컨텍스트에 포함.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | uuid | PK | `photoId` |
| work_request_id | uuid | FK → work_requests, NOT NULL | |
| file_name | varchar(255) | NOT NULL | 원본 파일명 |
| storage_key | varchar(500) | NOT NULL | 원본 저장 경로. EXIF 제거 후 저장 → `originalUrl` |
| thumbnail_key | varchar(500) | NOT NULL | 320px 썸네일 → `thumbnailUrl` |
| size | int | NOT NULL | bytes. 10MB 초과 413 |
| uploaded_at | timestamptz | NOT NULL | |

요청당 최대 5장 (409 `PHOTO_LIMIT_EXCEEDED`), jpg/png/webp만 (400 `UNSUPPORTED_FILE_TYPE`) — 서비스 검증. 업로드는 요청 생성 이후만 가능하므로 E_02에서 사진을 먼저 올리려면 DRAFT 선생성 (정합성 메모 #7).

---

## 4. agent_runs — AI 실행 단위

API 11·12. "AI 검증 시작" 한 번 = run 하나. 재실행하면 새 행.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | uuid | PK | `runId` |
| work_request_id | uuid | FK → work_requests, NOT NULL | |
| status | run_status | NOT NULL, default RUNNING | `RUNNING` / `DONE` / `FAILED` |
| started_at | timestamptz | NOT NULL | |
| finished_at | timestamptz | | 3 step 모두 DONE 또는 FAILED 시 |
| input_snapshot | jsonb |  | 실행 시점의 요청 전체 컨텍스트. 명세 5.11의 "서버가 스냅샷 구성"을 저장까지 |
| ai_config_id | uuid | FK → ai_configs | 어떤 설정(provider·model·prompt_version)으로 실행됐는지 |
| created_at | timestamptz | NOT NULL | JPA Auditing |
| updated_at | timestamptz | NOT NULL | JPA Auditing |

동일 요청에 `status=RUNNING`인 run이 있으면 409 `RUN_ALREADY_IN_PROGRESS`. 화면은 `started_at` 최신 run을 본다.

---

## 5. agent_steps — 에이전트별 실행 단계

API 11·12. E_03 카드 하나 = 행 하나. 폴링 응답 `steps[]`의 소스.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | uuid | PK | |
| run_id | uuid | FK → agent_runs, NOT NULL | |
| agent_code | agent_code | NOT NULL | `A1` 규격·호환 / `A2` 법령 / `A3` 안전서류 |
| status | agent_step_status | NOT NULL, default WAITING | `WAITING` → `RUNNING` → `DONE` / `FAILED` |
| message | varchar(200) | | 카드 진행 메시지 (`관련 조문 검색 중…`) |
| error_message | text | | FAILED 시. HTTP는 200 유지 |
| started_at | timestamptz | | |
| finished_at | timestamptz | | |

UNIQUE `(run_id, agent_code)`. Mock 단계에서는 `GET /agent-runs/{runId}` 호출마다 WAITING인 step 하나를 DONE으로 전이. 3개 모두 DONE이면 `allDone: true` 반환하면서 `agent_runs.status=DONE`, `work_requests.status=AI_DONE`으로 전환.

---

## 6. agent_results — AI 결과물 (엔지니어 수정 대상)

API 7·12·13. run·에이전트 단위로 1행. 항목 편집은 `payload_json` 전체 치환.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | uuid | PK | `agentResultId` |
| run_id | uuid | FK → agent_runs, NOT NULL | |
| agent_code | agent_code | NOT NULL | |
| payload_json | jsonb | NOT NULL | 아래 구조. `PATCH /agent-results/{id}`로 치환 |
| edited | boolean | NOT NULL, default false | 항목 중 하나라도 수정·추가·삭제되면 true. S_02 "엔지니어 수정" 배지 |
| original_json | jsonb |  | AI 원본 스냅샷. `payload_json`이 수정본이 되므로 원본은 여기 보존. S_02 diff·감사 근거 |
| created_at | timestamptz | NOT NULL | JPA Auditing |
| updated_at | timestamptz | NOT NULL | JPA Auditing. 결과 수정 시 갱신 |

UNIQUE `(run_id, agent_code)`. `PENDING`·`APPROVED` 상태에서는 409 `RESULT_LOCKED`.

`payload_json` 구조:

```json
// A1 · A2 (항목형)
{ "items": [
  { "itemId": "i-01", "text": "규격 적합: 3000 psi >= 요구 2500 psi", "edited": false },
  { "itemId": "i-02", "text": "대체 호환: SS-8-VCR-2", "edited": true }
] }

// A3 (문서형)
{ "documents": [
  { "docId": "d-01", "type": "WORK_PERMIT",     "name": "작업허가서 초안",   "content": "…", "edited": false },
  { "docId": "d-02", "type": "RISK_ASSESSMENT", "name": "위험성평가서 초안", "content": "…", "edited": false }
] }
```

치환 규칙 (명세 5.13): 배열에 없는 기존 `itemId`는 삭제, `itemId` 없이 `text`만 오면 신규 추가(서버가 id 채번). SAFETY_MANAGER 조회 시 `editable: false`.

---

## 7. approvals — 승인 · 거절

API 15·7. append-only. 거절 → 재제출 → 재결정이 가능하므로 요청당 다건.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | uuid | PK | `approvalId` |
| work_request_id | uuid | FK → work_requests, NOT NULL | |
| approver_id | uuid | FK → users, NOT NULL | 역할 SAFETY_MANAGER 검증 (403) |
| decision | approval_decision | NOT NULL | `APPROVE` / `REJECT` |
| reason | text | REJECT 시 필수, 10자 이상 | 요청자(E_05)에게 그대로 전달. 없으면 400 `REJECT_REASON_REQUIRED` |
| reason_category | varchar(50) | | S_01 거절 사유 TOP5 집계 키. 5종 고정 여부는 정합성 메모 #6 |
| decided_at | timestamptz | NOT NULL, default now() | KPI(오늘 처리·이번 달 승인/거절) 집계 키 |

인덱스: `(work_request_id, decided_at)` 최신 1건 조회 / `(decided_at)` 대시보드 — 권장 인덱스로 코드(JPA)에는 아직 없음.
상세 응답의 `approval`은 최신 1건, 미처리 시 `null`. 같은 트랜잭션에서 `work_requests.status`를 APPROVED/REJECTED로 갱신.

---

## 8. ai_configs — 에이전트 설정

루브릭 AI-Ready "Security & Config Isolation"에 대응. 프롬프트·모델·파라미터를 코드 밖으로.

| 컬럼 | 타입 | 제약 | 설명 |
|---|---|---|---|
| id | uuid | PK | |
| agent_code | agent_code | NOT NULL | 에이전트별 설정 |
| provider | varchar(20) | NOT NULL | `MOCK` / `LOCAL_LLM` / `OPENAI` |
| model_name | varchar(60) | | |
| prompt_version | varchar(30) | | `docs/05_ai_ready/prompts/` 파일 버전과 매칭 |
| temperature | numeric(3,2) | | |
| max_tokens | int | | |
| egress_allowed | boolean | NOT NULL, default false | 외부 전송 허용. 온프레미스 기본 false |
| is_active | boolean | NOT NULL, default true | 에이전트당 활성 1개 — 목표는 부분 유니크 `UNIQUE (agent_code) WHERE is_active`. 코드에는 없고 시드 1회 삽입·서비스 검증으로 보장 |
| created_at | timestamptz | NOT NULL | JPA Auditing |
| updated_at | timestamptz | NOT NULL | JPA Auditing |

API 키는 넣지 않음 (환경변수). Mock → 실제 LLM 전환은 `provider` 값 변경으로.

---

## Enum

| Enum | 값 |
|---|---|
| user_role | ENGINEER, SAFETY_MANAGER |
| work_request_status | DRAFT, AI_RUNNING, AI_DONE, PENDING, APPROVED, REJECTED |
| product_type | VALVE, FITTING_TUBE, REGULATOR, FILTER, ETC |
| run_status | RUNNING, DONE, FAILED |
| agent_code | A1, A2, A3 |
| agent_step_status | WAITING, RUNNING, DONE, FAILED |
| approval_decision | APPROVE, REJECT |

## 관계

| 관계 | 종류 | 근거 |
|---|---|---|
| users → work_requests | 1:N | 엔지니어가 요청 여러 건 |
| users → approvals | 1:N | 안전관리자가 여러 건 결정 |
| work_requests → work_request_photos | 1:N | 최대 5장 |
| work_requests → agent_runs | 1:N | 재실행 |
| work_requests → approvals | 1:N | 재제출 후 재결정 (append-only) |
| agent_runs → agent_steps | 1:N (고정 3) | A1·A2·A3 |
| agent_runs → agent_results | 1:N (고정 3) | 에이전트별 결과 1건 |
| ai_configs → agent_runs | 1:N |  |

N:M은 이번 범위에 없음. 법령 마스터(`law_index`)와 결과의 N:M, 설비 마스터·호환표(`equipments`, `parts`, `part_compatibility`)는 Phase 2 (A1 호환표 연동과 함께).

## API ↔ 테이블 쓰기 매핑

| API | INSERT | UPDATE |
|---|---|---|
| 1 `POST /auth/signup` | users | |
| 5 `POST /work-requests` | work_requests (DRAFT 또는 필수 검증 후) | |
| 8 `PATCH /work-requests/{id}` | | work_requests (필드, updated_at) |
| 9 `POST …/photos` | work_request_photos | |
| 11 `POST /agent-runs` | agent_runs, agent_steps ×3 | work_requests.status=AI_RUNNING |
| 12 `GET /agent-runs/{runId}` (Mock) | agent_results (step DONE 시) | agent_steps.status·message / 마지막: agent_runs.status=DONE, work_requests.status=AI_DONE |
| 13 `PATCH /agent-results/{id}` | | agent_results.payload_json·edited·updated_at |
| 14 `PATCH …/submit-approval` | | work_requests.status=PENDING·engineer_note·submitted_at |
| 15 `POST /approvals` | approvals | work_requests.status=APPROVED/REJECTED |

## 정규화 메모

- `agent_steps`(진행 상태)와 `agent_results`(결과물)를 분리한 이유: 갱신 주체와 주기가 다름. steps는 오케스트레이터가 초 단위로, results는 엔지니어가 편집. 한 테이블이면 폴링 UPDATE와 편집 UPDATE가 같은 행을 경합.
- `approvals.reason_category`를 컬럼으로 둔 이유: `reason` 자유 텍스트만으로는 TOP5 집계 불가. enum으로 굳히면 `reject_reason` 타입으로 승격.
- JSON 컬럼(`operating_condition`, `spec_json`, `input_snapshot`, `payload_json`, `original_json`)은 PoC에서 H2/PostgreSQL 호환을 위해 `text + JsonMapConverter`로 저장. PostgreSQL 단독 운영 시 `jsonb` 전환. 이 문서와 DBML의 `jsonb` 표기는 목표 타입.
- `work_requests.request_no`는 명세에 없던 추가 컬럼. 응답에 `requestNo` 한 필드가 늘어남 (명세 5.5·5.6·5.7 반영 필요).
- 설비·라인을 문자열로 둔 것은 명세 그대로. 드롭다운 옵션은 FE 상수. 마스터 분리는 Phase 2에서 A1 호환표와 같이.
- `default now()`·`default false` 등 기본값 표기는 목표 스키마. 현재 코드는 DB default 없이 JPA Auditing(`created_at`/`updated_at`)과 빌더가 값을 채운다.
- `original_json` 없이 `edited: true`만 있으면 "무엇이 바뀌었나"를 알 수 없음. 원본 보존 컬럼을 둔 이유.

---

## 기존 API 명세서 v1.0과 달라진 부분

이 ERD를 기준으로 명세서에서 고칠 곳. 응답 포맷이 바뀌는 건 1번 하나고, 나머지는 7장 DB 매핑 표에만 추가.

| # | 구분 | 변경 내용 | 명세 반영 위치 |
|---|---|---|---|
| 1 | 컬럼 추가 · **응답 변경** | `work_requests.request_no varchar(20) UNIQUE NOT NULL`. 형식 `WR-YYYYMMDD-NNN`, 서버 채번. 응답에 `requestNo` 필드 추가 | 5.5 `POST /work-requests` 201 · 5.6 목록 `content[]` · 5.7 상세 · 7장 DB 매핑 |
| 2 | 제약 명시 | UNIQUE 4개: `users.email`, `work_requests.request_no`, `agent_steps (run_id, agent_code)`, `agent_results (run_id, agent_code)` | 7장 |
| 3 | 컬럼 추가 | `agent_runs.input_snapshot jsonb` — 실행 시점 요청 전체 컨텍스트 저장 | 7장 `agent_runs` |
| 4 | 컬럼 추가 | `agent_results.original_json jsonb` — AI 원본 보존, `payload_json`은 수정본 | 7장 `agent_results` |
| 5 | 테이블 추가 | `ai_configs` (agent_code, provider, model_name, prompt_version, temperature, max_tokens, egress_allowed, is_active). `agent_runs.ai_config_id` FK. 활성 설정은 `UNIQUE (agent_code) WHERE is_active` | 7장 테이블 목록 |
| 6 | 구현 메모 | `work_requests`의 `equipment`·`line`·`substance`·`operating_condition`·`product_name`·`product_type`·`spec_json`은 DB NOT NULL 아님 (DRAFT 허용). `draft=false`일 때 서비스 계층 검증 | 5.5 비고 |
| 7 | 구현 메모 | `request_no` 채번은 당일 최대 일련번호 +1. 동시 생성 대비 시퀀스 또는 행 잠금 | 5.5 비고 |

3·4·5는 코드에 이미 구현됨. 명세 7장에만 반영하면 됨.
