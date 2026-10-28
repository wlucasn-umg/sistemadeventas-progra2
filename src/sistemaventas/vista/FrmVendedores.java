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
import javax.swing.JPasswordField;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.DefaultTableModel;
import sistemaventas.datos.BaseDatosMemoria;
import sistemaventas.datos.VendedorDAO;
import sistemaventas.modelo.Vendedor;
import sistemaventas.modelo.Venta;

/**
 * Catálogo de vendedores como ventana interna del MDI.
 * Altas, cambios, bajas y búsqueda sobre la base de datos MySQL (VendedorDAO).
 */
public class FrmVendedores extends JInternalFrame {

    private final VendedorDAO dao = new VendedorDAO();
    // Las ventas aún viven en memoria; se usan para no borrar vendedores con ventas
    private final BaseDatosMemoria db = BaseDatosMemoria.getInstancia();
    // Vendedores que se ven ahora en la tabla (misma posición que las filas)
    private final List<Vendedor> mostrados = new ArrayList<>();

    private final JTextField txtBuscar = new JTextField(20);
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnTodos = new JButton("Mostrar todos");
    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnEditar = new JButton("Editar");
    private final JButton btnEliminar = new JButton("Eliminar");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Nombre", "Teléfono", "Puesto", "Usuario"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);

    public FrmVendedores() {
        super("Vendedores", true, true, true, true);
        setSize(780, 460);
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
        norte.setBorder(BorderFactory.createTitledBorder("Buscar por ID, nombre, puesto o usuario"));
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
        btnNuevo.addActionListener(e -> nuevoVendedor());
        btnEditar.addActionListener(e -> editarVendedor());
        btnEliminar.addActionListener(e -> eliminarVendedor());
    }

    private void cargarTabla(String filtro) {
        modeloTabla.setRowCount(0);
        mostrados.clear();
        try {
            List<Vendedor> lista = filtro.isEmpty() ? dao.listar() : dao.buscar(filtro);
            for (Vendedor v : lista) {
                mostrados.add(v);
                modeloTabla.addRow(new Object[]{
                    v.getIdVendedor(), v.getNombre(), v.getTelefono(), v.getPuesto(),
                    v.getUsuario().getNombreUsuario()});
            }
        } catch (SQLException e) {
            errorBD(e);
        }
    }

    private void nuevoVendedor() {
        JTextField id = new JTextField(20);
        JTextField nombre = new JTextField(20);
        JTextField telefono = new JTextField(20);
        JTextField puesto = new JTextField(20);
        JTextField usuario = new JTextField(20);
        JPasswordField clave = new JPasswordField(20);

        while (pedirDatos("Nuevo vendedor", false, id, nombre, telefono, puesto, usuario, clave)) {
            String claveTexto = new String(clave.getPassword());
            String error = validar(id.getText().trim(), nombre.getText().trim(),
                    usuario.getText().trim(), claveTexto, null);
            if (error != null) {
                aviso(error);
                continue; // vuelve a mostrar el formulario con lo que ya escribiste
            }
            try {
                dao.insertar(new Vendedor(nombre.getText().trim(), telefono.getText().trim(),
                        id.getText().trim(), puesto.getText().trim(), usuario.getText().trim(),
                        claveTexto), claveTexto);
            } catch (SQLException e) {
                errorBD(e);
                return;
            }
            cargarTabla(txtBuscar.getText().trim());
            return;
        }
    }

