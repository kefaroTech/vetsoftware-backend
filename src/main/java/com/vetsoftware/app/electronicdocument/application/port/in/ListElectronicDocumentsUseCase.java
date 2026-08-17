package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListElectronicDocumentsUseCase {
    /**
     * {@code documentType} y {@code dianStatus} son los dos selectores de la
     * pantalla de documentos; null = sin filtrar. Se resuelven en servidor (BE-06)
     * porque filtrar en cliente sobre una lista paginada oculta resultados.
     */
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('pos.read') and @authz.isMyCompany(#companyId))")
    PageResult<ElectronicDocumentDto> listByCompany(Long companyId, Long branchId,
            ElectronicDocumentType documentType, DianStatus dianStatus, int page, int pageSize);
}
