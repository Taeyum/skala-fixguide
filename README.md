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
├── backend/          # 백엔드
├── frontend/         # 프론트엔드
└── infra/            # 인프라
```

## 시작하기

```bash
cp .env.example .env
docker-compose up -d
```
