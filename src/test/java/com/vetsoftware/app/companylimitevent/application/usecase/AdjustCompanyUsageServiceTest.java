package com.vetsoftware.app.companylimitevent.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companylimitevent.application.command.AdjustCompanyUsageCommand;
import com.vetsoftware.app.companylimitevent.application.port.out.CompanyLimitEventRepository;
import com.vetsoftware.app.companylimitevent.application.port.out.CompanyUsageAdjustmentPort;
import com.vetsoftware.app.companylimitevent.domain.CompanyLimitEvent;
import com.vetsoftware.app.companylimitevent.domain.LimitEventType;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdjustCompanyUsageService — la válvula de escape auditable")
class AdjustCompanyUsageServiceTest {

    /**
     * Reloj fijo, no un doble: un {@code Clock} no es un puerto y se construye de
     * verdad.
     */
    private static final Clock RELOJ = Clock.fixed(Instant.parse("2026-03-14T15:30:00Z"),
            ZoneOffset.UTC);

    @Mock
    private CompanyLimitEventRepository repository;
    @Mock
    private CompanyUsageAdjustmentPort usageAdjustmentPort;

    private AdjustCompanyUsageService service;

    @org.junit.jupiter.api.BeforeEach
    void crearElServicio() {
        service = new AdjustCompanyUsageService(repository, usageAdjustmentPort, RELOJ);
    }

    @Test
    @DisplayName("R-LIMIT-19 · corregir 500 mascotas duplicadas de una migración escribe"
            + " USAGE_ADJUSTED y no un update del contador escrito a mano")
    void corregir_500_mascotas_duplicadas_de_una_migracion_escribe_USAGE_ADJUSTED_y_no_un_update_del_contador() {
        when(usageAdjustmentPort.currentUsage(42L, "ANIMAL"))
                .thenReturn(new CompanyUsageAdjustmentPort.UsageSnapshot(100, 600));
        when(usageAdjustmentPort.adjustUsage(42L, "ANIMAL", -500)).thenReturn(100);
        when(repository.append(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(new AdjustCompanyUsageCommand(42L, 1L, "ANIMAL", -500, 3L, "MIGRATION",
                "Migración duplicada del 14/03, ticket SOP-118"));

        ArgumentCaptor<CompanyLimitEvent> escrito = ArgumentCaptor
                .forClass(CompanyLimitEvent.class);
        verify(repository).append(escrito.capture());
        assertThat(escrito.getValue()).satisfies(hecho -> {
            assertThat(hecho.getEventType()).isEqualTo(LimitEventType.USAGE_ADJUSTED);
            assertThat(hecho.getRequestedDelta()).isEqualTo(-500);
            assertThat(hecho.getActor().systemUserId()).isEqualTo(3L);
            assertThat(hecho.getReasonCode()).isEqualTo("MIGRATION");
            assertThat(hecho.getReason()).contains("SOP-118");
        });
        verify(usageAdjustmentPort).adjustUsage(42L, "ANIMAL", -500);
    }

    @Test
    @DisplayName("copia los dos números de ANTES de mover el contador: si copiara los de después,"
            + " la fila no diría de qué cifra se partía")
    void copia_los_dos_numeros_de_antes_de_mover_el_contador() {
        when(usageAdjustmentPort.currentUsage(42L, "ANIMAL"))
                .thenReturn(new CompanyUsageAdjustmentPort.UsageSnapshot(100, 600));
        when(usageAdjustmentPort.adjustUsage(42L, "ANIMAL", -500)).thenReturn(100);
        when(repository.append(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.execute(new AdjustCompanyUsageCommand(42L, 1L, "ANIMAL", -500, 3L, "MIGRATION",
                "Migración duplicada"));

        ArgumentCaptor<CompanyLimitEvent> escrito = ArgumentCaptor
                .forClass(CompanyLimitEvent.class);
        verify(repository).append(escrito.capture());
        assertThat(escrito.getValue().getUsedQuantity()).isEqualTo(600);
        assertThat(escrito.getValue().getLimitQuantity()).isEqualTo(100);
    }
}
