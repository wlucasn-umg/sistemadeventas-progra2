package sistemaventas.datos;

import java.util.ArrayList;
import sistemaventas.modelo.Cliente;
import sistemaventas.modelo.Inventario;
import sistemaventas.modelo.Vendedor;
import sistemaventas.modelo.Venta;

/**
 * Almacén único en memoria (patrón Singleton).
 * Todas las pantallas leen y escriben aquí, así los datos
 * se comparten sin importar qué ventana los use.
 */
public class BaseDatosMemoria {

    private static final BaseDatosMemoria INSTANCIA = new BaseDatosMemoria();

    private final Inventario inventario = new Inventario("INV001");
    private final ArrayList<Vendedor> vendedores = new ArrayList<>();
    private final ArrayList<Cliente> clientes = new ArrayList<>();
    private final ArrayList<Venta> ventas = new ArrayList<>();

    private BaseDatosMemoria() {
    }

    public static BaseDatosMemoria getInstancia() {
        return INSTANCIA;
    }

    public Inventario getInventario() {
        return inventario;
    }

    public ArrayList<Vendedor> getVendedores() {
        return vendedores;
    }

    public ArrayList<Cliente> getClientes() {
        return clientes;
    }

    public ArrayList<Venta> getVentas() {
        return ventas;
    }
}