package com.vetsoftware.app.subscription.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionProrationLine;
import com.vetsoftware.app.subscription.domain.TaxTreatment;
import com.vetsoftware.app.subscriptionbilling.application.command.CreateSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.port.in.CreateSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("SubscriptionBillingProrationChargeAdapter - cruza a subscriptionbilling bajo SYSTEM")
class SubscriptionBillingProrationChargeAdapterTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final Long LINEA = 500L;
    private static final Long OTROSI = 900L;

    @Mock
    private CreateSubscriptionChargeUseCase createChargeUseCase;
    @Mock
    private SystemAuthRunner systemAuthRunner;

    @InjectMocks
    private SubscriptionBillingProrationChargeAdapter adapter;

    private static SubscriptionProrationLine linea(TaxTreatment tratamiento) {
        return new SubscriptionProrationLine(EMPRESA, CONTRATO, LINEA, "Modulo extra",
                LocalDate.of(2026, 9, 20), LocalDate.of(2026, 10, 9), BigDecimal.ONE,
                new BigDecimal("30000.00"), new BigDecimal("20000.00"), new BigDecimal("19.00"),
                tratamiento, 20, 30, OTROSI);
    }

    @BeforeEach
    void ejecutaElBloqueDeInmediato() {
        // El adaptador solo importa QUE corra bajo SystemAuthRunner.run, no cuando: el
        // mock ejecuta el bloque en el acto para poder verificar el cargo que produce.
        doAnswer(invocacion -> {
            invocacion.getArgument(0, Runnable.class).run();
            return null;
        }).when(systemAuthRunner).run(any());
    }

    @Test
    @DisplayName("cobra dentro de SystemAuthRunner.run y no fuera de el")
    void cobra_dentro_de_system_auth_runner() {
        adapter.chargeProration(linea(TaxTreatment.TAXED));

        verify(systemAuthRunner).run(any());
        verify(createChargeUseCase).execute(any());
    }

    @Test
    @DisplayName("el cargo va con ChargeType.PRORATION y arrastra empresa, contrato, linea, periodo, "
            + "importes y el otrosi")
    void el_cargo_va_con_charge_type_proration() {
        adapter.chargeProration(linea(TaxTreatment.TAXED));

        ArgumentCaptor<CreateSubscriptionChargeCommand> captor = ArgumentCaptor
                .forClass(CreateSubscriptionChargeCommand.class);
        verify(createChargeUseCase).execute(captor.capture());
        CreateSubscriptionChargeCommand comando = captor.getValue();
        assertThat(comando.chargeType()).isEqualTo(ChargeType.PRORATION);
        assertThat(comando.companyId()).isEqualTo(EMPRESA);
        assertThat(comando.subscriptionId()).isEqualTo(CONTRATO);
        assertThat(comando.subscriptionItemId()).isEqualTo(LINEA);
        assertThat(comando.servicePeriodStart()).isEqualTo(LocalDate.of(2026, 9, 20));
        assertThat(comando.servicePeriodEnd()).isEqualTo(LocalDate.of(2026, 10, 9));
        assertThat(comando.subtotalAmount()).isEqualByComparingTo("20000.00");
        assertThat(comando.prorationDays()).isEqualTo(20);
        assertThat(comando.periodDays()).isEqualTo(30);
        assertThat(comando.amendmentId()).isEqualTo(OTROSI);
    }

    @ParameterizedTest
    @EnumSource(TaxTreatment.class)
    @DisplayName("traduce el TaxTreatment de subscription al homonimo de subscriptionbilling, uno a "
            + "uno y sin colapsar ninguno")
    void traduce_el_tax_treatment_uno_a_uno(TaxTreatment origen) {
        adapter.chargeProration(linea(origen));

        ArgumentCaptor<CreateSubscriptionChargeCommand> captor = ArgumentCaptor
                .forClass(CreateSubscriptionChargeCommand.class);
        verify(createChargeUseCase).execute(captor.capture());
        assertThat(captor.getValue().taxTreatment().name()).isEqualTo(origen.name());
    }
}
