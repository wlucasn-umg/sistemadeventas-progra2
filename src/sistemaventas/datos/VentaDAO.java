package sistemaventas.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import sistemaventas.modelo.Cliente;
import sistemaventas.modelo.DetalleFactura;
import sistemaventas.modelo.EstadoVenta;
import sistemaventas.modelo.Factura;
import sistemaventas.modelo.MetodoPago;
import sistemaventas.modelo.Producto;
import sistemaventas.modelo.SeguimientoVenta;
import sistemaventas.modelo.Vendedor;
import sistemaventas.modelo.Venta;

/**
 * Acceso a datos de ventas: tablas venta, factura, detalle_factura y seguimiento_venta.
 * Una venta se guarda ya facturada, todo en una sola transacción (si algo falla,
 * no se guarda nada y el stock no cambia).
 * El id de una venta se ve como "V" + número (V1, V2...).
 */
public class VentaDAO {

    /** Producto agrupado: la misma unidad repetida varias veces se guarda como una sola línea. */
    private static class Linea {
        String codigo;
        String nombre;
        int cantidad;
        double precio;
    }

    /** Número que irá en la próxima venta (el id de MySQL, que arma "V" + número). */
    public int siguienteId() throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT COALESCE(MAX(id_venta), 0) + 1 FROM venta");
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt(1);
        }
    }

    /**
     * Guarda la venta ya facturada: venta, factura, detalle, historial y descuento de stock.
     * numero es el número que se usó para armar el id de la venta ("V" + numero).
     */
    public void guardarFacturada(Venta v, int numero) throws SQLException {
        Factura f = v.getFactura();
        if (f == null) {
            throw new SQLException("La venta no tiene factura.");
        }
        Map<String, Linea> lineas = agrupar(f);

        try (Connection cn = ConexionBD.getConexion()) {
            cn.setAutoCommit(false);
            try {
                // 1. Descontar el stock; si no alcanza, se cancela todo
                for (Linea l : lineas.values()) {
                    try (PreparedStatement ps = cn.prepareStatement(
                            "UPDATE producto SET stock = stock - ? WHERE codigo = ? AND stock >= ?")) {
                        ps.setInt(1, l.cantidad);
                        ps.setString(2, l.codigo);
                        ps.setInt(3, l.cantidad);
                        if (ps.executeUpdate() == 0) {
                            throw new SQLException("Stock insuficiente de " + l.nombre
                                    + " (o el producto ya no existe).");
                        }
                    }
                }

                int idVendedor = buscarId(cn, "SELECT id_vendedor FROM vendedor WHERE codigo = ?",
                        v.getVendedor().getIdVendedor(), "El vendedor ya no existe.");
                int idCliente = buscarId(cn, "SELECT id_cliente FROM cliente WHERE nit = ? LIMIT 1",
                        v.getCliente().getNit(), "El cliente ya no existe.");

                // 2. La venta
                try (PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO venta (id_venta, fecha_venta, usuario_emision, total, metodo_pago, "
                        + "estado_venta, stock_descontado, id_vendedor, id_cliente) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)")) {
                    ps.setInt(1, numero);
                    ps.setTimestamp(2, new Timestamp(v.getFechaVenta().getTime()));
                    ps.setString(3, v.getUsuarioEmision());
                    ps.setDouble(4, v.getTotal());
                    ps.setString(5, v.getMetodoPago().name());
                    ps.setString(6, v.getEstadoVenta().name());
                    ps.setBoolean(7, v.isStockDescontado());
                    ps.setInt(8, idVendedor);
                    ps.setInt(9, idCliente);
                    ps.executeUpdate();
                }

                // 3. La factura
                int idFactura;
                try (PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO factura (id_venta, nombre_empresa, tasa_iva, subtotal, iva, total) "
                        + "VALUES (?, ?, 12.00, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                    ps.setInt(1, numero);
                    ps.setString(2, f.getNombreEmpresa());
                    ps.setDouble(3, f.getSubtotal());
                    ps.setDouble(4, f.getIva());
                    ps.setDouble(5, f.getTotal());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idFactura = rs.getInt(1);
                    }
                }

                // 4. El detalle de la factura
                for (Linea l : lineas.values()) {
                    int idProducto = buscarId(cn, "SELECT id_producto FROM producto WHERE codigo = ?",
                            l.codigo, "El producto " + l.nombre + " ya no existe.");
                    try (PreparedStatement ps = cn.prepareStatement(
                            "INSERT INTO detalle_factura (id_factura, id_producto, cantidad, "
                            + "precio_unitario, subtotal) VALUES (?, ?, ?, ?, ?)")) {
                        ps.setInt(1, idFactura);
                        ps.setInt(2, idProducto);
                        ps.setInt(3, l.cantidad);
                        ps.setDouble(4, l.precio);
                        ps.setDouble(5, l.cantidad * l.precio);
                        ps.executeUpdate();
                    }
                }

                // 5. El historial de seguimiento
                for (SeguimientoVenta s : v.getHistorial()) {
                    insertarSeguimiento(cn, numero, s);
                }
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        }
    }

    /** Carga todas las ventas con su factura, productos e historial. */
    public List<Venta> listar() throws SQLException {
        Map<Integer, Venta> ventas = new LinkedHashMap<>();
        try (Connection cn = ConexionBD.getConexion()) {
            String sqlVentas = "SELECT ve.id_venta, ve.fecha_venta, ve.usuario_emision, "
                    + "ve.metodo_pago, ve.estado_venta, ve.stock_descontado, "
                    + "v.codigo AS v_codigo, v.nombre AS v_nombre, v.telefono AS v_telefono, "
                    + "v.puesto AS v_puesto, COALESCE(u.nombre_usuario, '') AS v_usuario, "
                    + "c.nit, c.nombre AS c_nombre, c.telefono AS c_telefono, c.direccion_envio "
                    + "FROM venta ve "
                    + "JOIN vendedor v ON v.id_vendedor = ve.id_vendedor "
                    + "LEFT JOIN usuario_vendedor u ON u.id_vendedor = v.id_vendedor "
                    + "JOIN cliente c ON c.id_cliente = ve.id_cliente "
                    + "ORDER BY ve.id_venta";
            try (PreparedStatement ps = cn.prepareStatement(sqlVentas);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Vendedor vendedor = new Vendedor(rs.getString("v_nombre"), rs.getString("v_telefono"),
                            rs.getString("v_codigo"), rs.getString("v_puesto"),
                            rs.getString("v_usuario"), "");
                    Cliente cliente = new Cliente(rs.getString("c_nombre"), rs.getString("c_telefono"),
                            rs.getString("nit"), rs.getString("direccion_envio"));
                    int id = rs.getInt("id_venta");
                    ventas.put(id, new Venta("V" + id,
                            new Date(rs.getTimestamp("fecha_venta").getTime()),
                            rs.getString("usuario_emision"),
                            MetodoPago.valueOf(rs.getString("metodo_pago")),
                            EstadoVenta.valueOf(rs.getString("estado_venta")),
                            rs.getBoolean("stock_descontado"), vendedor, cliente));
                }
            }

            // Facturas y productos (con el precio al que se vendieron)
            String sqlDetalle = "SELECT f.id_venta, f.nombre_empresa, d.cantidad, d.precio_unitario, "
                    + "p.codigo, p.nombre, cat.nombre AS categoria, p.stock "
                    + "FROM detalle_factura d "
                    + "JOIN factura f ON f.id_factura = d.id_factura "
                    + "JOIN producto p ON p.id_producto = d.id_producto "
                    + "JOIN categoria cat ON cat.id_categoria = p.id_categoria "
                    + "ORDER BY f.id_venta, d.id_detalle";
            Map<Integer, Integer> contadores = new HashMap<>();
            try (PreparedStatement ps = cn.prepareStatement(sqlDetalle);
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idVenta = rs.getInt("id_venta");
                    Venta v = ventas.get(idVenta);
                    if (v == null) {
                        continue;
                    }
                    if (v.getFactura() == null) {
                        v.setFactura(new Factura("F-V" + idVenta, rs.getString("nombre_empresa")));
                    }
                    Producto p = new Producto(rs.getString("codigo"), rs.getString("nombre"),
                            rs.getString("categoria"), rs.getDouble("precio_unitario"),
                            rs.getInt("stock"));
                    int cantidad = rs.getInt("cantidad");
                    for (int i = 0; i < cantidad; i++) {
                        v.getProductos().add(p);
                    }
                    int n = contadores.merge(idVenta, 1, Integer::sum);
                    v.getFactura().agregarDetalle(new DetalleFactura("D" + n, cantidad, p));
                }
            }
            for (Venta v : ventas.values()) {
                v.calcularTotal();
            }

            // Historial de seguimiento
            Map<Integer, Integer> seguimientos = new HashMap<>();
            try (PreparedStatement ps = cn.prepareStatement(
                    "SELECT id_venta, estado, fecha_hora, comentario FROM seguimiento_venta "
                    + "ORDER BY id_venta, id_seguimiento");
                 ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    int idVenta = rs.getInt("id_venta");
                    Venta v = ventas.get(idVenta);
                    if (v == null) {
                        continue;
                    }
                    int n = seguimientos.merge(idVenta, 1, Integer::sum);
                    v.getHistorial().add(new SeguimientoVenta("S" + n,
                            EstadoVenta.valueOf(rs.getString("estado")),
                            new Date(rs.getTimestamp("fecha_hora").getTime()),
                            rs.getString("comentario")));
                }
            }
        }
        return new ArrayList<>(ventas.values());
    }

    /**
     * Guarda el cambio de estado que la venta ya tiene en memoria (entregada o cancelada):
     * actualiza la venta y agrega el último paso del historial.
     * Si devolverStock es true, regresa las unidades al inventario (cancelación de una venta facturada).
     */
    public void actualizarEstado(Venta v, boolean devolverStock) throws SQLException {
        int numero = numero(v.getIdVenta());
        try (Connection cn = ConexionBD.getConexion()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE venta SET estado_venta = ?, stock_descontado = ? WHERE id_venta = ?")) {
                    ps.setString(1, v.getEstadoVenta().name());
                    ps.setBoolean(2, v.isStockDescontado());
                    ps.setInt(3, numero);
                    ps.executeUpdate();
                }
                if (!v.getHistorial().isEmpty()) {
                    insertarSeguimiento(cn, numero, v.getHistorial().get(v.getHistorial().size() - 1));
                }
                if (devolverStock) {
                    Map<String, Integer> unidades = new LinkedHashMap<>();
                    for (Producto p : v.getProductos()) {
                        unidades.merge(p.getIdProducto(), 1, Integer::sum);
                    }
                    for (Map.Entry<String, Integer> e : unidades.entrySet()) {
                        try (PreparedStatement ps = cn.prepareStatement(
                                "UPDATE producto SET stock = stock + ? WHERE codigo = ?")) {
                            ps.setInt(1, e.getValue());
                            ps.setString(2, e.getKey());
                            ps.executeUpdate();
                        }
                    }
                }
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        }
    }

    // ---------- métodos de apoyo ----------

    private Map<String, Linea> agrupar(Factura f) {
        Map<String, Linea> lineas = new LinkedHashMap<>();
        for (DetalleFactura d : f.getDetalles()) {
            Producto p = d.getProducto();
            Linea l = lineas.get(p.getIdProducto());
            if (l == null) {
                l = new Linea();
                l.codigo = p.getIdProducto();
                l.nombre = p.getNombreProducto();
                l.precio = d.getPrecioUnitario();
                lineas.put(l.codigo, l);
            }
            l.cantidad += d.getCantidad();
        }
        return lineas;
    }

    private int buscarId(Connection cn, String sql, String valor, String mensajeSiNoExiste)
            throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, valor);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) {
                    throw new SQLException(mensajeSiNoExiste);
                }
                return rs.getInt(1);
            }
        }
    }

    private void insertarSeguimiento(Connection cn, int numeroVenta, SeguimientoVenta s)
            throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO seguimiento_venta (id_venta, estado, fecha_hora, comentario) "
                + "VALUES (?, ?, ?, ?)")) {
            ps.setInt(1, numeroVenta);
            ps.setString(2, s.getEstado().name());
            ps.setTimestamp(3, new Timestamp(s.getFechaHora().getTime()));
            ps.setString(4, s.getComentario());
            ps.executeUpdate();
        }
    }

    private int numero(String idVenta) {
        return Integer.parseInt(idVenta.substring(1));
    }
}