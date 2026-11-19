# BiblioCano — Informe de la Evaluación Integradora Final

**Asignatura:** Diseño de Software (IEI-050), Universidad Santo Tomás
**Docente:** Giovanni Cáceres R.
**Equipo:** Alex Díaz (líder), Matías Toro, Nicolás Cano y Tais Montecinos
**Versión:** 0.2.0, 19 de noviembre de 2026 (línea base: avance del 19/11, versión 0.1.0)
**Repositorio:** carpeta compartida con diagramas, código, pruebas y evidencias (enlace en Moodle)

---

## 1. Integrantes y roles

| Integrante | Rol | Responsabilidad |
|---|---|---|
| Alex Díaz | Líder de proyecto | Coordinación, distribución de tareas y entregas |
| Matías Toro | Diagramas | Casos de uso, clases y modelo de datos |
| Nicolás Cano | Programa | Implementación Java, pruebas y base de datos |
| Tais Montecinos | Presentación | Diapositivas y conducción de la exposición |

## 2. Descripción del problema

El control del inventario bibliográfico y de la circulación de libros se lleva en planillas manuales o en
papel. Esto produce tres problemas operativos:

- **Disponibilidad desconocida en tiempo real**: no se sabe qué ejemplares físicos están disponibles o
  prestados sin revisar los registros a mano.
- **Falta de validación de socios**: se puede autorizar un préstamo a quien tiene libros vencidos o multas
  impagas.
- **Trazabilidad deficiente**: vincular un ejemplar con un socio y controlar las fechas límite es lento,
  lo que causa pérdidas de material y demoras en la atención.

## 3. Objetivo del sistema

**General.** Desarrollar un sistema orientado a objetos que automatice la gestión del catálogo y el proceso
de préstamos y devoluciones, asegurando el control de disponibilidad de ejemplares y la validación del
estado de los socios en tiempo real.

**Específicos.** (1) Centralizar el registro de libros, ejemplares y socios. (2) Automatizar las reglas
previas a un préstamo: ejemplar disponible y socio sin multas ni préstamos vencidos. (3) Registrar la
trazabilidad de cada préstamo: fecha de emisión, fecha límite, ejemplar asignado y estado. (4) Facilitar
la consulta del catálogo según disponibilidad.

## 4. Actores

- **Bibliotecario** (actor principal): opera el sistema; inicia sesión, gestiona el catálogo, registra
  préstamos y devoluciones.
- **Socio / Lector**: consulta el catálogo. Solicita el préstamo **en el mesón**, al bibliotecario; por eso
  no es actor de «Registrar préstamo» (ver control de cambios, C1).

## 5. Funcionalidades principales

| Funcionalidad | Caso de uso | Estado |
|---|---|---|
| Autenticación del bibliotecario | Iniciar sesión | Implementada |
| Registro de libros y ejemplares | Gestionar libros y ejemplares | Modelada |
| Búsqueda por título, autor o ISBN con disponibilidad | Consultar catálogo | Implementada |
| **Registro de préstamo con validación automática** | **Registrar préstamo** (incluye validar socio y verificar disponibilidad) | **Implementada (funcionalidad central)** |
| Devolución con cálculo de multa por atraso | Registrar devolución, extendido por Calcular multa | Implementada |

## 6. Alcance

- **Diseño**: el sistema completo, en seis diagramas UML coherentes entre sí.
- **Implementación**: Java 21 con las cinco clases del núcleo y la funcionalidad central *Registrar
  préstamo de libro*, más devolución con multa, consulta de catálogo e inicio de sesión. Persistencia en
  PostgreSQL. Demostración por consola, que es la que se evalúa; la interfaz Swing es adicional.
- **Fuera de alcance**: el mantenedor de libros y ejemplares, el pago de multas y las reservas.

## 7. Diagramas

Todos los diagramas describen el mismo sistema y usan los mismos nombres que el código. Las fuentes están
en PlantUML (`docs/diagramas/fuentes/`), así que cada diagrama se regenera y queda versionado junto al código.

