package com.vetsoftware.app.submodule.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.submodule.application.port.out.CatalogItemChildrenQueryPort;
import com.vetsoftware.app.submodule.application.port.out.CompanyEntitlementChildrenQueryPort;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.SubModuleHasActiveChildrenException;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
import com.vetsoftware.app.submodule.testsupport.SubModuleMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteSubModuleService")
class DeleteSubModuleServiceTest {

    @Mock
    private SubModuleRepository repository;
    @Mock
    private CatalogItemChildrenQueryPort catalogItemChildrenQueryPort;
    @Mock
    private CompanyEntitlementChildrenQueryPort companyEntitlementChildrenQueryPort;
    @InjectMocks
    private DeleteSubModuleService service;

    @Nested
    @DisplayName("eliminacion")
    class Eliminacion {

        @Test
        @DisplayName("elimina el submodulo cuando ningun articulo del catalogo lo abre")
        void elimina_el_submodulo_cuando_no_tiene_hijos_activos() {
            when(repository.findById(SubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(SubModuleMother.reportes()));
            when(catalogItemChildrenQueryPort
                    .existsActiveBySubModuleId(SubModuleMother.SUB_MODULE_ID)).thenReturn(false);
            when(companyEntitlementChildrenQueryPort
                    .existsActiveBySubModuleId(SubModuleMother.SUB_MODULE_ID)).thenReturn(false);

            service.execute(SubModuleMother.SUB_MODULE_ID);

            verify(repository).delete(SubModuleMother.SUB_MODULE_ID);
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("lanza SubModuleNotFoundException si el submodulo no existe y no consulta hijos")
        void lanza_submodule_not_found_si_el_submodulo_no_existe() {
            when(repository.findById(SubModuleMother.SUB_MODULE_ID)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(SubModuleMother.SUB_MODULE_ID))
                    .isInstanceOf(SubModuleNotFoundException.class)
                    .hasMessageContaining("SubModule not found: " + SubModuleMother.SUB_MODULE_ID);

            verifyNoInteractions(catalogItemChildrenQueryPort, companyEntitlementChildrenQueryPort);
        }

        @Test
        @DisplayName("no elimina si algun articulo vivo del catalogo abre el submodulo")
        void no_elimina_si_tiene_hijos_activos() {
            when(repository.findById(SubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(SubModuleMother.reportes()));
            when(catalogItemChildrenQueryPort
                    .existsActiveBySubModuleId(SubModuleMother.SUB_MODULE_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(SubModuleMother.SUB_MODULE_ID))
                    .isInstanceOf(SubModuleHasActiveChildrenException.class)
                    .hasMessageContaining("catalogItemSubModule");

            verify(repository, never()).delete(SubModuleMother.SUB_MODULE_ID);
            verifyNoInteractions(companyEntitlementChildrenQueryPort);
        }

        @Test
        @DisplayName("no elimina si alguna empresa lo tiene concedido, aunque el catalogo este limpio")
        void no_elimina_si_alguna_empresa_lo_tiene_concedido() {
            when(repository.findById(SubModuleMother.SUB_MODULE_ID))
                    .thenReturn(Optional.of(SubModuleMother.reportes()));
            when(catalogItemChildrenQueryPort
                    .existsActiveBySubModuleId(SubModuleMother.SUB_MODULE_ID)).thenReturn(false);
            when(companyEntitlementChildrenQueryPort
                    .existsActiveBySubModuleId(SubModuleMother.SUB_MODULE_ID)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(SubModuleMother.SUB_MODULE_ID))
                    .isInstanceOf(SubModuleHasActiveChildrenException.class)
                    .hasMessageContaining("companyEntitlement");

            verify(repository, never()).delete(SubModuleMother.SUB_MODULE_ID);
        }
    }
}
