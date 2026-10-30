package co.edu.uco.inventario.model;

import java.util.UUID;

public class Producto {
    private String id;
    private String nombre;
    private String sku;
    private int    cantidad;
    private int    stockMinimo;

    public Producto() { this.id = UUID.randomUUID().toString(); }

    public Producto(String nombre, String sku, int cantidad, int stockMinimo) {
        this();
        this.nombre = nombre;
        this.sku = sku;
        this.cantidad = cantidad;
        this.stockMinimo = stockMinimo;
    }

    public boolean isStockBajo()     { return cantidad > 0 && cantidad <= stockMinimo; }
    public boolean isSinExistencia() { return cantidad <= 0; }

    public String getId()               { return id; }
    public void setId(String id)        { this.id = id; }
    public String getNombre()           { return nombre; }
    public void setNombre(String n)     { this.nombre = n; }
    public String getSku()              { return sku; }
    public void setSku(String s)        { this.sku = s; }
    public int getCantidad()            { return cantidad; }
    public void setCantidad(int c)      { this.cantidad = c; }
    public int getStockMinimo()         { return stockMinimo; }
    public void setStockMinimo(int sm)  { this.stockMinimo = sm; }
}