### 7.1 Casos de uso

Dos actores y siete casos de uso. «Registrar préstamo» **incluye** (siempre ejecuta) «Validar estado del
socio» y «Verificar disponibilidad del ejemplar»; «Calcular multa» **extiende** «Registrar devolución» solo
cuando la entrega es posterior a la fecha esperada. Las flechas apuntan en sentidos opuestos: del base al
incluido, y de la extensión al base.

<!-- figura: ../diagramas/png/1-casos-de-uso.png | Figura 1. Diagrama de casos de uso -->

### 7.2 Diagrama de clases

Cinco clases de dominio, dos enumeraciones y la jerarquía de excepciones de negocio. Las decisiones que el
diagrama expresa y el código cumple:

- **Composición `Libro 1 ◆── 0..* Ejemplar`**: solo `Libro.agregarEjemplar` crea un ejemplar (su constructor
  es de paquete) y en la base el borrado es en cascada. `0..*` porque un título puede no tener copias.
- **`Bibliotecario` crea el `Prestamo`** (patrón Creador) después de validar; `Socio.validarEstado`
  responde por el propio socio (Experto en información).
- **Navegabilidad**: `Prestamo` conoce a su ejemplar y a su bibliotecario, pero no al revés; `Socio` y
  `Prestamo` se conocen mutuamente porque la regla de vencidos se pregunta al socio.
- **Herencia**: las cuatro excepciones de negocio especializan `BibliotecaException` (abstracta), lo que
  permite a la interfaz tratarlas en un solo lugar.

El diagrama complementario muestra cómo se conecta el dominio con la interfaz y la base: `ServicioPrestamos`
depende de **interfaces** de repositorio, implementadas en memoria (pruebas) y en PostgreSQL (inversión de
dependencias). Los objetos de los diagramas de secuencia y comunicación están todos aquí.

<!-- figura: ../diagramas/png/3-clases.png | Figura 2. Diagrama de clases del dominio -->

<!-- figura: ../diagramas/png/3b-capas.png | Figura 3. Servicios, repositorios y presentación -->

### 7.3 Modelo de datos

Cinco tablas que traducen las clases persistentes. El script `src/main/resources/db/01-esquema.sql` es este
diagrama escrito en SQL. Además de claves primarias y foráneas, la base protege las reglas: `CHECK` de los
estados, «activo ⇔ sin fecha de devolución real» y un **índice único parcial** que impide dos préstamos
activos del mismo ejemplar, incluso si dos bibliotecarios lo intentan al mismo tiempo.

<!-- figura: ../diagramas/png/2-modelo-de-datos.png | Figura 4. Modelo de datos (PostgreSQL) -->

### 7.4 Diagrama de secuencia

Funcionalidad central, con los objetos reales. La ventana delega en `ServicioPrestamos`, que busca al socio
y al ejemplar (1.1.1 y 1.1.2) y le pide al bibliotecario registrar el préstamo (1.1.3). El bibliotecario
valida al socio (1.1.3.1) y al ejemplar (1.1.3.2), crea el préstamo (1.1.3.3) y marca el ejemplar como
prestado (1.1.3.4); el servicio lo guarda (1.1.4). Los fragmentos `alt` muestran las tres salidas de error.

<!-- figura: ../diagramas/png/4-secuencia.png | Figura 5. Diagrama de secuencia: Registrar préstamo -->

### 7.5 Diagrama de actividades

El mismo proceso visto como flujo de trabajo, con calles para el bibliotecario y el sistema. Las decisiones
siguen el orden del código (existencia, socio, ejemplar) y se agrega el rechazo por préstamo concurrente.

<!-- figura: ../diagramas/png/5-actividades.png | Figura 6. Diagrama de actividades: Registrar préstamo -->

### 7.6 Diagrama de comunicación

Los mismos objetos y la misma numeración que la secuencia, para el escenario exitoso. La numeración decimal
indica el anidamiento: 1.1.3.1 ocurre dentro de 1.1.3. Los valores devueltos se escriben como asignación
(`s := buscarPorId(idSocio)`), no como mensajes nuevos.

