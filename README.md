# FixGuide

## 프로젝트 소개

<!-- 프로젝트 개요를 작성하세요 -->

## 프로젝트 구조

```
fixguide/
├── README.md
├── docker-compose.yml
├── .env.example
├── .github/          # GitHub 워크플로우 및 설정
├── docs/             # 프로젝트 문서
│   ├── 01_planning/      # 기획
│   ├── 02_usecase/       # 유스케이스
│   ├── 03_wireframe/     # 와이어프레임
│   ├── 04_architecture/  # 아키텍처
│   ├── 05_ai_ready/      # AI Ready
│   ├── 06_erd/           # ERD
│   ├── 07_api/           # API 명세
│   ├── 08_presentation/  # 발표 자료
│   ├── 09_qa/            # QA
│   └── CONTRACT.md       # 계약/규약 문서
├── backend/          # 백엔드 (Spring Boot, com.skala.fixguide) → backend/README.md 참고
│   └── Dockerfile
├── frontend/         # 프론트엔드 (Vue 3 + Vite + Router + Pinia)
│   └── Dockerfile
└── infra/            # 인프라
    └── db/init/      # DB 최초 기동 시 실행할 SQL
```

## 기술 스택

| 구분 | 스택 |
|------|------|
| Frontend | Vue.js (Vite) |
| Backend | Spring Boot 3.3.5 (Java 21, Gradle) · Swagger UI |
| Database | PostgreSQL 16 (도커) / H2 in-memory (로컬 단독 실행, 테스트) |
| Cache / Session | Redis 7 (JWT Refresh Token, 블랙리스트 저장용) |
| 인증 | Spring Security + JWT (jjwt) |
| 환경 | Docker Compose |

## 시작하기

### 사전 준비

- Docker Desktop 설치
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
docker compose up -d db redis    # DB, Redis만 실행 (백엔드/프론트는 로컬에서 직접 띄울 때)
docker compose up -d --build backend  # 백엔드 재빌드
docker compose down              # 전체 중지
docker compose down -v           # 전체 중지 + DB 데이터 삭제
```

### 참고

- 백엔드는 Spring 프로파일로 DB를 고릅니다. 도커에서는 `SPRING_PROFILES_ACTIVE=docker`로 PostgreSQL에 붙고, IDE나 `./gradlew bootRun`으로 단독 실행하면 기본 `local` 프로파일이 H2 in-memory를 씁니다.
- 도커 DB에 IDE에서 붙고 싶으면 `SPRING_PROFILES_ACTIVE=docker DB_HOST=localhost ./gradlew bootRun` 으로 실행합니다.
- JWT, CORS, 시드 데이터 등 백엔드 환경 변수 목록과 시드 계정은 `backend/README.md`에 있습니다.
- `infra/db/init/` 에 `.sql` 파일을 두면 DB 최초 생성 시 자동 실행됩니다. (이미 볼륨이 있으면 실행 안 됨 → `down -v` 후 재실행)

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
