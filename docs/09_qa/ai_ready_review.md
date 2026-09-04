# AI-Ready 산출물 검토 (2026-09-04)

## 요약
- 검토 파일: `docs/05_ai_ready/prompts.md`, `playground_validation.md`, `schemas/agent-input.schema.json`, `schemas/a1.items.schema.json`, `schemas/a2.items.schema.json`, `schemas/a3.documents.schema.json`, `validate_schemas.py` / `docs/07_api/README.md` (2.4 · 5.7 · 5.11~5.13 · 7 · 9장) / `backend/.../agent/**`, `init/SeedDataInitializer.java` / `frontend/src/views/engineer/AgentResultEditView.vue`
- **읽지 못한 파일**: `docs/06_erd/erd.md`, `docs/06_erd/schema_postgres.sql`, `docs/02_usecase/usecase_spec.md` — 로컬·원격 모든 브랜치에 존재하지 않음(`docs/06_erd/`, `docs/02_usecase/`는 `.gitkeep`뿐). A 항목 중 ERD 대조와 E 항목 중 Use-Case 대조는 **검증 불가**로 표기.
- 결론: **수정 후 가능** — 프롬프트·스키마·검증 스크립트는 루브릭 문구에 직접 대응하고 실제 Mock 출력과도 맞는다. 다만 ERD·Use-Case 문서가 저장소에 없고, API 명세 7장 DB 매핑이 코드보다 뒤처져 있으며, 명세 5.13의 A3 부분 수정 예시대로 호출하면 코드가 `type`·`name`을 유실한다.

