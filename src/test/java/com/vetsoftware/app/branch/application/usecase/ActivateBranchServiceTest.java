package com.vetsoftware.app.branch.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.branch.application.dto.BranchDto;
import com.vetsoftware.app.branch.application.port.out.BranchRepository;
import com.vetsoftware.app.branch.domain.Branch;
import com.vetsoftware.app.branch.domain.BranchNotFoundException;
import com.vetsoftware.app.branch.domain.CityRef;
import com.vetsoftware.app.branch.domain.CompanyRef;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Reactivación (parte del "ciclo de vida" de la sucursal, no un borrado). */
@ExtendWith(MockitoExtension.class)
class ActivateBranchServiceTest {

    @Mock
    private BranchRepository repository;
    @InjectMocks
    private ActivateBranchService service;

    private final CityRef city = new CityRef(5L, "Bogotá");
    private final CompanyRef company = new CompanyRef(9L, "Vet SAS", "900123456");

    private Branch branch(boolean active) {
        return new Branch(3L, "Sede", "S", null, null, city, company,
                LocalDateTime.of(2020, 1, 1, 10, 0), active);
    }

    @Test
    void activa_una_sucursal_inactiva_y_persiste() {
        Branch inactiva = branch(false);
        when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.of(inactiva));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BranchDto dto = service.execute(3L, 9L);

        assertThat(inactiva.isActive()).isTrue();
        assertThat(dto.active()).isTrue();
        verify(repository).save(inactiva);
    }

    @Test
    void es_idempotente_si_ya_estaba_activa() {
        Branch activa = branch(true);
        when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.of(activa));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.execute(3L, 9L).active()).isTrue();
    }

    @Test
    void lanza_y_no_escribe_si_no_pertenece_a_la_empresa() {
        when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(3L, 9L))
                .isInstanceOf(BranchNotFoundException.class);
        verify(repository, never()).save(any());
    }
}
