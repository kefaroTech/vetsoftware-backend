package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.util.Optional;

/**
 * B5/3.7 — Lee la clave técnica (technical_key) de la {@code NumberingResolution} activa de la empresa para un
 * tipo de documento. La DIAN la usa en el cálculo del CUFE de la factura; MATIAS la puede exigir por request.
 * Se consulta en la transmisión (misma resolución que numeró el documento). Vacío si no hay resolución activa
 * o la resolución no tiene clave técnica (p. ej. resoluciones de notas, que usan CUDE).
 */
public interface TechnicalKeyQueryPort {
    Optional<String> findActiveTechnicalKey(Long companyId, Long branchId, ElectronicDocumentType documentType);
}
