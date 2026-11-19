# Control de cambios respecto del avance (línea base del 19/11/2026)

La pauta (sección 7) pide que la entrega final identifique qué se modificó respecto del avance. La
línea base está congelada en [`docs/avance-de-hoy-2026-11-19/`](avance-de-hoy-2026-11-19/) y en el
tag `v0.1.0`; los diagramas vigentes están en [`docs/diagramas/`](diagramas/). Para ver cada diferencia
en git: `git diff v0.1.0 -- docs/diagramas`.

**Criterio general.** No se cambió el problema, el alcance ni el estilo de los diagramas de Matías. Se
corrigió lo que impedía que los seis diagramas y el código describieran **el mismo sistema** (criterio
«Coherencia del proyecto», 20 %) o lo que contradecía la notación UML (criterio «Diseño UML», 30 %).

| ID | Diagrama | Qué cambió | Por qué |
|---|---|---|---|
| C1 | Casos de uso | Se quitó la línea Socio → «Registrar préstamo». El socio sigue como actor de «Consultar catálogo». | Un actor es un rol que **interactúa con el sistema**. El socio pide el libro en el mesón; quien opera el sistema es el bibliotecario. Con la línea, la secuencia debía mostrar al socio enviando mensajes al sistema, y no lo hace. |
| C2 | Casos de uso | El «extend» de «Calcular multa» declara su punto de extensión (*entrega fuera de plazo*) y su condición. | Un «extend» es condicional: sin la condición, el diagrama no dice cuándo se ejecuta la extensión. |
| C3 | Clases | `Libro ◆ Ejemplar` pasa de `1..*` a `0..*`, y `agregarEjemplar(ejemplar)` pasa a `agregarEjemplar(codigoBarra, ubicacion) : Ejemplar`. | En una composición **el todo crea la parte**: si el ejemplar llegara construido desde fuera, existiría antes que su libro y la composición sería falsa. `0..*` porque un título puede registrarse antes de que lleguen sus copias o quedarse sin ellas. |
| C4 | Clases | `Socio.consultarCatalogo()` se trasladó a `ServicioCatalogo.buscar()`. `validarEstado()` pasa a `validarEstado(hoy)` y Socio conoce sus `prestamosActivos`. Se agregan `tienePrestamosVencidos(hoy)` y `registrarMulta()`. | Consultar el catálogo es una acción del **actor**, no una responsabilidad del objeto Socio. La regla del objetivo dice «sin multas **ni préstamos vencidos**», pero el diagrama solo tenía `tieneMulta`: la mitad de la regla no era implementable. |
| C5 | Clases | `password` pasa a `hashPassword`; `iniciarSesion(usuario, pass)` pasa a `autenticar(password)` y el inicio de sesión lo coordina `ServicioAcceso`. `registrarPrestamo` y `registrarDevolucion` reciben `hoy`; la devolución retorna la multa. | Una contraseña nunca se guarda en claro. Un método de instancia no puede buscar al bibliotecario por usuario: para llamarlo ya hace falta el objeto. La fecha se recibe para que las pruebas fijen «hoy» y verifiquen plazos y multas. |
| C6 | Clases | `multa : double` pasa a `int` (pesos). Se eliminó `EstadoPrestamo.VENCIDO` y se agregó `estaVencido(hoy)`. Constantes `DIAS_PLAZO = 14` y `MULTA_POR_DIA = 500`. | `double` no representa montos exactos (0,1 + 0,2 ≠ 0,3) y el peso chileno no usa decimales. «Vencido» depende de la fecha de hoy: guardado como estado, exigiría un proceso diario que lo actualice y, si falla, la base quedaría mintiendo. |
| C7 | Clases | Navegabilidad explícita (`Prestamo → Bibliotecario`, `Prestamo → Ejemplar`), jerarquía de excepciones de dominio y un diagrama complementario de capas (3b) con `ServicioPrestamos` y los repositorios. | Los diagramas de secuencia y comunicación usan esos objetos: si no están en un diagrama de clases, los diagramas dinámicos muestran mensajes a clases que no existen (el problema original, ver C12). |
| C8 | Modelo de datos | `LIBRO ‖—‖< EJEMPLAR` pasa a `‖—o<` (cero o muchos). | Coherencia con C3. |
| C9 | Modelo de datos | `password` pasa a `password_hash`. | Coherencia con C5. |
| C10 | Modelo de datos | `multa DECIMAL(10,2)` pasa a `INTEGER`; restricciones `CHECK` para los estados y para «activo ⇔ sin fecha real». | Coherencia con C6. Las restricciones impiden en la base estados que el dominio ya prohíbe. |
| C11 | Modelo de datos | Índice único parcial: un solo préstamo `ACTIVO` por `codigo_barra`. `ON DELETE CASCADE` de libro a ejemplar. | Si dos bibliotecarios prestan la misma copia al mismo tiempo, ambos pasan la validación en memoria; la base rechaza al segundo. El cascade es la composición en SQL. |
| C12 | Secuencia | Rehecho sobre las clases reales. Los participantes del avance («Sistema de Biblioteca», «Catálogo de Libros», «Registro de Préstamos») **no existían en el diagrama de clases**. Se agrega la validación del socio, las firmas reales, la numeración, las barras de activación y los `alt` con las tres salidas de error. | Es el cambio más importante. La pauta exige que todos los diagramas representen el mismo sistema, y el caso de uso incluye «Validar estado del socio» con «include» (obligatorio), pero la secuencia nunca lo validaba. |
| C13 | Actividades | Calles Bibliotecario/Sistema; decisiones de existencia, socio habilitado y ejemplar disponible; rechazo por préstamo concurrente. «Actualizar disponibilidad del libro» pasa a «Marcar ejemplar como PRESTADO». | La disponibilidad es del **ejemplar**, no del libro (el problema 1 del enunciado). Faltaba la validación del socio, igual que en C12. |
| C14 | Comunicación | Mismos objetos y numeración decimal (1, 1.1, 1.1.1…) que la secuencia. Se quitaron los mensajes 4, 6, 7 y 8 del avance. | En el avance las **respuestas** («devolverDisponibilidad», «confirmarRegistro»…) estaban numeradas como mensajes nuevos. En un diagrama de comunicación los retornos no se dibujan como mensajes: se escriben como asignación (`s := buscarPorId(idSocio)`). La numeración decimal muestra qué llamada está anidada en cuál. |
| C15 | Implementación | Proyecto Java 21 con Maven, pruebas JUnit 5, PostgreSQL en Docker, interfaz Swing e integración continua. | El avance declaraba el programa como pendiente. La interfaz es **adicional**: el alcance dice «sin GUI» y lo evaluado sigue siendo dominio + pruebas por consola. |
| C16 | Alcance | Además de «Registrar préstamo» (funcionalidad central) se implementó «Registrar devolución» con su multa, «Consultar catálogo» e «Iniciar sesión». «Gestionar libros y ejemplares» queda modelado y no implementado. | Se implementó lo necesario para demostrar el «extend» y para que la interfaz sea usable. La gestión del catálogo es un mantenedor sin reglas de negocio nuevas. |
| C17 | Documento | Apellido corregido: Tais **Montecinos**. Secciones renumeradas (el avance pasaba de la 1 a la 3). | Erratas. |

## Lo que se mantuvo del avance

- Problema, objetivos, actores, funcionalidades y alcance de diseño.
- Las cinco clases del núcleo, sus atributos principales y las asociaciones `Bibliotecario 1 — 0..* Prestamo`,
  `Socio 1 — 0..* Prestamo` y `Prestamo 0..* — 1 Ejemplar`.
- `Bibliotecario.registrarPrestamo(...) : Prestamo`: el bibliotecario sigue siendo el **creador** del préstamo
  (patrón Creador de GRASP). El código respeta esa decisión del diagrama original.
- Las cinco tablas del modelo de datos con sus claves y relaciones.
- Los `«include»` de «Registrar préstamo» y el `«extend»` de «Calcular multa», con sus flechas en el
  sentido correcto (estaban bien).

## Historial

| Versión | Fecha | Qué |
|---|---|---|
| 0.1.0 | 19/11/2026 | Línea base: avance entregado en clase. |
| 0.2.0 | 19/11/2026 | Diagramas corregidos (C1 a C14), implementación Java, pruebas y documentación. En revisión del equipo. |
| 1.0.0 | (antes de la defensa) | Versión que se presenta, tras la revisión del equipo. |
