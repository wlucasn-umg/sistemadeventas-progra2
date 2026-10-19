package sistemaventas.vista;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.table.DefaultTableModel;
import sistemaventas.datos.BaseDatosMemoria;
import sistemaventas.modelo.EstadoVenta;
import sistemaventas.modelo.Producto;
import sistemaventas.modelo.SeguimientoVenta;
import sistemaventas.modelo.Venta;

/**
 * Consulta de ventas como ventana interna del MDI.
 * Busca, muestra el detalle con el historial de seguimiento y cambia el estado.
 */
public class FrmVentas extends JInternalFrame {

    private final BaseDatosMemoria db = BaseDatosMemoria.getInstancia();
    // Ventas que se ven ahora en la tabla (misma posición que las filas)
    private final List<Venta> mostradas = new ArrayList<>();
    private final SimpleDateFormat formatoFecha = new SimpleDateFormat("dd/MM/yyyy HH:mm");

    private final JTextField txtBuscar = new JTextField(18);
    private final JComboBox<String> cbEstado = new JComboBox<>();
    private final JButton btnBuscar = new JButton("Buscar");
    private final JButton btnTodas = new JButton("Mostrar todas");
    private final JButton btnDetalle = new JButton("Ver detalle e historial");
    private final JButton btnEntregar = new JButton("Marcar como entregada");
    private final JButton btnCancelar = new JButton("Cancelar venta");

    private final DefaultTableModel modeloTabla = new DefaultTableModel(
            new Object[]{"ID", "Fecha", "Cliente", "Vendedor", "Pago", "Estado", "Subtotal", "Factura"}, 0) {
        @Override
        public boolean isCellEditable(int fila, int columna) {
            return false;
        }
    };
    private final JTable tabla = new JTable(modeloTabla);

    public FrmVentas() {
        super("Consulta de ventas", true, true, true, true);
        setSize(940, 480);

        cbEstado.addItem("Todos");
        for (EstadoVenta e : EstadoVenta.values()) {
            cbEstado.addItem(e.name());
        }

        construirPantalla();
        cargarTabla();
        // Al volver a esta ventana se refresca la tabla
        addInternalFrameListener(new InternalFrameAdapter() {
            @Override
            public void internalFrameActivated(InternalFrameEvent e) {
                cargarTabla();
            }
        });
    }

