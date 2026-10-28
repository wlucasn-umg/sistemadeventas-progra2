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
    private ArrayList<SeguimientoVenta> historial;
    private Factura factura;
    private Despacho despacho;
    private boolean stockDescontado = false;

    public Venta(String idVenta, String usuarioEmision, MetodoPago metodoPago,
                 Vendedor vendedor, Cliente cliente) {
        this.idVenta = idVenta;
        this.fechaVenta = new Date();
        this.usuarioEmision = usuarioEmision;
        this.metodoPago = metodoPago;
        this.vendedor = vendedor;
        this.cliente = cliente;
        this.productos = new ArrayList<>();
        this.historial = new ArrayList<>();
        this.total = 0;
        cambiarEstado(EstadoVenta.PENDIENTE, "Venta creada");
    }

    // Para reconstruir una venta guardada en la base de datos (no agrega historial: se carga aparte)
    public Venta(String idVenta, Date fechaVenta, String usuarioEmision, MetodoPago metodoPago,
                 EstadoVenta estadoVenta, boolean stockDescontado, Vendedor vendedor, Cliente cliente) {
        this.idVenta = idVenta;
        this.fechaVenta = fechaVenta;
        this.usuarioEmision = usuarioEmision;
        this.metodoPago = metodoPago;
        this.estadoVenta = estadoVenta;
        this.stockDescontado = stockDescontado;
        this.vendedor = vendedor;
        this.cliente = cliente;
        this.productos = new ArrayList<>();
        this.historial = new ArrayList<>();
        this.total = 0;
    }

    public String getIdVenta() {
        return idVenta;
    }

    public Date getFechaVenta() {
        return fechaVenta;
    }

    public String getUsuarioEmision() {
        return usuarioEmision;
    }

    public double getTotal() {
        return total;
    }

    public MetodoPago getMetodoPago() {
        return metodoPago;
    }

    public EstadoVenta getEstadoVenta() {
        return estadoVenta;
    }

    public Vendedor getVendedor() {
        return vendedor;
    }

    public Cliente getCliente() {
        return cliente;
    }

    public ArrayList<Producto> getProductos() {
        return productos;
    }

    public Factura getFactura() {
        return factura;
    }

    public void setFactura(Factura factura) {
        this.factura = factura;
    }

    public Despacho getDespacho() {
        return despacho;
    }

    public void setDespacho(Despacho despacho) {
        this.despacho = despacho;
    }

    public boolean isStockDescontado() {
        return stockDescontado;
    }

    public ArrayList<SeguimientoVenta> getHistorial() {
        return historial;
    }

    // Cuántas unidades de ese producto lleva esta venta
    public int cantidadEnVenta(Producto p) {
        int n = 0;
        for (Producto x : productos) {
            if (x == p) {
                n++;
            }
        }
        return n;
    }

    // Devuelve false si no alcanza el stock (y no agrega nada)
    public boolean agregarProducto(Producto p) {
        if (cantidadEnVenta(p) + 1 > p.getStock()) {
            System.out.println("Stock insuficiente de " + p.getNombreProducto()
                    + " (disponible: " + p.getStock() + ").");
            return false;
        }
        productos.add(p);
        calcularTotal();
        return true;
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

    public void cambiarEstado(EstadoVenta nuevoEstado, String comentario) {
        this.estadoVenta = nuevoEstado;
        String idSeg = "S" + (historial.size() + 1);
        historial.add(new SeguimientoVenta(idSeg, nuevoEstado, comentario));
    }

    // Reglas del flujo: se entrega solo lo facturado; se cancela todo lo que no esté entregado o cancelado
    public boolean puedeEntregarse() {
        return estadoVenta == EstadoVenta.FACTURADA;
    }

    public boolean puedeCancelarse() {
        return estadoVenta != EstadoVenta.ENTREGADA && estadoVenta != EstadoVenta.CANCELADA;
    }

    // ---------- Flujo: orden de venta -> despacho -> salida de inventario -> factura ----------

    public boolean puedeConfirmarse() {
        return estadoVenta == EstadoVenta.PENDIENTE && !productos.isEmpty();
    }

    public boolean puedeDespacharse() {
        return estadoVenta == EstadoVenta.EN_PROCESO
                && despacho != null && despacho.getEstado() == EstadoDespacho.PENDIENTE;
    }

    public boolean puedeFacturarse() {
        return estadoVenta == EstadoVenta.DESPACHADA && factura == null;
    }

    // Confirma la orden de venta y genera su orden de despacho. No toca el inventario.
    public boolean confirmarOrden() {
        if (!puedeConfirmarse()) {
            System.out.println("La orden " + idVenta + " no se puede confirmar (estado: "
                    + estadoVenta + ", productos: " + productos.size() + ").");
            return false;
        }
        cambiarEstado(EstadoVenta.EN_PROCESO, "Orden confirmada por el vendedor");
        vendedor.registrarVenta();
        despacho = new Despacho("DSP-" + idVenta);
        return true;
    }

    // Confirma el despacho: aquí sale el inventario. Si no alcanza el stock, no hace nada.
    public boolean confirmarDespacho() {
        if (!puedeDespacharse()) {
            System.out.println("La orden " + idVenta + " no se puede despachar (estado: " + estadoVenta + ").");
            return false;
        }
        ArrayList<Producto> revisados = new ArrayList<>();
        for (Producto p : productos) {
            if (revisados.contains(p)) {
                continue;
            }
            revisados.add(p);
            if (cantidadEnVenta(p) > p.getStock()) {
                System.out.println("Stock insuficiente de " + p.getNombreProducto()
                        + ": no se puede confirmar el despacho.");
                return false;
            }
        }
        for (Producto p : productos) {
            p.disminuirStock(1);
        }
        stockDescontado = true;
        despacho.confirmar();
        cambiarEstado(EstadoVenta.DESPACHADA, "Despacho " + despacho.getIdDespacho()
                + " confirmado: salida de inventario");
        return true;
    }

    // Emite la factura de una venta ya despachada (el inventario ya salió en el despacho)
    public boolean facturar(String idFactura, String nombreEmpresa) {
        if (!puedeFacturarse()) {
            System.out.println("La venta " + idVenta + " no se puede facturar (estado: " + estadoVenta
                    + "). Primero debe confirmarse el despacho.");
            return false;
        }
        Factura nueva = new Factura(idFactura, nombreEmpresa);
        ArrayList<Producto> vistos = new ArrayList<>();
        int contador = 1;
        for (Producto p : productos) {
            if (vistos.contains(p)) {
                continue;
            }
            vistos.add(p);
            nueva.agregarDetalle(new DetalleFactura("D" + contador, cantidadEnVenta(p), p));
            contador++;
        }
        factura = nueva;
        factura.emitirFactura();
        cambiarEstado(EstadoVenta.FACTURADA, "Factura " + idFactura + " emitida");
        return true;
    }

    // Flujo anterior (consola): confirma la orden sin despacho
    public void registrarVenta() {
        cambiarEstado(EstadoVenta.EN_PROCESO, "Venta confirmada por el vendedor");
        vendedor.registrarVenta();
        System.out.println("Venta " + idVenta + " registrada por " + vendedor.getNombre());
    }

    public void cancelarVenta() {
        if (!puedeCancelarse()) {
            System.out.println("La venta " + idVenta + " no se puede cancelar (estado: " + estadoVenta + ").");
            return;
        }
        String comentario = "Venta cancelada";
        if (stockDescontado) {
            for (Producto p : productos) {
                p.aumentarStock(1);
            }
            stockDescontado = false;
            comentario = "Venta cancelada (stock devuelto al inventario)";
        }
        if (despacho != null && despacho.getEstado() == EstadoDespacho.PENDIENTE) {
            despacho.anular();
        }
        cambiarEstado(EstadoVenta.CANCELADA, comentario);
        System.out.println("Venta " + idVenta + " cancelada.");
    }

    // Flujo anterior (consola): emite la factura y descuenta el stock en un solo paso.
    // El flujo nuevo usa confirmarOrden() -> confirmarDespacho() -> facturar().
    public void generarFactura(String idFactura, String nombreEmpresa) {
        if (factura != null) {
            System.out.println("La venta " + idVenta + " ya tiene factura.");
            return;
        }
        if (estadoVenta == EstadoVenta.CANCELADA) {
            System.out.println("La venta " + idVenta + " está cancelada, no se puede facturar.");
            return;
        }

        // Revisa el stock por producto (por si cambió desde que se agregaron)
        ArrayList<Producto> revisados = new ArrayList<>();
        for (Producto p : productos) {
            if (revisados.contains(p)) {
                continue;
            }
            revisados.add(p);
            if (cantidadEnVenta(p) > p.getStock()) {
                System.out.println("Stock insuficiente de " + p.getNombreProducto()
                        + ": no se puede facturar.");
                return;
            }
        }

        Factura nueva = new Factura(idFactura, nombreEmpresa);
        int contador = 1;
        for (Producto p : productos) {
            nueva.agregarDetalle(new DetalleFactura("D" + contador, 1, p));
            contador++;
        }
        factura = nueva;
        factura.emitirFactura();

        for (Producto p : productos) {
            p.disminuirStock(1);
        }
        stockDescontado = true;
        cambiarEstado(EstadoVenta.FACTURADA, "Factura " + idFactura + " emitida");
    }

    public void marcarEntregada() {
        if (!puedeEntregarse()) {
            System.out.println("La venta " + idVenta + " no se puede entregar (estado: " + estadoVenta + ").");
            return;
        }
        cambiarEstado(EstadoVenta.ENTREGADA, "Pedido entregado al cliente");
    }

    public void verHistorial() {
        System.out.println("--- Historial de la venta " + idVenta + " ---");
        for (SeguimientoVenta s : historial) {
            System.out.println(s.toString());
        }
    }
}