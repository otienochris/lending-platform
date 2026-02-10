
FROM eclipse-temurin:21-jdk-alpine AS builder
LABEL authors="Chris"
COPY  ./target/*.jar artifact.jar
RUN java -Djarmode=layertools -jar artifact.jar extract


FROM eclipse-temurin:21-jdk-alpine
COPY --from=builder dependencies/ ./
RUN true
COPY --from=builder snapshot-dependencies/ ./
RUN true
COPY --from=builder spring-boot-loader/ ./
RUN true
COPY --from=builder application/ ./
ENTRYPOINT ["java", "org.springframework.boot.loader.launch.JarLauncher"]
EXPOSE 8086