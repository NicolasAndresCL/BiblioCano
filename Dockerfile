# syntax=docker/dockerfile:1
# BiblioCano · imagen de la aplicación (multi-stage: se compila con JDK + Maven, se ejecuta con JRE)

# ---------------------------------------------------------------- build
FROM maven:3.9.9-eclipse-temurin-21 AS builder
WORKDIR /build
# Primero solo el pom: si el código cambia pero las dependencias no, esta capa queda en caché
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
# Las pruebas ya corrieron en el CI y en `compose run pruebas`; aquí solo se empaqueta
RUN mvn -B -ntp -DskipTests package

# ---------------------------------------------------------------- runtime
FROM eclipse-temurin:25-jre-alpine AS runtime
RUN addgroup -S app && adduser -S -G app app
WORKDIR /app
COPY --from=builder --chown=app:app /build/target/bibliocano.jar ./bibliocano.jar
COPY --from=builder --chown=app:app /build/target/lib ./lib
USER app
ENV TZ=America/Santiago
# Programa de consola de vida corta (no un servidor): no expone puertos ni necesita HEALTHCHECK
ENTRYPOINT ["java", "-Dstdout.encoding=UTF-8", "-jar", "/app/bibliocano.jar"]
CMD ["demo"]
