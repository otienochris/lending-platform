# =========================
# Build stage
# =========================
FROM maven:3.9.9-eclipse-temurin-21 AS builder

WORKDIR /build

# Copy everything (parent + modules)
COPY . .

# Build only the target module, but from the parent reactor
ARG SERVICE_NAME
RUN mvn -pl ${SERVICE_NAME} -am clean package -DskipTests


# =========================
# Runtime stage
# =========================
FROM eclipse-temurin:21-jre

WORKDIR /app

ARG SERVICE_NAME

# Copy the fat jar from the module
COPY --from=builder /build/${SERVICE_NAME}/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java","-jar","app.jar"]
