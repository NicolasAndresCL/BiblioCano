-- =====================================================================================
-- BiblioCano: datos de ejemplo. Son los MISMOS de DatosDemo.java (modo memoria): cada socio y
-- ejemplar existe para un escenario de la demostración. Si se cambia algo aquí, cambiarlo allá.
--
-- Fechas relativas a CURRENT_DATE: el préstamo de Valentina siempre lleva 6 días de atraso.
-- =====================================================================================

-- Contraseña de la demo: biblio2026 (hash PBKDF2-HMAC-SHA256, 100 000 iteraciones, sal aleatoria)
INSERT INTO bibliotecario (nombre, usuario, password_hash) VALUES
    ('Ana Rojas', 'bibliotecaria', '100000:xwoolX9zseKXPaiXvbOIIQ==:vEud1BKfCen/Alss54/1wNff6uNh9vrtIlsS8BXzOoA=');

-- id 1: habilitado (camino feliz) · id 2: multa impaga · id 3: préstamo vencido
INSERT INTO socio (rut, nombre, correo, tiene_multa) VALUES
    ('12.345.678-5', 'Camila Fuentes', 'camila@correo.cl', FALSE),
    ('9.876.543-3', 'Diego Morales', 'diego@correo.cl', TRUE),
    ('15.222.333-K', 'Valentina Soto', 'valentina@correo.cl', FALSE);

INSERT INTO libro (isbn, titulo, autor, categoria) VALUES
    ('9780132350884', 'Clean Code', 'Robert C. Martin', 'Ingeniería de software'),
    ('9780201633610', 'Design Patterns', 'Gamma, Helm, Johnson y Vlissides', 'Ingeniería de software'),
    ('9780307474728', 'Cien años de soledad', 'Gabriel García Márquez', 'Novela'),
    -- título sin copias: muestra la multiplicidad 0..* de Libro ◆── Ejemplar
    ('9780321193681', 'UML Distilled', 'Martin Fowler', 'Ingeniería de software');

INSERT INTO ejemplar (codigo_barra, isbn, ubicacion, estado) VALUES
    ('EJ-0001', '9780132350884', 'Estante A-1', 'DISPONIBLE'),
    ('EJ-0002', '9780132350884', 'Estante A-1', 'PRESTADO'),
    ('EJ-0003', '9780201633610', 'Estante A-2', 'DISPONIBLE'),
    ('EJ-0004', '9780201633610', 'Bodega', 'MANTENIMIENTO'),
    ('EJ-0005', '9780307474728', 'Estante C-4', 'DISPONIBLE'),
    ('EJ-0006', '9780307474728', 'Estante C-4', 'DISPONIBLE');

-- Préstamo vencido de Valentina: hace 20 días, con plazo de 14 → 6 días de atraso ($3.000)
INSERT INTO prestamo (id_bibliotecario, id_socio, codigo_barra, fecha_prestamo,
                      fecha_devolucion_esperada, estado, multa) VALUES
    (1, 3, 'EJ-0002', CURRENT_DATE - 20, CURRENT_DATE - 6, 'ACTIVO', 0);
