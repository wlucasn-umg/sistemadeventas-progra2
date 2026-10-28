package sistemaventas.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.DefaultTableModel;
import sistemaventas.datos.ClienteDAO;
import sistemaventas.datos.ProductoDAO;
import sistemaventas.datos.VendedorDAO;
import sistemaventas.datos.VentaDAO;
import sistemaventas.modelo.Cliente;
import sistemaventas.modelo.MetodoPago;
import sistemaventas.modelo.Producto;
import sistemaventas.modelo.Vendedor;
import sistemaventas.modelo.Venta;

/**
 * Formulario de facturación como ventana interna del MDI.
 * Clientes, vendedores y productos salen de MySQL; la venta se guarda ya facturada
 * en una sola transacción (VentaDAO), que también descuenta el stock.
 */
public class FrmFacturacion extends JInternalFrame {

    private static final double TASA_IVA = 0.12;
    private static final String NOMBRE_EMPRESA = "Mi Tienda Guatemala";

    private final ClienteDAO clienteDAO = new ClienteDAO();
    private final VendedorDAO vendedorDAO = new VendedorDAO();
    private final ProductoDAO productoDAO = new ProductoDAO();
    private final VentaDAO ventaDAO = new VentaDAO();

    // Listas leídas de MySQL (misma posición que los combos)
    private List<Cliente> clientes = new ArrayList<>();
    private List<Vendedor> vendedores = new ArrayList<>();
    private List<Producto> productos = new ArrayList<>();
    private final ArrayList<Producto> productosAgregados = new ArrayList<>();

