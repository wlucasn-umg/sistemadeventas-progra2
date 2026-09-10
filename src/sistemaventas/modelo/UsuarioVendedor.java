package sistemaventas.modelo;

public class UsuarioVendedor {
    private String nombreUsuario;
    private String contraUsuario;

    public UsuarioVendedor(String nombreUsuario, String contraUsuario) {
        this.nombreUsuario = nombreUsuario;
        this.contraUsuario = contraUsuario;
    }

    public String getNombreUsuario() {
        return nombreUsuario;
    }

    public void setNombreUsuario(String nombreUsuario) {
        this.nombreUsuario = nombreUsuario;
    }

    public void setContraUsuario(String contraUsuario) {
        this.contraUsuario = contraUsuario;
    }

    public boolean iniciarSesion(String usuarioIngresado, String contraIngresada) {
        boolean exito = this.nombreUsuario.equals(usuarioIngresado)
                && this.contraUsuario.equals(contraIngresada);
        if (exito) {
            System.out.println("Sesión iniciada correctamente.");
        } else {
            System.out.println("Usuario o contraseña incorrectos.");
        }
        return exito;
    }

    public void cerrarSesion() {
        System.out.println("Sesión cerrada.");
    }
}