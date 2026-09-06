package com.vetsoftware.app.paymentgateway.application.port.in;

import com.vetsoftware.app.paymentgateway.application.dto.FirstPeriodPaymentDto;
import org.springframework.security.access.prepost.PreAuthorize;

/** Estado del cobro del primer periodo del contrato vigente de una empresa. */
public interface FindFirstPeriodPaymentUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscription.read')"
            + " and @authz.isMyCompany(#companyId))")
    FirstPeriodPaymentDto execute(Long companyId);
}