    private final JComboBox<String> cbCliente = new JComboBox<>();
    private final JComboBox<String> cbVendedor = new JComboBox<>();
    private final JComboBox<MetodoPago> cbMetodoPago = new JComboBox<>(MetodoPago.values());
    private final JComboBox<String> cbProducto = new JComboBox<>();
    private final JTextField txtCantidad = new JTextField(6);
    private final JButton btnAgregar = new JButton("Agregar producto");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"Producto", "Precio", "Cantidad", "Subtotal"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);

    private final JTextField txtSubtotal = new JTextField(10);
    private final JTextField txtIva = new JTextField(10);
    private final JTextField txtTotal = new JTextField(10);
    private final JButton btnGenerar = new JButton("Generar factura");

    public FrmFacturacion() {
        super("Facturación", true, true, true, true);
        setSize(820, 560);
        construirPantalla();
        llenarCombos();
        // Cada vez que vuelves a esta ventana, se refrescan los combos
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                llenarCombos();
            }
        });
    }

    private void construirPantalla() {
        // --- Datos de la venta ---
        JPanel panelDatos = new JPanel(new GridBagLayout());
        panelDatos.setBorder(BorderFactory.createTitledBorder("Datos de la venta"));
        GridBagConstraints g = new GridBagConstraints();
        g.insets = new Insets(4, 6, 4, 6);
        g.anchor = GridBagConstraints.WEST;
        g.fill = GridBagConstraints.HORIZONTAL;
        agregarFila(panelDatos, g, 0, "Cliente:", cbCliente);
        agregarFila(panelDatos, g, 1, "Vendedor:", cbVendedor);
        agregarFila(panelDatos, g, 2, "Método de pago:", cbMetodoPago);

        // --- Agregar productos ---
        cbProducto.setPrototypeDisplayValue("Nombre de producto largo");
        JPanel panelProducto = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panelProducto.setBorder(BorderFactory.createTitledBorder("Productos"));
        panelProducto.add(new JLabel("Producto:"));
        panelProducto.add(cbProducto);
        panelProducto.add(new JLabel("Cantidad:"));
        panelProducto.add(txtCantidad);
        panelProducto.add(btnAgregar);

        JPanel norte = new JPanel(new BorderLayout());
        norte.add(panelDatos, BorderLayout.NORTH);
        norte.add(panelProducto, BorderLayout.CENTER);

        // --- Totales ---
        txtSubtotal.setEditable(false);
        txtIva.setEditable(false);
        txtTotal.setEditable(false);
        JPanel panelTotales = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        panelTotales.add(new JLabel("Subtotal:"));
        panelTotales.add(txtSubtotal);
        panelTotales.add(new JLabel("IVA (12%):"));
        panelTotales.add(txtIva);
        panelTotales.add(new JLabel("Total:"));
        panelTotales.add(txtTotal);
        panelTotales.add(btnGenerar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(norte, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(tabla), BorderLayout.CENTER);
        getContentPane().add(panelTotales, BorderLayout.SOUTH);

        btnAgregar.addActionListener(e -> agregarProducto());
        btnGenerar.addActionListener(e -> generarFactura());
    }

    private void agregarFila(JPanel panel, GridBagConstraints g, int fila, String texto, JComponent campo) {
        g.gridy = fila;
        g.gridx = 0;
        g.weightx = 0;
        panel.add(new JLabel(texto), g);
        g.gridx = 1;
        g.weightx = 1;
        panel.add(campo, g);
    }

    /** Lee clientes, vendedores y productos de MySQL y rellena los combos. */
    private void llenarCombos() {
        try {
            clientes = clienteDAO.listar();
            vendedores = vendedorDAO.listar();
            productos = productoDAO.listar();
        } catch (SQLException ex) {
            errorBD("No se pudieron cargar clientes, vendedores y productos.", ex);
            return;
        }

        List<String> nombres = new ArrayList<>();
        for (Cliente c : clientes) {
            nombres.add(c.getNombre());
        }
        recargar(cbCliente, nombres);

        nombres = new ArrayList<>();
        for (Vendedor v : vendedores) {
            nombres.add(v.getNombre());
        }
        recargar(cbVendedor, nombres);

        nombres = new ArrayList<>();
        for (Producto p : productos) {
            nombres.add(p.getNombreProducto());
        }
        recargar(cbProducto, nombres);
    }

    private void recargar(JComboBox<String> combo, List<String> nombres) {
        int seleccion = combo.getSelectedIndex();
        combo.removeAllItems();
        for (String n : nombres) {
            combo.addItem(n);
        }
        if (seleccion >= 0 && seleccion < combo.getItemCount()) {
            combo.setSelectedIndex(seleccion);
        }
    }

    private void agregarProducto() {
        if (productos.isEmpty()) {
            aviso("No hay productos registrados todavía.");
            return;
        }
        int idx = cbProducto.getSelectedIndex();
        if (idx < 0 || idx >= productos.size()) {
            aviso("Selecciona un producto.");
            return;
        }
        int cantidad;
        try {
            cantidad = Integer.parseInt(txtCantidad.getText().trim());
            if (cantidad <= 0) {
                throw new NumberFormatException();
            }
        } catch (NumberFormatException ex) {
            aviso("Ingresa una cantidad válida (número entero mayor que 0).");
            return;
        }

        Producto p = productos.get(idx);
        // Revisa el stock contando lo que ya llevas de ese mismo producto
        int yaAgregadas = 0;
        for (Producto x : productosAgregados) {
            if (x.getIdProducto().equals(p.getIdProducto())) {
                yaAgregadas++;
            }
        }
        int disponibles = p.getStock() - yaAgregadas;
        if (cantidad > disponibles) {
            aviso("Stock insuficiente de " + p.getNombreProducto()
                    + ". Disponible: " + Math.max(disponibles, 0) + ".");
            return;
        }

        for (int i = 0; i < cantidad; i++) {
            productosAgregados.add(p);
        }
        modeloTabla.addRow(new Object[]{p.getNombreProducto(), p.getPrecio(), cantidad, p.getPrecio() * cantidad});
        actualizarTotales();
        txtCantidad.setText("");
    }

    private void actualizarTotales() {
        double subtotal = 0;
        for (Producto p : productosAgregados) {
            subtotal += p.getPrecio();
        }
        double iva = subtotal * TASA_IVA;
        double total = subtotal + iva;
        txtSubtotal.setText(String.format(Locale.US, "%.2f", subtotal));
        txtIva.setText(String.format(Locale.US, "%.2f", iva));
        txtTotal.setText(String.format(Locale.US, "%.2f", total));
    }

    private void generarFactura() {
        if (clientes.isEmpty() || vendedores.isEmpty()) {
            aviso("Faltan clientes o vendedores registrados.");
            return;
        }
        int idxC = cbCliente.getSelectedIndex();
        int idxV = cbVendedor.getSelectedIndex();
        if (idxC < 0 || idxV < 0 || idxC >= clientes.size() || idxV >= vendedores.size()) {
            aviso("Selecciona cliente y vendedor.");
            return;
        }
        if (productosAgregados.isEmpty()) {
            aviso("Agrega al menos un producto.");
            return;
        }

        Cliente cliente = clientes.get(idxC);
        Vendedor vendedor = vendedores.get(idxV);
        MetodoPago metodoPago = (MetodoPago) cbMetodoPago.getSelectedItem();

        try {
            int numero = ventaDAO.siguienteId();
            String idVenta = "V" + numero;
            Venta venta = new Venta(idVenta, vendedor.getUsuario().getNombreUsuario(),
                    metodoPago, vendedor, cliente);
            for (Producto p : productosAgregados) {
                if (!venta.agregarProducto(p)) {
                    aviso("Stock insuficiente de " + p.getNombreProducto() + ".");
                    return;
                }
            }
            venta.registrarVenta();
            venta.generarFactura("F-" + idVenta, NOMBRE_EMPRESA);
            if (venta.getFactura() == null) {
                aviso("No se pudo generar la factura. Revisa el stock de los productos.");
                return;
            }

            // Guarda venta, factura, detalle, historial y descuenta el stock en MySQL
            ventaDAO.guardarFacturada(venta, numero);

            JOptionPane.showMessageDialog(this,
                    "Factura F-" + idVenta + " generada.\nTotal con IVA: Q"
                    + String.format(Locale.US, "%.2f", venta.getFactura().getTotal()),
                    "Factura generada", JOptionPane.INFORMATION_MESSAGE);
            limpiarPantalla();
            llenarCombos(); // el stock cambió: se vuelve a leer de MySQL
        } catch (SQLException ex) {
            errorBD("No se pudo guardar la venta. No se descontó stock.", ex);
        }
    }

    private void limpiarPantalla() {
        productosAgregados.clear();
        modeloTabla.setRowCount(0);
        txtCantidad.setText("");
        txtSubtotal.setText("");
        txtIva.setText("");
        txtTotal.setText("");
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.INFORMATION_MESSAGE);
    }

    private void errorBD(String mensaje, SQLException ex) {
        JOptionPane.showMessageDialog(this, mensaje + "\n" + ex.getMessage(),
                "Error de base de datos", JOptionPane.ERROR_MESSAGE);
    }
}