    private void editarVendedor() {
        Vendedor v = seleccionado();
        if (v == null) {
            return;
        }
        String idOriginal = v.getIdVendedor();
        JTextField id = new JTextField(v.getIdVendedor(), 20);
        JTextField nombre = new JTextField(v.getNombre(), 20);
        JTextField telefono = new JTextField(v.getTelefono(), 20);
        JTextField puesto = new JTextField(v.getPuesto(), 20);
        JTextField usuario = new JTextField(v.getUsuario().getNombreUsuario(), 20);
        JPasswordField clave = new JPasswordField(20);

        while (pedirDatos("Editar vendedor", true, id, nombre, telefono, puesto, usuario, clave)) {
            String claveTexto = new String(clave.getPassword());
            String error = validar(id.getText().trim(), nombre.getText().trim(),
                    usuario.getText().trim(), claveTexto, idOriginal);
            if (error != null) {
                aviso(error);
                continue;
            }
            try {
                dao.actualizar(idOriginal, new Vendedor(nombre.getText().trim(),
                        telefono.getText().trim(), id.getText().trim(), puesto.getText().trim(),
                        usuario.getText().trim(), claveTexto), claveTexto);
            } catch (SQLException e) {
                errorBD(e);
                return;
            }
            cargarTabla(txtBuscar.getText().trim());
            return;
        }
    }

    private void eliminarVendedor() {
        Vendedor v = seleccionado();
        if (v == null) {
            return;
        }
        try {
            int ventas = 0;
            for (Venta venta : db.getVentas()) {
                if (venta.getVendedor() != null
                        && venta.getVendedor().getIdVendedor().equalsIgnoreCase(v.getIdVendedor())) {
                    ventas++;
                }
            }
            if (ventas > 0 || dao.tieneVentas(v.getIdVendedor())) {
                aviso("No se puede eliminar a " + v.getNombre() + ": ya tiene ventas registradas.");
                return;
            }
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar al vendedor " + v.getNombre() + " (ID " + v.getIdVendedor() + ")?",
                    "Eliminar vendedor", JOptionPane.YES_NO_OPTION);
            if (respuesta == JOptionPane.YES_OPTION) {
                dao.eliminar(v.getIdVendedor());
                cargarTabla(txtBuscar.getText().trim());
            }
        } catch (SQLException e) {
            errorBD(e);
        }
    }

    private boolean pedirDatos(String titulo, boolean edicion, JTextField id, JTextField nombre,
            JTextField telefono, JTextField puesto, JTextField usuario, JPasswordField clave) {
        JPanel panel = new JPanel(new GridLayout(6, 2, 6, 6));
        panel.add(new JLabel("ID:"));
        panel.add(id);
        panel.add(new JLabel("Nombre:"));
        panel.add(nombre);
        panel.add(new JLabel("Teléfono:"));
        panel.add(telefono);
        panel.add(new JLabel("Puesto:"));
        panel.add(puesto);
        panel.add(new JLabel("Usuario:"));
        panel.add(usuario);
        panel.add(new JLabel(edicion ? "Contraseña (vacío = no cambiar):" : "Contraseña:"));
        panel.add(clave);
        return JOptionPane.showConfirmDialog(this, panel, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    /** idOriginal es null cuando se trata de un vendedor nuevo. */
    private String validar(String id, String nombre, String usuario, String clave, String idOriginal) {
        if (id.isEmpty()) {
            return "El ID es obligatorio.";
        }
        if (nombre.isEmpty()) {
            return "El nombre es obligatorio.";
        }
        if (usuario.isEmpty()) {
            return "El nombre de usuario es obligatorio.";
        }
        if (idOriginal == null && clave.isEmpty()) {
            return "La contraseña es obligatoria.";
        }
        try {
            boolean mismoId = idOriginal != null && idOriginal.equalsIgnoreCase(id);
            if (!mismoId && dao.existeCodigo(id)) {
                return "Ya existe un vendedor con ese ID.";
            }
            if (dao.existeUsuario(usuario, idOriginal == null ? "" : idOriginal)) {
                return "Ese nombre de usuario ya está en uso.";
            }
        } catch (SQLException e) {
            return "No se pudo verificar en la base de datos: " + e.getMessage();
        }
        return null;
    }

    private Vendedor seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= mostrados.size()) {
            aviso("Selecciona un vendedor de la tabla.");
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