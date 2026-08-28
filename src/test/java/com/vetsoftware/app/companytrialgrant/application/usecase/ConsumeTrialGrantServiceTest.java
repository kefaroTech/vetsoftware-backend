package com.vetsoftware.app.companytrialgrant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companytrialgrant.application.command.ConsumeTrialGrantCommand;
import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.application.port.out.CompanyTrialGrantRepository;
import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrant;
import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrantNotFoundException;
import com.vetsoftware.app.companytrialgrant.domain.TrialOutcome;
import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import com.vetsoftware.app.companytrialgrant.domain.TrialWindowRef;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ConsumeTrialGrantService — resolver una prueba")
class ConsumeTrialGrantServiceTest {

    private static final Long ANA = 42L;
    private static final Long INVENTARIO = 11L;
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-10-01T00:05:00Z"),
            ZoneOffset.UTC);

    @Mock
    private CompanyTrialGrantRepository repository;

    private ConsumeTrialGrantService service;

    @BeforeEach
    void crearElServicio() {
        service = new ConsumeTrialGrantService(repository, RELOJ);
    }

    private static CompanyTrialGrant concesionCon(TrialPolicyOutcome politica) {
        return CompanyTrialGrant.grant(
                new TrialWindowRef(5L, ANA, LocalDate.of(2026, 9, 1), LocalDate.of(2026, 9, 30),
                        true),
                INVENTARIO, LocalDate.of(2026, 9, 1), 30, 30, politica, 7L, null,
                LocalDateTime.of(2026, 9, 1, 8, 0));
    }

    @Test
    @DisplayName("R-TRIAL-28 · sin desenlace dado se deriva de la política congelada en la"
            + " concesión, no de la que el catálogo tenga hoy")
    void sin_desenlace_dado_se_deriva_de_la_politica_congelada() {
        when(repository.findByCompanyIdAndCatalogItemId(ANA, INVENTARIO))
                .thenReturn(Optional.of(concesionCon(TrialPolicyOutcome.READ_ONLY)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyTrialGrantDto resuelta = service
                .execute(new ConsumeTrialGrantCommand(ANA, INVENTARIO, null));

        assertThat(resuelta.outcome()).isEqualTo(TrialOutcome.READ_ONLY);
        assertThat(resuelta.live()).isFalse();
    }

    @Test
    @DisplayName("R-TRIAL-30 · quitar el módulo antes de vencer marca ABANDONED, no el desenlace"
            + " de la política")
    void quitar_el_modulo_antes_de_vencer_marca_ABANDONED() {
        when(repository.findByCompanyIdAndCatalogItemId(ANA, INVENTARIO))
                .thenReturn(Optional.of(concesionCon(TrialPolicyOutcome.CONVERT_TO_PAID)));
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        CompanyTrialGrantDto resuelta = service
                .execute(new ConsumeTrialGrantCommand(ANA, INVENTARIO, TrialOutcome.ABANDONED));

        assertThat(resuelta.outcome()).isEqualTo(TrialOutcome.ABANDONED);
    }

    @Test
    @DisplayName("sin concesión no escribe nada")
    void sin_concesion_no_escribe_nada() {
        when(repository.findByCompanyIdAndCatalogItemId(ANA, INVENTARIO))
                .thenReturn(Optional.empty());

        assertThatThrownBy(
                () -> service.execute(new ConsumeTrialGrantCommand(ANA, INVENTARIO, null)))
                .isInstanceOf(CompanyTrialGrantNotFoundException.class);

        verify(repository, never()).save(any());
    }
}
