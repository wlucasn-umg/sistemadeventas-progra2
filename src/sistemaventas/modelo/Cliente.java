package sistemaventas.modelo;

public class Cliente extends Persona {
    private String nit;
    private String direccionEnvio;

    public Cliente(String nombre, String telefono, String nit, String direccionEnvio) {
        super(nombre, telefono);
        this.nit = nit;
        this.direccionEnvio = direccionEnvio;
    }

    public String getNit() {
        return nit;
    }

    public void setNit(String nit) {
        this.nit = nit;
    }

    public String getDireccionEnvio() {
        return direccionEnvio;
    }

    public void setDireccionEnvio(String direccionEnvio) {
        this.direccionEnvio = direccionEnvio;
    }

    public void registrarInformacion() {
        System.out.println("Registrando información del cliente: " + nombre);
    }

    public void realizarCompra() {
        System.out.println(nombre + " está realizando una compra.");
    }

    @Override
    public void mostrarInformacion() {
        super.mostrarInformacion();
        System.out.println("NIT: " + nit);
        System.out.println("Dirección de envío: " + direccionEnvio);
    }
}