FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline
COPY src ./src
RUN mvn -q -DskipTests package

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S farmatrade && adduser -S -G farmatrade farmatrade
COPY --from=build /workspace/target/lot-service-0.0.1-SNAPSHOT.jar /app/lot-service.jar
RUN mkdir -p /app/uploads/lots && chown -R farmatrade:farmatrade /app/uploads
USER farmatrade
EXPOSE 8082
HEALTHCHECK --interval=30s --timeout=5s --start-period=30s --retries=3 \
  CMD wget -qO- http://127.0.0.1:8082/actuator/health | grep -q '"status":"UP"' || exit 1
ENTRYPOINT ["java", "-jar", "/app/lot-service.jar"]
