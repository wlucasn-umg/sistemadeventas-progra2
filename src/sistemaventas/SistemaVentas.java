package sistemaventas;

import sistemaventas.modelo.*;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;

public class SistemaVentas {

    static Scanner sc = new Scanner(System.in, StandardCharsets.UTF_8);
    static Inventario inventario = new Inventario("INV001");
    static ArrayList<Vendedor> vendedores = new ArrayList<>();
    static ArrayList<Cliente> clientes = new ArrayList<>();
    static ArrayList<Venta> ventas = new ArrayList<>();

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        int opcion;
        do {
            System.out.println("\n===== MENÚ PRINCIPAL =====");
            System.out.println("1. Registrar producto");
            System.out.println("2. Listar productos");
            System.out.println("3. Registrar vendedor");
            System.out.println("4. Listar vendedores");
            System.out.println("5. Registrar cliente");
            System.out.println("6. Listar clientes");
            System.out.println("7. Registrar venta");
            System.out.println("8. Listar ventas");
            System.out.println("9. Ver historial de una venta");
            System.out.println("0. Salir");
            System.out.print("Selecciona una opción: ");
            opcion = Integer.parseInt(sc.nextLine());

            switch (opcion) {
                case 1:
                    registrarProducto();
                    break;
                case 2:
                    inventario.listarProducto();
                    break;
                case 3:
                    registrarVendedor();
                    break;
                case 4:
                    listarVendedores();
                    break;
                case 5:
                    registrarCliente();
                    break;
                case 6:
                    listarClientes();
                    break;
                case 7:
                    registrarVenta();
                    break;
                case 8:
                    listarVentas();
                    break;
                case 9:
                    verHistorialVenta();
                    break;
                case 0:
                    System.out.println("Saliendo del sistema...");
                    break;
                default:
                    System.out.println("Opción inválida.");
            }
        } while (opcion != 0);
    }

    static void registrarProducto() {
        System.out.print("ID del producto: ");
        String id = sc.nextLine();

        System.out.print("Nombre del producto: ");
        String nombre = sc.nextLine();

        System.out.print("Categoría: ");
        String categoria = sc.nextLine();

        System.out.print("Precio: ");
        double precio = Double.parseDouble(sc.nextLine());

        System.out.print("Stock inicial: ");
        int stock = Integer.parseInt(sc.nextLine());

        Producto p = new Producto(id, nombre, categoria, precio, stock);
        inventario.agregarProducto(p);
    }

    static void registrarVendedor() {
        System.out.print("ID del vendedor: ");
        String id = sc.nextLine();

        System.out.print("Nombre: ");
        String nombre = sc.nextLine();

        System.out.print("Teléfono: ");
        String telefono = sc.nextLine();

        System.out.print("Puesto: ");
        String puesto = sc.nextLine();

        System.out.print("Nombre de usuario (para iniciar sesión): ");
        String nombreUsuario = sc.nextLine();

        System.out.print("Contraseña: ");
        String contrasena = sc.nextLine();

        Vendedor v = new Vendedor(nombre, telefono, id, puesto, nombreUsuario, contrasena);
        vendedores.add(v);
        System.out.println("Vendedor registrado correctamente.");
    }

    static void listarVendedores() {
        if (vendedores.isEmpty()) {
            System.out.println("No hay vendedores registrados.");
            return;
        }
        for (Vendedor v : vendedores) {
            System.out.println(v.getIdVendedor() + " - " + v.getNombre()
                    + " | Puesto: " + v.getPuesto()
                    + " | Usuario: " + v.getUsuario().getNombreUsuario());
        }
    }

    static void registrarCliente() {
        System.out.print("Nombre: ");
        String nombre = sc.nextLine();

        System.out.print("Teléfono: ");
        String telefono = sc.nextLine();

        System.out.print("NIT: ");
        String nit = sc.nextLine();

        System.out.print("Dirección de envío: ");
        String direccion = sc.nextLine();

        Cliente c = new Cliente(nombre, telefono, nit, direccion);
        clientes.add(c);
        System.out.println("Cliente registrado correctamente.");
    }

    static void listarClientes() {
        if (clientes.isEmpty()) {
            System.out.println("No hay clientes registrados.");
            return;
        }
        for (Cliente c : clientes) {
            System.out.println(c.getNit() + " - " + c.getNombre()
                    + " | Dirección: " + c.getDireccionEnvio());
        }
    }

    static void registrarVenta() {
        if (vendedores.isEmpty()) {
            System.out.println("Debes registrar al menos un vendedor primero.");
            return;
        }
        if (clientes.isEmpty()) {
            System.out.println("Debes registrar al menos un cliente primero.");
            return;
        }

        System.out.print("ID de la venta: ");
        String idVenta = sc.nextLine();

        System.out.println("--- Vendedores disponibles ---");
        for (int i = 0; i < vendedores.size(); i++) {
            System.out.println(i + ". " + vendedores.get(i).getNombre());
        }
        System.out.print("Selecciona el número del vendedor: ");
        int idxV = Integer.parseInt(sc.nextLine());
        Vendedor vendedorSel = vendedores.get(idxV);

        System.out.println("--- Clientes disponibles ---");
        for (int i = 0; i < clientes.size(); i++) {
            System.out.println(i + ". " + clientes.get(i).getNombre());
        }
        System.out.print("Selecciona el número del cliente: ");
        int idxC = Integer.parseInt(sc.nextLine());
        Cliente clienteSel = clientes.get(idxC);

        System.out.println("Método de pago:");
        System.out.println("1. Efectivo");
        System.out.println("2. Tarjeta");
        System.out.println("3. Transferencia");
        System.out.print("Selecciona una opción: ");
        int opcionPago = Integer.parseInt(sc.nextLine());
        MetodoPago metodoPago;
        switch (opcionPago) {
            case 1: metodoPago = MetodoPago.EFECTIVO; break;
            case 2: metodoPago = MetodoPago.TARJETA; break;
            case 3: metodoPago = MetodoPago.TRANSFERENCIA; break;
            default:
                System.out.println("Opción inválida, se asignará Efectivo por defecto.");
                metodoPago = MetodoPago.EFECTIVO;
        }

        Venta venta = new Venta(idVenta, vendedorSel.getUsuario().getNombreUsuario(),
                metodoPago, vendedorSel, clienteSel);

        String continuar;
        do {
            System.out.println("--- Productos disponibles ---");
            inventario.listarProducto();
            System.out.print("Ingresa el ID del producto a agregar: ");
            String idProd = sc.nextLine();

            Producto productoSel = buscarProducto(idProd);
            if (productoSel == null) {
                System.out.println("Producto no encontrado.");
            } else {
                venta.agregarProducto(productoSel);
                System.out.println("Producto agregado a la venta.");
            }

            System.out.print("¿Agregar otro producto? (s/n): ");
            continuar = sc.nextLine();
        } while (continuar.equalsIgnoreCase("s"));

        venta.registrarVenta();
        ventas.add(venta);
        System.out.println("Total de la venta: Q" + venta.getTotal());

        System.out.print("¿Generar factura ahora? (s/n): ");
        String generar = sc.nextLine();
        if (generar.equalsIgnoreCase("s")) {
            System.out.print("Nombre de la empresa para la factura: ");
            String empresa = sc.nextLine();
            venta.generarFactura("F-" + idVenta, empresa);
            venta.getFactura().verFactura();
        }
    }

    static void listarVentas() {
        if (ventas.isEmpty()) {
            System.out.println("No hay ventas registradas.");
            return;
        }
        for (Venta v : ventas) {
            System.out.println(v.getIdVenta() + " | Total: Q" + v.getTotal()
                    + " | Estado: " + v.getEstadoVenta());
        }
    }

    static void verHistorialVenta() {
        if (ventas.isEmpty()) {
            System.out.println("No hay ventas registradas.");
            return;
        }
        System.out.print("ID de la venta: ");
        String id = sc.nextLine();
        for (Venta v : ventas) {
            if (v.getIdVenta().equals(id)) {
                v.verHistorial();
                return;
            }
        }
        System.out.println("Venta no encontrada.");
    }

    static Producto buscarProducto(String id) {
        for (Producto p : inventario.getProductos()) {
            if (p.getIdProducto().equals(id)) {
                return p;
            }
        }
        return null;
    }
}