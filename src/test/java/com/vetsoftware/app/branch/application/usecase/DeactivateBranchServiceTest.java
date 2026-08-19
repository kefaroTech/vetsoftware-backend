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

/**
 * "Eliminación" (baja de operación) de sucursal: es un cambio de estado a
 * inactiva, NO un soft-delete. Regla de integridad: NO se puede desactivar la
 * ÚLTIMA sede activa de la empresa (la dejaría sin sede operativa y bloquearía
 * citas/cuentas/POS). Desactivar una ya inactiva es idempotente y no cuenta
 * contra esa regla.
 */
@ExtendWith(MockitoExtension.class)
class DeactivateBranchServiceTest {

    @Mock
    private BranchRepository repository;
    @InjectMocks
    private DeactivateBranchService service;

    private final CityRef city = new CityRef(5L, "Bogotá");
    private final CompanyRef company = new CompanyRef(9L, "Vet SAS", "900123456");

    private Branch branch(boolean active) {
        return new Branch(3L, "Sede", "S", null, null, city, company,
                LocalDateTime.of(2020, 1, 1, 10, 0), null, active);
    }

    @Test
    void desactiva_una_activa_cuando_hay_otra_sede_activa() {
        Branch activa = branch(true);
        when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.of(activa));
        when(repository.existsOtherActiveByCompanyId(9L, 3L)).thenReturn(true); // queda otra activa
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        BranchDto dto = service.execute(3L, 9L);

        assertThat(activa.isActive()).isFalse();
        assertThat(dto.active()).isFalse();
        assertThat(dto.id()).as("sigue existiendo, no se borra").isEqualTo(3L);
        verify(repository).save(activa);
    }

    @Test
    void rechaza_desactivar_la_ultima_sede_activa_y_no_muta_ni_escribe() {
        Branch ultimaActiva = branch(true);
        when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.of(ultimaActiva));
        when(repository.existsOtherActiveByCompanyId(9L, 3L)).thenReturn(false); // no hay otra
                                                                                 // activa

        assertThatThrownBy(() -> service.execute(3L, 9L)).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("última sucursal activa");

        assertThat(ultimaActiva.isActive()).as("no debe desactivarse").isTrue();
        verify(repository, never()).save(any());
    }

    @Test
    void permite_desactivar_una_ya_inactiva_aunque_sea_la_unica_sin_consultar_el_guard() {
        Branch inactiva = branch(false);
        when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.of(inactiva));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Idempotente: como ya está inactiva, el guard ni siquiera se evalúa (no reduce
        // sedes activas).
        BranchDto dto = service.execute(3L, 9L);

        assertThat(dto.active()).isFalse();
        verify(repository, never()).existsOtherActiveByCompanyId(any(), any());
        verify(repository).save(inactiva);
    }

    @Test
    void lanza_y_no_escribe_si_no_pertenece_a_la_empresa() {
        when(repository.findByIdAndCompanyId(3L, 9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.execute(3L, 9L))
                .isInstanceOf(BranchNotFoundException.class);
        verify(repository, never()).save(any());
    }
}
