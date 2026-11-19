# Changelog

Todos los cambios relevantes de BiblioCano. Formato [Keep a Changelog](https://keepachangelog.com/es-ES/1.1.0/)
y [versionado semántico](https://semver.org/lang/es/). Las diferencias de diseño respecto del avance,
con su justificación, están en [`docs/control-de-cambios.md`](docs/control-de-cambios.md).

## [Sin publicar]

## [0.2.0] — 2026-11-19

### Añadido

- Implementación Java 21 con Maven: dominio (`Libro`, `Ejemplar`, `Socio`, `Bibliotecario`, `Prestamo`),
  servicios de acceso, catálogo y préstamos, y persistencia en PostgreSQL (JDBC) y en memoria.
- Funcionalidad central «Registrar préstamo de libro» con sus reglas: socio sin multa ni préstamos
  vencidos y ejemplar disponible. También «Registrar devolución» con cálculo de multa ($500 por día),
  «Consultar catálogo» e «Iniciar sesión» (contraseñas con hash PBKDF2).
- Protección contra préstamos concurrentes del mismo ejemplar (UPDATE condicional + índice único parcial).
- Pruebas JUnit 5: unitarias de dominio y servicio, e integración contra PostgreSQL (incluye 8 hilos
  prestando el mismo ejemplar). Cobertura mínima del 80 % verificada por JaCoCo.
- Demostración por consola (`demo`) con 12 escenarios: los caminos exitosos y siete de error.
- Interfaz Swing (adicional al alcance): inicio de sesión, catálogo, préstamo y devolución.
- Docker Compose (PostgreSQL 17, pruebas con Maven, imagen de la aplicación) y configuraciones de
  ejecución de IntelliJ en `.run/`.
- CI en GitHub Actions (compilar, validar diagramas, pruebas con PostgreSQL, imagen + demo), release por
  tag, Dependabot y plantillas de PR e issues. `scripts/verificar.ps1` reproduce el CI en local.
- Diagramas en PlantUML versionado (`docs/diagramas/fuentes/`) y sus PNG.
- Documentación: README, control de cambios, informe final, guía de defensa y reparto de tareas.

### Cambiado

- Los seis diagramas del avance se corrigieron para que describan el mismo sistema que el código
  (cambios C1 a C14 del control de cambios).

## [0.1.0] — 2026-11-19

### Añadido

- Línea base: avance entregado en clase (documento, seis diagramas y transcripción), en
  `docs/avance-de-hoy-2026-11-19/`.
