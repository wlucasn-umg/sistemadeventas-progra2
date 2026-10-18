package sistemaventas.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
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
import sistemaventas.modelo.Cliente;

/**
 * Catálogo de clientes como ventana interna del MDI.
 * Altas, cambios, bajas y búsqueda sobre BaseDatosMemoria.
 */
public class FrmClientes extends JInternalFrame {

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
        String f = filtro.toLowerCase();
        for (Cliente c : db.getClientes()) {
            boolean coincide = f.isEmpty()
                    || c.getNit().toLowerCase().contains(f)
                    || c.getNombre().toLowerCase().contains(f);
            if (coincide) {
                mostrados.add(c);
                modeloTabla.addRow(new Object[]{
                    c.getNit(), c.getNombre(), c.getTelefono(), c.getDireccionEnvio()});
            }
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
            db.getClientes().add(new Cliente(nombre.getText().trim(), telefono.getText().trim(),
                    nit.getText().trim(), direccion.getText().trim()));
            cargarTabla(txtBuscar.getText().trim());
            return;
        }
    }

    private void editarCliente() {
        Cliente c = seleccionado();
        if (c == null) {
            return;
        }
        JTextField nit = new JTextField(c.getNit(), 20);
        JTextField nombre = new JTextField(c.getNombre(), 20);
        JTextField telefono = new JTextField(c.getTelefono(), 20);
        JTextField direccion = new JTextField(c.getDireccionEnvio(), 20);

        while (pedirDatos("Editar cliente", nit, nombre, telefono, direccion)) {
            String error = validar(nit.getText().trim(), nombre.getText().trim(), c);
            if (error != null) {
                aviso(error);
                continue;
            }
            c.setNit(nit.getText().trim());
            c.setNombre(nombre.getText().trim());
            c.setTelefono(telefono.getText().trim());
            c.setDireccionEnvio(direccion.getText().trim());
            cargarTabla(txtBuscar.getText().trim());
            return;
        }
    }

    private void eliminarCliente() {
        Cliente c = seleccionado();
        if (c == null) {
            return;
        }
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Eliminar al cliente " + c.getNombre() + " (NIT " + c.getNit() + ")?",
                "Eliminar cliente", JOptionPane.YES_NO_OPTION);
        if (respuesta == JOptionPane.YES_OPTION) {
            db.getClientes().remove(c);
            cargarTabla(txtBuscar.getText().trim());
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

    private String validar(String nit, String nombre, Cliente actual) {
        if (nit.isEmpty()) {
            return "El NIT es obligatorio.";
        }
        if (nombre.isEmpty()) {
            return "El nombre es obligatorio.";
        }
        for (Cliente otro : db.getClientes()) {
            if (otro != actual && otro.getNit().equalsIgnoreCase(nit)) {
                return "Ya existe un cliente con ese NIT.";
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
}