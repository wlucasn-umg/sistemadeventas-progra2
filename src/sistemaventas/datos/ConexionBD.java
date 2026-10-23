package sistemaventas.datos;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Maneja la conexión con la base de datos MySQL del sistema.
 * Los datos de acceso se leen de conexion.properties (no se sube a Git).
 */
public class ConexionBD {

    private static final String ARCHIVO = "conexion.properties";

    public static Connection getConexion() throws SQLException {
        Properties p = new Properties();
        try (FileInputStream in = new FileInputStream(ARCHIVO)) {
            p.load(in);
        } catch (IOException e) {
            throw new SQLException("No se encontró " + ARCHIVO
                    + " en la carpeta del proyecto.", e);
        }
        String url = "jdbc:mysql://" + p.getProperty("host", "localhost")
                + ":" + p.getProperty("puerto", "3306")
                + "/" + p.getProperty("base", "sistema_ventas")
                + "?allowPublicKeyRetrieval=true"
                + "&serverTimezone=America/Guatemala"
                + "&characterEncoding=UTF-8";
        return DriverManager.getConnection(url,
                p.getProperty("usuario"), p.getProperty("clave"));
    }

    // Solo para probar la conexión; se puede quitar después
    public static void main(String[] args) {
        try (Connection cn = getConexion()) {
            System.out.println("Conexion exitosa a: " + cn.getCatalog());
        } catch (SQLException e) {
            System.out.println("Error de conexion: " + e.getMessage());
        }
    }
}