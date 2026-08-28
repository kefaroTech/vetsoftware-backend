package com.vetsoftware.app.subscriptionpayment.application.port.in;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.auth.application.dto.EmployeeContext;
import com.vetsoftware.app.auth.infrastructure.security.Authz;
import com.vetsoftware.app.dunning.application.command.RecordDunningEventCommand;
import com.vetsoftware.app.dunning.application.dto.DunningEventDto;
import com.vetsoftware.app.dunning.application.port.in.RecordDunningEventUseCase;
import com.vetsoftware.app.dunning.domain.DunningEventType;
import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.SendQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.CreateQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.DeleteQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.SendQuoteUseCase;
import com.vetsoftware.app.subscription.application.command.CreateRequestedSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.CreateSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.AddSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.command.CancelSubscriptionCommand;
import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionItemQuantityCommand;
import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionStatusCommand;
import com.vetsoftware.app.subscription.application.command.RemoveSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionDto;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import com.vetsoftware.app.subscription.application.port.in.AddSubscriptionItemUseCase;
import com.vetsoftware.app.subscription.application.port.in.CancelSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionItemQuantityUseCase;
import com.vetsoftware.app.subscription.application.port.in.ChangeSubscriptionStatusUseCase;
import com.vetsoftware.app.subscription.application.port.in.CreateRequestedSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.CreateSubscriptionUseCase;
import com.vetsoftware.app.subscription.application.port.in.RemoveSubscriptionItemUseCase;
import com.vetsoftware.app.subscription.domain.BillingCycle;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscriptionpayment.application.command.ApplyBillingDocumentCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.ChangeSubscriptionPaymentStatusCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.ReconcileSubscriptionPaymentCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.RegisterSubscriptionPaymentCommand;
import com.vetsoftware.app.subscriptionpayment.application.command.ReverseBillingDocumentApplicationCommand;
import com.vetsoftware.app.subscriptionpayment.application.dto.BillingDocumentApplicationDto;
import com.vetsoftware.app.subscriptionpayment.application.dto.SubscriptionPaymentDto;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(SaasBillingMutationAuthorizationTest.Cableado.class)
@DisplayName("Cobro SaaS — mutaciones exclusivas de plataforma")
class SaasBillingMutationAuthorizationTest {

    private static final Long COMPANY_ID = 42L;

    @Autowired
    private RegisterSubscriptionPaymentUseCase registerPayment;
    @Autowired
    private ChangeSubscriptionPaymentStatusUseCase changePaymentStatus;
    @Autowired
    private ReconcileSubscriptionPaymentUseCase reconcilePayment;
    @Autowired
    private ApplyBillingDocumentUseCase applyDocument;
    @Autowired
    private ReverseBillingDocumentApplicationUseCase reverseApplication;
    @Autowired
    private RecordDunningEventUseCase recordDunningEvent;
    @Autowired
    private CreateRequestedSubscriptionUseCase createSubscription;
    @Autowired
    private AddSubscriptionItemUseCase addSubscriptionItem;
    @Autowired
    private RemoveSubscriptionItemUseCase removeSubscriptionItem;
    @Autowired
    private ChangeSubscriptionItemQuantityUseCase changeSubscriptionItemQuantity;
    @Autowired
    private ChangeSubscriptionStatusUseCase changeSubscriptionStatus;
    @Autowired
    private CancelSubscriptionUseCase cancelSubscription;
    @Autowired
    private CreateQuoteUseCase createQuote;
    @Autowired
    private SendQuoteUseCase sendQuote;
    @Autowired
    private CreateSubscriptionUseCase createSubscriptionCore;
    @Autowired
    private DeleteQuoteUseCase deleteQuote;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("ADMIN tenant")
    class TenantAdmin {

