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

    /** Lectura scoped a la empresa: evita IDOR cross-tenant al consultar un documento por id directo. */
    Optional<ElectronicDocument> findByIdAndCompanyId(Long id, Long companyId);

    /** Ubica una factura por su CUFE dentro de la empresa (para enlazar la nota credito con el original). */
    Optional<ElectronicDocument> findByCufe(String cufe, Long companyId);

    /** ¿Ya existe un documento para esta cuenta? Idempotencia de la auto-emisión al cerrar la cuenta. */
    boolean existsByOpenAccountId(Long openAccountId);

    /**
     * Documento previamente registrado con este client_request_id dentro de la empresa. Idempotencia de la
     * venta POS: si el POST se reintenta con la misma key (respuesta perdida), se devuelve el ya emitido en
     * vez de registrar y transmitir otra venta.
     */
    Optional<ElectronicDocument> findByCompanyIdAndClientRequestId(Long companyId, String clientRequestId);

    /** El documento emitido al cerrar una cuenta (para imprimir su recibo). Scoped a la empresa. */
    Optional<ElectronicDocument> findByOpenAccountId(Long openAccountId, Long companyId);

    List<ElectronicDocument> findAllByCompanyId(Long companyId);

    /** Documentos en un estado DIAN dado (p. ej. CONTINGENCIA para el job de reintento). */
    List<ElectronicDocument> findByDianStatus(DianStatus status);

    /**
     * Persiste SOLO los campos del ciclo de vida DIAN (estado + sellos + número) tras una transmisión.
     * No reescribe líneas ni pagos (inmutabilidad del contenido fiscal).
     */
    ElectronicDocument updateDianResult(ElectronicDocument document);
}
