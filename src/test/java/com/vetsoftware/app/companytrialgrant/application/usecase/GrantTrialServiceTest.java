package com.vetsoftware.app.companytrialgrant.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companytrialgrant.application.command.GrantTrialCommand;
import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.application.port.out.CompanyTrialGrantRepository;
import com.vetsoftware.app.companytrialgrant.application.port.out.TrialWindowQueryPort;
import com.vetsoftware.app.companytrialgrant.domain.CompanyTrialGrant;
import com.vetsoftware.app.companytrialgrant.domain.TrialAlreadyGrantedException;
import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import com.vetsoftware.app.companytrialgrant.domain.TrialWindowNotOpenException;
import com.vetsoftware.app.companytrialgrant.domain.TrialWindowRef;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
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
@DisplayName("GrantTrialService — conceder la prueba de un artículo")
class GrantTrialServiceTest {

    private static final Long ANA = 42L;
    private static final Long INVENTARIO = 11L;
    private static final Long SERVICIOS = 13L;
    private static final Long COTIZACION = 7L;
    private static final LocalDate INICIO = LocalDate.of(2026, 9, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 9, 30);
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-09-16T09:00:00Z"),
            ZoneOffset.UTC);

    @Mock
    private CompanyTrialGrantRepository repository;
    @Mock
    private TrialWindowQueryPort trialWindowQueryPort;

    private GrantTrialService service;

    @BeforeEach
    void crearElServicio() {
        service = new GrantTrialService(repository, trialWindowQueryPort, RELOJ);
    }

    private static GrantTrialCommand comandoDe(Long articulo, LocalDate dia, int dias) {
        return new GrantTrialCommand(ANA, articulo, dia, dias, dias, TrialPolicyOutcome.LIMITED,
                COTIZACION, null);
    }

    @Nested
    @DisplayName("Creación")
    class Creacion {

        @Test
        @DisplayName("un módulo añadido el día 16 de una ventana que acaba el 30 hereda los días"
                + " que quedan, no los suyos completos")
        void un_modulo_anadido_a_mitad_de_ventana_hereda_los_dias_que_quedan() {
            when(trialWindowQueryPort.findOpenByCompanyId(ANA))
                    .thenReturn(Optional.of(new TrialWindowRef(5L, ANA, INICIO, FIN, true)));
            when(repository.existsByCompanyIdAndCatalogItemId(ANA, INVENTARIO)).thenReturn(false);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            CompanyTrialGrantDto concedida = service
                    .execute(comandoDe(INVENTARIO, LocalDate.of(2026, 9, 16), 30));

            assertThat(concedida.trialEndDate()).isEqualTo(FIN);
            assertThat(concedida.effectiveDays()).isEqualTo(15);
            assertThat(concedida.daysGranted()).isEqualTo(30);
        }

        @Test
        @DisplayName("la concesión cuelga de la ventana de SU empresa, resuelta por el puerto"
                + " acotado")
        void la_concesion_cuelga_de_la_ventana_de_su_empresa() {
            when(trialWindowQueryPort.findOpenByCompanyId(ANA))
                    .thenReturn(Optional.of(new TrialWindowRef(5L, ANA, INICIO, FIN, true)));
            when(repository.existsByCompanyIdAndCatalogItemId(ANA, INVENTARIO)).thenReturn(false);
            when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

            service.execute(comandoDe(INVENTARIO, LocalDate.of(2026, 9, 16), 30));

            ArgumentCaptor<CompanyTrialGrant> guardada = ArgumentCaptor
                    .forClass(CompanyTrialGrant.class);
            verify(repository).save(guardada.capture());
            assertThat(guardada.getValue().getTrialWindowId()).isEqualTo(5L);
            assertThat(guardada.getValue().getTrialWindowEndDate()).isEqualTo(FIN);
            assertThat(guardada.getValue().getCompanyId()).isEqualTo(ANA);
        }
    }

    @Nested
    @DisplayName("R-TRIAL-04 · un artículo no se regala dos veces, jamás")
    class UnaSolaVez {

        @Test
        @DisplayName("quitar Servicios el día 20 y reponerlo el 22 no crea una concesión nueva")
        void quitar_servicios_el_dia_20_y_reponerlo_el_22_no_crea_una_nueva() {
            when(repository.existsByCompanyIdAndCatalogItemId(ANA, SERVICIOS)).thenReturn(true);

            assertThatThrownBy(
                    () -> service.execute(comandoDe(SERVICIOS, LocalDate.of(2026, 9, 22), 30)))
                    .isInstanceOf(TrialAlreadyGrantedException.class)
                    .hasMessageContaining("never granted twice");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("conceder Agenda en una ventana de 2028 a quien ya la probó en 2026 falla")
        void conceder_agenda_anos_despues_a_quien_ya_la_probo_falla() {
            when(repository.existsByCompanyIdAndCatalogItemId(ANA, 10L)).thenReturn(true);

            assertThatThrownBy(() -> service.execute(comandoDe(10L, LocalDate.of(2028, 5, 1), 30)))
                    .isInstanceOf(TrialAlreadyGrantedException.class);

            verify(repository, never()).save(any());
        }
    }

    @Nested
    @DisplayName("R-TRIAL-09 · la ventana tiene que estar abierta")
    class VentanaAbierta {

        @Test
        @DisplayName("añadir un módulo sin ventana viva entra pagando, no en prueba")
        void anadir_un_modulo_sin_ventana_viva_entra_pagando_no_en_prueba() {
            when(repository.existsByCompanyIdAndCatalogItemId(ANA, INVENTARIO)).thenReturn(false);
            when(trialWindowQueryPort.findOpenByCompanyId(ANA)).thenReturn(Optional.empty());

            assertThatThrownBy(
                    () -> service.execute(comandoDe(INVENTARIO, LocalDate.of(2026, 10, 5), 30)))
                    .isInstanceOf(TrialWindowNotOpenException.class)
                    .hasMessageContaining("added as paid");

            verify(repository, never()).save(any());
        }

        @Test
        @DisplayName("añadir un módulo el día 35 de una ventana de 30 tampoco entra en prueba")
        void anadir_un_modulo_el_dia_35_de_una_ventana_de_30_entra_pagando_no_en_prueba() {
            when(repository.existsByCompanyIdAndCatalogItemId(ANA, INVENTARIO)).thenReturn(false);
            when(trialWindowQueryPort.findOpenByCompanyId(ANA))
                    .thenReturn(Optional.of(new TrialWindowRef(5L, ANA, INICIO, FIN, true)));

            assertThatThrownBy(
                    () -> service.execute(comandoDe(INVENTARIO, LocalDate.of(2026, 10, 5), 30)))
                    .isInstanceOf(TrialWindowNotOpenException.class);

            verify(repository, never()).save(any());
        }
    }
}
