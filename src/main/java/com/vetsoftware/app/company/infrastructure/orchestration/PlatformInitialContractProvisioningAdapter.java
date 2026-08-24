package com.vetsoftware.app.company.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.company.application.port.out.InitialContractProvisioningPort;
import com.vetsoftware.app.entitlement.application.command.InitializeCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.port.in.InitializeCompanyEntitlementsUseCase;
import com.vetsoftware.app.subscription.application.command.CreateInitialSubscriptionCommand;
import com.vetsoftware.app.subscription.application.port.in.CreateInitialSubscriptionUseCase;
import org.springframework.stereotype.Component;

/** Une company con los slices que materializan contrato y entitlements. */
@Component
public class PlatformInitialContractProvisioningAdapter implements InitialContractProvisioningPort {

    private final CreateInitialSubscriptionUseCase createInitialSubscriptionUseCase;
    private final InitializeCompanyEntitlementsUseCase initializeCompanyEntitlementsUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public PlatformInitialContractProvisioningAdapter(
            CreateInitialSubscriptionUseCase createInitialSubscriptionUseCase,
            InitializeCompanyEntitlementsUseCase initializeCompanyEntitlementsUseCase,
            SystemAuthRunner systemAuthRunner) {
        this.createInitialSubscriptionUseCase = createInitialSubscriptionUseCase;
        this.initializeCompanyEntitlementsUseCase = initializeCompanyEntitlementsUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public void provisionForCompany(Long companyId) {
        systemAuthRunner.run(() -> createInitialSubscriptionUseCase
                .execute(new CreateInitialSubscriptionCommand(companyId, null, null)));
        initializeCompanyEntitlementsUseCase
                .execute(new InitializeCompanyEntitlementsCommand(companyId));
    }
}
