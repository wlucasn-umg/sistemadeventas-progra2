package sistemaventas.modelo;

import java.util.Date;

public class SeguimientoVenta {
    private String idSeguimiento;
    private EstadoVenta estado;
    private Date fechaHora;
    private String comentario;

    public SeguimientoVenta(String idSeguimiento, EstadoVenta estado, String comentario) {
        this.idSeguimiento = idSeguimiento;
        this.estado = estado;
        this.fechaHora = new Date();
        this.comentario = comentario;
    }

    // Para reconstruir un seguimiento guardado en la base de datos,
    // conservando su fecha original
    public SeguimientoVenta(String idSeguimiento, EstadoVenta estado,
            Date fechaHora, String comentario) {
        this.idSeguimiento = idSeguimiento;
        this.estado = estado;
        this.fechaHora = fechaHora;
        this.comentario = comentario;
    }

    public String getIdSeguimiento() {
        return idSeguimiento;
    }

    public EstadoVenta getEstado() {
        return estado;
    }

    public Date getFechaHora() {
        return fechaHora;
    }

    public String getComentario() {
        return comentario;
    }

    @Override
    public String toString() {
        return fechaHora + " - " + estado + (comentario != null && !comentario.isEmpty() ? " (" + comentario + ")" : "");
    }
}