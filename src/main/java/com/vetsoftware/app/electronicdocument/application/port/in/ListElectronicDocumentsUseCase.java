package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import com.vetsoftware.app.electronicdocument.application.dto.PageResult;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListElectronicDocumentsUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('pos.read') and @authz.isMyCompany(#companyId))")
    PageResult<ElectronicDocumentDto> listByCompany(Long companyId, Long branchId, int page,
            int pageSize);
}
