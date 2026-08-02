package com.vetsoftware.app.inventory.domain;

/** La sesión de conteo pedida no existe (o no pertenece a la empresa del solicitante). */
public class InventoryCountNotFoundException extends RuntimeException {
  public InventoryCountNotFoundException(Long id) {
    super("Conteo de inventario no encontrado: " + id);
  }
}
