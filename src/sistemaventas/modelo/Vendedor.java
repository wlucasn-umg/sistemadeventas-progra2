package sistemaventas.modelo;

public class Vendedor extends Persona {
    private String idVendedor;
    private String puesto;
    private UsuarioVendedor usuario;

    public Vendedor(String nombre, String telefono, String idVendedor, String puesto,
                     String nombreUsuario, String contraUsuario) {
        super(nombre, telefono);
        this.idVendedor = idVendedor;
        this.puesto = puesto;
        this.usuario = new UsuarioVendedor(nombreUsuario, contraUsuario);
    }

    public String getIdVendedor() {
        return idVendedor;
    }

    public void setIdVendedor(String idVendedor) {
        this.idVendedor = idVendedor;
    }

    public String getPuesto() {
        return puesto;
    }

    public void setPuesto(String puesto) {
        this.puesto = puesto;
    }

    public UsuarioVendedor getUsuario() {
        return usuario;
    }

    public void registrarVenta() {
        System.out.println("Registrando venta del vendedor: " + nombre);
    }
}