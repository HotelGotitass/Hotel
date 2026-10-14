FROM eclipse-temurin:21-jdk-alpine AS build
WORKDIR /app
COPY pom.xml .
COPY mvnw .
COPY .mvn .mvn
COPY src src
# Dar permisos de ejecución al wrapper de maven
RUN chmod +x ./mvnw
RUN ./mvnw clean package -DskipTests

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar
# Crear el directorio para subir imágenes
RUN mkdir uploads
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
