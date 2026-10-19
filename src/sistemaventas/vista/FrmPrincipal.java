package sistemaventas.vista;

import java.beans.PropertyVetoException;
import javax.swing.JDesktopPane;
import javax.swing.JFrame;
import javax.swing.JInternalFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/**
 * Ventana principal (contenedor MDI).
 * Aquí viven el menú y el escritorio donde se abren los formularios internos.
 */
public class FrmPrincipal extends JFrame {

    private final JDesktopPane escritorio = new JDesktopPane();

    public FrmPrincipal() {
        super("Sistema de Ventas");
        // DISPOSE y no EXIT: cerrar esta ventana no debe matar el programa completo
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setSize(1100, 700);
        setLocationRelativeTo(null);
        add(escritorio);
        construirMenu();
    }

    private void construirMenu() {
        JMenuBar barra = new JMenuBar();

        JMenu mnuCatalogos = new JMenu("Catálogos");
        mnuCatalogos.add(crearItem("Clientes", () -> abrirFormulario(new FrmClientes())));
        mnuCatalogos.add(crearItem("Vendedores", () -> abrirFormulario(new FrmVendedores())));
        mnuCatalogos.add(crearItem("Productos", () -> abrirFormulario(new FrmProductos())));

        JMenu mnuVentas = new JMenu("Ventas");
        mnuVentas.add(crearItem("Facturación", () -> abrirFormulario(new FrmFacturacion())));
        mnuVentas.add(crearItem("Consulta de ventas", () -> abrirFormulario(new FrmVentas())));

        JMenu mnuSistema = new JMenu("Sistema");
        mnuSistema.add(crearItem("Salir", this::salir));

        barra.add(mnuCatalogos);
        barra.add(mnuVentas);
        barra.add(mnuSistema);
        setJMenuBar(barra);
    }

    private JMenuItem crearItem(String texto, Runnable accion) {
        JMenuItem item = new JMenuItem(texto);
        item.addActionListener(e -> accion.run());
        return item;
    }

    private void salir() {
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Seguro que deseas cerrar la ventana principal?",
                "Salir", JOptionPane.YES_NO_OPTION);
        if (respuesta == JOptionPane.YES_OPTION) {
            dispose();
        }
    }

    /**
     * Abre un formulario interno dentro del escritorio.
     * Si ese formulario ya estaba abierto, lo trae al frente en vez de abrir otro.
     */
    public void abrirFormulario(JInternalFrame formulario) {
        for (JInternalFrame abierto : escritorio.getAllFrames()) {
            if (abierto.getClass().equals(formulario.getClass())) {
                try {
                    abierto.setIcon(false);
                    abierto.setSelected(true);
                } catch (PropertyVetoException ex) {
                    // si no se puede traer al frente, no pasa nada
                }
                abierto.toFront();
                return;
            }
        }
        escritorio.add(formulario);
        formulario.setLocation(
                Math.max(0, (escritorio.getWidth() - formulario.getWidth()) / 2),
                Math.max(0, (escritorio.getHeight() - formulario.getHeight()) / 2));
        formulario.setVisible(true);
        try {
            formulario.setSelected(true);
        } catch (PropertyVetoException ex) {
            // ignorar
        }
    }

    public static void main(String[] args) {
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (Exception ex) {
            System.out.println("No se pudo aplicar Nimbus, se usará el tema por defecto.");
        }
        SwingUtilities.invokeLater(() -> new FrmPrincipal().setVisible(true));
    }
}