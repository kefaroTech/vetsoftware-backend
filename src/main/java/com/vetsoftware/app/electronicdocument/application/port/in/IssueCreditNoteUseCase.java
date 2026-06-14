package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.command.IssueCreditNoteCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface IssueCreditNoteUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('electronicDocument.emit') and @authz.isMyCompany(#command.companyId))")
    ElectronicDocumentDto execute(IssueCreditNoteCommand command);
}
