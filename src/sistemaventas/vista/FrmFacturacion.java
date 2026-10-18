package sistemaventas.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
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
import sistemaventas.datos.BaseDatosMemoria;
import sistemaventas.modelo.Cliente;
import sistemaventas.modelo.MetodoPago;
import sistemaventas.modelo.Producto;
import sistemaventas.modelo.Vendedor;
import sistemaventas.modelo.Venta;

/**
 * Formulario de facturación como ventana interna del MDI.
 * Lee y escribe siempre en BaseDatosMemoria (Singleton).
 */
public class FrmFacturacion extends JInternalFrame {

    private static final double TASA_IVA = 0.12;
    private static final String NOMBRE_EMPRESA = "Mi Tienda Guatemala";

    private final BaseDatosMemoria db = BaseDatosMemoria.getInstancia();
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

    private void llenarCombos() {
        List<String> clientes = new ArrayList<>();
        for (Cliente c : db.getClientes()) {
            clientes.add(c.getNombre());
        }
        recargar(cbCliente, clientes);

        List<String> vendedores = new ArrayList<>();
        for (Vendedor v : db.getVendedores()) {
            vendedores.add(v.getNombre());
        }
        recargar(cbVendedor, vendedores);

        List<String> productos = new ArrayList<>();
        for (Producto p : db.getInventario().getProductos()) {
            productos.add(p.getNombreProducto());
        }
        recargar(cbProducto, productos);
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
        List<Producto> inventario = db.getInventario().getProductos();
        if (inventario.isEmpty()) {
            aviso("No hay productos registrados todavía.");
            return;
        }
        int idx = cbProducto.getSelectedIndex();
        if (idx < 0 || idx >= inventario.size()) {
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

        Producto p = inventario.get(idx);
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
        if (db.getClientes().isEmpty() || db.getVendedores().isEmpty()) {
            aviso("Faltan clientes o vendedores registrados.");
            return;
        }
        int idxC = cbCliente.getSelectedIndex();
        int idxV = cbVendedor.getSelectedIndex();
        if (idxC < 0 || idxV < 0) {
            aviso("Selecciona cliente y vendedor.");
            return;
        }
        if (productosAgregados.isEmpty()) {
            aviso("Agrega al menos un producto.");
            return;
        }

        Cliente cliente = db.getClientes().get(idxC);
        Vendedor vendedor = db.getVendedores().get(idxV);
        MetodoPago metodoPago = (MetodoPago) cbMetodoPago.getSelectedItem();

        String idVenta = siguienteIdVenta();
        Venta venta = new Venta(idVenta, vendedor.getUsuario().getNombreUsuario(),
                metodoPago, vendedor, cliente);
        for (Producto p : productosAgregados) {
            venta.agregarProducto(p);
        }
        venta.registrarVenta();
        venta.generarFactura("F-" + idVenta, NOMBRE_EMPRESA);
        db.getVentas().add(venta);

        JOptionPane.showMessageDialog(this,
                "Factura F-" + idVenta + " generada.\nTotal con IVA: Q"
                + String.format(Locale.US, "%.2f", venta.getFactura().getTotal()),
                "Factura generada", JOptionPane.INFORMATION_MESSAGE);
        limpiarPantalla();
    }

    private String siguienteIdVenta() {
        int n = db.getVentas().size() + 1;
        while (existeVenta("V" + n)) {
            n++;
        }
        return "V" + n;
    }

    private boolean existeVenta(String id) {
        for (Venta v : db.getVentas()) {
            if (v.getIdVenta().equals(id)) {
                return true;
            }
        }
        return false;
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
}