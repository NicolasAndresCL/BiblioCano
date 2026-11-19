# AVANCE DE HOY — línea base del 19 de noviembre de 2026

Esta carpeta conserva **sin modificaciones** el avance entregado en la Etapa 1 de la Evaluación
Integradora Final. Es la línea base que exige la pauta (sección 7, «Control de cambios»): todo lo
que cambió después se compara contra estos archivos y queda registrado en
[`docs/control-de-cambios.md`](../control-de-cambios.md).

**No se editan.** Si un diagrama cambia, el nuevo va en `docs/diagramas/` y la diferencia se
explica en el control de cambios. En git, esta línea base corresponde al tag `v0.1.0`.

| Archivo | Contenido |
|---|---|
| `Avance BiblioCano - 2026-11-19.pdf` | Exportación del documento compartido del equipo tal como estaba al cierre de la clase |
| `diagramas/1-casos-de-uso.png` | Diagrama de casos de uso (Matías Toro) |
| `diagramas/2-modelo-de-datos.png` | Modelo de datos (Matías Toro) |
| `diagramas/3-clases.png` | Diagrama de clases inicial (Matías Toro) |
| `diagramas/4-secuencia.png` | Diagrama de secuencia, «Préstamo de libros» |
| `diagramas/5-actividades.png` | Diagrama de actividades, «Préstamo de libros» |
| `diagramas/6-comunicacion.png` | Diagrama de comunicación, «Préstamo de libros» |

## Texto del avance (transcripción literal)

**Equipo.** Líder: Alex Díaz · Diagramas: Matías Toro · Programa: Nicolás Cano · Presentación:
Tais Montecinos.

**Nombre del sistema:** BiblioCano.

**1. Descripción del problema.** Actualmente, el control del inventario bibliográfico y el registro
de circulación de libros se realiza mediante planillas manuales o registros en papel. Esta forma de
trabajo provoca tres problemas operativos críticos:

- **Desconocimiento de disponibilidad en tiempo real:** no es posible saber con certeza qué
  ejemplares físicos de un libro están disponibles o prestados sin revisar manualmente los registros.
- **Falta de validación de socios:** se corre el riesgo de autorizar nuevos préstamos a usuarios que
  mantienen libros vencidos o multas impagas.
- **Trazabilidad deficiente:** resulta lento vincular un ejemplar específico con un socio y controlar
  las fechas límite de devolución, generando pérdidas de material y demoras en la atención.

**3. Objetivo del sistema.** *General:* desarrollar un sistema de software orientado a objetos que
automatice la gestión del catálogo de libros y el proceso de préstamos y devoluciones, asegurando el
control de disponibilidad de ejemplares y la validación del estado de los socios en tiempo real.
*Específicos:* centralizar el registro de libros, ejemplares físicos y socios habilitados; automatizar
las reglas de negocio previas a un préstamo (ejemplar disponible, socio sin bloqueos ni préstamos
vencidos); registrar la trazabilidad completa de cada préstamo (fecha de emisión, fecha límite,
ejemplar asignado y estado); facilitar la consulta rápida del catálogo según disponibilidad.

**4. Usuarios o actores.** *Bibliotecario (actor principal):* inicia sesión, gestiona el catálogo de
libros y ejemplares, consulta disponibilidad, registra préstamos y procesa devoluciones con cálculo
de multas. *Socio / Lector (principal/secundario):* consulta el catálogo y solicita el préstamo de
ejemplares.

**5. Funcionalidades principales.** (1) Autenticación de usuario. (2) Gestión de catálogo (Libro y
Ejemplar). (3) Consulta de catálogo por título, autor o ISBN con ejemplares disponibles.
(4) **Registro de préstamo de libros (funcionalidad central)**, previa validación automática de reglas
de negocio. (5) Registro de devolución y multas.

**6. Alcance de la implementación.** *Diseño:* arquitectura completa con diagramas UML coherentes
(casos de uso, clases, modelo de datos, secuencia, actividades y comunicación). *Implementación:* sin
interfaz gráfica ni aplicación completa; Java (consola/lógica de dominio) con las clases del núcleo
(Libro, Ejemplar, Socio, Bibliotecario, Prestamo) y la funcionalidad **Registrar Préstamo de Libro**,
con pruebas de flujo exitoso y de flujo alternativo/error (ejemplar no disponible o socio con multa).

**Pendientes declarados en el avance:** «faltaría el Trello y el programa en Java».

> Nota de transcripción: el documento original salta de la sección 1 a la 3 (no tiene sección 2) y
> escribe «Tais Montecino»; se transcribe tal cual. La grafía correcta es Montecinos.
