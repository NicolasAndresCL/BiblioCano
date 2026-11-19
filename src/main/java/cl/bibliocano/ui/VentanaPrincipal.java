package cl.bibliocano.ui;

import cl.bibliocano.Contexto;
import cl.bibliocano.Version;
import cl.bibliocano.dominio.Bibliotecario;
import cl.bibliocano.dominio.Ejemplar;
import cl.bibliocano.dominio.Libro;
import cl.bibliocano.dominio.Prestamo;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.WindowConstants;
import javax.swing.table.DefaultTableModel;

/**
 * Ventana principal con una pestaña por caso de uso: consultar catálogo, registrar préstamo y
 * registrar devolución. Cada botón llama a un método de servicio y muestra lo que devuelve; la
 * ventana no decide nada (la prueba es que las mismas reglas se cumplen en la demo por consola).
 */
public final class VentanaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd-MM-yyyy");
    private static final Locale CHILE = Locale.of("es", "CL");

    private final transient Contexto contexto;
    private final transient Bibliotecario bibliotecario;

    // Catálogo
    private final JTextField txtBuscar = new JTextField(24);
    private final DefaultTableModel modeloLibros = modelo("Título", "Autor", "ISBN", "Categoría", "Disponibles");
    private final DefaultTableModel modeloEjemplares = modelo("Código de barras", "Ubicación", "Estado");
    private final JTable tblLibros = new JTable(modeloLibros);
    private transient List<Libro> librosMostrados = new ArrayList<>();

    // Préstamo
    private final JTextField txtIdSocio = new JTextField(10);
    private final JTextField txtCodigoPrestamo = new JTextField(14);
    private final JTextArea txtComprobante = comprobante();

    // Devolución
    private final JTextField txtCodigoDevolucion = new JTextField(14);
    private final JTextArea txtResultadoDevolucion = comprobante();

    public VentanaPrincipal(Contexto contexto, Bibliotecario bibliotecario) {
        super("BiblioCano " + Version.actual());
        this.contexto = contexto;
        this.bibliotecario = bibliotecario;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JTabbedPane pestanas = new JTabbedPane();
        pestanas.addTab("Consultar catálogo", panelCatalogo());
        pestanas.addTab("Registrar préstamo", panelPrestamo());
        pestanas.addTab("Registrar devolución", panelDevolucion());

        JLabel estado = new JLabel("Sesión: " + bibliotecario.getNombre() + "   ·   Datos: "
                + contexto.getDescripcion());
        estado.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));

        getContentPane().add(pestanas, BorderLayout.CENTER);
        getContentPane().add(estado, BorderLayout.SOUTH);
        setSize(900, 560);
        setLocationRelativeTo(null);
        buscar();
    }

    // ----------------------------------------------------------------- catálogo

    private JPanel panelCatalogo() {
        JButton btnBuscar = new JButton("Buscar");
        btnBuscar.addActionListener(e -> buscar());
        txtBuscar.addActionListener(e -> buscar());
        JPanel barra = new JPanel(new FlowLayout(FlowLayout.LEFT));
        barra.add(new JLabel("Título, autor o ISBN:"));
        barra.add(txtBuscar);
        barra.add(btnBuscar);

        tblLibros.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tblLibros.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting()) {
                mostrarEjemplares();
            }
        });
        JSplitPane division = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                new JScrollPane(tblLibros), new JScrollPane(new JTable(modeloEjemplares)));
        division.setResizeWeight(0.6);

        JPanel panel = new JPanel(new BorderLayout());
        panel.add(barra, BorderLayout.NORTH);
        panel.add(division, BorderLayout.CENTER);
        return panel;
    }

    private void buscar() {
        List<Libro> libros = Dialogos.ejecutar(this, () -> contexto.getCatalogo().buscar(txtBuscar.getText()));
        if (libros == null) {
            return;
        }
        librosMostrados = libros;
        modeloLibros.setRowCount(0);
        for (Libro libro : libros) {
            modeloLibros.addRow(new Object[] {libro.getTitulo(), libro.getAutor(), libro.getIsbn(),
                libro.getCategoria(),
                libro.obtenerEjemplaresDisponibles().size() + " de " + libro.getEjemplares().size()});
        }
        modeloEjemplares.setRowCount(0);
    }

    private void mostrarEjemplares() {
        modeloEjemplares.setRowCount(0);
        int fila = tblLibros.getSelectedRow();
        if (fila < 0 || fila >= librosMostrados.size()) {
            return;
        }
        for (Ejemplar ejemplar : librosMostrados.get(fila).getEjemplares()) {
            modeloEjemplares.addRow(new Object[] {ejemplar.getCodigoBarra(), ejemplar.getUbicacion(),
                ejemplar.getEstado()});
        }
    }

    // ----------------------------------------------------------------- préstamo

    private JPanel panelPrestamo() {
        JButton btnPrestar = new JButton("Registrar préstamo");
        btnPrestar.addActionListener(e -> prestar());
        return formulario(new String[] {"ID del socio", "Código de barras del ejemplar"},
                new JTextField[] {txtIdSocio, txtCodigoPrestamo}, btnPrestar, txtComprobante);
    }

    private void prestar() {
        Prestamo prestamo = Dialogos.ejecutar(this, () -> contexto.getPrestamos().registrarPrestamo(
                bibliotecario, leerIdSocio(), txtCodigoPrestamo.getText().strip()));
        if (prestamo == null) {
            return;
        }
        txtComprobante.setText("""
                COMPROBANTE DE PRÉSTAMO N.º %d

                Socio:      %s (RUT %s)
                Libro:      %s
                Ejemplar:   %s · %s
                Prestado:   %s
                Devolver:   %s  (multa de $%d por día de atraso)
                Atendió:    %s
                """.formatted(prestamo.getIdPrestamo(), prestamo.getSocio().getNombre(),
                prestamo.getSocio().getRut(), prestamo.getEjemplar().getLibro().getTitulo(),
                prestamo.getEjemplar().getCodigoBarra(), prestamo.getEjemplar().getUbicacion(),
                prestamo.getFechaPrestamo().format(FECHA), prestamo.getFechaDevolucionEsperada().format(FECHA),
                Prestamo.MULTA_POR_DIA, bibliotecario.getNombre()));
        txtIdSocio.setText("");
        txtCodigoPrestamo.setText("");
        buscar();
    }

    private int leerIdSocio() {
        try {
            return Integer.parseInt(txtIdSocio.getText().strip());
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("El ID del socio debe ser un número entero");
        }
    }

    // ----------------------------------------------------------------- devolución

    private JPanel panelDevolucion() {
        JButton btnDevolver = new JButton("Registrar devolución");
        btnDevolver.addActionListener(e -> devolver());
        return formulario(new String[] {"Código de barras del ejemplar"},
                new JTextField[] {txtCodigoDevolucion}, btnDevolver, txtResultadoDevolucion);
    }

    private void devolver() {
        Prestamo prestamo = Dialogos.ejecutar(this, () -> contexto.getPrestamos().registrarDevolucion(
                bibliotecario, txtCodigoDevolucion.getText().strip()));
        if (prestamo == null) {
            return;
        }
        String multa = prestamo.getMulta() == 0 ? "Sin multa: devuelto dentro del plazo."
                : "MULTA: $" + String.format(CHILE, "%,d", prestamo.getMulta())
                        + ". El socio queda inhabilitado hasta pagarla.";
        txtResultadoDevolucion.setText("""
                DEVOLUCIÓN DEL PRÉSTAMO N.º %d

                Socio:      %s
                Libro:      %s (%s)
                Esperada:   %s
                Entregado:  %s

                %s
                """.formatted(prestamo.getIdPrestamo(), prestamo.getSocio().getNombre(),
                prestamo.getEjemplar().getLibro().getTitulo(), prestamo.getEjemplar().getCodigoBarra(),
                prestamo.getFechaDevolucionEsperada().format(FECHA),
                prestamo.getFechaDevolucionReal().format(FECHA), multa));
        txtCodigoDevolucion.setText("");
        buscar();
    }

    // ----------------------------------------------------------------- utilidades de armado

    private static JPanel formulario(String[] etiquetas, JTextField[] campos, JButton boton, JTextArea resultado) {
        JPanel contenedor = new JPanel(new GridBagLayout());
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;
        for (int i = 0; i < etiquetas.length; i++) {
            g.gridx = 0;
            g.gridy = i;
            contenedor.add(new JLabel(etiquetas[i]), g);
            g.gridx = 1;
            contenedor.add(campos[i], g);
        }
        g.gridx = 1;
        g.gridy = etiquetas.length;
        contenedor.add(boton, g);

        JPanel panel = new JPanel(new BorderLayout(8, 8));
        panel.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));
        panel.add(contenedor, BorderLayout.NORTH);
        panel.add(new JScrollPane(resultado), BorderLayout.CENTER);
        return panel;
    }

    private static JTextArea comprobante() {
        JTextArea area = new JTextArea();
        area.setEditable(false);
        area.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 13));
        return area;
    }

    private static DefaultTableModel modelo(String... columnas) {
        return new DefaultTableModel(columnas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int fila, int columna) {
                return false;
            }
        };
    }
}
