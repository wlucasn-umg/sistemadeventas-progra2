package sistemaventas.modelo;

public class Producto {
    private String idProducto;
    private String nombreProducto;
    private String categoriaProducto;
    private double precio;
    private int stock;

    public Producto(String idProducto, String nombreProducto, String categoriaProducto,
                     double precio, int stock) {
        this.idProducto = idProducto;
        this.nombreProducto = nombreProducto;
        this.categoriaProducto = categoriaProducto;
        this.precio = precio;
        this.stock = stock;
    }

    public String getIdProducto() {
        return idProducto;
    }

    public String getNombreProducto() {
        return nombreProducto;
    }

    public void setNombreProducto(String nombreProducto) {
        this.nombreProducto = nombreProducto;
    }

    public String getCategoriaProducto() {
        return categoriaProducto;
    }

    public void setCategoriaProducto(String categoriaProducto) {
        this.categoriaProducto = categoriaProducto;
    }

    public double getPrecio() {
        return precio;
    }

    public void setPrecio(double precio) {
        this.precio = precio;
    }

    public int getStock() {
        return stock;
    }

    public void registrarProducto() {
        System.out.println("Producto registrado: " + nombreProducto);
    }

    public void actualizarProducto(String nombreProducto, double precio) {
        this.nombreProducto = nombreProducto;
        this.precio = precio;
        System.out.println("Producto actualizado: " + nombreProducto);
    }

    public boolean verificarStock(int cantidadSolicitada) {
        return stock >= cantidadSolicitada;
    }

    // Estos dos no vienen en Producto según el diagrama,
    // pero los necesitamos internamente para que Inventario pueda mover el stock
    void aumentarStock(int cantidad) {
        this.stock += cantidad;
    }

    void disminuirStock(int cantidad) {
        this.stock -= cantidad;
    }
}