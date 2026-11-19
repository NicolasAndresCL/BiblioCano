# Guía de defensa · BiblioCano

**Asignatura:** Diseño de Software (IEI-050) · Evaluación Integradora Final, etapa 2
**Equipo:** Alex Díaz, Matías Toro, Nicolás Cano y Tais Montecinos
**Preparado:** 19 de noviembre de 2026, para la defensa de la clase siguiente
**Complementa:** el informe (`docs/informe/informe-final.md`), los diagramas y el control de cambios

---

## 0. Cómo usar este documento

La pauta dice que **el docente puede preguntarle a cualquier integrante sobre cualquier parte**, y la
defensa individual vale 10 % (más 10 % del desafío entre equipos). Por eso esta guía es para los cuatro,
no solo para quien expone cada bloque.

1. **Secciones 1 y 2**: el guion de 20 minutos y el banco de preguntas. Leerlos completos una vez.
2. **Secciones 3 a 7**: los ganchos de memoria. Se estudian con esta regla: leer el gancho, cerrar los ojos,
   **ver** la escena y responder el «Pruébate» **sin mirar**. Un gancho que no se practica se olvida igual
   que la definición.
3. **Sección 8**: la tabla de autoevaluación, para tapar la columna derecha y responder.

| Técnica | Cómo funciona | Para qué se usa aquí |
|---|---|---|
| Acrónimo o frase | Las iniciales forman una frase | Listas y órdenes (los seis diagramas, los pasos del préstamo) |
| Frase-ancla | Una regla corta y rítmica | Decisiones de diseño que hay que justificar |
| Imagen absurda | Una escena exagerada | Conceptos abstractos (inversión de dependencias, concurrencia) |
| Contraste en espejo | Dos conceptos ligados por letras compartidas | Pares que se confunden (include y extend, rombos) |
| Palacio de la memoria | Cada concepto en un lugar de un recorrido | Todo el proyecto en orden |

---

## 1. Guion de la exposición (20 minutos)

| Bloque | Min | Expone | Qué mostrar | Frase de cierre del bloque |
|---|---|---|---|---|
| Problema, objetivo y alcance | 3 | Alex | Los tres problemas, el objetivo general, actores, alcance (qué se modeló y qué se implementó) | «Todo lo que sigue responde a esos tres problemas.» |
| Casos de uso, clases y modelo de datos | 5 | Matías | Fig. 1 (include/extend), Fig. 2 (composición, Creador), Fig. 4 (índice único) | «El SQL del proyecto es este diagrama escrito en otro idioma.» |
| Secuencia, actividades y comunicación | 4 | Tais | Fig. 5, 6 y 7: el mismo préstamo en tres vistas, con la misma numeración | «Tres vistas, los mismos objetos, los mismos números.» |
| Implementación Java | 4 | Nicolás | IntelliJ: paquetes, `Bibliotecario.registrarPrestamo` comparado con la secuencia, `ServicioPrestamos` | «Cada mensaje del diagrama es una línea de este método.» |
| Pruebas, decisiones y conclusiones | 4 | Nicolás (2) y Alex (2) | Demo por consola en vivo (12 escenarios), las pruebas en verde; decisiones y control de cambios | «El diseño se validó cuando se convirtió en código que pasa pruebas.» |

**Plan B si falla Docker o la red**: en IntelliJ, la configuración «BiblioCano - Demo consola (memoria)» corre
los mismos 12 escenarios sin base de datos. Y la salida ya está guardada en `docs/evidencias/demo-consola.txt`.

**Al cambiar de expositor**: quien termina anuncia al siguiente y su tema («Matías les muestra cómo esto se
modeló»). Ahorra segundos y da sensación de equipo.

---

## 2. Banco de preguntas probables, con respuesta

Respuesta en tres tiempos: **qué** (el término exacto), **por qué** (la propiedad del problema) y **dónde**
se ve (diagrama, clase o prueba). Si no se sabe algo, se dice qué parte sí se sabe y dónde está la respuesta.

### 2.1 Casos de uso

**¿Por qué «Validar estado del socio» es include y no extend?** Porque se ejecuta **siempre** al registrar un
préstamo: no hay préstamo sin validación. Extend es para lo **condicional**, como la multa, que solo ocurre si
la entrega es tardía.

**¿Por qué el socio no está conectado a «Registrar préstamo»?** Un actor es un rol que interactúa
**directamente** con el sistema. El socio pide el libro en el mesón; quien opera el sistema es el
bibliotecario. Sí interactúa en «Consultar catálogo».

