# 1. Build giai đoạn
FROM maven:3.8.5-openjdk-17 AS build
COPY . .
RUN mvn clean package -DskipTests

# 2. Chạy giai đoạn
FROM openjdk:17.0.1-jdk-slim
COPY --from=build /target/WebDichVu_SpringBoot-0.0.1-SNAPSHOT.jar doan.jar
EXPOSE 8080
ENTRYPOINT ["java","-jar","doan.jar"]