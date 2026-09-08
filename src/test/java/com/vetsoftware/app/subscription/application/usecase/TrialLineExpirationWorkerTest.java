package com.vetsoftware.app.subscription.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.companytrialgrant.application.command.ConsumeTrialGrantCommand;
import com.vetsoftware.app.companytrialgrant.application.dto.CompanyTrialGrantDto;
import com.vetsoftware.app.companytrialgrant.application.port.in.ConsumeTrialGrantUseCase;
import com.vetsoftware.app.companytrialgrant.domain.TrialOrigin;
import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import com.vetsoftware.app.entitlement.application.command.RecalculateCompanyEntitlementsCommand;
import com.vetsoftware.app.entitlement.application.port.in.RecalculateCompanyEntitlementsUseCase;
import com.vetsoftware.app.subscription.application.port.out.TrialLineSuccessionPort;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("TrialLineExpirationWorker — vencimiento por linea")
class TrialLineExpirationWorkerTest {

    private static final Long COMPANY_ID = 42L;
    private static final LocalDate TRIAL_END = LocalDate.of(2026, 3, 30);

    @Mock
    private TrialLineSuccessionPort successionPort;
    @Mock
    private ConsumeTrialGrantUseCase consumeTrialGrant;
    @Mock
    private RecalculateCompanyEntitlementsUseCase recalculateEntitlements;

    private TrialLineExpirationWorker worker() {
        return new TrialLineExpirationWorker(successionPort, consumeTrialGrant,
                recalculateEntitlements);
    }

    @Nested
    @DisplayName("los tres desenlaces de la politica congelada")
    class TresDesenlaces {

        @Test
        @DisplayName("LIMITED abre la sucesora FREE_LIMITED")
        void limited_abre_free_limited() {
            when(successionPort.transitionIfOpen(eq(COMPANY_ID), eq(1L), eq(TRIAL_END),
                    eq("FREE_LIMITED"))).thenReturn(true);

            int transitioned = worker().processCompany(COMPANY_ID,
                    List.of(grant(1L, TrialPolicyOutcome.LIMITED)));

            assertThat(transitioned).isEqualTo(1);
            verify(successionPort).transitionIfOpen(COMPANY_ID, 1L, TRIAL_END, "FREE_LIMITED");
        }

        @Test
        @DisplayName("READ_ONLY abre la sucesora EXPIRED_READ_ONLY")
        void read_only_abre_expired_read_only() {
            when(successionPort.transitionIfOpen(eq(COMPANY_ID), eq(2L), eq(TRIAL_END),
                    eq("EXPIRED_READ_ONLY"))).thenReturn(true);

            worker().processCompany(COMPANY_ID, List.of(grant(2L, TrialPolicyOutcome.READ_ONLY)));

            verify(successionPort).transitionIfOpen(COMPANY_ID, 2L, TRIAL_END, "EXPIRED_READ_ONLY");
        }

        @Test
        @DisplayName("CONVERT_TO_PAID abre la sucesora PAID")
        void convert_to_paid_abre_paid() {
            when(successionPort.transitionIfOpen(eq(COMPANY_ID), eq(3L), eq(TRIAL_END), eq("PAID")))
                    .thenReturn(true);

            worker().processCompany(COMPANY_ID,
                    List.of(grant(3L, TrialPolicyOutcome.CONVERT_TO_PAID)));

            verify(successionPort).transitionIfOpen(COMPANY_ID, 3L, TRIAL_END, "PAID");
        }
    }

    @Test
    @DisplayName("consume la concesion dejando que resuelva su propia politica")
    void consume_la_concesion_con_desenlace_vacio() {
        when(successionPort.transitionIfOpen(any(), any(), any(), any())).thenReturn(true);

        worker().processCompany(COMPANY_ID, List.of(grant(1L, TrialPolicyOutcome.LIMITED)));

        ArgumentCaptor<ConsumeTrialGrantCommand> command = ArgumentCaptor
                .forClass(ConsumeTrialGrantCommand.class);
        verify(consumeTrialGrant).execute(command.capture());
        assertThat(command.getValue().companyId()).isEqualTo(COMPANY_ID);
        assertThat(command.getValue().catalogItemId()).isEqualTo(1L);
        assertThat(command.getValue().outcome()).isNull();
    }

    @Test
    @DisplayName("una linea ya sucedida por una compra a mitad de prueba no escribe nada mas,"
            + " pero la concesion se sigue consumiendo")
    void linea_ya_sucedida_solo_consume_la_concesion() {
        when(successionPort.transitionIfOpen(COMPANY_ID, 1L, TRIAL_END, "FREE_LIMITED"))
                .thenReturn(false);

        int transitioned = worker().processCompany(COMPANY_ID,
                List.of(grant(1L, TrialPolicyOutcome.LIMITED)));

        assertThat(transitioned).isZero();
        verify(consumeTrialGrant).execute(new ConsumeTrialGrantCommand(COMPANY_ID, 1L, null));
    }

    @Test
    @DisplayName("recalcula entitlements una sola vez por empresa, sin importar cuantas lineas"
            + " vencieron")
    void recalcula_entitlements_una_sola_vez() {
        when(successionPort.transitionIfOpen(any(), any(), any(), any())).thenReturn(true);

        worker().processCompany(COMPANY_ID,
                List.of(grant(1L, TrialPolicyOutcome.LIMITED),
                        grant(2L, TrialPolicyOutcome.READ_ONLY),
                        grant(3L, TrialPolicyOutcome.CONVERT_TO_PAID)));

        verify(recalculateEntitlements, times(1))
                .execute(new RecalculateCompanyEntitlementsCommand(COMPANY_ID));
        verify(consumeTrialGrant, times(3)).execute(any());
    }

    @Test
    @DisplayName("una empresa sin ninguna linea que vencer no toca ningun puerto")
    void sin_concesiones_no_hace_nada() {
        int transitioned = worker().processCompany(COMPANY_ID, List.of());

        assertThat(transitioned).isZero();
        verify(recalculateEntitlements).execute(any());
        verify(consumeTrialGrant, never()).execute(any());
    }

    private static CompanyTrialGrantDto grant(Long catalogItemId, TrialPolicyOutcome outcome) {
        return new CompanyTrialGrantDto(catalogItemId * 100, COMPANY_ID, catalogItemId, 900L,
                LocalDate.of(2026, 3, 1), 30, 30, TRIAL_END, 30, outcome, 800L, null,
                TrialOrigin.QUOTE, null, null, true);
    }
}
