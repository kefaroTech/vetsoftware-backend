package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.command.TransmitElectronicDocumentCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface TransmitElectronicDocumentUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('pos.create') and @authz.isMyCompany(#command.companyId))")
    ElectronicDocumentDto execute(TransmitElectronicDocumentCommand command);
}
