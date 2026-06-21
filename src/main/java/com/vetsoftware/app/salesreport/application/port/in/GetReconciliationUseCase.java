package com.vetsoftware.app.salesreport.application.port.in;

import com.vetsoftware.app.salesreport.application.dto.ReconciliationDto;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetReconciliationUseCase {
    @PreAuthorize("hasAuthority('admin.all') or (hasAuthority('salesreport.read') and @authz.isMyCompany(#companyId))")
    ReconciliationDto get(Long companyId, LocalDate from, LocalDate to);
}
