# Stage 1: Build the JAR
FROM gradle:8.7-jdk21-alpine AS builder
WORKDIR /app

COPY build.gradle settings.gradle ./
COPY src ./src

# Use cache mount to persist Gradle dependencies across builds
RUN --mount=type=cache,target=/root/.gradle gradle bootJar -x test --no-daemon && rm -f /app/build/libs/*-plain.jar

# Stage 2: Ultra-lightweight runtime (~150MB)
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Install Tini in the runtime image
RUN apk add --no-cache tini

COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080

# Use Tini as PID 1 to safely spawn and manage the JVM process, preventing zombie processes
ENTRYPOINT ["tini", "--", "java", "-jar", "app.jar"]