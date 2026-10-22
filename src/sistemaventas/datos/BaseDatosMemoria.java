package sistemaventas.datos;

import java.util.ArrayList;
import sistemaventas.modelo.Cliente;
import sistemaventas.modelo.Inventario;
import sistemaventas.modelo.Producto;
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
        cargarDatosEjemplo();
    }

    /**
     * Datos de prueba para que el sistema abra con información.
     * Se eliminarán cuando se integre la base de datos.
     */
    private void cargarDatosEjemplo() {
        // Productos: id, nombre, categoría, precio, stock
        inventario.agregarProducto(new Producto("P001", "Teclado USB", "Periféricos", 85.00, 20));
        inventario.agregarProducto(new Producto("P002", "Mouse óptico", "Periféricos", 45.50, 35));
        inventario.agregarProducto(new Producto("P003", "Monitor 24 pulgadas", "Pantallas", 950.00, 8));
        inventario.agregarProducto(new Producto("P004", "Memoria USB 32GB", "Almacenamiento", 60.00, 50));
        inventario.agregarProducto(new Producto("P005", "Cable HDMI", "Accesorios", 35.00, 40));

        // Clientes: nombre, teléfono, NIT, dirección de envío
        clientes.add(new Cliente("María López", "5555-1234", "1234567-8", "Zona 1, Guatemala"));
        clientes.add(new Cliente("Carlos Pérez", "4444-5678", "8765432-1", "Villa Nueva"));
        clientes.add(new Cliente("Ana Gómez", "3333-9012", "CF", "Mixco"));

        // Vendedores: nombre, teléfono, id, puesto, usuario, contraseña
        vendedores.add(new Vendedor("Luis Ramírez", "2222-3456", "V001", "Vendedor", "lramirez", "1234"));
        vendedores.add(new Vendedor("Sofía Castillo", "2222-7890", "V002", "Supervisora", "scastillo", "1234"));
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