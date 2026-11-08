# Giai đoạn 1: Build JAR
FROM gradle:8.7-jdk21-alpine AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

# Dùng cache mount để giữ lại thư viện Gradle giữa các lần build
RUN --mount=type=cache,target=/root/.gradle gradle bootJar -x test --no-daemon && rm -f /app/build/libs/*-plain.jar

RUN apk add --no-cache tini
# Giai đoạn 2: Runtime siêu nhẹ (~150MB)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Tini được cài trong runtime image
RUN apk add --no-cache tini

COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080

# Dùng Tini làm PID 1, nó sẽ sinh ra và quản lý JVM an toàn và đồng thời hạn chế tình trạng Zombie Process
ENTRYPOINT ["tini", "--", "java", "-jar", "app.jar"]

