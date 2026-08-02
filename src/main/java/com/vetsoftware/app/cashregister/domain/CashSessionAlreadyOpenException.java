package com.vetsoftware.app.cashregister.domain;

/** Ya existe una sesión de caja OPEN para ese (empresa, sede, terminal): no se puede abrir otra. */
public class CashSessionAlreadyOpenException extends RuntimeException {

  public CashSessionAlreadyOpenException(String branchName, String terminal, String employeeName) {
    super(message(branchName, terminal, employeeName));
  }

  private static String message(String branchName, String terminal, String employeeName) {
    String branch = textOr(branchName, "seleccionada");
    String terminalCode = textOr(terminal, "sin nombre");
    String employee = textOr(employeeName, "Empleado no identificado");
    return "La terminal '"
        + terminalCode
        + "' de la sede '"
        + branch
        + "' ya tiene una caja abierta. Responsable: "
        + employee
        + ".";
  }

  private static String textOr(String value, String fallback) {
    return value == null || value.isBlank() ? fallback : value.trim();
  }
}
