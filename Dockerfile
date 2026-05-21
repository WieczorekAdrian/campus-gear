# Krok 1: Budowanie aplikacji za pomocą Mavena
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY src ./src
RUN mvn clean package -DskipTests

# Krok 2: Uruchomienie aplikacji
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
# Gwiazdka (*) skopiuje każdy wygenerowany plik jar jako app.jar
COPY --from=build /app/target/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]