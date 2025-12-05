# Build stage (Maven + JDK 21)
FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /app

# (optional optimization: copy pom and download deps separately)
COPY pom.xml .
RUN mvn -q -DskipTests dependency:go-offline

COPY . .
RUN mvn clean package -DskipTests

# Run stage (JDK 21)
FROM eclipse-temurin:21-jdk AS run
WORKDIR /app
COPY --from=build /app/target/crypto-advisor-0.0.1-SNAPSHOT.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