**¿«Iniciar sesión» es realmente un caso de uso?** Es discutible: no entrega valor por sí solo, es una
precondición. Lo mantuvimos porque es una funcionalidad del enunciado y está implementada; en el diseño no
se incluye en los demás casos para no repetirlo en todos.

### 2.2 Diagrama de clases

**¿Por qué Libro–Ejemplar es composición?** Porque una copia física no tiene sentido sin su título: si se
elimina el libro, sus ejemplares desaparecen. En el código solo `Libro.agregarEjemplar` puede crear un
`Ejemplar` (constructor de paquete) y en SQL hay `ON DELETE CASCADE`.

**¿Por qué 0..* y no 1..*?** Un título puede registrarse antes de que lleguen sus copias, o quedarse sin ellas
si se dan de baja. En los datos de ejemplo, *UML Distilled* no tiene copias.

**¿Por qué Socio–Prestamo no es composición?** El préstamo involucra a tres objetos (socio, ejemplar,
bibliotecario) y su historial debe sobrevivir. No «muere» con ninguno.

**¿Quién crea el préstamo y por qué?** `Bibliotecario`, por el patrón **Creador** de GRASP: es quien registra
el préstamo y tiene los datos para iniciarlo. Así no puede existir un préstamo que no haya pasado la
validación.

**¿Por qué la validación está en Socio y no en el servicio?** Patrón **Experto en información**: quien tiene
los datos (sus multas y sus préstamos) responde. Si estuviera en el servicio, Socio sería una clase anémica.

**¿Para qué sirve ServicioPrestamos si las reglas están en el dominio?** Es el **Controlador**: recibe la
petición, busca los objetos, delega las reglas y guarda. No decide nada.

**¿Por qué excepciones y no un boolean?** Un `false` no dice **por qué** se rechazó. La excepción lleva el
motivo y obliga a quien llama a tratarlo. Son no comprobadas (`RuntimeException`) porque son reglas de
negocio, no fallas que se puedan reintentar.

### 2.3 Modelo de datos

**¿Por qué el estado «vencido» no está en la tabla?** Depende de la fecha de hoy: guardado, necesitaría un
proceso diario que lo actualice y, si falla, la base quedaría mintiendo. Se calcula con
`Prestamo.estaVencido(hoy)`.

**¿Por qué la multa es INTEGER?** El peso chileno no usa decimales y `double` acumula errores de redondeo
(0,1 + 0,2 ≠ 0,3).

**¿Qué hace el índice único parcial?** Impide que un ejemplar tenga dos préstamos con estado `ACTIVO`. Es la
última defensa si dos bibliotecarios prestan la misma copia en el mismo instante.

### 2.4 Secuencia, actividades y comunicación

**¿Qué diferencia hay entre el diagrama de secuencia y el de comunicación?** Muestran la misma interacción:
la secuencia destaca el **orden en el tiempo** (eje vertical); la comunicación destaca **quién se conecta con
quién** (los enlaces) y el orden va en la numeración.

**¿Por qué los números tienen tantos puntos (1.1.3.1)?** La numeración decimal indica anidamiento: 1.1.3.1 es
un mensaje que ocurre **dentro** de la ejecución de 1.1.3.

**¿Por qué no numeraron las respuestas en el de comunicación?** Las respuestas no son mensajes nuevos: se
escriben como asignación (`s := buscarPorId(idSocio)`). En el avance estaban numeradas, y lo corregimos (C14).

**¿Qué es el fragmento alt?** Una alternativa: se ejecuta solo uno de sus operandos, según la guarda entre
corchetes. Ahí están las salidas de error.

**¿Qué son las calles del diagrama de actividades?** Particiones que muestran **quién** realiza cada acción:
bibliotecario o sistema.

### 2.5 Código y pruebas

**Muestre dónde está en el código el mensaje 1.1.3.2.** `Bibliotecario.registrarPrestamo`, la llamada a
`ejemplar.estaDisponible()`. Los comentarios del servicio llevan los mismos números.

**¿Cómo probaron un préstamo vencido sin esperar 14 días?** La fecha entra como parámetro: el servicio recibe
un `Clock`. En las pruebas el reloj está fijo, y los datos de ejemplo ponen un préstamo de hace 20 días.

