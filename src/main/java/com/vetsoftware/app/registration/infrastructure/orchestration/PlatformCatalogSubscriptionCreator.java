package com.vetsoftware.app.registration.infrastructure.orchestration;

import com.vetsoftware.app.registration.application.port.out.InitialSubscriptionCreator;
import org.springframework.stereotype.Component;

/**
 * Adapta el puerto de salida del alta pública a
 * {@link FreeTrialContractProvisioner}, que es donde vive la secuencia real
 * (ventana, concesiones, contrato, entitlements) — la misma que usa el alta
 * desde la consola de plataforma.
 */
@Component
public class PlatformCatalogSubscriptionCreator implements InitialSubscriptionCreator {

    private final FreeTrialContractProvisioner provisioner;

    public PlatformCatalogSubscriptionCreator(FreeTrialContractProvisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public void createInitialContract(Long companyId, String companyName) {
        provisioner.provision(companyId, companyName);
    }
}
