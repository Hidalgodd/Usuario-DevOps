# --- Etapa 1: Build (Construcción) ---
# Usamos una imagen de Maven con JDK 21 para compilar el código
FROM maven:3.9.6-eclipse-temurin-21-alpine AS build
WORKDIR /app

# 1. Copiamos el POM y descargamos las dependencias
# Esto se hace primero para aprovechar la caché de capas de Docker
COPY pom.xml .
RUN mvn dependency:go-offline -B

# 2. Copiamos el código fuente y compilamos el archivo JAR
COPY src ./src
RUN mvn clean package -DskipTests

# --- Etapa 2: Runtime (Ejecución) ---
# Usamos solo el JRE 21 (más ligero) para correr la aplicación
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# 3. Seguridad: Creamos un usuario de sistema para no ejecutar como root
RUN addgroup -S spring && adduser -S spring -G spring

# 4. Copiamos el artefacto construido desde la etapa anterior
COPY --from=build /app/target/*.jar app.jar
COPY --chown=spring:spring ./logs /app/logs

USER spring:spring

# 5. Exponemos el puerto del microservicio usuario
EXPOSE 8081

# 6. Comando de inicio con optimización de memoria para contenedores
ENTRYPOINT ["java", "-XX:+UseContainerSupport", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]
