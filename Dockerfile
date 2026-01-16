# Build stage
FROM gradle:8.5-jdk17 AS build
WORKDIR /app

# 1단계: 종속성 파일만 복사 (캐시 대상)
COPY build.gradle settings.gradle ./
COPY gradle ./gradle
COPY gradlew ./

# 2단계: 종속성 다운로드 (이 레이어가 캐시됨)
RUN ./gradlew dependencies --no-daemon || true

# 3단계: 소스 복사 및 빌드
COPY src ./src
RUN ./gradlew build -x test --no-daemon --build-cache && \
    rm -f build/libs/*-plain.jar

# Run stage (Alpine 이미지로 경량화)
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# curl 설치 (healthcheck용)
RUN apk add --no-cache curl

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
