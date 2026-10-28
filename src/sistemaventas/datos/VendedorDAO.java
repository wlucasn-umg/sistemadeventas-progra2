package sistemaventas.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import sistemaventas.modelo.Vendedor;

/**
 * Acceso a datos de vendedores (tablas vendedor y usuario_vendedor).
 * El código (V001, V002...) identifica al vendedor dentro de la aplicación.
 * La contraseña nunca se devuelve: al leer, el vendedor trae la clave vacía.
 */
public class VendedorDAO {

    private static final String SELECT_BASE =
            "SELECT v.codigo, v.nombre, v.telefono, v.puesto, "
            + "COALESCE(u.nombre_usuario, '') AS usuario "
            + "FROM vendedor v LEFT JOIN usuario_vendedor u ON u.id_vendedor = v.id_vendedor ";

    public List<Vendedor> listar() throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(SELECT_BASE + "ORDER BY v.codigo");
             ResultSet rs = ps.executeQuery()) {
            return leer(rs);
        }
    }

    public List<Vendedor> buscar(String texto) throws SQLException {
        String sql = SELECT_BASE
                + "WHERE v.codigo LIKE ? OR v.nombre LIKE ? OR v.puesto LIKE ? "
                + "OR u.nombre_usuario LIKE ? ORDER BY v.codigo";
        String patron = "%" + texto + "%";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            for (int i = 1; i <= 4; i++) {
                ps.setString(i, patron);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return leer(rs);
            }
        }
    }

    public boolean existeCodigo(String codigo) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT 1 FROM vendedor WHERE codigo = ?")) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** True si otro vendedor (distinto de codigoExcluir) ya usa ese nombre de usuario. */
    public boolean existeUsuario(String nombreUsuario, String codigoExcluir) throws SQLException {
        String sql = "SELECT 1 FROM usuario_vendedor u "
                + "JOIN vendedor v ON v.id_vendedor = u.id_vendedor "
                + "WHERE u.nombre_usuario = ? AND v.codigo <> ?";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nombreUsuario);
            ps.setString(2, codigoExcluir);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** True si el vendedor ya tiene ventas registradas en la base de datos. */
    public boolean tieneVentas(String codigo) throws SQLException {
        String sql = "SELECT 1 FROM venta ve JOIN vendedor v ON v.id_vendedor = ve.id_vendedor "
                + "WHERE v.codigo = ? LIMIT 1";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, codigo);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** Inserta el vendedor y su usuario en una sola transacción. */
    public void insertar(Vendedor v, String clave) throws SQLException {
        try (Connection cn = ConexionBD.getConexion()) {
            cn.setAutoCommit(false);
            try {
                int idVendedor;
                try (PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO vendedor (codigo, nombre, telefono, puesto) VALUES (?, ?, ?, ?)",
                        Statement.RETURN_GENERATED_KEYS)) {
                    ps.setString(1, v.getIdVendedor());
                    ps.setString(2, v.getNombre());
                    ps.setString(3, v.getTelefono());
                    ps.setString(4, v.getPuesto());
                    ps.executeUpdate();
                    try (ResultSet rs = ps.getGeneratedKeys()) {
                        rs.next();
                        idVendedor = rs.getInt(1);
                    }
                }
                try (PreparedStatement ps = cn.prepareStatement(
                        "INSERT INTO usuario_vendedor (id_vendedor, nombre_usuario, contrasena) "
                        + "VALUES (?, ?, ?)")) {
                    ps.setInt(1, idVendedor);
                    ps.setString(2, v.getUsuario().getNombreUsuario());
                    ps.setString(3, clave);
                    ps.executeUpdate();
                }
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        }
    }

    /**
     * Actualiza al vendedor que tenía el código original (el código también puede cambiar).
     * Si clave viene vacía, la contraseña actual no se modifica.
     */
    public void actualizar(String codigoOriginal, Vendedor v, String clave) throws SQLException {
        try (Connection cn = ConexionBD.getConexion()) {
            cn.setAutoCommit(false);
            try {
                try (PreparedStatement ps = cn.prepareStatement(
                        "UPDATE vendedor SET codigo = ?, nombre = ?, telefono = ?, puesto = ? "
                        + "WHERE codigo = ?")) {
                    ps.setString(1, v.getIdVendedor());
                    ps.setString(2, v.getNombre());
                    ps.setString(3, v.getTelefono());
                    ps.setString(4, v.getPuesto());
                    ps.setString(5, codigoOriginal);
                    ps.executeUpdate();
                }
                String sql = clave.isEmpty()
                        ? "UPDATE usuario_vendedor u JOIN vendedor v ON v.id_vendedor = u.id_vendedor "
                          + "SET u.nombre_usuario = ? WHERE v.codigo = ?"
                        : "UPDATE usuario_vendedor u JOIN vendedor v ON v.id_vendedor = u.id_vendedor "
                          + "SET u.nombre_usuario = ?, u.contrasena = ? WHERE v.codigo = ?";
                try (PreparedStatement ps = cn.prepareStatement(sql)) {
                    ps.setString(1, v.getUsuario().getNombreUsuario());
                    if (clave.isEmpty()) {
                        ps.setString(2, v.getIdVendedor());
                    } else {
                        ps.setString(2, clave);
                        ps.setString(3, v.getIdVendedor());
                    }
                    ps.executeUpdate();
                }
                cn.commit();
            } catch (SQLException e) {
                cn.rollback();
                throw e;
            }
        }
    }

    /** Elimina al vendedor; su usuario se borra solo (ON DELETE CASCADE). */
    public void eliminar(String codigo) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                     "DELETE FROM vendedor WHERE codigo = ?")) {
            ps.setString(1, codigo);
            ps.executeUpdate();
        }
    }

    private List<Vendedor> leer(ResultSet rs) throws SQLException {
        List<Vendedor> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(new Vendedor(
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("codigo"),
                    rs.getString("puesto"),
                    rs.getString("usuario"),
                    "")); // la contraseña no se lee de la base de datos
        }
        return lista;
    }

    // Solo para probar; se puede quitar después
    public static void main(String[] args) {
        try {
            VendedorDAO dao = new VendedorDAO();
            System.out.println("--- Todos los vendedores ---");
            for (Vendedor v : dao.listar()) {
                System.out.println(v.getIdVendedor() + " | " + v.getNombre() + " | "
                        + v.getTelefono() + " | " + v.getPuesto() + " | "
                        + v.getUsuario().getNombreUsuario());
            }
            System.out.println("--- Buscar 'sof' ---");
            for (Vendedor v : dao.buscar("sof")) {
                System.out.println(v.getIdVendedor() + " | " + v.getNombre());
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}