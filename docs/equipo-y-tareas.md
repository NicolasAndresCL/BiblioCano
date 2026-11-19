# Equipo, tareas realizadas y pendientes para la defensa

La pauta (sección 6) pide identificar las tareas de cada integrante y la lista de pendientes para la
defensa. Este documento se actualiza a medida que se cierran.

## Integrantes y roles

| Integrante | Rol | Responsabilidad principal |
|---|---|---|
| **Alex Díaz** | Líder de proyecto | Coordina, distribuye tareas, supervisa entregas y realiza la entrega en Moodle |
| **Matías Toro** | Diagramas | Casos de uso, clases, modelo de datos y su defensa |
| **Nicolás Cano** | Programa | Implementación Java, pruebas, base de datos, repositorio y CI |
| **Tais Montecinos** | Presentación | Diapositivas, hilo de la exposición y ensayo cronometrado |

## Tareas realizadas

| Fecha | Integrante | Tarea | Evidencia |
|---|---|---|---|
| 19/11 | Equipo | Problema, objetivos, actores, funcionalidades y alcance | Avance, secciones 1 a 6 |
| 19/11 | Matías Toro | Casos de uso, modelo de datos y diagrama de clases inicial | `docs/avance-de-hoy-2026-11-19/diagramas/` 1 a 3 |
| 19/11 | Equipo | Primeras versiones de secuencia, actividades y comunicación | `docs/avance-de-hoy-2026-11-19/diagramas/` 4 a 6 |
| 19/11 | Alex Díaz | Coordinación y entrega del avance | Moodle |
| 19/11 | Nicolás Cano | Repositorio, proyecto Maven, dominio, servicios, persistencia PostgreSQL y en memoria | `src/main/java/` |
| 19/11 | Nicolás Cano | 68 pruebas JUnit (unitarias + integración con PostgreSQL), demo por consola | `src/test/java/`, `docs/evidencias/` |
| 19/11 | Nicolás Cano | Interfaz Swing, Docker Compose, CI y configuraciones de IntelliJ | `src/main/java/cl/bibliocano/ui/`, `docker-compose.yml`, `.github/` |
| 19/11 | Nicolás Cano | Diagramas en PlantUML alineados con el código y control de cambios | `docs/diagramas/`, `docs/control-de-cambios.md` |

## Pendientes para la defensa

| # | Pendiente | Responsable | Prioridad |
|---|---|---|---|
| 1 | Revisar los diagramas corregidos y el control de cambios (C1 a C14): aceptarlos o discutirlos. Quien defienda un diagrama tiene que poder explicar cada cambio. | Matías (todos leen) | Alta |
| 2 | Crear el repositorio en GitHub, subir el proyecto y dar acceso de lectura al docente. | Nicolás | Alta |
| 3 | Pegar el enlace del repositorio en los comentarios de la entrega de Moodle y comprobar que abre. | Alex | Alta |
| 4 | Diapositivas siguiendo los cinco bloques de la guía de defensa (los PNG de `docs/diagramas/png/` ya están listos para insertar). | Tais | Alta |
| 5 | Estudiar la guía de defensa completa: el docente puede preguntar a cualquiera sobre cualquier parte. | Todos | Alta |
| 6 | Ensayo cronometrado de 20 minutos con los cambios de expositor. | Todos (coordina Tais) | Alta |
| 7 | Preparar al menos una pregunta técnica válida para otro equipo (sin ella se pierden 2 puntos del desafío). | Alex (todos aportan) | Alta |
| 8 | Tablero de tareas (Trello, como dice el avance, o GitHub Projects en el mismo repositorio). | Alex | Media |
| 9 | Capturas de la interfaz Swing para el informe y las diapositivas. | Nicolás | Media |
| 10 | Probar la demo en el equipo con el que se expondrá; plan B: modo memoria (sin Docker). | Nicolás | Media |
| 11 | Tras la revisión del equipo, cortar la versión 1.0.0 (CHANGELOG, tag y release). | Nicolás | Media |

## Reparto de la exposición (20 min + 5 de preguntas)

| Bloque de la pauta | Tiempo | Expone | Apoyo |
|---|---|---|---|
| Problema, objetivo y alcance | 3 min | Alex | — |
| Casos de uso, clases y modelo de datos | 5 min | Matías | — |
| Secuencia, actividades y comunicación | 4 min | Tais | Matías |
| Implementación en Java | 4 min | Nicolás | — |
| Pruebas, decisiones y conclusiones | 4 min | Nicolás (pruebas en vivo, 2 min) y Alex (decisiones y cierre, 2 min) | — |
| Preguntas y defensa técnica | 5 min | Todos | — |

El guion detallado y el banco de preguntas están en [`defensa/guia-defensa.md`](defensa/guia-defensa.md).