    private void construirPantalla() {
        JPanel norte = new JPanel(new FlowLayout(FlowLayout.LEFT));
        norte.add(new JLabel("Buscar (ID, cliente, vendedor o factura):"));
        norte.add(txtBuscar);
        norte.add(new JLabel("Estado:"));
        norte.add(cbEstado);
        norte.add(btnBuscar);
        norte.add(btnTodas);

        tabla.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

        JPanel sur = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sur.add(btnDetalle);
        sur.add(btnEntregar);
        sur.add(btnCancelar);

        getContentPane().setLayout(new BorderLayout());
        getContentPane().add(norte, BorderLayout.NORTH);
        getContentPane().add(new JScrollPane(tabla), BorderLayout.CENTER);
        getContentPane().add(sur, BorderLayout.SOUTH);

        btnBuscar.addActionListener(e -> cargarTabla());
        txtBuscar.addActionListener(e -> cargarTabla());
        cbEstado.addActionListener(e -> cargarTabla());
        btnTodas.addActionListener(e -> {
            txtBuscar.setText("");
            cbEstado.setSelectedIndex(0);
            cargarTabla();
        });
        btnDetalle.addActionListener(e -> verDetalle());
        btnEntregar.addActionListener(e -> marcarEntregada());
        btnCancelar.addActionListener(e -> cancelarVenta());

        // Doble clic en una fila abre el detalle
        tabla.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    verDetalle();
                }
            }
        });
    }

    private void cargarTabla() {
        String filtro = txtBuscar.getText().trim().toLowerCase();
        String estado = (String) cbEstado.getSelectedItem();
        boolean todosLosEstados = estado == null || estado.equals("Todos");

        modeloTabla.setRowCount(0);
        mostradas.clear();
        for (Venta v : db.getVentas()) {
            if (!todosLosEstados && !v.getEstadoVenta().name().equals(estado)) {
                continue;
            }
            String idFactura = v.getFactura() == null ? "-" : v.getFactura().getIdFactura();
            String texto = (v.getIdVenta() + " " + v.getCliente().getNombre() + " "
                    + v.getVendedor().getNombre() + " " + idFactura).toLowerCase();
            if (!filtro.isEmpty() && !texto.contains(filtro)) {
                continue;
            }
            mostradas.add(v);
            modeloTabla.addRow(new Object[]{
                v.getIdVenta(),
                formatoFecha.format(v.getFechaVenta()),
                v.getCliente().getNombre(),
                v.getVendedor().getNombre(),
                v.getMetodoPago(),
                v.getEstadoVenta(),
                String.format(Locale.US, "%.2f", v.getTotal()),
                idFactura});
        }
    }

    private void verDetalle() {
        Venta v = seleccionada();
        if (v == null) {
            return;
        }
        StringBuilder sb = new StringBuilder();
        sb.append("Venta: ").append(v.getIdVenta()).append("\n");
        sb.append("Fecha: ").append(formatoFecha.format(v.getFechaVenta())).append("\n");
        sb.append("Cliente: ").append(v.getCliente().getNombre())
                .append(" (NIT ").append(v.getCliente().getNit()).append(")\n");
        sb.append("Vendedor: ").append(v.getVendedor().getNombre()).append("\n");
        sb.append("Método de pago: ").append(v.getMetodoPago()).append("\n");
        sb.append("Estado actual: ").append(v.getEstadoVenta()).append("\n\n");

        sb.append("Productos:\n");
        Map<Producto, Integer> cantidades = new LinkedHashMap<>();
        for (Producto p : v.getProductos()) {
            cantidades.merge(p, 1, Integer::sum);
        }
        for (Map.Entry<Producto, Integer> e : cantidades.entrySet()) {
            Producto p = e.getKey();
            int cantidad = e.getValue();
            sb.append(String.format(Locale.US, "  %s  x%d  Q%.2f  =  Q%.2f",
                    p.getNombreProducto(), cantidad, p.getPrecio(), p.getPrecio() * cantidad)).append("\n");
        }

        sb.append(String.format(Locale.US, "\nSubtotal: Q%.2f\n", v.getTotal()));
        if (v.getFactura() != null) {
            double totalConIva = v.getFactura().getTotal();
            sb.append(String.format(Locale.US, "IVA: Q%.2f\n", totalConIva - v.getTotal()));
            sb.append(String.format(Locale.US, "Total con IVA: Q%.2f\n", totalConIva));
            sb.append("Factura: ").append(v.getFactura().getIdFactura()).append("\n");
        } else {
            sb.append("Todavía no tiene factura.\n");
        }

        sb.append("\nHistorial de seguimiento:\n");
        for (SeguimientoVenta s : v.getHistorial()) {
            sb.append("  ").append(formatoFecha.format(s.getFechaHora()))
                    .append("  -  ").append(s.getEstado());
            if (s.getComentario() != null && !s.getComentario().isEmpty()) {
                sb.append("  (").append(s.getComentario()).append(")");
            }
            sb.append("\n");
        }

        JTextArea area = new JTextArea(sb.toString(), 20, 52);
        area.setEditable(false);
        area.setCaretPosition(0);
        JOptionPane.showMessageDialog(this, new JScrollPane(area),
                "Detalle de la venta " + v.getIdVenta(), JOptionPane.PLAIN_MESSAGE);
    }

    private void marcarEntregada() {
        Venta v = seleccionada();
        if (v == null) {
            return;
        }
        if (!v.puedeEntregarse()) {
            aviso("Solo se puede marcar como entregada una venta FACTURADA.\nEstado actual: " + v.getEstadoVenta());
            return;
        }
        v.marcarEntregada();
        cargarTabla();
    }

    private void cancelarVenta() {
        Venta v = seleccionada();
        if (v == null) {
            return;
        }
        if (!v.puedeCancelarse()) {
            aviso("Esta venta ya no se puede cancelar.\nEstado actual: " + v.getEstadoVenta());
            return;
        }
        int respuesta = JOptionPane.showConfirmDialog(this,
                "¿Cancelar la venta " + v.getIdVenta() + "?",
                "Cancelar venta", JOptionPane.YES_NO_OPTION);
        if (respuesta == JOptionPane.YES_OPTION) {
            v.cancelarVenta();
            cargarTabla();
        }
    }

    private Venta seleccionada() {
        int fila = tabla.getSelectedRow();
        if (fila < 0 || fila >= mostradas.size()) {
            aviso("Selecciona una venta de la tabla.");
            return null;
        }
        return mostradas.get(fila);
    }

    private void aviso(String mensaje) {
        JOptionPane.showMessageDialog(this, mensaje, "Aviso", JOptionPane.INFORMATION_MESSAGE);
    }
}