## 발견 사항
| # | 심각도 | 항목 | 파일:위치 | 문제 | 제안 수정 |
| --- | --- | --- | --- | --- | --- |
| 1 | HIGH | A/E | `docs/06_erd/`, `docs/02_usecase/` | ERD v3(`erd.md`, `schema_postgres.sql`)와 Use-Case 명세가 어떤 브랜치에도 없다. `prompts.md:96`이 "ERD 3장" 을 인용하지만 참조 대상이 저장소에 없음 | 작성자 로컬 파일을 커밋. 없으면 `prompts.md:96`, `:358` 의 ERD 인용을 "명세 5.11 사진 메타" 인용으로 바꿈 |
| 2 | HIGH | C | `AgentService.java` patchResult (`entry = new LinkedHashMap<>(in)`) · `docs/07_api/README.md:636-643` | 명세 5.13 A3 예시는 `{docId, content}`만 보내는데 코드는 받은 맵으로 통째 교체해 `type`·`name`이 사라진다. 저장 결과가 `a3.documents.schema.json` 위반 | **코드 수정**: 기존 항목이 있으면 `old`를 복사한 뒤 받은 필드만 덮어쓰기(merge). FE는 전체 필드를 보내 현재 화면은 정상 |
| 3 | HIGH | A | `docs/07_api/README.md:791-808` (7장 DB 매핑) | `ai_configs` 테이블, `agent_runs.input_snapshot`, `agent_results.original_json`, `work_requests.request_no` 가 표에 없다. 코드(`AgentRun`, `AgentResult`, `AiConfig`, `WorkRequest`)에는 전부 존재 | 7장 표에 4개 항목 추가. `prompts.md`는 이미 반영돼 있어 명세만 갱신하면 됨 |
| 4 | MED | C | `AgentStepResponse.java` · `docs/07_api/README.md:609` | 명세 5.12는 FAILED step에 `errorMessage`를 포함한다고 하나 DTO에 필드가 없다. 엔티티 `AgentStep.errorMessage`는 있음 | DTO에 `errorMessage` 추가(null이면 생략). `prompts.md:286`의 "FAILED 미세팅" 서술과 함께 "Phase 2 적용" 문구로 정리 |
| 5 | MED | C | `AgentRunStartRequest.java` · `AgentService.start` | 명세 5.11 요청의 `agents` 배열을 DTO는 받지만 서비스가 무시하고 항상 3종 실행. `prompts.md:40` "step 단위 독립 실행" 설명과는 모순 없음 | 명세 5.11에 "PoC는 `agents` 무시, 항상 A1·A2·A3" 한 줄 추가(코드 유지) |
| 6 | MED | B/C | `MockAgentEngine.java:44` · `prompts.md:192-201` | A2 프롬프트는 "조문 번호 필수, 없으면 `[확인 필요]`", "(단계) 표기"를 요구하지만 Mock A2 2번 항목은 조문 번호·단계가 없다. 스키마는 통과하나 프롬프트 규칙 위반 예시가 시드로 화면에 노출됨 | Mock 문구를 `"[확인 필요] 고압가스 안전관리법 시행규칙 — … (작업 전)"` 형식으로 수정(코드, 1줄) |
| 7 | MED | C | `validate_schemas.py:69-76` · `playground_validation.md:59-70` | "MockAgentEngine 실제 출력 통과"라고 쓰였지만 스크립트는 손으로 옮긴 축약본을 검증한다. 실제 API 응답으로 재검증한 결과는 통과(A1·A2 `items` 키 `itemId/text/edited`, A3 `docId/type/name/content/edited`) | 문서에 "코드에서 복사한 대표값" 으로 표현 완화. 여유 있으면 스크립트가 `GET /work-requests/{id}` 응답 JSON 파일을 읽도록 변경 |
| 8 | MED | B | `prompts.md:340-346` | 전환 지점을 "`AgentEngine` 인터페이스 추출"이라 쓰지만 인터페이스는 아직 없고 `MockAgentEngine` 구체 클래스만 있다(`agent/service/` 파일 2개) | "현재 구체 클래스, 전환 시 인터페이스 추출 예정"으로 정정하거나 인터페이스를 미리 추출(코드 10분) |
| 9 | MED | E | `AgentResultEditView.vue:40-43` · `a3.documents.schema.json:64-72` | E_04에서 A3 "항목 추가"로 만든 새 문서는 `name`·`content`만 있고 `type`이 없어 스키마(`type` 필수, 2건 고정)를 깨뜨린다. 백엔드도 검증하지 않음 | 프롬프트 설계상 A3는 2건 고정이므로 FE에서 A3 "항목 추가" 버튼 제거 또는 `type` 선택 추가. 문서 3장에 "A3는 추가 불가, 본문 편집만" 명시 |
| 10 | LOW | A | `prompts.md:57-58`, `agent-input.schema.json:25-29` | `request_no` 는 스냅샷에 포함되지만 판단 근거로 쓰지 않는다고 명시됨. **영향 없음** | 없음 |
| 11 | LOW | B | `prompts.md:41`, `:348` | Security 원칙 설명이 "API 키는 환경변수"로 끝남. `.env.example`에 AI 키 항목이 없어 근거가 약함 | `.env.example`에 `OPENAI_API_KEY=` 주석 항목 추가 후 문서에서 인용 |
| 12 | LOW | D | `prompts.md:38` | Interface First 근거로 FE가 아는 엔드포인트만 적고 클래스명이 없다 | "`AgentService`·`AgentController`·DTO 는 불변, 교체는 `MockAgentEngine`" 한 줄 추가 |
| 13 | LOW | B | `prompts.md` 5·6·7장 | 프롬프트가 `.st`/`.md` 별도 파일이 아니라 문서 본문 코드블록에 있음. `prompt_version=v1.0`은 시드(`SeedDataInitializer.java:347`)와 일치 | 발표용으로는 충분. 전환 시 `backend/src/main/resources/prompts/a1.v1.0.st` 등으로 분리 계획만 12장에 추가 |

## 명세·ERD 변경 미반영 목록
| 확인 항목 | 결과 | 근거 |
| --- | --- | --- |
| `ai_configs` 8개 컬럼과 "설정을 코드 밖으로" 설명 | `docs/05_ai_ready`에는 **반영** (`prompts.md:8-10`, `:41`, `:344-348`). `docs/07_api` 7장에는 **누락** | `AiConfig.java:29-55` 필드 8개와 일치 |
| `agent_runs.input_snapshot` 개념 | **반영**. `prompts.md:49-53`이 `AgentService.snapshotOf` 11개 필드를 정의, 명세 5.11 "서버가 스냅샷 구성"과 일치 | 단, 명세 5.11의 "사진 메타 포함"은 코드·문서 모두 미구현이며 문서가 이를 12장 결정 사항으로 남김 |
| `original_json` vs `payload_json` 분리 | **반영** (`prompts.md:297`) | `AgentResult.java` 두 컬럼 존재 |
| `request_no` 영향 | **영향 없음** (`prompts.md:58`) | — |
| 상태값·코드값 | **일치**. `AI_RUNNING/AI_DONE`, `WAITING/RUNNING/DONE/FAILED`, `A1/A2/A3` | 명세 2.2·2.4, 코드 enum 동일 |
| ERD v3 원문 대조 | **검증 불가** — 파일 없음 | 발견 사항 #1 |