<!-- figura: ../diagramas/png/6-comunicacion.png | Figura 7. Diagrama de comunicación: Registrar préstamo -->

### 7.7 Coherencia entre diagramas y código

| Regla o paso | Caso de uso | Clase y método | Tabla o restricción | Prueba |
|---|---|---|---|---|
| Socio sin multa ni vencidos | Validar estado del socio | `Socio.validarEstado(hoy)` | `socio.tiene_multa`, préstamos activos | `rechazaSocioConMulta`, `rechazaSocioConVencido` |
| Ejemplar disponible | Verificar disponibilidad | `Ejemplar.estaDisponible()` | `ejemplar.estado` (CHECK) | `rechazaEjemplarNoDisponible` |
| Crear el préstamo con plazo de 14 días | Registrar préstamo | `Bibliotecario.registrarPrestamo` | `prestamo` | `prestaEjemplarDisponible` |
| Un solo préstamo activo por copia | Registrar préstamo | `RepositorioPrestamos.registrar` | índice único parcial | `dobleRegistroDelMismoEjemplar`, `ochoHilosALaVez` |
| Multa de $500 por día | Calcular multa («extend») | `Prestamo.calcularMulta(fecha)` | `prestamo.multa` (INTEGER) | `calculaMulta` (4 casos) |
| Composición | Gestionar libros y ejemplares | `Libro.agregarEjemplar` | `ON DELETE CASCADE` | `libroCreaEjemplares` |

## 8. Implementación Java

Proyecto Maven con Java 21, abierto en IntelliJ. No usa frameworks: lo que muestra el diagrama de clases es lo
que existe en el código.

| Paquete | Contenido |
|---|---|
| `cl.bibliocano.dominio` | `Libro`, `Ejemplar`, `Socio`, `Bibliotecario`, `Prestamo`, enumeraciones y excepciones |
| `cl.bibliocano.servicio` | `ServicioPrestamos`, `ServicioCatalogo`, `ServicioAcceso` e interfaces de repositorio |
| `cl.bibliocano.persistencia` | `BibliotecaPostgres` (JDBC, transacciones) y `BibliotecaEnMemoria` |
| `cl.bibliocano.consola` / `ui` | Demostración por consola y ventanas Swing, sin reglas de negocio |

El núcleo de la funcionalidad central, tal como aparece en la secuencia:

```java
public Prestamo registrarPrestamo(Socio socio, Ejemplar ejemplar, LocalDate hoy) {
    if (!socio.validarEstado(hoy)) {                       // 1.1.3.1
        throw new SocioInhabilitadoException(...);
    }
    if (!ejemplar.estaDisponible()) {                      // 1.1.3.2
        throw new EjemplarNoDisponibleException(...);
    }
    Prestamo prestamo = new Prestamo(socio, ejemplar, this, hoy);   // 1.1.3.3
    ejemplar.cambiarEstado(EstadoEjemplar.PRESTADO);                // 1.1.3.4
    return prestamo;
}
```

Si una regla falla, la excepción sale **antes** de modificar cualquier objeto: un préstamo rechazado no deja
rastro. Las pruebas lo comprueban explícitamente.

## 9. Pruebas

**Pruebas automatizadas (JUnit 5).** 68 pruebas, todas en verde: 60 unitarias sobre dominio y servicios (en
memoria, con la fecha fijada) y 8 de integración contra PostgreSQL 17 real. La cobertura de líneas de dominio,
servicio y persistencia es de **91,6 %** (umbral exigido por el build: 80 %).

**Demostración por consola.** Recorre doce escenarios contra PostgreSQL; cada uno declara qué espera y qué
obtuvo:

