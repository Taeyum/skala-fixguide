# infra/db/init

이 폴더의 `*.sql` 은 PostgreSQL 컨테이너가 **볼륨을 처음 만들 때 한 번** 실행됩니다.

그런데 테이블은 백엔드가 기동하면서 Hibernate(`ddl-auto: update`)가 만들기 때문에,
이 시점에는 테이블이 없어 `INSERT` 시드를 여기 둘 수 없습니다.

테스트용 초기 데이터는 백엔드의 `SeedDataInitializer` 가 넣습니다.
- DB 가 비어 있을 때만 실행 (계정 · 모든 상태의 요청 · AI 실행 이력 · 결과 · 사진 · 승인 이력)
- 끄기: `.env` 의 `SEED_ENABLED=false`
- 다시 넣기: `docker compose down -v && docker compose up -d --build`

스키마가 확정되어 Flyway 로 옮기면 그때 SQL 마이그레이션은 `backend/src/main/resources/db/migration/` 에 둡니다.
