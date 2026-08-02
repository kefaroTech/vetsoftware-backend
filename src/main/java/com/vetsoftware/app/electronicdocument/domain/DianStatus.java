package com.vetsoftware.app.electronicdocument.domain;

/** Estado del documento en el ciclo de validacion DIAN. En F2 todo documento nace PENDIENTE. */
public enum DianStatus {
  PENDIENTE,
  VALIDADO,
  RECHAZADO,
  CONTINGENCIA,
  NO_ELECTRONICO
}
