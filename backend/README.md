# FixGuide Backend

REQ-F-0001 부품 교체 요청·승인 시스템의 백엔드입니다.
기준 문서: **WRA API 명세서 v1.0** · **WRA 화면정의서 v2.0**

## 스택

| 항목 | 값 |
|---|---|
| 언어·런타임 | Java 21 |
| 프레임워크 | Spring Boot 3.3.5 (Web · Validation · Data JPA · Security) |
| 인증 | JWT (jjwt 0.12) · `Authorization: Bearer {accessToken}` |
| DB | 로컬 H2(in-memory) / 컨테이너 PostgreSQL 16 |
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

- H2 in-memory, 기동 시 시드 데이터 자동 주입
- Swagger UI: http://localhost:8080/swagger-ui.html
- H2 콘솔: http://localhost:8080/h2-console (JDBC URL `jdbc:h2:mem:fixguide`)

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
| `DB_HOST` / `DB_PORT` / `DB_NAME` / `DB_USER` / `DB_PASSWORD` | `db` / `5432` / `fixguide` / `fixguide` / `fixguide` | docker 프로파일 전용 |

## 시드 계정

회원가입 화면이 이번 스코프에서 제외되어, 로그인용 계정을 시드로 넣어 둡니다.
비밀번호는 모두 `Passw0rd!23` 입니다.

| 이메일 | 이름 | 역할 | 로그인 후 이동 |
|---|---|---|---|
| `engineer@fixguide.dev` | 이엔지 | ENGINEER | `/home` |
| `engineer2@fixguide.dev` | 김현장 | ENGINEER | `/home` |
| `safety@fixguide.dev` | 박안전 | SAFETY_MANAGER | `/manage/requests` |

## 이 브랜치가 구현한 API

| # | Method | Path | 화면 | 비고 |
|---|---|---|---|---|
| 2 | POST | `/api/v1/auth/login` | WRA_C_00 | 역할별 `redirectPath` 반환 |
| 3 | GET | `/api/v1/auth/me` | 공통 | 새로고침 시 역할 확인 |
| 4 | GET | `/api/v1/dashboard/summary?role=` | E_01 · S_01 | 역할 KPI + 거절 사유 TOP5 |
| 6 | GET | `/api/v1/work-requests` | E_01 · E_05 · S_01 | `mine` · `status` · 페이지네이션 · `nextAction` |

> 회원가입(`POST /auth/signup`)은 팀 합의에 따라 이번 범위에서 제외했습니다.
> 나머지 API(요청 등록·AI 검증·결과 수정·제출·승인)는 담당자가 이어서 붙입니다.

## 빠른 확인 (curl)

```bash
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"engineer@fixguide.dev","password":"Passw0rd!23"}' | jq -r .accessToken)

curl -s http://localhost:8080/api/v1/auth/me -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/v1/dashboard/summary?role=engineer" -H "Authorization: Bearer $TOKEN"
curl -s "http://localhost:8080/api/v1/work-requests?mine=true&size=5" -H "Authorization: Bearer $TOKEN"
```

## 테스트

```bash
./gradlew test
```

MockMvc 통합 테스트로 로그인 성공/401, `/auth/me` 401, 대시보드 역할 분기·403,
목록의 역할별 노출 범위와 `nextAction` 계산을 검증합니다.
