# syntax=docker/dockerfile:1
# ---------- build ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
# Cache mount do ~/.m2 compartilhado entre builds e reexecuções: menos rede, build estável.
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -ntp -q dependency:go-offline
COPY src ./src
RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn -B -ntp -q clean package -DskipTests

# ---------- runtime ----------
FROM eclipse-temurin:21-jre
WORKDIR /app
# curl para o healthcheck do compose (actuator/health).
RUN apt-get update && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
