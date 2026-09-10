package sistemaventas.modelo;

public class DetalleFactura {
    private String idDetalle;
    private int cantidad;
    private double precioUnitario;
    private double subtotal;
    private double total;
    private Producto producto;   // asociación: el detalle "referencia" a un Producto

    public DetalleFactura(String idDetalle, int cantidad, Producto producto) {
        this.idDetalle = idDetalle;
        this.cantidad = cantidad;
        this.producto = producto;
        this.precioUnitario = producto.getPrecio();
        calcularSubtotal();
    }

    public String getIdDetalle() {
        return idDetalle;
    }

    public int getCantidad() {
        return cantidad;
    }

    public double getPrecioUnitario() {
        return precioUnitario;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getTotal() {
        return total;
    }

    public Producto getProducto() {
        return producto;
    }

    public void calcularSubtotal() {
        this.subtotal = cantidad * precioUnitario;
        this.total = subtotal;   // aquí se puede sumar algún recargo si aplicara
    }
}
