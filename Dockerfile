# Imagen base con Gradle y JDK 17
FROM gradle:8.4.0-jdk17-alpine AS build

# Copiamos los archivos del proyecto al contenedor
COPY . /app
WORKDIR /app

# Construimos el proyecto usando Gradle
RUN gradle build --no-daemon

# Usamos una imagen más liviana para producción
FROM eclipse-temurin:17-jre-alpine

# Copiamos el resultado de la build al nuevo contenedor
COPY --from=build /app/build/libs/*.jar /app/app.jar

# Expone el puerto 8080
EXPOSE 8080

# Comando para ejecutar la app
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
