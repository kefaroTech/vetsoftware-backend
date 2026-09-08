package com.vetsoftware.app.registration.infrastructure.orchestration;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Puro adaptador: la secuencia real la prueba
 * {@link FreeTrialContractProvisionerTest}.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("PlatformCatalogSubscriptionCreator — delega en el provisioner compartido")
class PlatformCatalogSubscriptionCreatorTest {

    @Mock
    private FreeTrialContractProvisioner provisioner;

    @InjectMocks
    private PlatformCatalogSubscriptionCreator creator;

    @Test
    @DisplayName("delega companyId y companyName tal cual, sin transformarlos")
    void delega_en_el_provisioner() {
        creator.createInitialContract(42L, "Veterinaria Vetrina");

        verify(provisioner).provision(42L, "Veterinaria Vetrina");
    }
}
