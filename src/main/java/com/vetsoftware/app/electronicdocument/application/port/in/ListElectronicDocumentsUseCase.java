package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;

public interface ListElectronicDocumentsUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('electronicDocument.read') and @authz.isMyCompany(#companyId))")
    List<ElectronicDocumentDto> listByCompany(Long companyId);
}
