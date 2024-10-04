
FROM maven:3.9.9-amazoncorretto-17-al2023 AS build


WORKDIR /app

COPY pom.xml /app/
COPY src /app/src/

RUN mvn clean package -DskipTests

FROM eclipse-temurin:17.0.12_7-jre-jammy

RUN apt-get update && apt-get install -y \
    fontconfig libfreetype6 libfontconfig1 \
    && rm -rf /var/lib/apt/lists/*

COPY --from=build /app/target/*.jar /app/application.jar

EXPOSE 8080

WORKDIR /app

ENTRYPOINT ["java", "-jar", "application.jar"]


