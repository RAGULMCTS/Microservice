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

RUN mvn -q -pl user-service dependency:go-offline

# DB credentials and the JWT secret are NOT here and NOT in this module's
# source tree - they live only in environment variables supplied at
# container runtime (see docker-compose.yml + .env). Nothing secret ever
# gets baked into this image's layers.
COPY user-service/src user-service/src
RUN mvn -q -pl user-service package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring

# Pre-create the log dir owned by "spring" - on first mount, Docker copies
# this image path's contents (and ownership) into the fresh named volume
# mounted here by docker-compose, so the non-root JVM can write to it.
RUN mkdir -p /var/log/renthub/user-service && chown -R spring:spring /var/log/renthub
USER spring:spring

WORKDIR /app
COPY --from=build /workspace/user-service/target/user-service-*.jar app.jar

EXPOSE 8091

ENTRYPOINT ["java", "-jar", "app.jar"]
