# Imagem única da aplicação: o build do frontend vai para o static/ do Spring Boot, que serve a SPA
# e a API no mesmo domínio.
#
#   docker build -t travel-planner .

# --- 1. Frontend --------------------------------------------------------------------------------
FROM node:22-alpine AS frontend
WORKDIR /build/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci --no-audit --no-fund
COPY frontend/ ./
RUN npm run build

# --- 2. Backend ---------------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS backend
WORKDIR /build/backend
# Dependências numa camada própria (cache): só baixam de novo quando o pom.xml muda.
COPY backend/pom.xml ./
RUN mvn -B -q dependency:go-offline
COPY backend/ ./
COPY --from=frontend /build/frontend/dist/ src/main/resources/static/
RUN mvn -B -q -DskipTests package && cp target/travel-planner-*.jar /build/app.jar

# --- 3. Runtime ---------------------------------------------------------------------------------
FROM eclipse-temurin:21-jre
# JVM leve para 1 vCPU no Cloud Run.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
RUN groupadd --system app && useradd --system --gid app --no-create-home app
WORKDIR /app
COPY --from=backend /build/app.jar app.jar
USER app
# O Cloud Run injeta PORT (8080).
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
