package sistemaventas.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
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
import sistemaventas.datos.ProductoDAO;
import sistemaventas.modelo.Producto;
import sistemaventas.modelo.Venta;

/**
 * Catálogo de productos como ventana interna del MDI.
 * Altas, cambios, bajas y búsqueda sobre la base de datos MySQL (ProductoDAO).
 */
public class FrmProductos extends JInternalFrame {

    private final ProductoDAO dao = new ProductoDAO();
    // Las ventas aún viven en memoria; se usan para no borrar productos ya vendidos
    private final BaseDatosMemoria db = BaseDatosMemoria.getInstancia();
    // Productos que se ven ahora en la tabla (misma posición que las filas)
    private final List<Producto> mostrados = new ArrayList<>();

    private final JTextField txtBuscar = new JTextField(20);
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnTodos = new JButton("Mostrar todos");
    private final JButton btnNuevo = new JButton("Nuevo");
    private final JButton btnEditar = new JButton("Editar");
    private final JButton btnEliminar = new JButton("Eliminar");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"Código", "Nombre", "Categoría", "Precio (Q)", "Stock"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);

    public FrmProductos() {
        super("Productos", true, true, true, true);
        setSize(800, 460);
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
        norte.setBorder(BorderFactory.createTitledBorder("Buscar por código, nombre o categoría"));
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
        btnNuevo.addActionListener(e -> nuevoProducto());
        btnEditar.addActionListener(e -> editarProducto());
        btnEliminar.addActionListener(e -> eliminarProducto());
    }

    private void cargarTabla(String filtro) {
        modeloTabla.setRowCount(0);
        mostrados.clear();
        try {
            List<Producto> lista = filtro.isEmpty() ? dao.listar() : dao.buscar(filtro);
            for (Producto p : lista) {
                mostrados.add(p);
                modeloTabla.addRow(new Object[]{
                    p.getIdProducto(), p.getNombreProducto(), p.getCategoriaProducto(),
                    String.format(Locale.US, "%.2f", p.getPrecio()), p.getStock()});
            }
        } catch (SQLException e) {
            errorBD(e);
        }
    }

    private void nuevoProducto() {
        JTextField codigo = new JTextField(20);
        JTextField nombre = new JTextField(20);
        JTextField categoria = new JTextField(20);
        JTextField precio = new JTextField(20);
        JTextField stock = new JTextField(20);

        while (pedirDatos("Nuevo producto", codigo, nombre, categoria, precio, stock)) {
            String error = validar(codigo.getText().trim(), nombre.getText().trim(),
                    categoria.getText().trim(), precio.getText(), stock.getText(), true);
            if (error != null) {
                aviso(error);
                continue; // vuelve a mostrar el formulario con lo que ya escribiste
            }
            try {
                dao.insertar(new Producto(
                        codigo.getText().trim(), nombre.getText().trim(), categoria.getText().trim(),
                        leerPrecio(precio.getText()), leerStock(stock.getText())));
            } catch (SQLException e) {
                errorBD(e);
                return;
            }
            cargarTabla(txtBuscar.getText().trim());
            return;
        }
    }

    private void editarProducto() {
        Producto p = seleccionado();
        if (p == null) {
            return;
        }
        JTextField codigo = new JTextField(p.getIdProducto(), 20);
        codigo.setEditable(false); // el código identifica al producto en la base de datos
        JTextField nombre = new JTextField(p.getNombreProducto(), 20);
        JTextField categoria = new JTextField(p.getCategoriaProducto(), 20);
        JTextField precio = new JTextField(String.format(Locale.US, "%.2f", p.getPrecio()), 20);
        JTextField stock = new JTextField(String.valueOf(p.getStock()), 20);

        while (pedirDatos("Editar producto", codigo, nombre, categoria, precio, stock)) {
            String error = validar(codigo.getText().trim(), nombre.getText().trim(),
                    categoria.getText().trim(), precio.getText(), stock.getText(), false);
            if (error != null) {
                aviso(error);
                continue;
            }
            try {
                dao.actualizar(new Producto(
                        p.getIdProducto(), nombre.getText().trim(), categoria.getText().trim(),
                        leerPrecio(precio.getText()), leerStock(stock.getText())));
            } catch (SQLException e) {
                errorBD(e);
                return;
            }
            cargarTabla(txtBuscar.getText().trim());
            return;
        }
    }

    private void eliminarProducto() {
        Producto p = seleccionado();
        if (p == null) {
            return;
        }
        try {
            int ventas = 0;
            for (Venta v : db.getVentas()) {
                for (Producto vp : v.getProductos()) {
                    if (vp.getIdProducto().equalsIgnoreCase(p.getIdProducto())) {
                        ventas++;
                        break;
                    }
                }
            }
            if (ventas > 0 || dao.tieneFacturas(p.getIdProducto())) {
                aviso("No se puede eliminar " + p.getNombreProducto()
                        + ": ya aparece en ventas o facturas.");
                return;
            }
            int respuesta = JOptionPane.showConfirmDialog(this,
                    "¿Eliminar el producto " + p.getNombreProducto() + " (código " + p.getIdProducto() + ")?",
                    "Eliminar producto", JOptionPane.YES_NO_OPTION);
            if (respuesta == JOptionPane.YES_OPTION) {
                dao.eliminar(p.getIdProducto());
                cargarTabla(txtBuscar.getText().trim());
            }
        } catch (SQLException e) {
            errorBD(e);
        }
    }

    private boolean pedirDatos(String titulo, JTextField codigo, JTextField nombre,
            JTextField categoria, JTextField precio, JTextField stock) {
        JPanel panel = new JPanel(new GridLayout(5, 2, 6, 6));
        panel.add(new JLabel("Código:"));
        panel.add(codigo);
        panel.add(new JLabel("Nombre:"));
        panel.add(nombre);
        panel.add(new JLabel("Categoría:"));
        panel.add(categoria);
        panel.add(new JLabel("Precio (Q):"));
        panel.add(precio);
        panel.add(new JLabel("Stock:"));
        panel.add(stock);
        return JOptionPane.showConfirmDialog(this, panel, titulo,
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE) == JOptionPane.OK_OPTION;
    }

    private String validar(String codigo, String nombre, String categoria,
            String precioTxt, String stockTxt, boolean esNuevo) {
        if (codigo.isEmpty()) {
            return "El código es obligatorio.";
        }
        if (nombre.isEmpty()) {
            return "El nombre es obligatorio.";
        }
        if (categoria.isEmpty()) {
            return "La categoría es obligatoria.";
        }
        Double precio = leerPrecio(precioTxt);
        if (precio == null || precio.isNaN() || precio.isInfinite() || precio < 0) {
            return "El precio debe ser un número mayor o igual a 0.";
        }
        Integer stock = leerStock(stockTxt);
        if (stock == null || stock < 0) {
            return "El stock debe ser un número entero mayor o igual a 0.";
        }
        if (esNuevo) {
            try {
                if (dao.existeCodigo(codigo)) {
                    return "Ya existe un producto con ese código.";
                }
            } catch (SQLException e) {
                return "No se pudo verificar el código: " + e.getMessage();
            }
        }
        return null;
    }

    private Double leerPrecio(String texto) {
        try {
            return Double.parseDouble(texto.trim().replace(',', '.'));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Integer leerStock(String texto) {
        try {
            return Integer.parseInt(texto.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Producto seleccionado() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= mostrados.size()) {
            aviso("Selecciona un producto de la tabla.");
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