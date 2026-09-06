package com.vetsoftware.app.paymentgateway.application.port.in;

import com.vetsoftware.app.paymentgateway.application.command.CreateWompiPaymentSourceCommand;
import com.vetsoftware.app.paymentgateway.application.dto.WompiPaymentMethodDto;
import org.springframework.security.access.prepost.PreAuthorize;

public interface CreateWompiPaymentSourceUseCase {

    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscriptionPaymentMethod.create')"
            + " and @authz.isMyCompany(#command.companyId))")
    WompiPaymentMethodDto execute(CreateWompiPaymentSourceCommand command);
}
