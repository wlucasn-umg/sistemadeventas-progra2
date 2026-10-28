package sistemaventas.modelo;

import java.util.Date;

/**
 * Orden de despacho de una venta (relación 1 a 1 con Venta).
 * Nace cuando se confirma la orden de venta; al confirmarla sale el inventario.
 */
public class Despacho {
    private String idDespacho;
    private EstadoDespacho estado;
    private Date fechaGeneracion;
    private Date fechaConfirmacion;

    public Despacho(String idDespacho) {
        this.idDespacho = idDespacho;
        this.estado = EstadoDespacho.PENDIENTE;
        this.fechaGeneracion = new Date();
    }

    // Para reconstruir un despacho guardado en la base de datos
    public Despacho(String idDespacho, EstadoDespacho estado,
                    Date fechaGeneracion, Date fechaConfirmacion) {
        this.idDespacho = idDespacho;
        this.estado = estado;
        this.fechaGeneracion = fechaGeneracion;
        this.fechaConfirmacion = fechaConfirmacion;
    }

    public String getIdDespacho() {
        return idDespacho;
    }

    public EstadoDespacho getEstado() {
        return estado;
    }

    public Date getFechaGeneracion() {
        return fechaGeneracion;
    }

    public Date getFechaConfirmacion() {
        return fechaConfirmacion;
    }

    public void confirmar() {
        this.estado = EstadoDespacho.CONFIRMADO;
        this.fechaConfirmacion = new Date();
    }

    public void anular() {
        this.estado = EstadoDespacho.ANULADO;
    }

    @Override
    public String toString() {
        return "Despacho " + idDespacho + " (" + estado + ")";
    }
}