**¿Qué pasa si se rechaza un préstamo? ¿Queda algo a medias?** No. Las validaciones lanzan la excepción
**antes** de cambiar cualquier objeto, y las pruebas verifican que el ejemplar siga disponible. En la base,
las escrituras van en una transacción: todo o nada.

**¿Cuántas pruebas hay y qué cubren?** 68: 60 unitarias (dominio y servicios, en memoria) y 8 de integración
contra PostgreSQL, incluida una con 8 hilos que piden el mismo ejemplar a la vez. Cobertura de líneas: 91,6 %.

**¿Por qué Postgres y Docker si la pauta no lo pide?** La pauta no lo exige, pero la regla de un ejemplar por
préstamo activo solo se puede proteger de verdad en una base con transacciones. Docker evita instalar algo en
cada computador: se levanta con un comando.

**¿La interfaz tiene lógica?** No. La ventana llama al servicio y muestra lo que devuelve; la prueba es que
la demo por consola cumple las mismas reglas sin ventana.

### 2.6 Proceso

**¿Qué modelo de ciclo de vida siguieron?** **Iterativo-incremental**: el avance fue el primer incremento
(versión 0.1.0) y esta entrega el segundo (0.2.0). El control de cambios registra lo que cambió entre ambos.
Cuidado: Scrum es un **marco de gestión**, no un modelo de ciclo de vida.

---

## 3. Preguntas para los otros equipos (desafío)

Una pregunta válida es técnica, tiene respuesta razonable y se relaciona con su UML, código, pruebas o
coherencia. Sin al menos una pregunta válida, el equipo pierde 2 puntos del desafío. Escoger la que calce con
lo que el otro equipo muestre:

1. «Su relación entre *X* e *Y* es composición: si se elimina *X*, ¿qué pasa con *Y* en su código?»
2. «¿Qué clase crea el objeto *Z* y por qué esa y no otra?»
3. «Su caso de uso tiene un include: ¿dónde aparece esa validación en su diagrama de secuencia?»
4. «En su diagrama de comunicación, ¿qué indica la numeración 1.2 frente a 2?»
5. «¿En qué capa está la regla de negocio *R*: en la interfaz, en el controlador o en la entidad?»
6. «¿Qué prueba demuestra su escenario de error y qué resultado esperan?»
7. «¿Qué ocurre si dos usuarios reservan el mismo recurso al mismo tiempo?»
8. «¿Esa multiplicidad 1..* permite crear el objeto antes de tener su primera parte?»

**Cómo preguntar**: corto, una sola pregunta, nombrando el diagrama o la clase. Nada que parezca trampa: el
docente descarta las preguntas hechas solo para perjudicar.

---

## 4. El palacio de la memoria: la biblioteca

Recorran la biblioteca de BiblioCano siempre en el mismo orden. Cada lugar guarda un bloque de la defensa.

| N.º | Lugar | Escena (imagínenla con detalle) | Concepto que guarda |
|---|---|---|---|
| 1 | Puerta | Tres carteles rotos: un letrero «¿HAY O NO HAY?», un socio con grillete de deudor y un libro con GPS apagado | Los tres problemas: disponibilidad, validación del socio, trazabilidad |
| 2 | Mostrador | La bibliotecaria es la única que toca el computador; el socio grita desde el otro lado del mesón | Actor = rol que interactúa directo con el sistema (C1) |
| 3 | Puerta del mostrador | Un torniquete que **siempre** gira (include) y una alarma que suena **solo** si el libro llega tarde (extend) | Include obligatorio, extend condicional |
| 4 | Estantería | Un libro madre amamanta a sus copias; si se quema el libro, las copias se vuelven ceniza | Composición Libro ◆ Ejemplar, 0..* |
| 5 | Oficina | La bibliotecaria con un sello gigante «PRESTADO» es la única que puede fabricar comprobantes | Patrón Creador: Bibliotecario crea Prestamo |
| 6 | Archivo | Un fichero de metal con cinco cajones y un candado con forma de índice | Modelo de datos: cinco tablas e índice único parcial |
| 7 | Reloj de pared | El reloj no tiene agujas: cada uno trae su propia hora en el bolsillo | «Vencido» se calcula; la fecha entra como parámetro (Clock) |
| 8 | Sala de lectura | Una cinta transportadora numerada 1, 1.1, 1.1.1… con cajas dentro de cajas | Numeración decimal de secuencia y comunicación |
| 9 | Enchufe de la pared | Dos aparatos distintos (una caja de cartón «memoria» y un servidor «Postgres») encajan en el mismo enchufe | Inversión de dependencias: interfaces de repositorio |
| 10 | Caja | Dos manos agarran el mismo libro; la caja registradora solo abre a la primera | Concurrencia: UPDATE condicional + índice único |
| 11 | Salida | Un semáforo con 68 luces verdes y un letrero «12 de 12» | Las pruebas: 68 JUnit, 12 escenarios, 91,6 % |

