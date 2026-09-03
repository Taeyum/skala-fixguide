# ---- build stage ----
FROM eclipse-temurin:21-jdk AS build
WORKDIR /app

# 의존성 캐시를 위해 빌드 설정만 먼저 복사
COPY gradlew build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew && ./gradlew dependencies --no-daemon || true

# 소스 복사 후 빌드 (테스트는 CI에서 실행)
COPY src ./src
RUN ./gradlew bootJar --no-daemon -x test

# ---- run stage ----
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
