package sistemaventas.modelo;

import java.util.ArrayList;
import java.util.Date;

public class Inventario {
    private String idInventario;
    private Date fechaActualizacion;
    private ArrayList<Producto> productos;   // agregación: Inventario "tiene" Productos, pero no los crea ni los destruye

    public Inventario(String idInventario) {
        this.idInventario = idInventario;
        this.fechaActualizacion = new Date();
        this.productos = new ArrayList<>();
    }

    public String getIdInventario() {
        return idInventario;
    }

    public Date getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void agregarProducto(Producto p) {
        productos.add(p);
        fechaActualizacion = new Date();
        System.out.println("Producto agregado al inventario: " + p.getNombreProducto());
    }

    public void eliminarProducto(String idProducto) {
        productos.removeIf(p -> p.getIdProducto().equals(idProducto));
        fechaActualizacion = new Date();
        System.out.println("Producto eliminado del inventario.");
    }

    public void aumentarStock(String idProducto, int cantidad) {
        for (Producto p : productos) {
            if (p.getIdProducto().equals(idProducto)) {
                p.aumentarStock(cantidad);
                fechaActualizacion = new Date();
                return;
            }
        }
    }

    public void disminuirStock(String idProducto, int cantidad) {
        for (Producto p : productos) {
            if (p.getIdProducto().equals(idProducto)) {
                p.disminuirStock(cantidad);
                fechaActualizacion = new Date();
                return;
            }
        }
    }

    public void listarProducto() {
        for (Producto p : productos) {
            System.out.println(p.getIdProducto() + " - " + p.getNombreProducto()
                    + " | Stock: " + p.getStock() + " | Precio: Q" + p.getPrecio());
        }
    }
}