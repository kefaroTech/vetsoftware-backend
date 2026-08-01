package com.vetsoftware.app.salesreport.application.port.in;

import com.vetsoftware.app.salesreport.application.dto.SalesBookDto;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetSalesBookUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('salesreport.read') and @authz.isMyCompany(#companyId))")
    SalesBookDto get(Long companyId, LocalDate from, LocalDate to, Long branchId);
}