> **Pruébate:** recorran la biblioteca de memoria y digan, lugar por lugar, qué concepto hay.

---

## 5. Fichas mnemotécnicas

### 5.1 Los seis diagramas, en el orden de la pauta · frase

> **«Cada Clase Deja Sus Apuntes Completos»** → **C**asos de uso, **C**lases, **D**atos (modelo de datos),
> **S**ecuencia, **A**ctividades, **C**omunicación.

- Las tres primeras son el bloque de Matías (estructura); las tres últimas, el de Tais (comportamiento).

**Pruébate:** ¿qué diagramas expone Tais y en qué orden?

**Respuesta:** Secuencia, actividades y comunicación.

### 5.2 Include y extend · contraste en espejo

> **«Include = Inevitable; Extend = Eventual»** → include se ejecuta **siempre**; extend, solo si se cumple
> su condición.
> **«Include sale del base, extend llega al base»** → la flecha de include va del caso base al incluido; la de
> extend, de la extensión al caso base.

**Pruébate:** ¿hacia dónde apunta la flecha de «Calcular multa»?

**Respuesta:** Hacia «Registrar devolución» (extend: de la extensión al caso base).

### 5.3 Rombos · asociación forma → significado

> **«Hueco = Huye, Lleno = Letal»** → rombo blanco (agregación): la parte puede irse y sobrevivir. Rombo negro
> (composición): si muere el todo, muere la parte.

- En BiblioCano: Libro ◆ Ejemplar (negro). No hay agregación: en la duda, asociación simple.

**Pruébate:** ¿qué tres cosas del proyecto prueban que Libro–Ejemplar es composición?

**Respuesta:** Solo `Libro.agregarEjemplar` crea ejemplares (constructor de paquete), `ON DELETE CASCADE` en
SQL, y la copia no tiene sentido sin su título.

### 5.4 Multiplicidad · frase-ancla

> **«Se lee cruzado: el número vive al otro lado»** → `Libro 1 ◆── 0..* Ejemplar`: un libro tiene cero o
> muchos ejemplares; cada ejemplar pertenece a exactamente un libro.

**Pruébate:** ¿qué significa el 1 que está junto a Libro?

**Respuesta:** Que cada ejemplar pertenece a exactamente un libro.

### 5.5 Los tres patrones GRASP del préstamo · acrónimo

> **«CEC: Crea, Entiende, Coordina»** → **C**reador: Bibliotecario crea el Prestamo. **E**xperto en
> información: Socio valida su propio estado. **C**ontrolador: ServicioPrestamos coordina sin decidir.

**Pruébate:** ¿qué patrón justifica que `validarEstado` esté en Socio?

**Respuesta:** Experto en información: Socio tiene los datos (multa y préstamos activos).

### 5.6 Los pasos del servicio · frase

> **«Busca, Busca, Bibliotecario, Bodega»** → 1.1.1 buscar socio, 1.1.2 buscar ejemplar, 1.1.3 el
> bibliotecario registra, 1.1.4 guardar en el repositorio.

**Pruébate:** ¿qué número tiene el mensaje que guarda el préstamo?

**Respuesta:** 1.1.4: `registrar(p)` a RepositorioPrestamos.

### 5.7 Los pasos dentro del bibliotecario · frase

> **«Vamos De Compras, Campeón»** → 1.1.3.1 **V**alidarEstado, 1.1.3.2 está**D**isponible, 1.1.3.3
> **C**rear Prestamo, 1.1.3.4 **C**ambiarEstado(PRESTADO).

- Primero el socio, después el ejemplar: las pruebas verifican ese orden.

**Pruébate:** si el socio tiene multa y el ejemplar está prestado, ¿qué error informa el sistema?

**Respuesta:** Socio inhabilitado, porque el socio se valida primero.

### 5.8 Numeración de comunicación · imagen

> **«Cajas dentro de cajas»** → 1.1.3.1 es una caja dentro de la caja 1.1.3: ocurre mientras 1.1.3 se ejecuta.
> Las respuestas no son cajas nuevas: se escriben `s := buscarPorId(...)`.

