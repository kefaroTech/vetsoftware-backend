package com.vetsoftware.app.electronicdocument.application.port.in;

import com.vetsoftware.app.electronicdocument.application.command.IssueDebitNoteCommand;
import com.vetsoftware.app.electronicdocument.application.dto.ElectronicDocumentDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface IssueDebitNoteUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('electronicDocument.emit') and @authz.isMyCompany(#command.companyId))")
    ElectronicDocumentDto execute(IssueDebitNoteCommand command);
}
