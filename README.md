# FixGuide (Argus)

반도체 팹 **부품 교체 요청·승인 시스템** (REQ-F-0001). 설비 엔지니어가 교체 요청을 등록하면 AI 에이전트 3종이 규격·법령·안전서류를 검토하고, 엔지니어가 결과를 수정해 제출하면 안전관리자가 승인·거절합니다.

- 엔지니어: 로그인 → 요청 등록(설비·물질·운전조건·제품 스펙·사진) → AI 검증 진행 확인 → 결과 수정·엔지니어 설명 작성 → 제출 → 내 요청 현황 확인 → 거절 시 재제출
- 안전관리자: 승인 대기 목록·KPI → 요청 상세(AI 결과·엔지니어 설명) → 승인 / 거절(사유 필수)
- AI: 현재 PoC 는 **Mock** (`MockAgentEngine`). 프롬프트·입출력 JSON 스키마는 `docs/05_ai_ready/` 에 설계돼 있고, 실제 LLM 전환 시 이 클래스만 교체합니다.

## 프로젝트 구조

```
fixguide/
├── README.md
├── docker-compose.yml        # db · redis · backend · frontend
├── .env.example
├── .github/
├── docs/                     # 산출물 (아래 "문서" 표 참고)
├── backend/                  # Spring Boot 3.3.5 · com.skala.fixguide (→ backend/README.md)
│   └── src/main/java/com/skala/fixguide/
│       ├── auth/             # 로그인·회원가입·로그아웃, JWT, 토큰 블랙리스트
│       ├── user/             # User · Role
│       ├── workrequest/      # 요청 등록·목록·상세·수정·제출, 사진
│       ├── agent/            # AI 실행·폴링·결과 수정, MockAgentEngine, AiConfig
│       ├── approval/         # 승인·거절
│       ├── dashboard/        # 역할별 KPI
│       ├── common/           # 설정(Security·CORS·OpenAPI), 에러 포맷, 공통 엔티티
│       └── init/             # 시드 데이터
├── frontend/                 # Vue 3 + Vite (→ 아래 "화면")
│   └── src/
│       ├── api/              # axios 클라이언트, 도메인별 API, 응답 정규화
│       ├── views/            # auth · engineer · safety 화면
│       ├── components/       # 공통 · 도메인 컴포넌트
│       ├── stores/           # Pinia (auth, requestDraft)
│       ├── composables/      # 폴링, 비동기 상태
│       └── router/           # 라우트 + 인증·역할 가드
└── infra/
    └── db/init/              # (README 참고) 시드는 백엔드 SeedDataInitializer 가 담당
```

## 문서

| 폴더 | 파일 | 내용 |
|------|------|------|
| `docs/01_planning/` | `Argus_설계문서.pdf` | 기획·설계 문서 |
| `docs/02_usecase/` | `use_case.png` | 유스케이스 다이어그램 (UC-00 ~ UC-12, 액터 3) |
| `docs/03_wireframe/` | `와이어프레임.pdf` | 화면 10개 와이어프레임 + 화면별 API·DB 매핑 |
| `docs/04_architecture/` | `시스템아키텍처.png` | 시스템 아키텍처 (AI 오케스트레이터·벡터 DB 는 목표 구성) |
| `docs/05_ai_ready/` | `prompts.md`, `schemas/*.json`, `playground_validation.md`, `validate_schemas.py` | 에이전트 3종 프롬프트, 입력 1종·출력 3종 JSON 스키마, 검증 스크립트 |
| `docs/06_erd/` | `erd.md`, `argus_erd.dbml`, `erd.png`, `schema_postgres.sql` | 엔티티 정의서(정본), dbdiagram 소스·이미지, Hibernate 생성 DDL 참고본 |
| `docs/07_api/` | `README.md` | API 명세서 v1.0 — 16개 API, 상태 전이, DB 매핑, 에러 코드 |
| `docs/08_presentation/` | — | 발표 자료 (예정) |
| `docs/09_qa/` | `ai_ready_review.md` | AI-Ready 산출물 검토 보고서 |
| `docs/CONTRACT.md` | | 팀 규약 (작성 예정) |

## 기술 스택

| 구분 | 스택 |
|------|------|
| Frontend | Vue 3 · Vite · Vue Router · Pinia · axios |
| Backend | Spring Boot 3.3.5 (Java 21, Gradle) · Spring Data JPA · Spring Security · springdoc (Swagger UI) |
| Database | PostgreSQL 16 (도커·로컬·테스트 공통, 테스트는 Testcontainers) |
| Cache | Redis 7 (로그아웃 토큰 블랙리스트) |
| 인증 | JWT (jjwt 0.12), `Authorization: Bearer {accessToken}`, 1시간 유효 |
| AI | Mock 에이전트 3종 (A1 규격·호환 / A2 적용 법령 / A3 안전서류 초안). 설정은 `ai_configs` 테이블, 프롬프트는 `docs/05_ai_ready/prompts.md` |
| 테스트 | JUnit 5 + MockMvc + Testcontainers (백엔드 8개 클래스) · ESLint/oxlint (프론트) |
| 환경 | Docker Compose |

