package com.vetsoftware.app.electronicdocument.application.port.out;

import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocument;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import java.util.List;
import java.util.Optional;

/**
 * Puerto de salida. Inmutabilidad fiscal: solo save (alta) y lecturas; sin
 * update ni delete.
 */
public interface ElectronicDocumentRepository {
    ElectronicDocument save(ElectronicDocument document);

    Optional<ElectronicDocument> findById(Long id);

    /**
     * Lectura scoped a la empresa: evita IDOR cross-tenant al consultar un
     * documento por id directo.
     */
    Optional<ElectronicDocument> findByIdAndCompanyId(Long id, Long companyId);

    /**
     * Ubica una factura por su CUFE dentro de la empresa (para enlazar la nota
     * credito con el original).
     */
    Optional<ElectronicDocument> findByCufe(String cufe, Long companyId);

    /**
     * ¿Ya existe un documento para esta cuenta? Idempotencia de la auto-emisión al
     * cerrar la cuenta.
     */
    boolean existsByOpenAccountId(Long openAccountId);

    /**
     * ¿Ya existe un documento de este TIPO para la cuenta? Idempotencia de la
     * conversión POS→FE: la cuenta ya tiene el DOC_EQUIV_POS, así que se filtra por
     * FE_VENTA para no convertir dos veces (doble ingreso).
     */
    boolean existsByOpenAccountIdAndDocumentType(Long openAccountId,
            ElectronicDocumentType documentType);

    /**
     * Documento previamente registrado con este client_request_id dentro de la
     * empresa. Idempotencia de la venta POS: si el POST se reintenta con la misma
     * key (respuesta perdida), se devuelve el ya emitido en vez de registrar y
     * transmitir otra venta.
     */
    Optional<ElectronicDocument> findByCompanyIdAndClientRequestId(Long companyId,
            String clientRequestId);

    /**
     * El documento emitido al cerrar una cuenta (para imprimir su recibo). Scoped a
     * la empresa.
     */
    Optional<ElectronicDocument> findByOpenAccountId(Long openAccountId, Long companyId);

    /**
     * Documentos de la empresa, con filtros OPCIONALES por sede, tipo de documento
     * y estado DIAN (null = sin filtrar por ese criterio).
     *
     * <p>
     * BE-06: tipo y estado los filtraba la pantalla EN CLIENTE sobre la lista
     * completa. Al pasar a paginacion real, un filtro de cliente solo veria las
     * paginas ya cargadas, asi que tiene que resolverse aqui.
     */
    PageResult<ElectronicDocument> findAllByCompanyId(Long companyId, Long branchId,
            ElectronicDocumentType documentType, DianStatus dianStatus, int page, int pageSize);

    /**
     * Documentos en un estado DIAN dado (p. ej. CONTINGENCIA para el job de
     * reintento).
     */
    List<ElectronicDocument> findByDianStatus(DianStatus status);

    /**
     * Persiste SOLO los campos del ciclo de vida DIAN (estado + sellos + número)
     * tras una transmisión. No reescribe líneas ni pagos (inmutabilidad del
     * contenido fiscal).
     */
    ElectronicDocument updateDianResult(ElectronicDocument document);
}
