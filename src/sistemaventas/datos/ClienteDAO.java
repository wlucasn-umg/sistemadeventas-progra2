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
 * El NIT se usa como identificador desde la pantalla, igual que el código en productos.
 * Cada método abre y cierra su propia conexión.
 */
public class ClienteDAO {

    private static final String SELECT_BASE =
            "SELECT nombre, telefono, nit, direccion_envio FROM cliente ";

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

    /**
     * True si ya hay un cliente con ese NIT.
     * Al editar, pasa el NIT original en nitIgnorado para no chocar contigo mismo
     * (en un alta nueva, pasa null).
     */
    public boolean existeNit(String nit, String nitIgnorado) throws SQLException {
        String sql = "SELECT 1 FROM cliente WHERE nit = ?"
                + (nitIgnorado != null ? " AND nit <> ?" : "");
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, nit);
            if (nitIgnorado != null) {
                ps.setString(2, nitIgnorado);
            }
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        }
    }

    /** True si el cliente ya aparece en alguna venta (no se puede borrar). */
    public boolean tieneVentas(String nit) throws SQLException {
        String sql = "SELECT 1 FROM venta v "
                + "JOIN cliente c ON c.id_cliente = v.id_cliente "
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
            ps.setString(2, vacioANull(c.getTelefono()));
            ps.setString(3, c.getNit());
            ps.setString(4, vacioANull(c.getDireccionEnvio()));
            ps.executeUpdate();
        }
    }

    /** Actualiza al cliente que tenía nitOriginal (el NIT también se puede cambiar). */
    public void actualizar(String nitOriginal, Cliente c) throws SQLException {
        String sql = "UPDATE cliente SET nombre = ?, telefono = ?, nit = ?, "
                + "direccion_envio = ? WHERE nit = ?";
        try (Connection cn = ConexionBD.getConexion();
             PreparedStatement ps = cn.prepareStatement(sql)) {
            ps.setString(1, c.getNombre());
            ps.setString(2, vacioANull(c.getTelefono()));
            ps.setString(3, c.getNit());
            ps.setString(4, vacioANull(c.getDireccionEnvio()));
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

    // ---------- métodos de apoyo ----------

    private List<Cliente> leer(ResultSet rs) throws SQLException {
        List<Cliente> lista = new ArrayList<>();
        while (rs.next()) {
            lista.add(new Cliente(
                    rs.getString("nombre"),
                    nulo(rs.getString("telefono")),
                    rs.getString("nit"),
                    nulo(rs.getString("direccion_envio"))));
        }
        return lista;
    }

    /** Teléfono y dirección son opcionales: texto vacío se guarda como NULL. */
    private String vacioANull(String s) {
        return (s == null || s.trim().isEmpty()) ? null : s.trim();
    }

    /** Al leer, NULL se convierte en texto vacío para que la pantalla no falle. */
    private String nulo(String s) {
        return s == null ? "" : s;
    }

    // Solo para probar; se puede quitar después
    public static void main(String[] args) {
        try {
            ClienteDAO dao = new ClienteDAO();
            System.out.println("--- Todos los clientes ---");
            for (Cliente c : dao.listar()) {
                System.out.println(c.getNit() + " | " + c.getNombre()
                        + " | " + c.getTelefono() + " | " + c.getDireccionEnvio());
            }
            System.out.println("--- Buscar 'mar' ---");
            for (Cliente c : dao.buscar("mar")) {
                System.out.println(c.getNit() + " | " + c.getNombre());
            }
            System.out.println("--- Prueba de alta, cambio y baja ---");
            dao.insertar(new Cliente("Cliente Prueba", "1111-1111", "PRUEBA-1", "Zona 10"));
            System.out.println("Existe PRUEBA-1: " + dao.existeNit("PRUEBA-1", null));
            dao.actualizar("PRUEBA-1", new Cliente("Cliente Prueba 2", "", "PRUEBA-2", ""));
            System.out.println("Existe PRUEBA-2: " + dao.existeNit("PRUEBA-2", null));
            System.out.println("Tiene ventas: " + dao.tieneVentas("PRUEBA-2"));
            dao.eliminar("PRUEBA-2");
            System.out.println("Existe PRUEBA-2 tras borrar: " + dao.existeNit("PRUEBA-2", null));
        } catch (SQLException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}