package com.vetsoftware.app.openaccount.application.port.in;

import com.vetsoftware.app.openaccount.application.dto.OpenAccountsSummaryDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetOpenAccountsSummaryUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('openAccount.read') and @authz.isMyCompany(#companyId))")
    OpenAccountsSummaryDto summarize(Long companyId, Long branchId);
}
