# syntax=docker/dockerfile:1

FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY property-service/pom.xml property-service/pom.xml
COPY tenant-service/pom.xml tenant-service/pom.xml

RUN mvn -q -pl api-gateway dependency:go-offline

COPY api-gateway/src api-gateway/src
RUN mvn -q -pl api-gateway package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring

# Pre-create the log dir owned by "spring" - on first mount, Docker copies
# this image path's contents (and ownership) into the fresh named volume
# mounted here by docker-compose, so the non-root JVM can write to it.
RUN mkdir -p /var/log/renthub/api-gateway && chown -R spring:spring /var/log/renthub
USER spring:spring

WORKDIR /app
COPY --from=build /workspace/api-gateway/target/api-gateway-*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