| # | Escenario | Esperado | Resultado |
|---|---|---|---|
| 1 | Iniciar sesión con contraseña incorrecta | Rechazo | OK |
| 2 | Iniciar sesión con credenciales válidas | Éxito | OK |
| 3 | Consultar catálogo «clean» | 1 de 2 ejemplares disponibles | OK |
| 4 | **Préstamo a socio habilitado de ejemplar disponible** | **Éxito, devolver en 14 días** | **OK** |
| 5 | Ejemplar ya prestado | Rechazo (no disponible) | OK |
| 6 | Ejemplar en mantenimiento | Rechazo (no disponible) | OK |
| 7 | Socio con multa impaga | Rechazo (socio inhabilitado) | OK |
| 8 | Socio con préstamo vencido | Rechazo (socio inhabilitado) | OK |
| 9 | Socio inexistente | Rechazo (no encontrado) | OK |
| 10 | Devolución con 6 días de atraso | Multa de $3.000 y socio bloqueado | OK |
| 11 | Devolución dentro del plazo | Sin multa, ejemplar disponible | OK |
| 12 | Devolver un ejemplar sin préstamo activo | Rechazo (no encontrado) | OK |

La salida completa está en `docs/evidencias/demo-consola.txt`, y el resumen de JUnit y cobertura en
`docs/evidencias/pruebas-junit.txt`.

**Prueba de concurrencia.** Ocho hilos piden el mismo ejemplar al mismo tiempo; la base acepta exactamente
uno. Demuestra que la regla «un ejemplar, un préstamo activo» no depende de que los bibliotecarios no
coincidan.

## 10. Decisiones de diseño

- **Las reglas viven en el dominio.** El servicio coordina (patrón Controlador) y la interfaz solo muestra:
  la misma regla se cumple igual por consola, por la ventana Swing y en las pruebas.
- **Inversión de dependencias.** El servicio depende de interfaces de repositorio; por eso las reglas se
  prueban en memoria en milisegundos y, sin cambiar código, contra PostgreSQL.
- **«Vencido» se calcula.** Depende de la fecha; guardarlo exigiría un proceso diario que puede fallar.
- **Dinero en pesos enteros.** `double` introduce errores de redondeo y el peso chileno no usa decimales.
- **La fecha entra como parámetro.** Permite pruebas exactas de plazos y multas.
- **La base también protege la regla crítica.** Validar en memoria no basta si dos operaciones coinciden.
- **Modelo de ciclo de vida.** El trabajo siguió un modelo **iterativo-incremental**: el avance (0.1.0) fue el
  primer incremento y esta versión el segundo, con los cambios trazados en el control de cambios.

## 11. Control de cambios

Respecto del avance del 19/11 se registraron 17 cambios (C1 a C17), todos justificados en
`docs/control-de-cambios.md`. Los de mayor peso: la secuencia y la comunicación usaban objetos que no existían
en el diagrama de clases y no validaban al socio, pese a que el caso de uso lo incluye (C12 a C14); el socio no
podía validar préstamos vencidos porque el diagrama de clases no conocía sus préstamos (C4); la multa en
`double` y el estado `VENCIDO` almacenado (C6). Se mantuvieron el problema, el alcance, las cinco clases del
núcleo y la decisión de que el bibliotecario crea el préstamo.

## 12. Tareas realizadas y pendientes

El detalle por integrante está en `docs/equipo-y-tareas.md`. Pendientes para la defensa: revisión de los
diagramas corregidos por todo el equipo, repositorio compartido con el docente, diapositivas, ensayo
cronometrado y preguntas preparadas para los demás equipos.

## 13. Conclusiones

BiblioCano resuelve los tres problemas planteados: la disponibilidad es un estado del ejemplar consultable en
línea; la validación del socio es automática y no se puede omitir, porque el préstamo solo se crea después de
ella; y cada préstamo guarda quién, qué copia, cuándo y hasta cuándo. El aprendizaje central del proyecto fue
la coherencia: un diagrama de secuencia que envía mensajes a clases inexistentes no describe el sistema, y solo
se detectó al intentar implementarlo. Llevar el diseño a código y a pruebas fue lo que validó el diseño.
