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
    List<ElectronicDocument> findAllByCompanyId(Long companyId);

    /** Documentos en un estado DIAN dado (p. ej. CONTINGENCIA para el job de reintento). */
    List<ElectronicDocument> findByDianStatus(DianStatus status);

    /**
     * Persiste SOLO los campos del ciclo de vida DIAN (estado + sellos + número) tras una transmisión.
     * No reescribe líneas ni pagos (inmutabilidad del contenido fiscal).
     */
    ElectronicDocument updateDianResult(ElectronicDocument document);
}
