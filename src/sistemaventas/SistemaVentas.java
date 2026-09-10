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

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));

        int opcion;
        do {
            System.out.println("\n===== MENÚ PRINCIPAL =====");
            System.out.println("1. Registrar producto");
            System.out.println("2. Listar productos");
            System.out.println("3. Registrar vendedor");
            System.out.println("4. Listar vendedores");
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
}