**Pruébate:** ¿por qué el avance tenía un error con los mensajes 4 y 6?

**Respuesta:** Eran respuestas numeradas como mensajes nuevos; en comunicación los retornos no se numeran.

### 5.9 Flechas de secuencia · asociación forma → significado

> **«Lleno llama, punteado responde»** → flecha sólida con punta llena = llamada síncrona; punteada = retorno.
> La barra de activación es el tiempo en que el objeto está trabajando.

**Pruébate:** ¿qué es un fragmento alt?

**Respuesta:** Una alternativa: se ejecuta solo el operando cuya guarda se cumple.

### 5.10 Vencido calculado · rima

> **«Lo que depende del calendario no se guarda en el armario»** → «vencido» se calcula con
> `estaVencido(hoy)`; no es un valor de `EstadoPrestamo`.

**Pruébate:** ¿qué dos estados de préstamo se guardan?

**Respuesta:** ACTIVO y DEVUELTO.

### 5.11 Dinero · frase-ancla

> **«Cero coma uno más cero coma dos no da cero coma tres: pesos enteros»** → la multa es `int` (INTEGER en
> SQL), no `double`.

**Pruébate:** ¿cuánto paga Valentina y por qué?

**Respuesta:** $3.000: 6 días de atraso × $500 (prestó hace 20 días con plazo de 14).

### 5.12 Plazo y multa · números

> **«Catorce de plazo, quinientos de castigo»** → `DIAS_PLAZO = 14`, `MULTA_POR_DIA = 500`.

**Pruébate:** si un libro se devuelve el día 14 exacto, ¿hay multa?

**Respuesta:** No: el día límite todavía está dentro del plazo (hay una prueba para ese caso).

### 5.13 Inversión de dependencias · imagen absurda

> **«El enchufe no pregunta qué aparato es»** → ServicioPrestamos depende de interfaces
> (`RepositorioPrestamos`…); BibliotecaEnMemoria y BibliotecaPostgres las implementan.

**Pruébate:** ¿qué ganamos con eso?

**Respuesta:** Probar las reglas en memoria, rápido y sin base, y usar PostgreSQL sin cambiar el servicio.

### 5.14 Concurrencia · frase-ancla

> **«Revisa y marca en un solo golpe»** → `UPDATE ejemplar SET estado = 'PRESTADO' WHERE ... AND estado =
> 'DISPONIBLE'`: si otro lo marcó antes, actualiza cero filas y se rechaza. El índice único parcial es el
> segundo candado.

- Es la misma idea del `ControlConcurrencia` de BibliotecaBase: revisar disponibilidad y registrar, bajo el
  mismo candado.

**Pruébate:** ¿qué prueba lo demuestra?

**Respuesta:** `ochoHilosALaVez`: 8 hilos piden EJ-0006 a la vez y exactamente 1 lo obtiene.

### 5.15 Modelo, marco y técnica · frase-ancla

> **«El modelo arma, el marco maneja, la técnica toca»** → iterativo-incremental es el **modelo de ciclo de
> vida**; Scrum es un **marco de gestión**; prototipado o TDD son **técnicas**.

**Pruébate:** ¿qué modelo de ciclo de vida siguió el proyecto?

**Respuesta:** Iterativo-incremental: avance (0.1.0) y entrega (0.2.0) como incrementos.

### 5.16 Pata de gallo · forma → significado

> **«Raya es uno, círculo es cero, pata es muchos»** → `||` exactamente uno; `o{` cero o muchos.

**Pruébate:** ¿cómo se lee `LIBRO ||──o{ EJEMPLAR`?

**Respuesta:** Un libro tiene cero o muchos ejemplares; cada ejemplar es de exactamente un libro.

### 5.17 Las cifras de las pruebas · números

> **«68 = 60 + 8, y 12 de 12»** → 60 unitarias, 8 de integración con PostgreSQL; la demo pasa 12 de 12
> escenarios; cobertura de líneas 91,6 % (el build exige 80 %).

**Pruébate:** ¿qué parte del código no cuenta para la cobertura y por qué?

**Respuesta:** La interfaz Swing y la consola: son presentación, sin reglas; se prueban con la demo.

### 5.18 Los cambios más importantes · acrónimo