        @Test
        void no_puede_registrar_pagos_aunque_tenga_la_authority_heredada() {
            authenticateTenantAdmin();
            assertThatThrownBy(() -> registerPayment.execute(registerCommand()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void no_puede_cambiar_el_estado_de_un_pago() {
            authenticateTenantAdmin();
            assertThatThrownBy(() -> changePaymentStatus.execute(changeStatusCommand()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void no_puede_conciliar_un_pago() {
            authenticateTenantAdmin();
            assertThatThrownBy(() -> reconcilePayment
                    .execute(new ReconcileSubscriptionPaymentCommand(7L, COMPANY_ID)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void no_puede_aplicar_un_pago_a_una_factura_saas() {
            authenticateTenantAdmin();
            assertThatThrownBy(() -> applyDocument.execute(applyCommand()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void no_puede_revertir_una_aplicacion() {
            authenticateTenantAdmin();
            assertThatThrownBy(() -> reverseApplication
                    .execute(new ReverseBillingDocumentApplicationCommand(500L, COMPANY_ID)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void no_puede_anotar_eventos_de_cobranza() {
            authenticateTenantAdmin();
            assertThatThrownBy(() -> recordDunningEvent.execute(dunningCommand()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void no_puede_fijar_estado_periodos_gracia_ni_renovacion_del_contrato() {
            authenticateTenantAdmin();
            assertThatThrownBy(() -> createSubscription.execute(subscriptionCommand()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        /**
         * Las dos que siguen siendo exclusivas de plataforma, y por motivos distintos:
         * el cuerpo de {@code addItem} trae {@code unitAmount} —abrirlo seria un alta
         * gratuita autoservida— y el estado del contrato es la palanca de cobro.
         */
        @Test
        void no_puede_anadir_lineas_al_contrato_ni_forzar_su_estado() {
            authenticateTenantAdmin();

            assertThatThrownBy(() -> addSubscriptionItem.execute(addItemCommand()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> changeSubscriptionStatus.execute(changeStatusSubscription()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        /**
         * Las tres de autoservicio siguen cerradas a un ADMIN que NO tiene concedidas
         * {@code subscription.update} ni {@code subscription.cancel}: el rol
         * empresarial no basta, se autoriza por permiso granular.
         */
        @Test
        void sin_el_permiso_concedido_tampoco_puede_autoservirse() {
            authenticateTenantAdmin();

            assertThatThrownBy(() -> removeSubscriptionItem.execute(removeItemCommand()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                    () -> changeSubscriptionItemQuantity.execute(changeQuantityCommand()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> cancelSubscription.execute(cancelCommand()))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void no_puede_fijar_ni_enviar_terminos_crudos_de_una_cotizacion() {
            authenticateTenantAdmin();

            assertThatThrownBy(() -> createQuote.execute(createQuoteCommand()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> sendQuote.execute(new SendQuoteCommand(31L, COMPANY_ID)))
                    .isInstanceOf(AccessDeniedException.class);
        }

        @Test
        void no_puede_saltar_la_resolucion_comercial_ni_borrar_cotizaciones() {
            authenticateTenantAdmin();

            assertThatThrownBy(() -> createSubscriptionCore.execute(createSubscriptionCommand()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> deleteQuote.execute(31L, COMPANY_ID))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    /**
     * El autoservicio del contrato: lo que la clienta SI puede hacer desde su
     * propia cuenta. Son las tres operaciones cuyo cuerpo no lleva precio —el
     * cliente elige cuantas unidades o si se va, nunca a cuanto— y por eso son las
     * tres que se abrieron. Con ellas vuelve a ser alcanzable
     * {@code subscription_amendments.requested_by_employee_id}, que con el puerto
     * cerrado a SYSTEM no se podia escribir nunca por HTTP.
     */
    @Nested
    @DisplayName("Autoservicio del tenant")
    class AutoservicioDelTenant {

        @Test
        void puede_bajar_una_linea_cambiar_cantidad_y_cancelar_su_propio_contrato() {
            authenticateEmployee(COMPANY_ID);

            assertThat(removeSubscriptionItem.execute(removeItemCommand())).isNull();
            assertThat(changeSubscriptionItemQuantity.execute(changeQuantityCommand())).isNull();
            assertThat(cancelSubscription.execute(cancelCommand())).isNull();
        }

        /**
         * La mitad que de verdad importa del gate: tener el permiso no dice sobre QUE
         * filas. Con la misma authority pero desde otra empresa, {@code isMyCompany} es
         * falso y no pasa nada.
         */
        @Test
        void no_puede_tocar_el_contrato_de_otra_clinica_aunque_tenga_el_permiso() {
            authenticateEmployee(COMPANY_ID + 1);

            assertThatThrownBy(() -> removeSubscriptionItem.execute(removeItemCommand()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(
                    () -> changeSubscriptionItemQuantity.execute(changeQuantityCommand()))
                    .isInstanceOf(AccessDeniedException.class);
            assertThatThrownBy(() -> cancelSubscription.execute(cancelCommand()))
                    .isInstanceOf(AccessDeniedException.class);
        }
    }

    @Nested
    @DisplayName("SYSTEM")
    class SystemActor {

        @Test
        void puede_ejecutar_las_seis_mutaciones_de_plataforma() {
            authenticateSystem();

            assertThat(registerPayment.execute(registerCommand())).isNull();
            assertThat(changePaymentStatus.execute(changeStatusCommand())).isNull();
            assertThat(reconcilePayment
                    .execute(new ReconcileSubscriptionPaymentCommand(7L, COMPANY_ID))).isNull();
            assertThat(applyDocument.execute(applyCommand())).isNull();
            assertThat(reverseApplication
                    .execute(new ReverseBillingDocumentApplicationCommand(500L, COMPANY_ID)))
                    .isNull();
            assertThat(recordDunningEvent.execute(dunningCommand())).isNull();
            assertThat(createSubscription.execute(subscriptionCommand())).isNull();
            assertThat(addSubscriptionItem.execute(addItemCommand())).isNull();
            assertThat(removeSubscriptionItem.execute(removeItemCommand())).isNull();
            assertThat(changeSubscriptionItemQuantity.execute(changeQuantityCommand())).isNull();
            assertThat(changeSubscriptionStatus.execute(changeStatusSubscription())).isNull();
            assertThat(cancelSubscription.execute(cancelCommand())).isNull();
            assertThat(createQuote.execute(createQuoteCommand())).isNull();
            assertThat(sendQuote.execute(new SendQuoteCommand(31L, COMPANY_ID))).isNull();
            assertThat(createSubscriptionCore.execute(createSubscriptionCommand())).isNull();
            assertThatCode(() -> deleteQuote.execute(31L, COMPANY_ID)).doesNotThrowAnyException();
        }
    }

    private static void authenticateTenantAdmin() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("tenant-admin", "n/a",
                        "ROLE_ADMIN", "subscription_payment.create", "subscription_payment.update",
                        "billing_document_application.create",
                        "billing_document_application.update", "dunning_event.create",
                        "subscription.create"));
    }

    /**
     * Una empleada de carne y hueso, no un principal de cadena: {@code isMyCompany}
     * resuelve por {@code EmployeeContext} y con un {@code String} de principal
     * devolveria falso siempre, con lo que el test verde no probaria nada.
     */
    private static void authenticateEmployee(Long companyId) {
        EmployeeContext employee = new EmployeeContext(9L, companyId,
                Set.of("subscription.update", "subscription.cancel"), Set.of(1L));
        SecurityContextHolder.getContext().setAuthentication(new TestingAuthenticationToken(
                employee, "n/a", "subscription.update", "subscription.cancel"));
    }

    private static void authenticateSystem() {
        SecurityContextHolder.getContext()
                .setAuthentication(new TestingAuthenticationToken("system", "n/a", "ROLE_SYSTEM"));
    }

    private static RegisterSubscriptionPaymentCommand registerCommand() {
        return new RegisterSubscriptionPaymentCommand(COMPANY_ID, new BigDecimal("100000.00"),
                "COP", PaymentMethod.TRANSFER, null, null, LocalDateTime.of(2026, 8, 23, 10, 0),
                "payment-1");
    }

    private static ChangeSubscriptionPaymentStatusCommand changeStatusCommand() {
        return new ChangeSubscriptionPaymentStatusCommand(7L, COMPANY_ID,
                SubscriptionPaymentStatus.CONFIRMED);
    }

    private static ApplyBillingDocumentCommand applyCommand() {
        return new ApplyBillingDocumentCommand(COMPANY_ID, 100L, ApplicationSourceKind.PAYMENT, 7L,
                null, new BigDecimal("100000.00"), "application-1");
    }

    private static RecordDunningEventCommand dunningCommand() {
        return new RecordDunningEventCommand(COMPANY_ID, 11L, null, DunningEventType.GRACE_STARTED,
                1, null, "Mora", LocalDateTime.of(2026, 8, 23, 10, 0));
    }

    private static CreateRequestedSubscriptionCommand subscriptionCommand() {
        return new CreateRequestedSubscriptionCommand(COMPANY_ID, null, 3L, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, LocalDate.of(2026, 8, 23), null,
                LocalDate.of(2026, 8, 23), LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23),
                null, 5, true, List.of());
    }

    private static AddSubscriptionItemCommand addItemCommand() {
        return new AddSubscriptionItemCommand(11L, COMPANY_ID, "add-1", LocalDate.of(2026, 8, 23),
                "ampliacion", null, 2L, null, null);
    }

    private static RemoveSubscriptionItemCommand removeItemCommand() {
        return new RemoveSubscriptionItemCommand(11L, COMPANY_ID, 21L, "remove-1",
                LocalDate.of(2026, 8, 23), "retiro", null, 2L);
    }

    private static ChangeSubscriptionItemQuantityCommand changeQuantityCommand() {
        return new ChangeSubscriptionItemQuantityCommand(11L, COMPANY_ID, 21L, 3, "quantity-1",
                LocalDate.of(2026, 8, 23), "ampliacion", null, 2L);
    }

    private static ChangeSubscriptionStatusCommand changeStatusSubscription() {
        return new ChangeSubscriptionStatusCommand(11L, COMPANY_ID, SubscriptionStatus.READ_ONLY,
                com.vetsoftware.app.subscription.domain.SubscriptionStatusChangeReason.OVERDUE_BALANCE,
                "SYSTEM:TEST");
    }

    private static CancelSubscriptionCommand cancelCommand() {
        return new CancelSubscriptionCommand(11L, COMPANY_ID, LocalDateTime.of(2026, 8, 23, 10, 0),
                LocalDate.of(2026, 9, 22), "retiro", "cancel-1", null, 2L);
    }

    private static CreateQuoteCommand createQuoteCommand() {
        return new CreateQuoteCommand("quote-1", COMPANY_ID, null, null, null, null, 3L, "MONTHLY",
                LocalDate.of(2026, 9, 22), 15, List.of(), List.of());
    }

    private static CreateSubscriptionCommand createSubscriptionCommand() {
        return new CreateSubscriptionCommand(COMPANY_ID, null, 3L, BillingCycle.MONTHLY,
                SubscriptionStatus.ACTIVE, LocalDate.of(2026, 8, 23), null,
                LocalDate.of(2026, 8, 23), LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 23),
                null, 5, true, "SYSTEM:TEST", List.of());
    }

    static class MutationsStub
            implements
                RegisterSubscriptionPaymentUseCase,
                ChangeSubscriptionPaymentStatusUseCase,
                ReconcileSubscriptionPaymentUseCase,
                ApplyBillingDocumentUseCase,
                ReverseBillingDocumentApplicationUseCase,
                RecordDunningEventUseCase,
                CreateRequestedSubscriptionUseCase,
                AddSubscriptionItemUseCase,
                RemoveSubscriptionItemUseCase,
                ChangeSubscriptionItemQuantityUseCase,
                ChangeSubscriptionStatusUseCase,
                CancelSubscriptionUseCase,
                CreateQuoteUseCase,
                SendQuoteUseCase,
                CreateSubscriptionUseCase,
                DeleteQuoteUseCase {

        @Override
        public SubscriptionPaymentDto execute(RegisterSubscriptionPaymentCommand command) {
            return null;
        }

        @Override
        public SubscriptionPaymentDto execute(ChangeSubscriptionPaymentStatusCommand command) {
            return null;
        }

        @Override
        public SubscriptionPaymentDto execute(ReconcileSubscriptionPaymentCommand command) {
            return null;
        }

        @Override
        public BillingDocumentApplicationDto execute(ApplyBillingDocumentCommand command) {
            return null;
        }

        @Override
        public BillingDocumentApplicationDto execute(
                ReverseBillingDocumentApplicationCommand command) {
            return null;
        }

        @Override
        public DunningEventDto execute(RecordDunningEventCommand command) {
            return null;
        }

        @Override
        public SubscriptionDto execute(CreateRequestedSubscriptionCommand command) {
            return null;
        }

        @Override
        public SubscriptionItemDto execute(AddSubscriptionItemCommand command) {
            return null;
        }

        @Override
        public SubscriptionItemDto execute(RemoveSubscriptionItemCommand command) {
            return null;
        }

        @Override
        public SubscriptionItemDto execute(ChangeSubscriptionItemQuantityCommand command) {
            return null;
        }

        @Override
        public SubscriptionDto execute(ChangeSubscriptionStatusCommand command) {
            return null;
        }

        @Override
        public SubscriptionDto execute(CancelSubscriptionCommand command) {
            return null;
        }

        @Override
        public QuoteDto execute(CreateQuoteCommand command) {
            return null;
        }

        @Override
        public QuoteDto execute(SendQuoteCommand command) {
            return null;
        }

        @Override
        public SubscriptionDto execute(CreateSubscriptionCommand command) {
            return null;
        }

        @Override
        public void execute(Long id, Long companyId) {
        }
    }

    @TestConfiguration
    @EnableMethodSecurity
    static class Cableado {

        @Bean
        MutationsStub mutationsStub() {
            return new MutationsStub();
        }

        /**
         * El {@code Authz} de produccion, no un doble: las ramas de tenant de estos
         * puertos evaluan {@code @authz.isMyCompany(...)} y sin el bean el SpEL ni
         * siquiera compila —falla con {@code IllegalArgumentException}, no con
         * {@code AccessDeniedException}—, que es un verde que no probaria el gate sino
         * el cableado. Va real porque es una clase sin dependencias que decide sobre el
         * {@code SecurityContextHolder}, justo lo que estos tests ejercitan.
         */
        @Bean(name = "authz")
        Authz authz() {
            return new Authz();
        }
    }
}