## 코드 vs 문서 불일치
| 대상 | 문서 | 코드 | 판단 |
| --- | --- | --- | --- |
| A3 부분 수정 | 명세 5.13은 `{docId, content}`만 전송 | 받은 맵으로 교체해 `type`·`name` 유실 | **코드 수정** (명세 위반). 기존 항목과 merge |
| FAILED step 응답 | 명세 5.12 `errorMessage` 포함 | DTO에 없음, FAILED 세팅 코드도 없음 | **코드 수정**(필드 추가) + 문서에 "Phase 2" 표기 |
| `agents` 파라미터 | 명세 5.11 요청에 포함 | 무시하고 3종 실행 | **문서 수정** (PoC 동작 기준) |
| Mock A2 문구 | 조문 번호 필수·단계 표기 | 2번 항목 위반 | **코드 수정** (1줄) |
| Mock 출력 스키마 | 통과 주장 | 실제 응답으로 재검증 통과 | 일치. `validate_schemas.py`는 축약본이라 표현만 완화 |
| 시드 결과 | — | `SeedDataInitializer.seedAgentRun`이 `engine.payload()`를 그대로 사용 | 일치 |
| `AiConfig` 필드 | 문서 8개 | 코드 8개 + `BaseTimeEntity` | 일치 |
| 전환 지점 클래스명 | `AgentEngine` 인터페이스 | 구체 클래스만 존재 | **문서 수정** 또는 인터페이스 추출 |

## 루브릭 문구별 대응 여부
| 루브릭 문구 | 대응 산출물 | 충분 / 보완 필요 | 보완 내용 |
| --- | --- | --- | --- |
| AI 확장 지점 정의 및 프롬프트/JSON 스키마 타당성 | `prompts.md` 4~8장, `schemas/` 4종, `validate_schemas.py` 역예제 13종 | 충분 | #8 클래스명 정정 |
| 향후 AI 기능이 들어올 확장 지점이 서비스 흐름상 타당한가 | `prompts.md` 11장 (Mock→LLM 5단계, Phase 2 RAG) | 충분 | #8 |
| AI 프롬프트 설계 및 입출력 JSON 스키마가 기존 웹 구조와 호환되는가 | `playground_validation.md` 3장, 스키마 루트=`payload_json`, `$defs.modelOutput`=LLM 계약 분리 | 충분 | #7 표현 완화, #2 코드 수정 |
| Interface First | `prompts.md:38` | 보완 필요 | #12 근거 클래스명 추가 |
| Structured Data | `prompts.md:39`, 3장 표 | 충분 | — |
| Asynchronous Pipeline | `prompts.md:40`, 9장 타임아웃·재시도 표 | 충분 | #4 `errorMessage` 필드 |
| Security & Config Isolation | `prompts.md:41`, 11장 4단계 `egress_allowed` | 보완 필요 | #11 `.env.example` 근거 |

## 수정 우선순위 (발표까지 남은 시간 기준)
1. **(10분, HIGH)** ERD·Use-Case 파일 커밋 확인. 없으면 `prompts.md:96`, `:358`의 ERD 인용 문구를 명세 5.11 인용으로 교체.
2. **(15분, HIGH)** `docs/07_api/README.md` 7장 표에 `ai_configs`, `input_snapshot`, `original_json`, `request_no` 추가.
3. **(20분, HIGH)** `AgentService.patchResult` A3 부분 수정 merge 처리 + 테스트 1건. 시간 없으면 명세 5.13 A3 예시를 전체 필드 전송으로 바꿔 문서만 맞춤.
4. **(5분, MED)** `MockAgentEngine.java:44` A2 문구를 프롬프트 규칙에 맞게 수정.
5. **(10분, MED)** `prompts.md` 11장 "`AgentEngine` 인터페이스" 표현 정정, 12장에 프롬프트 파일 분리 계획 추가, 5.11 `agents` 무시 문구 추가.
6. **(15분, MED)** `AgentStepResponse`에 `errorMessage` 추가, FE A3 "항목 추가" 제거 또는 `type` 선택.
7. **(5분, LOW)** `.env.example`에 AI 키 항목, `prompts.md:38`에 불변 클래스명.
