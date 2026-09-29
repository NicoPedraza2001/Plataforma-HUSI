# ============================================================
# Etapa 1: compilacion. Usa Maven + JDK completo (imagen pesada)
# solo para generar el .jar; esta etapa no llega a la imagen final.
# ============================================================
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Se copian primero el pom.xml y el wrapper de Maven para aprovechar
# la cache de Docker: si no cambian las dependencias, este paso no se
# vuelve a ejecutar aunque cambie el codigo fuente.
COPY pom.xml .
COPY .mvn .mvn
COPY mvnw .
# mvnw se genera en Windows con saltos de linea CRLF; en un contenedor
# Linux eso rompe el shebang (#!/bin/sh queda como #!/bin/sh\r) y el
# script no se puede ejecutar. Se normaliza a LF antes de usarlo.
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw
RUN ./mvnw dependency:go-offline

COPY src src
RUN ./mvnw package -DskipTests

# ============================================================
# Etapa 2: ejecucion. Solo el JRE (sin Maven ni codigo fuente),
# imagen final liviana.
# ============================================================
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
