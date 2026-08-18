package com.vetsoftware.app.owner.application.usecase;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.owner.application.port.out.AnimalChildrenQueryPort;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.domain.OwnerHasActiveChildrenException;
import com.vetsoftware.app.owner.testsupport.OwnerMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El escenario de tenant ajeno ya lo cubre {@link OwnerTenantGuardTest}; esta
 * clase cubre el unico hijo de owner (animales) en sus dos desenlaces.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("DeleteOwnerService")
class DeleteOwnerServiceTest {

    @Mock
    private OwnerRepository repository;
    @Mock
    private AnimalChildrenQueryPort animalChildrenQueryPort;

    @InjectMocks
    private DeleteOwnerService service;

    private void ownerExiste() {
        when(repository.findByIdAndCompanyId(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                .thenReturn(Optional.of(OwnerMother.personaNatural()));
    }

    @Nested
    @DisplayName("borrado permitido")
    class BorradoPermitido {

        @Test
        @DisplayName("sin animales activos borra acotando por empresa")
        void sin_animales_activos_borra_acotando_por_empresa() {
            ownerExiste();
            when(animalChildrenQueryPort.existsActiveByOwnerId(OwnerMother.OWNER_ID))
                    .thenReturn(false);

            service.execute(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID);

            verify(repository).delete(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID);
        }
    }

    @Nested
    @DisplayName("borrado bloqueado")
    class BorradoBloqueado {

        @Test
        @DisplayName("con animales activos lanza OwnerHasActiveChildrenException y no borra")
        void con_animales_activos_lanza_excepcion_y_no_borra() {
            ownerExiste();
            when(animalChildrenQueryPort.existsActiveByOwnerId(OwnerMother.OWNER_ID))
                    .thenReturn(true);

            assertThatThrownBy(() -> service.execute(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                    .isInstanceOf(OwnerHasActiveChildrenException.class)
                    .hasMessageContaining("Cannot delete owner " + OwnerMother.OWNER_ID)
                    .hasMessageContaining("has active animal children");

            verify(repository, never()).delete(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID);
        }
    }
}