> **«SOS»** → **S**ecuencia con objetos que no existían (C12), **O**lvido de validar al socio en secuencia y
> actividades (C12, C13), **S**ocio que no conocía sus préstamos vencidos (C4).

**Pruébate:** ¿qué se mantuvo del diagrama de clases original de Matías?

**Respuesta:** Las cinco clases, las asociaciones principales y que el bibliotecario crea el préstamo.

---

## 6. Para responder cualquier pregunta: método QPD

| Letra | Paso | Ejemplo |
|---|---|---|
| **Q** | **Qué**: el término técnico exacto | «Es una composición.» |
| **P** | **Por qué**: la propiedad del problema que lo justifica | «Una copia no tiene sentido sin su título.» |
| **D** | **Dónde** se ve: diagrama, clase o prueba | «Solo `Libro.agregarEjemplar` la crea, y en SQL hay `ON DELETE CASCADE`.» |

---

## 7. Las reglas del evaluador, en ganchos

- **«Nombre exacto o no cuenta»**: en la Prueba N.º 1 los puntos se perdieron por describir un concepto sin
  nombrarlo. Decir «Creador», «Experto en información», «composición», no solo la idea.
- **«Diagrama que no explicas, diagrama que te hunde»**: la pauta lo dice textual. Todos deben poder explicar
  los seis, y los cambios del control de cambios.
- **«El código es la prueba del diagrama»**: la rúbrica premia la correspondencia. Ante cualquier pregunta de
  diseño, mostrar la línea de código o la prueba.
- **«Pregunta válida, no trampa»**: en el desafío, preguntas técnicas y respondibles.

---

## 8. Tabla de autoevaluación

| Gancho | Respuesta |
|---|---|
| Cada Clase Deja Sus Apuntes Completos | Casos de uso, clases, datos, secuencia, actividades, comunicación |
| Include = Inevitable; Extend = Eventual | Include siempre; extend solo si se cumple su condición |
| Include sale del base, extend llega al base | Sentido de las flechas de include y extend |
| Hueco = Huye, Lleno = Letal | Agregación (la parte sobrevive) y composición (la parte muere con el todo) |
| Se lee cruzado | La multiplicidad junto a una clase dice cuántos de esa clase hay por cada objeto del otro lado |
| CEC | Creador (Bibliotecario), Experto (Socio), Controlador (ServicioPrestamos) |
| Busca, Busca, Bibliotecario, Bodega | 1.1.1 a 1.1.4 del servicio |
| Vamos De Compras, Campeón | 1.1.3.1 a 1.1.3.4: validar, disponible, crear, cambiar estado |
| Cajas dentro de cajas | Numeración decimal = anidamiento; los retornos no se numeran |
| Lleno llama, punteado responde | Llamada síncrona y retorno en secuencia |
| Lo que depende del calendario no se guarda | Vencido se calcula con estaVencido(hoy) |
| 0,1 + 0,2 no da 0,3 | Multa en pesos enteros (int / INTEGER) |
| Catorce de plazo, quinientos de castigo | DIAS_PLAZO = 14, MULTA_POR_DIA = 500 |
| El enchufe no pregunta qué aparato es | Inversión de dependencias con interfaces de repositorio |
| Revisa y marca en un solo golpe | UPDATE condicional + índice único parcial |
| El modelo arma, el marco maneja, la técnica toca | Iterativo-incremental (modelo), Scrum (marco), prototipado (técnica) |
| Raya uno, círculo cero, pata muchos | Notación pata de gallo del modelo de datos |
| 68 = 60 + 8, y 12 de 12 | Cifras de las pruebas |
| SOS | Los tres cambios más importantes respecto del avance |
| QPD | Qué, Por qué, Dónde: cómo responder |

---

## 9. Calendario de repaso espaciado

| Cuándo | Qué hacer | Tiempo |
|---|---|---|
| Hoy (19/11) | Leer secciones 1 y 2; recorrer el palacio dos veces | 40 min |
| Mañana (20/11), al despertar | Palacio y tabla de autoevaluación **sin mirar** | 15 min |
| 22/11 | Banco de preguntas: uno pregunta, otro responde con QPD (en pareja o por llamada) | 30 min |
| 24/11 | Ensayo cronometrado de 20 minutos, todo el equipo | 45 min |
| 25/11 | Solo los ganchos que fallaron; repasar las preguntas para los otros equipos | 15 min |
| Día de la defensa, una hora antes | Palacio completo una vez; revisar que la demo corre (o el plan B) | 15 min |
