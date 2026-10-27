package sistemaventas.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import sistemaventas.modelo.Producto;

/**
 * Acceso a datos de productos (tabla producto, con su categoría).
 * Cada método abre y cierra su propia conexión.
 */
public class ProductoDAO {

    private static final String SELECT_BASE =
            "SELECT p.codigo, p.nombre, c.nombre AS categoria, p.precio, p.stock "
            + "FROM producto p JOIN categoria c ON c.id_categoria = p.id_categoria ";

    public List<Producto> listar() throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(SELECT_BASE + "ORDER BY p.codigo");
             ResultSet rs = ps.executeQuery()) {
            return leer(rs);
        }
    }

    public List<Producto> buscar(String texto) throws SQLException {
        String sql = SELECT_BASE
                + "WHERE p.codigo LIKE ? OR p.nombre LIKE ? OR c.nombre LIKE ? "
                + "ORDER BY p.codigo";
        String patron = "%" + texto + "%";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            ps.setString(3, patron);
            try (ResultSet rs = ps.executeQuery()) {
                return leer(rs);
            }
        }
    }

    public boolean existeCodigo(String codigo) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT 1 FROM producto WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** True si el producto ya aparece en el detalle de alguna factura. */
    public boolean tieneFacturas(String codigo) throws SQLException {
        String sql = "SELECT 1 FROM detalle_factura d "
                + "JOIN producto p ON p.id_producto = d.id_producto "
                + "WHERE p.codigo = ? LIMIT 1";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insertar(Producto p) throws SQLException {
        try (Connection cn = ConexionBD.getConexion()) {
            int idCategoria = obtenerOCrearCategoria(cn, p.getCategoriaProducto());
            int idInventario = obtenerInventario(cn);
            String sql = "INSERT INTO producto "
                    + "(codigo, nombre, precio, stock, id_categoria, id_inventario) "
                    + "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, p.getIdProducto());
                ps.setString(2, p.getNombreProducto());
                ps.setDouble(3, p.getPrecio());
                ps.setInt(4, p.getStock());
                ps.setInt(5, idCategoria);
                ps.setInt(6, idInventario);
                ps.executeUpdate();
            }
        }
    }

    public void actualizar(Producto p) throws SQLException {
        try (Connection cn = ConexionBD.getConexion()) {
            int idCategoria = obtenerOCrearCategoria(cn, p.getCategoriaProducto());
            String sql = "UPDATE producto SET nombre = ?, precio = ?, stock = ?, "
                    + "id_categoria = ? WHERE codigo = ?";
            try (PreparedStatement ps = cn.prepareStatement(sql)) {
                ps.setString(1, p.getNombreProducto());
                ps.setDouble(2, p.getPrecio());
                ps.setInt(3, p.getStock());
                ps.setInt(4, idCategoria);
                ps.setString(5, p.getIdProducto());
                ps.executeUpdate();
            }
        }
    }

    public void eliminar(String codigo) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                     "DELETE FROM producto WHERE codigo = ?")) {
            ps.setString(1, codigo);
            ps.executeUpdate();
        }
    }

    // ---------- métodos de apoyo ----------

    private List<Producto> leer(ResultSet rs) throws SQLException {
        List<Producto> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(new Producto(
                    rs.getString("codigo"),
                    rs.getString("nombre"),
                    rs.getString("categoria"),
                    rs.getDouble("precio"),
                    rs.getInt("stock")));
        }
        return lista;
    }

    /** Busca la categoría por nombre; si no existe, la crea. */
    private int obtenerOCrearCategoria(Connection cn, String nombre) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id_categoria FROM categoria WHERE nombre = ?")) {
            ps.setString(1, nombre);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
        }
        try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO categoria (nombre) VALUES (?)",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, nombre);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    /** Devuelve el id de la única bodega; si no existe, la crea. */
    private int obtenerInventario(Connection cn) throws SQLException {
        try (PreparedStatement ps = cn.prepareStatement(
                "SELECT id_inventario FROM inventario ORDER BY id_inventario LIMIT 1");
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        }
        try (PreparedStatement ps = cn.prepareStatement(
                "INSERT INTO inventario (codigo) VALUES ('INV001')",
                Statement.RETURN_GENERATED_KEYS)) {
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    // Solo para probar; se puede quitar después
    public static void main(String[] args) {
        try {
            ProductoDAO dao = new ProductoDAO();
            System.out.println("--- Todos los productos ---");
            for (Producto p : dao.listar()) {
                System.out.println(p.getIdProducto() + " | " + p.getNombreProducto()
                        + " | " + p.getCategoriaProducto() + " | Q" + p.getPrecio()
                        + " | stock " + p.getStock());
            }
            System.out.println("--- Buscar 'per' ---");
            for (Producto p : dao.buscar("per")) {
                System.out.println(p.getIdProducto() + " | " + p.getNombreProducto());
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}