package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida. Inmutabilidad fiscal: solo save (alta) y lecturas; sin update ni delete.
 */
public interface ElectronicDocumentRepository {
    ElectronicDocument save(ElectronicDocument document);
    Optional<ElectronicDocument> findById(Long id);

    /** Ubica una factura por su CUFE dentro de la empresa (para enlazar la nota credito con el original). */
    Optional<ElectronicDocument> findByCufe(String cufe, Long companyId);

    /** ¿Ya existe un documento para esta cuenta? Idempotencia de la auto-emisión al cerrar la cuenta. */
    boolean existsByOpenAccountId(Long openAccountId);

    List<ElectronicDocument> findAllByCompanyId(Long companyId);

    /** Documentos en un estado DIAN dado (p. ej. CONTINGENCIA para el job de reintento). */
    List<ElectronicDocument> findByDianStatus(DianStatus status);

    /**
     * Persiste SOLO los campos del ciclo de vida DIAN (estado + sellos + número) tras una transmisión.
     * No reescribe líneas ni pagos (inmutabilidad del contenido fiscal).
     */
    ElectronicDocument updateDianResult(ElectronicDocument document);
}
