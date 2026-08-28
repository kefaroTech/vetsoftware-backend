package com.vetsoftware.app.companytrialwindow.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companytrialwindow.application.command.OpenTrialWindowCommand;
import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import com.vetsoftware.app.companytrialwindow.application.port.out.CompanyTrialWindowRepository;
import com.vetsoftware.app.companytrialwindow.domain.CompanyAlreadyHasOpenTrialWindowException;
import com.vetsoftware.app.companytrialwindow.domain.CompanyTrialWindow;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("OpenTrialWindowService — abrir el reloj de la empresa")
class OpenTrialWindowServiceTest {

    private static final Long ANA = 42L;
    private static final Long COTIZACION = 7L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-09-01T08:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private CompanyTrialWindowRepository repository;

    private OpenTrialWindowService service;

    @BeforeEach
    void crearElServicio() {
        service = new OpenTrialWindowService(repository, RELOJ);
    }

    @Test
    @DisplayName("R-TRIAL-11 · un alta que solo quiere el plan gratuito crea igualmente su ventana,"
            + " con el fin derivado y no elegido")
    void un_alta_que_solo_quiere_el_plan_gratuito_crea_igualmente_su_ventana() {
        when(repository.existsOpenByCompanyId(ANA)).thenReturn(false);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyTrialWindowDto abierta = service
                .execute(new OpenTrialWindowCommand(ANA, LocalDate.of(2026, 9, 1), 30, COTIZACION));

        assertThat(abierta.endDate()).isEqualTo(LocalDate.of(2026, 9, 30));
        assertThat(abierta.open()).isTrue();
        ArgumentCaptor<CompanyTrialWindow> guardada = ArgumentCaptor
                .forClass(CompanyTrialWindow.class);
        verify(repository).save(guardada.capture());
        assertThat(guardada.getValue().getSourceQuoteId()).isEqualTo(COTIZACION);
    }

    @Test
    @DisplayName("R-TRIAL-01 · con una ventana ya abierta no escribe nada y dice qué pasó")
    void con_una_ventana_ya_abierta_no_escribe_nada() {
        when(repository.existsOpenByCompanyId(ANA)).thenReturn(true);

        assertThatThrownBy(() -> service.execute(
                new OpenTrialWindowCommand(ANA, LocalDate.of(2026, 10, 1), 30, COTIZACION)))
                .isInstanceOf(CompanyAlreadyHasOpenTrialWindowException.class)
                .hasMessageContaining("two ceilings valid at once");

        verify(repository, never()).save(any());
    }
}
