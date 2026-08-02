package com.vetsoftware.app.cashregister.domain;

/** El POS exige que el empleado autenticado tenga su propia caja OPEN en la sede de la venta. */
public class EmployeeCashSessionRequiredException extends RuntimeException {

  public EmployeeCashSessionRequiredException(Long branchId) {
    super("Debes abrir tu caja en la sede " + branchId + " antes de registrar ventas.");
  }
}
