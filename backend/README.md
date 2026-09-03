# FixGuide Backend

REQ-F-0001 부품 교체 요청·승인 시스템의 백엔드입니다.
기준 문서: **WRA API 명세서 v1.0** · **WRA 화면정의서 v2.0**

## 스택

| 항목 | 값 |
|---|---|
| 언어·런타임 | Java 21 |
| 프레임워크 | Spring Boot 3.3.5 (Web · Validation · Data JPA · Security) |
| 인증 | JWT (jjwt 0.12) · `Authorization: Bearer {accessToken}` |
| DB | PostgreSQL 16 (로컬·도커·테스트 모두 동일. 테스트는 Testcontainers) |
| 문서 | springdoc-openapi · Swagger UI |
| 빌드 | Gradle Wrapper |

## 패키지 구조 (도메인형 + MVC 3계층)

```
com.skala.fixguide
├── common          # 설정 · 공통 에러 포맷 · 공통 엔티티/DTO
│   ├── config      # SecurityConfig · CorsConfig · OpenApiConfig · AppConfig
│   ├── error       # ErrorCode · ApiException · ErrorResponse · GlobalExceptionHandler
│   ├── entity      # BaseTimeEntity · JsonMapConverter
│   └── dto         # PageResponse
├── auth            # 로그인 · 내 정보 (WRA_C_00)
│   ├── controller / service / dto
│   └── jwt         # JwtTokenProvider · 인증 필터 · 401/403 핸들러
├── user            # User 엔티티 · Role
├── workrequest     # 요청 목록 조회 (WRA_E_01 · E_05 · S_01)
├── approval        # 승인 이력 (대시보드 집계용 읽기)
├── dashboard       # 역할별 KPI (WRA_E_01 · S_01)
└── init            # 로컬/데모용 시드 데이터
```

계층 규칙은 `Controller → Service → Repository` 단방향입니다.
Controller 는 HTTP 관심사만, 비즈니스 규칙은 Service, 쿼리는 Repository 에 둡니다.

## 실행

### 1) 로컬 단독 (도커 불필요)

```bash
cd backend
./gradlew bootRun
```

- `docker compose up -d db redis` 로 PostgreSQL·Redis 를 먼저 띄운다 (기본 접속값 localhost:5432 / fixguide)
- 기동 시 시드 데이터 자동 주입
- Swagger UI: http://localhost:8080/swagger-ui.html

### 2) PostgreSQL 과 함께

```bash
SPRING_PROFILES_ACTIVE=docker ./gradlew bootRun
```

또는 루트의 `docker-compose.yml` 로 `backend` + `db` 를 함께 기동합니다.

### 환경변수

| 변수 | 기본값 | 설명 |
|---|---|---|
| `SERVER_PORT` | `8080` | 서버 포트 |
| `JWT_SECRET` | (개발용 기본값) | HS256 서명 키. **운영은 반드시 주입** |
| `JWT_EXPIRES_IN` | `3600` | 액세스 토큰 만료(초) |
| `CORS_ALLOWED_ORIGINS` | `http://localhost:5173,http://localhost:3000` | FE 오리진 |
| `SEED_ENABLED` | `true` | 시드 데이터 주입 여부 |
| `TOKEN_BLACKLIST_TYPE` | `memory` | 로그아웃 토큰 저장소. `redis` 로 바꾸면 Redis 사용 |
| `REDIS_HOST` / `REDIS_PORT` | `localhost` / `6379` | `TOKEN_BLACKLIST_TYPE=redis` 일 때만 사용 |
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `db` / `5432` / `fixguide` / `fixguide` / `fixguide` | docker 프로파일 전용 |
| `UPLOAD_DIR` | `uploads` | 제품 사진 저장 디렉터리 (도커는 `backend-uploads` 볼륨) |

## 시드 데이터

DB 가 비어 있을 때 `SeedDataInitializer` 가 계정 3개, 모든 상태의 요청 7건, AI 실행 이력·결과 6건, 사진 2장,
승인 이력 5건을 넣습니다. 명세서 16개 API 를 추가 준비 없이 바로 호출해 볼 수 있습니다.
어떤 요청에 무엇이 들어 있는지는 루트 `README.md` 의 "테스트 데이터" 표를 보세요.

### 시드 계정

비밀번호는 모두 `Passw0rd!23` 입니다.

| 이메일 | 이름 | 역할 | 로그인 후 이동 |
|---|---|---|---|
| `engineer@fixguide.dev` | 이엔지 | ENGINEER | `/home` |
| `engineer2@fixguide.dev` | 김현장 | ENGINEER | `/home` |
| `safety@fixguide.dev` | 박안전 | SAFETY_MANAGER | `/manage/requests` |

## 구현된 API (명세서 16개 전부)

