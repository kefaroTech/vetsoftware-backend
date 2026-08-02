package com.vetsoftware.app.numberingresolution.domain;

/**
 * Invariante: a lo sumo UNA resolución de numeración activa por (empresa, tipo de documento). Se
 * lanza al crear/reactivar/actualizar una resolución cuando ya existe otra activa para ese mismo
 * (company, documentType).
 */
public class NumberingResolutionAlreadyActiveException extends RuntimeException {
  public NumberingResolutionAlreadyActiveException(
      Long companyId, ElectronicDocumentType documentType) {
    super(
        "La empresa "
            + companyId
            + " ya tiene una resolución de numeración activa para "
            + documentType
            + ". Desactiva la actual antes de crear otra.");
  }
}
