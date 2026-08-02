package com.vetsoftware.app.owner.domain;

public enum OwnerDocumentType {
  CEDULA_CIUDADANIA(13),
  NIT(31),
  CEDULA_EXTRANJERIA(22),
  PASAPORTE(41),
  PEP(47);

  private final int dianCode;

  OwnerDocumentType(int dianCode) {
    this.dianCode = dianCode;
  }

  public int dianCode() {
    return dianCode;
  }
}
