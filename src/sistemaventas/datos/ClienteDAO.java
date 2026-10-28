package sistemaventas.datos;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import sistemaventas.modelo.Cliente;

/**
 * Acceso a datos de clientes (tabla cliente).
 * El NIT identifica al cliente dentro de la aplicación.
 */
public class ClienteDAO {

    private static final String SELECT_BASE =
            "SELECT nit, nombre, telefono, direccion_envio FROM cliente ";

    public List<Cliente> listar() throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(SELECT_BASE + "ORDER BY nombre");
             ResultSet rs = ps.executeQuery()) {
            return leer(rs);
        }
    }

    public List<Cliente> buscar(String texto) throws SQLException {
        String sql = SELECT_BASE + "WHERE nit LIKE ? OR nombre LIKE ? ORDER BY nombre";
        String patron = "%" + texto + "%";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, patron);
            ps.setString(2, patron);
            try (ResultSet rs = ps.executeQuery()) {
                return leer(rs);
            }
        }
    }

    public boolean existeNit(String nit) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                     "SELECT 1 FROM cliente WHERE nit = ?")) {
            ps.setString(1, nit);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** True si el cliente ya tiene ventas registradas en la base de datos. */
    public boolean tieneVentas(String nit) throws SQLException {
        String sql = "SELECT 1 FROM venta v JOIN cliente c ON c.id_cliente = v.id_cliente "
                + "WHERE c.nit = ? LIMIT 1";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nit);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    public void insertar(Cliente c) throws SQLException {
        String sql = "INSERT INTO cliente (nombre, telefono, nit, direccion_envio) "
                + "VALUES (?, ?, ?, ?)";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getTelefono());
            ps.setString(3, c.getNit());
            ps.setString(4, c.getDireccionEnvio());
            ps.executeUpdate();
        }
    }

    /** Actualiza al cliente que tenía el NIT original (el NIT también puede cambiar). */
    public void actualizar(String nitOriginal, Cliente c) throws SQLException {
        String sql = "UPDATE cliente SET nombre = ?, telefono = ?, nit = ?, "
                + "direccion_envio = ? WHERE nit = ?";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, c.getTelefono());
            ps.setString(3, c.getNit());
            ps.setString(4, c.getDireccionEnvio());
            ps.setString(5, nitOriginal);
            ps.executeUpdate();
        }
    }

    public void eliminar(String nit) throws SQLException {
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(
                     "DELETE FROM cliente WHERE nit = ?")) {
            ps.setString(1, nit);
            ps.executeUpdate();
        }
    }

    private List<Cliente> leer(ResultSet rs) throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(new Cliente(
                    rs.getString("nombre"),
                    rs.getString("telefono"),
                    rs.getString("nit"),
                    rs.getString("direccion_envio")));
        }
        return lista;
    }

    // Solo para probar; se puede quitar después
    public static void main(String[] args) {
        try {
            ClienteDAO dao = new ClienteDAO();
            System.out.println("--- Todos los clientes ---");
            for (Cliente c : dao.listar()) {
                System.out.println(c.getNit() + " | " + c.getNombre() + " | "
                        + c.getTelefono() + " | " + c.getDireccionEnvio());
            }
            System.out.println("--- Buscar 'ana' ---");
            for (Cliente c : dao.buscar("ana")) {
                System.out.println(c.getNit() + " | " + c.getNombre());
            }
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}