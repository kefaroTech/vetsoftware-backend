package com.vetsoftware.app.spa.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.spa.application.command.ChangeSpaStatusCommand;
import com.vetsoftware.app.spa.application.dto.SpaDto;
import com.vetsoftware.app.spa.application.port.out.SpaRepository;
import com.vetsoftware.app.spa.domain.Spa;
import com.vetsoftware.app.spa.domain.SpaNotFoundException;
import com.vetsoftware.app.spa.domain.SpaStatus;
import com.vetsoftware.app.spa.testsupport.SpaMother;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChangeSpaStatusService")
class ChangeSpaStatusServiceTest {

    @Mock
    private SpaRepository repository;

    private ChangeSpaStatusService service;

    @BeforeEach
    void crearServicio() {
        service = new ChangeSpaStatusService(repository);
    }

    @Nested
    @DisplayName("cambio de estado")
    class CambioDeEstado {

        @Test
        @DisplayName("con companyId busca por empresa, cambia el estado y persiste")
        void con_company_id_busca_por_empresa() {
            Spa existente = SpaMother.spaValido();
            when(repository.findByIdAndCompanyId(5L, SpaMother.CLINICA.id()))
                    .thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SpaDto dto = service
                    .execute(new ChangeSpaStatusCommand(5L, "completado", SpaMother.CLINICA.id()));

            ArgumentCaptor<Spa> guardado = ArgumentCaptor.forClass(Spa.class);
            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getStatus()).isEqualTo(SpaStatus.COMPLETADO);
            assertThat(dto.status()).isEqualTo("COMPLETADO");
        }

        @Test
        @DisplayName("sin companyId busca por id global")
        void sin_company_id_busca_por_id_global() {
            Spa existente = SpaMother.spaValido();
            when(repository.findById(5L)).thenReturn(Optional.of(existente));
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(new ChangeSpaStatusCommand(5L, "CANCELADO", null));

            verify(repository, never()).findByIdAndCompanyId(any(), any());
        }
    }

    @Nested
    @DisplayName("validaciones")
    class Validaciones {

        @Test
        @DisplayName("no toca el estado si el spa no existe en esa empresa")
        void no_toca_el_estado_si_el_spa_no_existe() {
            when(repository.findByIdAndCompanyId(5L, SpaMother.CLINICA.id()))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service
                    .execute(new ChangeSpaStatusCommand(5L, "COMPLETADO", SpaMother.CLINICA.id())))
                    .isInstanceOf(SpaNotFoundException.class).hasMessageContaining("5");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("un status fuera del enum no guarda nada")
        void un_status_fuera_del_enum_no_guarda_nada() {
            when(repository.findByIdAndCompanyId(5L, SpaMother.CLINICA.id()))
                    .thenReturn(Optional.of(SpaMother.spaValido()));

            assertThatThrownBy(() -> service
                    .execute(new ChangeSpaStatusCommand(5L, "NO_EXISTE", SpaMother.CLINICA.id())))
                    .isInstanceOf(IllegalArgumentException.class);

            verify(repository, never()).save(any());
        }
    }
}
