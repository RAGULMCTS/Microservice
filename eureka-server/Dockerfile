# syntax=docker/dockerfile:1

# ---------- Stage 1: build the jar with Maven ----------
FROM maven:3.9.9-eclipse-temurin-17 AS build
WORKDIR /workspace

# Copy every module's pom.xml (not source) first. Maven needs the full
# reactor's pom.xml files to resolve the <modules> list and dependency
# versions from the parent, even though we only build one module below.
# This also lets Docker cache the dependency-download layer separately
# from source code, so editing Java files doesn't force a re-download.
COPY pom.xml .
COPY eureka-server/pom.xml eureka-server/pom.xml
COPY config-server/pom.xml config-server/pom.xml
COPY api-gateway/pom.xml api-gateway/pom.xml
COPY user-service/pom.xml user-service/pom.xml
COPY property-service/pom.xml property-service/pom.xml
COPY tenant-service/pom.xml tenant-service/pom.xml

RUN mvn -q -pl eureka-server dependency:go-offline

# Now copy this module's source and build only it ("-pl") skipping tests
# (tests belong in CI, not in the image build).
COPY eureka-server/src eureka-server/src
RUN mvn -q -pl eureka-server package -DskipTests

# ---------- Stage 2: minimal runtime image ----------
FROM eclipse-temurin:17-jre-alpine

# Run as a non-root user so a compromised app process can't touch the
# rest of the container/host as root.
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

WORKDIR /app
# Only the built jar crosses into this stage - no JDK, no Maven, no source,
# no build cache. Smaller image, smaller attack surface.
COPY --from=build /workspace/eureka-server/target/eureka-server-*.jar app.jar

EXPOSE 8761

ENTRYPOINT ["java", "-jar", "app.jar"]
