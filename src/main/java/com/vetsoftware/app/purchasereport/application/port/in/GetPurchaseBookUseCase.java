package com.vetsoftware.app.purchasereport.application.port.in;

import com.vetsoftware.app.purchasereport.application.dto.PurchaseBookDto;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

public interface GetPurchaseBookUseCase {
    @PreAuthorize("hasRole('SYSTEM') or "
        + "(hasAuthority('purchasereport.read') and @authz.isMyCompany(#companyId))")
    PurchaseBookDto get(Long companyId, LocalDate from, LocalDate to, Long branchId);
}