| # | Method | Path | 화면 | 비고 |
|---|---|---|---|---|
| 1 | POST | `/api/v1/auth/signup` | WRA_C_01 | 역할 선택 필수 · 중복 이메일 409 |
| 2 | POST | `/api/v1/auth/login` | WRA_C_00 | 역할별 `redirectPath` 반환 |
| 3 | GET | `/api/v1/auth/me` | 공통 | 새로고침 시 역할 확인 |
| 4 | GET | `/api/v1/dashboard/summary?role=` | E_01 · S_01 | 역할 KPI + 거절 사유 TOP5 |
| 5 | POST | `/api/v1/work-requests` | E_02 | `draft=true` 면 임시저장 · 201 |
| 6 | GET | `/api/v1/work-requests` | E_01 · E_05 · S_01 | `mine` · `status` · 페이지네이션 · `nextAction` |
| 7 | GET | `/api/v1/work-requests/{id}` | E_04 · E_05 · S_02 | 사진 · 최신 AI 결과 · 최신 승인 이력 포함 |
| 8 | PATCH | `/api/v1/work-requests/{id}` | E_02 · E_04 | 부분 수정 · PENDING/APPROVED 는 409 |
| 9 | POST | `/api/v1/work-requests/{id}/photos` | E_02 | multipart `files` · jpg/png/webp · 10MB · 5장 |
| 10 | GET | `/api/v1/work-requests/{id}/photos` | S_02 | 원본 URL 은 `/api/v1/files/**` 정적 서빙 |
| 11 | POST | `/api/v1/agent-runs` | E_02 | AI 3종 실행 · 202 · `runId` 로 폴링 |
| 12 | GET | `/api/v1/agent-runs/{runId}` | E_03 | 호출마다 step 하나씩 완료되는 Mock 전이 · `allDone` |
| 13 | PATCH | `/api/v1/agent-results/{id}` | E_04 | `items`(A1·A2) / `documents`(A3) 전체 치환 |
| 14 | PATCH | `/api/v1/work-requests/{id}/submit-approval` | E_04 | AI_DONE·REJECTED 에서만 · 검증 실패 422 |
| 15 | POST | `/api/v1/approvals` | S_02 | 안전관리자 전용 · REJECT 는 사유 10자 이상 |
| 16 | POST | `/api/v1/auth/logout` | 공통 GNB | 토큰 블랙리스트 등록 → 즉시 무효화 |

### AI 검증은 Mock 상태 머신

`POST /agent-runs` 는 run 과 step 3개(WAITING)를 만들고 202 를 돌려줍니다. 실제 AI 호출은 없고,
`GET /agent-runs/{runId}` 를 **호출할 때마다 step 하나가 DONE 으로 바뀌며 결과가 생성**됩니다.
세 번 폴링하면 `allDone=true` 가 되고 요청은 `AI_DONE` 으로 전환됩니다. 실제 LLM 은 `MockAgentEngine` 만
`ai_configs.provider` 기반 구현으로 교체하면 됩니다.

### 로그아웃 블랙리스트 — Redis 연동 전 상태

로그아웃은 토큰의 `jti` 를 남은 유효시간만큼 TTL 로 보관해 즉시 무효화합니다. 저장소는 설정으로 갈아끼웁니다.

| `TOKEN_BLACKLIST_TYPE` | 구현 | 용도 |
|---|---|---|
| `memory` (기본) | `InMemoryTokenBlacklistStore` | 로컬 실행·테스트. 재시작하면 사라지고 인스턴스 간 공유 안 됨 |
| `redis` | `RedisTokenBlacklistStore` | 운영. Redis TTL 로 자동 만료 |

**기본값을 `memory` 로 둔 이유** — Redis 인프라는 별도 담당자가 준비 중이라, 그 전에도 `./gradlew bootRun` 과 `./gradlew test` 가 그대로 돌아가야 하기 때문입니다. 컨테이너가 올라오면 아래 두 값만 주입하면 전환됩니다.

```bash
TOKEN_BLACKLIST_TYPE=redis REDIS_HOST=redis REDIS_PORT=6379
```

전환이 실제로 되는지는 `TokenBlacklistStoreSelectionTest` 가 두 설정 모두에 대해 검증합니다.

> ⚠️ `redis` 로 켜면 **인증이 필요한 모든 요청이 Redis 조회 1회를 거칩니다.** 즉시 로그아웃을 얻는 대신 stateless JWT 의 이점을 일부 반납하는 트레이드오프입니다.

## 빠른 확인 (curl)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"engineer@fixguide.dev","password":"Passw0rd!23"}' | jq -r .accessToken)

curl -s http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/v1/dashboard/summary?role=engineer" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/v1/work-requests?mine=true&size=5" -H "Authorization: Bearer $TOKEN"

# 로그아웃 → 204, 같은 토큰 재사용 시 401 TOKEN_REVOKED
curl -s -o /dev/null -w '%{http_code}\n' -X POST http://localhost:8080/api/v1/auth/logout \
  -H "Authorization: Bearer $TOKEN"
curl -s http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"
```

## 테스트

```bash
./gradlew test
```

MockMvc 통합 테스트로 로그인 성공/401, `/auth/me` 401, 대시보드 역할 분기·403,
목록의 역할별 노출 범위와 `nextAction` 계산을 검증합니다.
