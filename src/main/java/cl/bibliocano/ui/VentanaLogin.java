package cl.bibliocano.ui;

import cl.bibliocano.Contexto;
import cl.bibliocano.Version;
import cl.bibliocano.dominio.Bibliotecario;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.WindowConstants;

/** Caso de uso «Iniciar sesión». Si las credenciales son válidas abre {@link VentanaPrincipal}. */
public final class VentanaLogin extends JFrame {

    private static final long serialVersionUID = 1L;

    private final transient Contexto contexto;
    private final JTextField txtUsuario = new JTextField(18);
    private final JPasswordField txtPassword = new JPasswordField(18);

    public VentanaLogin(Contexto contexto) {
        super("BiblioCano " + Version.actual() + " · Iniciar sesión");
        this.contexto = contexto;
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);

        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 4, 4, 4);
        g.anchor = GridBagConstraints.WEST;

        g.gridx = 0;
        g.gridy = 0;
        panel.add(new JLabel("Usuario"), g);
        g.gridx = 1;
        panel.add(txtUsuario, g);
        g.gridx = 0;
        g.gridy = 1;
        panel.add(new JLabel("Contraseña"), g);
        g.gridx = 1;
        panel.add(txtPassword, g);

        JButton btnEntrar = new JButton("Entrar");
        btnEntrar.addActionListener(e -> entrar());
        g.gridx = 1;
        g.gridy = 2;
        g.anchor = GridBagConstraints.EAST;
        panel.add(btnEntrar, g);

        g.gridx = 0;
        g.gridy = 3;
        g.gridwidth = 2;
        g.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel("<html><small>Datos: " + contexto.getDescripcion() + "</small></html>"), g);

        getRootPane().setDefaultButton(btnEntrar);
        setContentPane(panel);
        pack();
        setLocationRelativeTo(null);
    }

    private void entrar() {
        Bibliotecario bibliotecario = Dialogos.ejecutar(this, () -> contexto.getAcceso()
                .iniciarSesion(txtUsuario.getText(), new String(txtPassword.getPassword())));
        if (bibliotecario != null) {
            new VentanaPrincipal(contexto, bibliotecario).setVisible(true);
            dispose();
        } else {
            txtPassword.setText("");
        }
    }

    /** Abre la ventana en el hilo de Swing con la apariencia del sistema operativo. */
    public static void mostrar(Contexto contexto) {
        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (ReflectiveOperationException | UnsupportedLookAndFeelException e) {
                // sin la apariencia nativa se usa la de Java: la ventana funciona igual
            }
            new VentanaLogin(contexto).setVisible(true);
        });
    }
}