## 시작하기

### 사전 준비

- Docker Desktop (백엔드 테스트도 Testcontainers 로 Docker 를 사용)
- (선택) 로컬에서 직접 실행할 경우 Node 22, JDK 21

### 실행

```bash
# 1. 환경 변수 파일 생성 (값은 필요 시 수정)
cp .env.example .env

# 2. 전체 실행
docker compose up -d --build

# 3. 로그 확인
docker compose logs -f
```

| 서비스 | 주소 |
|--------|------|
| Frontend | http://localhost:5173 |
| Backend | http://localhost:8080 (Swagger: /swagger-ui.html) |
| DB | localhost:5432 (fixguide / fixguide) |
| Redis | localhost:6379 (비밀번호 없음) |

### 자주 쓰는 명령

```bash
docker compose up -d db redis          # DB, Redis 만 실행 (백엔드/프론트를 로컬에서 직접 띄울 때)
docker compose up -d --build backend   # 백엔드 재빌드
docker compose down                    # 전체 중지 (데이터 유지)
docker compose down -v                 # 전체 중지 + 볼륨 삭제 → 다음 기동 시 시드 재생성

# 로컬 직접 실행
cd backend && ./gradlew bootRun        # local 프로파일 → localhost:5432 PostgreSQL
cd frontend && npm ci && npm run dev   # VITE_API_BASE_URL 기본 http://localhost:8080

# 테스트
cd backend && ./gradlew test           # Testcontainers 가 PostgreSQL 컨테이너를 자동 기동
cd frontend && npm run lint && npm run build
```

### Postman Mock 서버 (백엔드 없이 프론트·API 계약 확인)

`docs/07_api/api.yaml` 을 Postman 에 임포트해 만든 Mock 서버입니다. 응답은 명세의 `example` 값이며 인증·상태 전이는 검사하지 않습니다.

```bash
MOCK=https://cf475eeb-f5fb-4f82-8f0a-e934e5820c4d.mock.pstmn.io

# 로그인
curl -s -X POST $MOCK/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"email":"engineer@fixguide.dev","password":"Passw0rd!23"}'

# 요청 목록
curl -s $MOCK/api/v1/work-requests

# 요청 상세 (아무 UUID나 매칭)
curl -s $MOCK/api/v1/work-requests/9f1c8a02-77b5-4e0a-9c31-2a4d6f8e1b30

# AI 폴링
curl -s $MOCK/api/v1/agent-runs/5e77b1c9-0000-0000-0000-000000000000

# 승인
curl -s -X POST $MOCK/api/v1/approvals \
  -H "Content-Type: application/json" \
  -d '{"workRequestId":"9f1c8a02-77b5-4e0a-9c31-2a4d6f8e1b30","decision":"APPROVE"}'
```

`api.yaml` 을 고치면 Postman 에 재임포트하고 Mock 서버를 재배포해야 반영됩니다. 프론트에서 쓰려면 `.env` 의 `VITE_API_BASE_URL` 을 위 주소로 바꾸면 됩니다.

### 참고

- 백엔드는 어디서 실행하든 PostgreSQL 만 씁니다. 도커에서는 `SPRING_PROFILES_ACTIVE=docker` 로 `db` 컨테이너에, IDE 나 `./gradlew bootRun` 은 기본 `local` 프로파일로 `localhost:5432` 에 붙습니다.
- JWT·CORS·시드 등 백엔드 환경 변수와 API 표는 `backend/README.md` 에 있습니다.
- 시드는 백엔드 기동 시 `SeedDataInitializer` 가 넣습니다. `infra/db/init/` 의 SQL 은 테이블 생성 전에 실행돼 쓸 수 없습니다 (`infra/db/init/README.md`).
- 제품 사진은 `backend-uploads` 볼륨에 저장되고 `/api/v1/files/**` 로 서빙됩니다.

## 화면

| 코드 | 경로 | 역할 | 내용 |
|------|------|------|------|
| C_00 | `/login` | 공통 | 로그인 → 역할별 홈으로 이동 |
| C_01 | `/signup` | 공통 | 회원가입 (역할 선택) |
| E_01 | `/home` | 엔지니어 | KPI 4종 + 최근 요청 |
| E_02 | `/requests/new` | 엔지니어 | 요청 등록 (유형별 스펙 필드, 사진), 임시 저장 / AI 검증 시작 |
| E_03 | `/requests/:id/run` | 엔지니어 | AI 3종 진행 카드 (2.5초 폴링) |
| E_04 | `/requests/:id/result` | 엔지니어 | AI 결과 수정 · 엔지니어 설명 · 제출 (거절 건 재제출 포함) |
| E_05 | `/my/requests` | 엔지니어 | 내 요청 목록, 거절 사유 확인 |
| S_01 | `/manage/requests` | 안전관리자 | 승인 대기 큐 · KPI · 거절 사유 TOP5 |
| S_02 | `/manage/requests/:id` | 안전관리자 | 요청 상세 · 승인 / 거절 |

