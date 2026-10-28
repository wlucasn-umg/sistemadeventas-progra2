package sistemaventas.modelo;

import java.util.ArrayList;

public class Factura {
    private String idFactura;
    private String nombreEmpresa;
    private double subtotal;
    private double iva;
    private double total;
    private ArrayList<DetalleFactura> detalles;   // composición: la Factura "contiene" sus detalles

    public Factura(String idFactura, String nombreEmpresa) {
        this.idFactura = idFactura;
        this.nombreEmpresa = nombreEmpresa;
        this.detalles = new ArrayList<>();
        this.subtotal = 0;
        this.iva = 0;
        this.total = 0;
    }

    public String getIdFactura() {
        return idFactura;
    }

    public String getNombreEmpresa() {
        return nombreEmpresa;
    }

    public double getSubtotal() {
        return subtotal;
    }

    public double getIva() {
        return iva;
    }

    public double getTotal() {
        return total;
    }

    public ArrayList<DetalleFactura> getDetalles() {
        return detalles;
    }

    public void agregarDetalle(DetalleFactura detalle) {
        detalles.add(detalle);
        calcularIva();
    }

    public void calcularIva() {
        subtotal = 0;
        for (DetalleFactura d : detalles) {
            subtotal += d.getSubtotal();
        }
        iva = subtotal * 0.12;   // IVA de Guatemala: 12%
        total = subtotal + iva;
    }

    public void emitirFactura() {
        System.out.println("Factura #" + idFactura + " emitida correctamente.");
    }

    public void verFactura() {
        System.out.println("===== " + nombreEmpresa + " =====");
        System.out.println("Factura: " + idFactura);
        for (DetalleFactura d : detalles) {
            System.out.println(d.getProducto().getNombreProducto() + " x" + d.getCantidad()
                    + " = Q" + d.getSubtotal());
        }
        System.out.println("Subtotal: Q" + subtotal);
        System.out.println("IVA: Q" + iva);
        System.out.println("Total: Q" + total);
    }
}