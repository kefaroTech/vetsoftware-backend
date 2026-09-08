package com.vetsoftware.app.company.infrastructure.orchestration;

import com.vetsoftware.app.company.application.port.out.InitialContractProvisioningPort;
import com.vetsoftware.app.registration.infrastructure.orchestration.FreeTrialContractProvisioner;
import org.springframework.stereotype.Component;

/**
 * Adapta el puerto de salida del alta desde la consola de plataforma a
 * {@link FreeTrialContractProvisioner}, que es donde vive la secuencia real
 * (ventana, concesiones, contrato, entitlements) — la misma que usa el alta
 * pública. Toda empresa nace con su ventana de prueba, venga de donde venga.
 */
@Component
public class PlatformInitialContractProvisioningAdapter implements InitialContractProvisioningPort {

    private final FreeTrialContractProvisioner provisioner;

    public PlatformInitialContractProvisioningAdapter(FreeTrialContractProvisioner provisioner) {
        this.provisioner = provisioner;
    }

    @Override
    public void provisionForCompany(Long companyId, String companyName) {
        provisioner.provision(companyId, companyName);
    }
}