API 16개의 요청·응답 형식은 `docs/07_api/README.md`, Swagger 에서 바로 호출하는 방법은 아래 시드 표 다음을 참고하세요.

## 테스트 데이터 (시드)

DB가 비어 있을 때 한 번만 들어갑니다. 다시 넣으려면 `docker compose down -v` 후 재기동하세요. 끄려면 `.env`에 `SEED_ENABLED=false`.

**계정** (비밀번호 공통 `Passw0rd!23`)

| 이메일 | 이름 | 역할 |
|--------|------|------|
| engineer@fixguide.dev | 이엔지 | ENGINEER |
| engineer2@fixguide.dev | 김현장 | ENGINEER |
| safety@fixguide.dev | 박안전 | SAFETY_MANAGER |

**요청** (이엔지 5건 + 김현장 2건, 모든 상태 1건 이상)

| 상태 | 설비 | 같이 들어가는 것 | 바로 해볼 수 있는 API |
|------|------|------------------|----------------------|
| DRAFT | 펌프 P-114 | 사진 2장 | 수정(8) · 사진 목록(10) · AI 실행(11) |
| AI_RUNNING | 가스캐비닛 GC-02 | AI run (1/3 완료) | 폴링(12) 호출마다 한 단계씩 진행 |
| AI_DONE | 스크러버 SCR-01 | AI 결과 3종 | 결과 수정(13) · 제출(14) |
| PENDING | 공정가스 밸브 V-7 | AI 결과 3종 | 안전관리자 승인/거절(15) |
| REJECTED | 펌프 P-208 | AI 결과 · 거절 이력 3건 | 재제출(14) |
| APPROVED | 가스캐비닛 GC-05 (김현장) | AI 결과 · 승인 이력 | 상세(7) |
| PENDING | 스크러버 SCR-03 (김현장) | AI 결과 3종 | 안전관리자 목록(6) |

Swagger(http://localhost:8080/swagger-ui.html)에서 로그인 → 응답의 accessToken 을 Authorize 에 넣으면 나머지 API 를 바로 호출할 수 있습니다.

## Git 컨벤션

### 브랜치 네이밍

```
<type>/<간단한-설명>
```

| 브랜치 | 용도 |
|--------|------|
| `main` | 배포 가능한 안정 버전 |
| `develop` | 개발 통합 브랜치 |
| `feat/*` | 기능 개발 (예: `feat/user-login`) |
| `fix/*` | 버그 수정 (예: `fix/null-pointer`) |
| `docs/*` | 문서 작업 (예: `docs/api-spec`) |
| `refactor/*` | 리팩토링 (예: `refactor/auth-service`) |
| `chore/*` | 설정, 빌드 등 (예: `chore/docker-compose`) |

- 소문자와 하이픈(`-`)만 사용
- 이슈 번호가 있으면 뒤에 붙임 (예: `feat/user-login-#12`)

### 커밋 메시지

```
<type>(<scope>): <subject>

<body>  (선택, 한글 가능)
```

| type | 설명 |
|------|------|
| `feat` | 새로운 기능 추가 |
| `fix` | 버그 수정 |
| `docs` | 문서 수정 |
| `style` | 코드 포맷팅 (로직 변경 없음) |
| `refactor` | 리팩토링 (기능 변경 없음) |
| `test` | 테스트 코드 추가/수정 |
| `chore` | 빌드, 설정, 패키지 등 |

**규칙**
- 제목은 영문 소문자, 명령문으로 작성 (`add`, `fix`, `update`, `remove`)
- 제목은 50자 이내, 마침표 금지
- scope는 선택 (예: `auth`, `api`, `ui`, `db`, `config`)
- 본문에는 '무엇을', '왜' 바꿨는지 작성
- 하나의 커밋에는 하나의 변경만

**예시**

```
feat(auth): add JWT based login

- 로그인 성공 시 Access/Refresh Token 발급
- Refresh Token은 Redis에 저장
```

```
fix(api): handle negative amount error
```

| 피해야 할 예시 | 이유 |
|----------------|------|
| `fix: 버그 수정` | 무슨 버그인지 불명확 |
| `Fixed login bug` | 과거형, 대문자 시작 |
| `feat: add login and signup` | 여러 변경을 한 커밋에 포함 |
| `wip` | 작업 중 상태로 커밋 |
