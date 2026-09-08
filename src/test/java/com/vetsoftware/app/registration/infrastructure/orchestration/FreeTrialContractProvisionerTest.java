package com.vetsoftware.app.registration.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.companytrialgrant.application.command.GrantTrialCommand;
import com.vetsoftware.app.companytrialgrant.application.port.in.GrantTrialUseCase;
import com.vetsoftware.app.companytrialgrant.domain.TrialPolicyOutcome;
import com.vetsoftware.app.companytrialwindow.application.command.OpenTrialWindowCommand;
import com.vetsoftware.app.companytrialwindow.application.dto.CompanyTrialWindowDto;
import com.vetsoftware.app.companytrialwindow.application.port.in.OpenTrialWindowUseCase;
import com.vetsoftware.app.companytrialwindow.domain.TrialOrigin;
import com.vetsoftware.app.entitlement.application.port.in.InitializeCompanyEntitlementsUseCase;
import com.vetsoftware.app.registration.application.port.out.EligibleTrialCatalogItemsPort;
import com.vetsoftware.app.registration.application.port.out.EligibleTrialCatalogItemsPort.EligibleTrialCatalogItem;
import com.vetsoftware.app.registration.domain.PlatformCatalogNotConfiguredException;
import com.vetsoftware.app.subscription.application.command.CreateInitialSubscriptionCommand;
import com.vetsoftware.app.subscription.application.port.in.CreateInitialSubscriptionUseCase;
import com.vetsoftware.app.subscription.domain.PlatformCatalogNotConfiguredForSubscriptionException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("FreeTrialContractProvisioner — el alta gratuita, paso a paso")
class FreeTrialContractProvisionerTest {

    private static final Long EMPRESA = 42L;
    private static final LocalDate HOY = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIN_DE_VENTANA = LocalDate.of(2026, 1, 30);

    @Mock
    private OpenTrialWindowUseCase openTrialWindowUseCase;
    @Mock
    private GrantTrialUseCase grantTrialUseCase;
    @Mock
    private EligibleTrialCatalogItemsPort eligibleTrialCatalogItemsPort;
    @Mock
    private CreateInitialSubscriptionUseCase createInitialSubscriptionUseCase;
    @Mock
    private InitializeCompanyEntitlementsUseCase initializeCompanyEntitlementsUseCase;
    @Mock
    private SystemAuthRunner systemAuthRunner;

    private FreeTrialContractProvisioner provisioner;

    @BeforeEach
    void setUp() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-01T10:00:00Z"), ZoneOffset.UTC);
        provisioner = new FreeTrialContractProvisioner(openTrialWindowUseCase, grantTrialUseCase,
                eligibleTrialCatalogItemsPort, createInitialSubscriptionUseCase,
                initializeCompanyEntitlementsUseCase, systemAuthRunner, clock);
        doAnswer(inv -> {
            ((Runnable) inv.getArgument(0)).run();
            return null;
        }).when(systemAuthRunner).run(any());
        when(systemAuthRunner.call(any()))
                .thenAnswer(inv -> ((Supplier<?>) inv.getArgument(0)).get());
    }

    private static CompanyTrialWindowDto ventanaAbierta() {
        return new CompanyTrialWindowDto(9L, EMPRESA, HOY, FIN_DE_VENTANA, 30, null,
                TrialOrigin.SIGNUP, null, true);
    }

    @Test
    @DisplayName("abre la ventana SIGNUP, concede cada artículo elegible y firma el contrato con"
            + " el fin de la ventana, antes de derivar los permisos")
    void ejecuta_los_cuatro_pasos_en_orden() {
        when(openTrialWindowUseCase.execute(any())).thenReturn(ventanaAbierta());
        when(eligibleTrialCatalogItemsPort.findAll(any()))
                .thenReturn(List.of(new EligibleTrialCatalogItem(100L, 30, "LIMITED"),
                        new EligibleTrialCatalogItem(105L, 30, "READ_ONLY")));

        provisioner.provision(EMPRESA, "Veterinaria Vetrina");

        ArgumentCaptor<OpenTrialWindowCommand> ventana = ArgumentCaptor
                .forClass(OpenTrialWindowCommand.class);
        verify(openTrialWindowUseCase).execute(ventana.capture());
        assertThat(ventana.getValue().companyId()).isEqualTo(EMPRESA);
        assertThat(ventana.getValue().windowDays()).isEqualTo(30);
        assertThat(ventana.getValue().sourceQuoteId()).isNull();

        ArgumentCaptor<GrantTrialCommand> concesiones = ArgumentCaptor
                .forClass(GrantTrialCommand.class);
        verify(grantTrialUseCase, times(2)).execute(concesiones.capture());
        assertThat(concesiones.getAllValues()).extracting(GrantTrialCommand::catalogItemId)
                .containsExactly(100L, 105L);
        assertThat(concesiones.getAllValues())
                .allSatisfy(command -> assertThat(command.sourceQuoteId()).isNull());
        assertThat(concesiones.getAllValues().get(1).policyTrialOutcome())
                .isEqualTo(TrialPolicyOutcome.READ_ONLY);

        ArgumentCaptor<CreateInitialSubscriptionCommand> contrato = ArgumentCaptor
                .forClass(CreateInitialSubscriptionCommand.class);
        verify(createInitialSubscriptionUseCase).execute(contrato.capture());
        assertThat(contrato.getValue().trialEndDate()).isEqualTo(FIN_DE_VENTANA);

        // El orden es la regla, no un detalle: las concesiones tienen que existir
        // antes de que se firme la linea que las referencia
        // (fk_subscription_items_trial_grant), y el contrato antes de derivar permisos.
        var orden = inOrder(openTrialWindowUseCase, grantTrialUseCase,
                createInitialSubscriptionUseCase, initializeCompanyEntitlementsUseCase);
        orden.verify(openTrialWindowUseCase).execute(any());
        orden.verify(grantTrialUseCase, times(2)).execute(any());
        orden.verify(createInitialSubscriptionUseCase).execute(any());
        orden.verify(initializeCompanyEntitlementsUseCase).execute(any());
    }

    @Test
    @DisplayName("si el catálogo no está sembrado, la señal de subscription se traduce y sigue"
            + " fallando entero (#364): no deriva permisos de un contrato que no existe")
    void si_falta_catalogo_sigue_fallando_entero() {
        when(openTrialWindowUseCase.execute(any())).thenReturn(ventanaAbierta());
        when(eligibleTrialCatalogItemsPort.findAll(any()))
                .thenReturn(List.of(new EligibleTrialCatalogItem(100L, 30, "LIMITED")));
        when(createInitialSubscriptionUseCase.execute(any()))
                .thenThrow(new PlatformCatalogNotConfiguredForSubscriptionException(EMPRESA));

        assertThatThrownBy(() -> provisioner.provision(EMPRESA, "Veterinaria Vetrina"))
                .isInstanceOf(PlatformCatalogNotConfiguredException.class)
                .hasMessageContaining("Veterinaria Vetrina");

        verify(initializeCompanyEntitlementsUseCase, never()).execute(any());
    }
}
