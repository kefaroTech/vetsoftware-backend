package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindElectronicDocumentUseCase {
    @PreAuthorize("hasAuthority('admin.all') or hasAuthority('pos.read')")
    ElectronicDocumentDto findById(Long id, Long companyId);
}
