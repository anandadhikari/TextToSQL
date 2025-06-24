# ---------- Build Stage ----------
FROM maven:3.9.6-eclipse-temurin-21 AS builder
WORKDIR /workspace/app

COPY pom.xml .
COPY src src

RUN mvn clean package -DskipTests
RUN ls -l /workspace/app/target

# ---------- Run Stage ----------
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app

COPY --from=builder /workspace/app/target/*.jar app.jar

# Set environment variables
ENV SPRING_PROFILES_ACTIVE=prod
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75.0"

EXPOSE 8080

ENTRYPOINT ["sh", "-c", "exec java ${JAVA_OPTS} -Dspring.profiles.active=${SPRING_PROFILES_ACTIVE} -jar /app/app.jar"]
