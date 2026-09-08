package com.vetsoftware.app.company.infrastructure.orchestration;

import static org.mockito.Mockito.verify;

import com.vetsoftware.app.registration.infrastructure.orchestration.FreeTrialContractProvisioner;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Puro adaptador: el alta desde la consola de plataforma nace con la misma
 * ventana de prueba que el alta pública, porque las dos delegan en el mismo
 * {@link FreeTrialContractProvisioner}. La secuencia real la prueba
 * {@code registration.infrastructure.orchestration.FreeTrialContractProvisionerTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformInitialContractProvisioningAdapter — delega en el provisioner compartido")
class PlatformInitialContractProvisioningAdapterTest {

    @Mock
    private FreeTrialContractProvisioner provisioner;

    @InjectMocks
    private PlatformInitialContractProvisioningAdapter adapter;

    @Test
    @DisplayName("delega companyId y companyName tal cual, sin transformarlos")
    void delega_en_el_provisioner() {
        adapter.provisionForCompany(9L, "Clinica Norte");

        verify(provisioner).provision(9L, "Clinica Norte");
    }
}
