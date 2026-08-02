package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface FindElectronicDocumentUseCase {
  @PreAuthorize("hasRole('SYSTEM') or hasAuthority('pos.read')")
  ElectronicDocumentDto findById(Long id, Long companyId);
}
