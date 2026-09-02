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

RUN mvn -q -pl config-server dependency:go-offline

# Note: config-server/src/main/resources/config-repo/*.yml is bundled into
# this jar at build time. Those files now only contain ${PLACEHOLDER}
# references, never real secrets - the real values are injected as
# environment variables into the *client* containers at "docker run" time,
# not baked in here. See the docker-compose.yml comments for how that works.
COPY config-server/src config-server/src
RUN mvn -q -pl config-server package -DskipTests

FROM eclipse-temurin:17-jre-alpine
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app
COPY --from=build /workspace/config-server/target/config-server-*.jar app.jar

EXPOSE 8888

ENTRYPOINT ["java", "-jar", "app.jar"]
