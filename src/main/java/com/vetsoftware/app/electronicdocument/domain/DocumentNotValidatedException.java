package com.vetsoftware.app.electronicdocument.domain;

/** Se intento corregir (nota credito/debito) un documento que no esta VALIDADO por la DIAN. */
public class DocumentNotValidatedException extends RuntimeException {
  public DocumentNotValidatedException(Long id, DianStatus status) {
    super(
        "El documento "
            + id
            + " no esta VALIDADO (estado actual: "
            + status
            + "); solo se puede corregir una factura validada.");
  }
}
