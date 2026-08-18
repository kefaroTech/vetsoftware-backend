package com.vetsoftware.app.owner.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.owner.application.dto.OwnerDto;
import com.vetsoftware.app.owner.application.port.out.OwnerRepository;
import com.vetsoftware.app.owner.domain.OwnerNotFoundException;
import com.vetsoftware.app.owner.testsupport.OwnerMother;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El escenario de fila afectada 0 (id inexistente o de otra empresa) ya lo
 * cubre {@link OwnerTenantGuardTest#reactivateRequiresOwnerInCurrentCompany}.
 * Esta clase cubre el camino feliz y la carrera entre el UPDATE y el SELECT
 * posterior.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ReactivateOwnerService")
class ReactivateOwnerServiceTest {

    @Mock
    private OwnerRepository repository;

    @InjectMocks
    private ReactivateOwnerService service;

    @Test
    @DisplayName("reactiva y devuelve el owner ya habilitado")
    void reactiva_y_devuelve_el_owner_ya_habilitado() {
        when(repository.reactivate(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                .thenReturn(Optional.of(OwnerMother.personaNatural()));

        OwnerDto dto = service.execute(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID);

        assertThat(dto.id()).isEqualTo(OwnerMother.OWNER_ID);
        verify(repository).reactivate(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID);
    }

    @Test
    @DisplayName("si el owner desaparece entre el UPDATE y el SELECT, falla como no-encontrado")
    void si_el_owner_desaparece_entre_el_update_y_el_select() {
        when(repository.reactivate(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID)).thenReturn(1);
        when(repository.findByIdAndCompanyId(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(OwnerMother.OWNER_ID, OwnerMother.COMPANY_ID))
                .isInstanceOf(OwnerNotFoundException.class)
                .hasMessageContaining("Owner not found: " + OwnerMother.OWNER_ID);
    }
}
