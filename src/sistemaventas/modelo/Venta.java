package sistemaventas.modelo;

import java.util.ArrayList;
import java.util.Date;

public class Venta {
    private String idVenta;
    private Date fechaVenta;
    private String usuarioEmision;
    private double total;
    private MetodoPago metodoPago;
    private EstadoVenta estadoVenta;
    private Vendedor vendedor;
    private Cliente cliente;
    private ArrayList<Producto> productos;
    private Factura factura;

    public Venta(String idVenta, String usuarioEmision, MetodoPago metodoPago,
                 Vendedor vendedor, Cliente cliente) {
        this.idVenta = idVenta;
        this.fechaVenta = new Date();
        this.usuarioEmision = usuarioEmision;
        this.metodoPago = metodoPago;
        this.vendedor = vendedor;
        this.cliente = cliente;
        this.productos = new ArrayList<>();
        this.total = 0;
        this.estadoVenta = EstadoVenta.PENDIENTE;
    }

    public String getIdVenta() {
        return idVenta;
    }

    public double getTotal() {
        return total;
    }

    public EstadoVenta getEstadoVenta() {
        return estadoVenta;
    }

    public Factura getFactura() {
        return factura;
    }

    public void agregarProducto(Producto p) {
        productos.add(p);
        calcularTotal();
    }

    public void eliminarProducto(Producto p) {
        productos.remove(p);
        calcularTotal();
    }

    public void calcularTotal() {
        total = 0;
        for (Producto p : productos) {
            total += p.getPrecio();
        }
    }

    public void registrarVenta() {
        estadoVenta = EstadoVenta.EN_PROCESO;
        vendedor.registrarVenta();
        System.out.println("Venta " + idVenta + " registrada por " + vendedor.getNombre());
    }

    public void cancelarVenta() {
        estadoVenta = EstadoVenta.CANCELADA;
        System.out.println("Venta " + idVenta + " cancelada.");
    }

    public void generarFactura(String idFactura, String nombreEmpresa) {
        factura = new Factura(idFactura, nombreEmpresa);
        int contador = 1;
        for (Producto p : productos) {
            DetalleFactura detalle = new DetalleFactura("D" + contador, 1, p);
            factura.agregarDetalle(detalle);
            contador++;
        }
        factura.emitirFactura();
        estadoVenta = EstadoVenta.FACTURADA;
    }
}