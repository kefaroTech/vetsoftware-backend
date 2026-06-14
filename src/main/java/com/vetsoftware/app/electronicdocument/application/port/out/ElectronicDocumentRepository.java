package com.vetsoftware.app.electronicdocument.application.port.out;

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
}
