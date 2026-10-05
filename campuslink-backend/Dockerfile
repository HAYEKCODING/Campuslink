# ===================== Build =====================
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /build
COPY pom.xml .
RUN mvn -B dependency:go-offline
COPY src ./src
RUN mvn -B clean package -DskipTests

# ===================== Runtime =====================
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
RUN addgroup -S campuslink && adduser -S campuslink -G campuslink
COPY --from=build /build/target/campuslink-backend.jar app.jar
RUN chown campuslink:campuslink app.jar
USER campuslink

EXPOSE 8080

HEALTHCHECK --interval=15s --timeout=5s --start-period=40s --retries=5 \
    CMD wget -qO- http://localhost:8080/api/actuator/health | grep -q '"status":"UP"' || exit 1

ENTRYPOINT ["java", "-jar", "app.jar"]
