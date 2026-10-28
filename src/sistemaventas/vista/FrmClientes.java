package sistemaventas.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.DefaultTableModel;
import sistemaventas.datos.BaseDatosMemoria;
import sistemaventas.datos.ClienteDAO;
import sistemaventas.modelo.Cliente;
import sistemaventas.modelo.Venta;

/**
 * Catálogo de clientes como ventana interna del MDI.
 * Altas, cambios, bajas y búsqueda sobre la base de datos MySQL (ClienteDAO).
 */
public class FrmClientes extends JInternalFrame {

    private final ClienteDAO dao = new ClienteDAO();
    // Las ventas aún viven en memoria; se usan para no borrar clientes con ventas
    private final BaseDatosMemoria db = BaseDatosMemoria.getInstancia();
    // Clientes que se ven ahora en la tabla (misma posición que las filas)
    private final List<Cliente> mostrados = new ArrayList<>();

    private final JTextField txtBuscar = new JTextField(20);
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnTodos = new JButton("Mostrar todos");
    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnEditar = new JButton("Editar");
    private final JButton btnEliminar = new JButton("Eliminar");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"NIT", "Nombre", "Teléfono", "Dirección de envío"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);

    public FrmClientes() {
        super("Clientes", true, true, true, true);
        setSize(760, 460);
        construirPantalla();
        cargarTabla("");
        // Al volver a esta ventana se refresca la tabla
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                cargarTabla(txtBuscar.getText().trim());
            }
        });
    }

    private void construirPantalla() {
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.setBorder(BorderFactory.createTitledBorder("Buscar por NIT o nombre"));
        norte.add(txtBuscar);
        norte.add(btnBuscar);
        norte.add(btnTodos);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.add(btnNuevo);
        sur.add(btnEditar);
        sur.add(btnEliminar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(norte, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(tabla), BorderLayout.CENTER);
        getContentPane().add(sur, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> cargarTabla(txtBuscar.getText().trim()));
        txtBuscar.addActionListener(e -> cargarTabla(txtBuscar.getText().trim()));
        btnTodos.addActionListener(e -> {
            txtBuscar.setText("");
            cargarTabla("");
        });
        btnNuevo.addActionListener(e -> nuevoCliente());
        btnEditar.addActionListener(e -> editarCliente());
        btnEliminar.addActionListener(e -> eliminarCliente());
    }

    private void cargarTabla(String filtro) {
        modeloTabla.setRowCount(0);
        mostrados.clear();
        try {
            List<Cliente> lista = filtro.isEmpty() ? dao.listar() : dao.buscar(filtro);
            for (Cliente c : lista) {
                mostrados.add(c);
                modeloTabla.addRow(new Object[]{
                    c.getNit(), c.getNombre(), c.getTelefono(), c.getDireccionEnvio()});
            }
        } catch (SQLException e) {
            errorBD(e);
        }
    }

    private void nuevoCliente() {
        JTextField nit = new JTextField(20);
        JTextField nombre = new JTextField(20);
        JTextField telefono = new JTextField(20);
        JTextField direccion = new JTextField(20);

        while (pedirDatos("Nuevo cliente", nit, nombre, telefono, direccion)) {
            String error = validar(nit.getText().trim(), nombre.getText().trim(), null);
            if (error != null) {
                aviso(error);
                continue; // vuelve a mostrar el formulario con lo que ya escribiste
            }
            try {
                dao.insertar(new Cliente(nombre.getText().trim(), telefono.getText().trim(),
                        nit.getText().trim(), direccion.getText().trim()));
            } catch (SQLException e) {
                errorBD(e);
                return;
            }
            cargarTabla(txtBuscar.getText().trim());
            return;
        }
    }

    private void editarCliente() {
        Cliente c = seleccionado();
        if (c == null) {
            return;
        }
        String nitOriginal = c.getNit();
        JTextField nit = new JTextField(c.getNit(), 20);
        JTextField nombre = new JTextField(c.getNombre(), 20);
        JTextField telefono = new JTextField(c.getTelefono(), 20);
        JTextField direccion = new JTextField(c.getDireccionEnvio(), 20);

        while (pedirDatos("Editar cliente", nit, nombre, telefono, direccion)) {
            String error = validar(nit.getText().trim(), nombre.getText().trim(), nitOriginal);
            if (error != null) {
                aviso(error);
                continue;
            }
            try {
                dao.actualizar(nitOriginal, new Cliente(nombre.getText().trim(),
                        telefono.getText().trim(), nit.getText().trim(),
                        direccion.getText().trim()));
            } catch (SQLException e) {
                errorBD(e);
                return;
            }
            cargarTabla(txtBuscar.getText().trim());
            return;
        }
    }

    private void eliminarCliente() {
        Cliente c = seleccionado();
        if (c == null) {
            return;
        }
        try {
            boolean enVentas = false;
            for (Venta v : db.getVentas()) {
                if (v.getCliente() != null
                        && v.getCliente().getNit().equalsIgnoreCase(c.getNit())) {
                    enVentas = true;
                    break;
                }
            }
            if (enVentas || dao.tieneVentas(c.getNit())) {
                aviso("No se puede eliminar a " + c.getNombre() + ": ya tiene ventas registradas.");
                return;
            }
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar al cliente " + c.getNombre() + " (NIT " + c.getNit() + ")?",
                    "Eliminar cliente", JOptionPane.YES_NO_OPTION);
            if (respuesta == JOptionPane.YES_OPTION) {
                dao.eliminar(c.getNit());
                cargarTabla(txtBuscar.getText().trim());
            }
        } catch (SQLException e) {
            errorBD(e);
        }
    }

    private boolean pedirDatos(String titulo, JTextField nit, JTextField nombre,
            JTextField telefono, JTextField direccion) {
        JPanel panel = new JPanel(new GridLayout(4, 2, 6, 6));
        panel.add(new JLabel("NIT:"));
        panel.add(nit);
        panel.add(new JLabel("Nombre:"));
        panel.add(nombre);
        panel.add(new JLabel("Teléfono:"));
        panel.add(telefono);
        panel.add(new JLabel("Dirección de envío:"));
        panel.add(direccion);
        return JOptionPane.showConfirmDialog(this, panel, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    /** nitOriginal es null cuando se trata de un cliente nuevo. */
    private String validar(String nit, String nombre, String nitOriginal) {
        if (nit.isEmpty()) {
            return "El NIT es obligatorio.";
        }
        if (nombre.isEmpty()) {
            return "El nombre es obligatorio.";
        }
        boolean mismoNit = nitOriginal != null && nitOriginal.equalsIgnoreCase(nit);
        if (!mismoNit) {
            try {
                if (dao.existeNit(nit)) {
                    return "Ya existe un cliente con ese NIT.";
                }
            } catch (SQLException e) {
                return "No se pudo verificar el NIT: " + e.getMessage();
            }
        }
        return null;
    }

    private Cliente seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= mostrados.size()) {
            aviso("Selecciona un cliente de la tabla.");
            return null;
        }
        return mostrados.get(fila);
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void errorBD(SQLException e) {
        JOptionPane.showMessageDialog(this,
                "Error con la base de datos:\n" + e.getMessage(),
                "Error", JOptionPane.ERROR_MESSAGE);
    }
}