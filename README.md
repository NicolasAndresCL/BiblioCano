# BiblioCano — préstamos de biblioteca con reglas que no se pueden saltar

[![CI](https://github.com/NicolasAndresCL/BiblioCano/actions/workflows/ci.yml/badge.svg)](https://github.com/NicolasAndresCL/BiblioCano/actions/workflows/ci.yml)
![Versión](https://img.shields.io/badge/versi%C3%B3n-0.2.0-blue)
![Java](https://img.shields.io/badge/java-21-E76F00)
![PostgreSQL](https://img.shields.io/badge/postgresql-17-336791)

| | |
|---|---|
| **Versión** | 0.2.0 |
| **Fecha** | 19 de noviembre de 2026 |
| **Autor** | [Nicolás Andrés Cano Leal](#autor) y equipo (ver [Equipo](#equipo)) |

**BiblioCano** es la Evaluación Integradora Final de **Diseño de Software (IEI-050)**, Universidad Santo
Tomás. Reemplaza las planillas en papel de una biblioteca por un sistema que **sabe en tiempo real qué
ejemplar está disponible**, **no le presta a un socio con multas o libros vencidos** y **deja trazado
cada préstamo** (quién, qué copia, cuándo, hasta cuándo).

Lo evaluado es el **diseño** (seis diagramas UML coherentes entre sí) y una **implementación Java que le
corresponde campo por campo**. La funcionalidad central implementada es *Registrar préstamo de libro*,
con pruebas del camino exitoso y de cada camino de error.

- **Dominio y servicios**: Java 21, sin frameworks (lo que se ve en el diagrama de clases es lo que hay).
- **Persistencia**: PostgreSQL 17 vía JDBC; el esquema es el modelo de datos traducido a SQL.
- **Pruebas**: JUnit 5 (68 pruebas, incluidas 8 de integración contra PostgreSQL) y cobertura con JaCoCo.
- **Interfaz**: consola (demostración evaluada) y Swing (adicional).
- **Todo dockerizado**: no hace falta instalar Java, Maven ni PostgreSQL para compilar, probar o demostrar.

Los documentos de la entrega están en [`docs/`](docs/): [informe final](docs/informe/informe-final.md),
[diagramas](docs/diagramas/), [control de cambios](docs/control-de-cambios.md) respecto del
[avance del 19/11](docs/avance-de-hoy-2026-11-19/), [reparto de tareas](docs/equipo-y-tareas.md) y
[guía de defensa](docs/defensa/guia-defensa.md).

## Arquitectura

```text
bibliocano/
├── pom.xml                          # Java 21, JUnit 5, driver PostgreSQL, JaCoCo (umbral 80 %)
├── Dockerfile                       # multi-stage: Maven compila, JRE alpine ejecuta, usuario no-root
├── docker-compose.yml               # db (PostgreSQL 17, puerto 5433) + pruebas + demo
├── src/main/java/cl/bibliocano/
│   ├── dominio/                     # Libro, Ejemplar, Socio, Bibliotecario, Prestamo: las REGLAS viven aquí
│   ├── servicio/                    # ServicioPrestamos/Catalogo/Acceso + interfaces Repositorio*
│   ├── persistencia/jdbc/           # BibliotecaPostgres: SQL explícito, transacciones
│   ├── persistencia/memoria/        # BibliotecaEnMemoria + DatosDemo: pruebas y modo sin Docker
│   ├── consola/DemoConsola.java     # recorre los 12 escenarios de prueba
│   ├── ui/                          # Swing: login, catálogo, préstamo, devolución (sin lógica)
│   ├── Contexto.java                # raíz de composición: elige PostgreSQL o memoria
│   └── App.java                     # entrada: gui | demo | reiniciar-datos [--memoria]
├── src/main/resources/db/           # 01-esquema.sql y 02-datos-demo.sql (los carga PostgreSQL al crear el volumen)
├── src/test/java/                   # unitarias (dominio, servicio) + integración (PostgreSQL real)
├── docs/                            # informe, diagramas (.puml + .png), control de cambios, defensa
├── scripts/verificar.ps1            # reproduce el CI en local sobre un clon limpio
└── .run/                            # configuraciones de ejecución compartidas de IntelliJ
```

**Flujo:** `VentanaPrincipal / DemoConsola → ServicioPrestamos → Bibliotecario (reglas) → RepositorioPrestamos → PostgreSQL`.
La interfaz nunca decide nada; el servicio no conoce SQL (depende de interfaces); el dominio no conoce ni la
interfaz ni la base.

## Levantar con Docker

Requisito único: **Docker Desktop** abierto.

```powershell
docker compose up -d --wait db                 # PostgreSQL con los datos de ejemplo
docker compose run --rm pruebas                # compila y corre las 68 pruebas + cobertura
docker compose run --rm --build demo           # demostración por consola contra PostgreSQL
docker compose down                            # baja todo y CONSERVA los datos
```

| Servicio | Dónde | Para qué |
|---|---|---|
| `db` | `localhost:5433` · base, usuario y clave `bibliocano` | PostgreSQL 17. El 5433 evita chocar con un PostgreSQL local en 5432 |
| `pruebas` | — (perfil `herramientas`) | `mvn verify` dentro de un contenedor con Java 21 |
| `demo` | — (perfil `herramientas`) | La imagen de la app ejecutando `demo` |

- `docker compose down` conserva la base. **`docker compose down -v` la borra**: al volver a subir `db` se
  recargan los datos de ejemplo.
- La demo **reinicia los datos de ejemplo** antes de empezar, para mostrar siempre lo mismo.
- Para volver a los datos de ejemplo sin borrar el volumen: `docker compose run --rm demo reiniciar-datos`.

## Abrir en IntelliJ

1. **File → Open** y elegir la carpeta `bibliocano/` (IntelliJ reconoce el `pom.xml`).
2. Si pide un JDK: **Project Structure → SDK → Download JDK → Eclipse Temurin 21**. IntelliJ lo descarga y lo
   administra él; no se instala Java en el sistema.
3. Con `docker compose up -d --wait db` arriba, elegir en la barra de ejecución una de las configuraciones de
   `.run/`:

| Configuración | Qué hace |
|---|---|
| BiblioCano - Interfaz (PostgreSQL) | La ventana Swing con la base de Docker |
| BiblioCano - Interfaz (memoria, sin Docker) | La ventana con datos de ejemplo en memoria (respaldo si Docker falla) |
| BiblioCano - Demo consola (PostgreSQL) | Los 12 escenarios en la consola de IntelliJ |
| BiblioCano - Demo consola (memoria) | Lo mismo sin base de datos |
| BiblioCano - Todas las pruebas | JUnit, incluida la integración con PostgreSQL |

Credenciales de la demo: usuario **`bibliotecaria`**, contraseña **`biblio2026`**.

## La interfaz

- **Iniciar sesión**: mismo mensaje si el usuario no existe o si la clave es errónea.
- **Consultar catálogo**: búsqueda por título, autor o ISBN; muestra «disponibles de total» y, al elegir un
  libro, sus ejemplares con ubicación y estado.
- **Registrar préstamo**: ID del socio + código de barras → comprobante con la fecha límite, o el motivo del
  rechazo (aviso amarillo).
- **Registrar devolución**: código de barras → multa calculada si hubo atraso.
- Si la base no responde, muestra un error rojo distinto («no hay conexión») en vez de cerrarse.

## Datos de ejemplo

| Dato | Para qué escenario |
|---|---|
| Socio 1, Camila Fuentes | Habilitado: préstamo exitoso |
| Socio 2, Diego Morales | Con multa impaga: rechazo |
| Socio 3, Valentina Soto | Con `EJ-0002` vencido hace 6 días: rechazo y devolución con multa de $3.000 |
| `EJ-0001`, `EJ-0003`, `EJ-0005`, `EJ-0006` | Disponibles |
| `EJ-0002` | Prestado (a Valentina) |
| `EJ-0004` | En mantenimiento |
| *UML Distilled* | Título sin copias (multiplicidad `0..*`) |

Viven en `src/main/resources/db/02-datos-demo.sql` y, idénticos, en `DatosDemo.java` (modo memoria).

## Variables de entorno

| Variable | Usada por | Por defecto | Para qué |
|---|---|---|---|
| `BIBLIOCANO_DB_URL` | App y pruebas | `jdbc:postgresql://localhost:5433/bibliocano` | Conexión JDBC. **Si está definida, corren las pruebas de integración** |
| `BIBLIOCANO_DB_USUARIO` | App | `bibliocano` | Usuario de la base |
| `BIBLIOCANO_DB_PASSWORD` | App | `bibliocano` | Clave de la base (solo desarrollo) |
| `BIBLIOCANO_MODO` | App | `postgres` | `memoria` equivale a `--memoria` |
| `BIBLIOCANO_DB_PUERTO` | Compose | `5433` | Puerto publicado de PostgreSQL |

## Verificación

```powershell
.\scripts\verificar.ps1                 # sobre un clon limpio de HEAD (lo que ve el CI)
.\scripts\verificar.ps1 -ArbolDeTrabajo # sobre la carpeta actual, mientras se programa
```

Reproduce, en el mismo orden, los cuatro trabajos del CI (`.github/workflows/ci.yml`), con un proyecto de
Compose y un puerto aparte (15433) que baja siempre al terminar:

1. **Compilar** con `-Xlint:all -Werror`: cualquier advertencia del compilador es un error.
2. **Diagramas**: PlantUML renderiza los 7 `.puml`; un error de sintaxis falla el trabajo.
3. **Pruebas**: `mvn verify` contra PostgreSQL 17: 68 pruebas, umbral de cobertura de líneas del 80 % sobre
   dominio, servicio y persistencia, y comprobación de que la integración **no quedó omitida**.
4. **Imagen + demo**: construye el `Dockerfile` y corre los 12 escenarios; falla si alguno no termina como
   se espera.

## Documentos y diagramas

| Documento | Fuente (se edita) | Salida (no se edita) |
|---|---|---|
| Diagramas UML y modelo de datos | `docs/diagramas/fuentes/*.puml` | `docs/diagramas/png/*.png` |
| Informe final | `docs/informe/informe-final.md` | `docs/entregables/Informe Final - BiblioCano (IEI-050).pdf` |
| Guía de defensa | `docs/defensa/guia-defensa.md` | `docs/entregables/Guia de Defensa - BiblioCano (IEI-050).pdf` |
| Control de cambios | `docs/control-de-cambios.md` | — |
| Tareas y pendientes | `docs/equipo-y-tareas.md` | — |
| Evidencias | salida real de `demo` y de `pruebas` | `docs/evidencias/*.txt` |

Regenerar los diagramas (PlantUML en Docker, sin Java local):

```powershell
docker run --rm -v "${PWD}\docs\diagramas\fuentes:/fuentes:ro" -v "${PWD}\docs\diagramas\png:/salida" `
    plantuml/plantuml:1.2025.4 -failfast2 -tpng -o /salida "/fuentes/*.puml"
```

Regenerar los PDF (Python con `pymupdf` y `mistune`, ver `scripts/requirements-docs.txt`):

```powershell
python scripts\md-a-pdf.py docs\informe\informe-final.md "docs\entregables\Informe Final - BiblioCano (IEI-050).pdf" "BiblioCano · Informe de la Evaluación Integradora Final" "Diseño de Software · IEI-050 · Universidad Santo Tomás · BiblioCano v0.2.0"
python scripts\md-a-pdf.py docs\defensa\guia-defensa.md "docs\entregables\Guia de Defensa - BiblioCano (IEI-050).pdf" "BiblioCano · Guía de defensa" "Diseño de Software · IEI-050 · Evaluación Integradora Final"
```

Si se cambia una clase o una firma en Java, se cambia también su `.puml` y se regeneran los PNG y el informe:
la rúbrica evalúa la correspondencia entre diseño y código.

## Decisiones de diseño

- **Las reglas viven en el dominio, no en el servicio ni en la ventana.** `Bibliotecario.registrarPrestamo`
  valida al socio y al ejemplar y crea el préstamo (patrón Creador); `Socio.validarEstado` responde por el
  propio socio (Experto en información). El servicio solo coordina (Controlador).
- **El servicio depende de interfaces de repositorio** (inversión de dependencias). Por eso las mismas reglas
  se prueban en milisegundos en memoria y, sin cambiar una línea, contra PostgreSQL.
- **La base también protege la regla más crítica.** Si dos bibliotecarios prestan la misma copia a la vez,
  ambos pasan la validación en memoria; el `UPDATE ... WHERE estado = 'DISPONIBLE'` y el índice único parcial
  dejan pasar solo a uno. Hay una prueba con 8 hilos que lo demuestra.
- **«Vencido» se calcula, no se guarda.** Depende de la fecha de hoy; guardado, necesitaría un proceso diario
  y podría quedar desactualizado.
- **Pesos enteros, no `double`.** Los montos de dinero no admiten el error de redondeo del punto flotante.
- **La fecha entra como parámetro (`Clock`).** Las pruebas fijan «hoy» y verifican plazos y multas exactos.
- **Composición real.** Solo `Libro.agregarEjemplar` crea un `Ejemplar` (constructor de paquete) y en SQL el
  borrado es en cascada: el rombo negro del diagrama se cumple en el código y en la base.
- **SQL explícito con JDBC, sin ORM.** Cada consulta se puede leer y defender; el esquema es el modelo de datos.
- **Swing y no JavaFX**: viene con el JDK y es lo que usa el material del curso.

## Equipo

| Integrante | Rol |
|---|---|
| Alex Díaz | Líder de proyecto |
| Matías Toro | Diagramas UML y modelo de datos |
| Nicolás Cano | Programación Java, pruebas e infraestructura |
| Tais Montecinos | Presentación |

Detalle de tareas realizadas y pendientes en [`docs/equipo-y-tareas.md`](docs/equipo-y-tareas.md).

## Autor

**Nicolás Andrés Cano Leal**\
Backend Engineer → SRE | Python · Django · Kubernetes · Terraform · CI/CD

- LinkedIn: [linkedin.com/in/nicolasandrescl](https://www.linkedin.com/in/nicolasandrescl/)
- Portafolio: [nicolasandrescl.github.io](https://nicolasandrescl.github.io/)
- Correo: [nicolas.cano.leal@gmail.com](mailto:nicolas.cano.leal@gmail.com)

BiblioCano v0.2.0 · 19 de noviembre de 2026
