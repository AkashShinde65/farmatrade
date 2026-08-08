# ==========================================================
# STAGE 1 - Build
# ==========================================================
FROM maven:3.9.8-eclipse-temurin-21 AS build

WORKDIR /workspace

# Leverage Docker layer caching for dependencies
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ==========================================================
# STAGE 2 - Runtime
# ==========================================================
FROM eclipse-temurin:21-jre-jammy AS runtime

# Non-root user for security
RUN groupadd -r farmatrade && useradd -r -g farmatrade bidding
WORKDIR /app

COPY --from=build /workspace/target/bidding-service.jar app.jar

RUN chown -R bidding:farmatrade /app
USER bidding

ENV JAVA_OPTS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
EXPOSE 8083

HEALTHCHECK --interval=30s --timeout=5s --start-period=40s --retries=3 \
  CMD curl -f http://localhost:8083/actuator/health || exit 1

ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
