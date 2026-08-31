package com.vetsoftware.app.infrastructure.observability.business;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.ReasonRejection;
import com.vetsoftware.app.aiproposal.infrastructure.retention.AiProposalRetentionMetrics;
import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.inventory.application.port.out.InventoryMetrics;
import com.vetsoftware.app.inventory.domain.StockMovementType;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import com.vetsoftware.app.subscription.application.port.out.SubscriptionEntitlementMetrics;
import com.vetsoftware.app.subscription.domain.SubscriptionStatus;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.IssueStatus;
import com.vetsoftware.app.subscriptionpayment.domain.ApplicationSourceKind;
import com.vetsoftware.app.subscriptionpayment.domain.PaymentMethod;
import com.vetsoftware.app.subscriptionpayment.domain.SubscriptionPaymentStatus;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilterReply;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.time.Duration;
import java.util.Locale;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Red de seguridad del issue #82: {@link MicrometerBusinessMetrics} deriva los
 * valores de varios tags directamente de enums (con {@code lower(...)} o con
 * {@code value()}), mientras que {@link BusinessMetricCardinalityFilter}
 * mantiene la lista blanca escrita a mano. Cuando las dos listas se separan,
 * Micrometer responde {@code DENY} y el meter desaparece <em>sin log, sin
 * contador y sin arranque fallido</em>.
 *
 * <p>
 * Este test convierte ese fallo silencioso en un CI rojo: añadir una constante
 * a cualquiera de estos enums sin tocar la lista blanca rompe el build antes de
 * llegar a producción.
 *
 * <p>
 * La comprobación se hace contra el comportamiento público del filtro
 * ({@code accept(Meter.Id)}) y no contra el {@code Map} privado, así que sigue
 * valiendo aunque cambie la estructura interna de la lista blanca.
 */
@DisplayName("Paridad entre los enums de origen y la lista blanca de cardinalidad")
class BusinessMetricEnumAllowlistParityTest {

    private final BusinessMetricCardinalityFilter filter = new BusinessMetricCardinalityFilter();

    /** Reproduce exactamente {@code MicrometerBusinessMetrics.lower(Enum)}. */
    private static String lower(Enum<?> value) {
        return value.name().toLowerCase(Locale.ROOT);
    }

    private MeterFilterReply replyFor(String meterName, String tagKey, String tagValue) {
        return filter.accept(
                new Meter.Id(meterName, Tags.of(tagKey, tagValue), null, null, Meter.Type.COUNTER));
    }

    private static String mensajeHuerfano(String tagKey, String tagValue, String derivacion) {
        return "El valor <" + tagValue + "> del tag <" + tagKey + "> -derivado por " + derivacion
                + "- no esta declarado en ALLOWED_VALUES de BusinessMetricCardinalityFilter."
                + " El filtro responde MeterFilterReply.DENY y Micrometer descarta el meter en"
                + " silencio: el panel pierde esa serie para siempre, sin log, sin contador de"
                + " descartes y sin arranque fallido, y el hueco del panel parece un dato real."
                + " Para arreglarlo, anade <" + tagValue + "> al Map.entry(\"" + tagKey
                + "\", Set.of(...)) de BusinessMetricCardinalityFilter.";
    }

    @Nested
    @DisplayName("enums de dominio cuyo nombre en minúsculas viaja como valor de tag")
    class EnumsDeDominio {

        @ParameterizedTest(name = "AppointmentStatus.{0}")
        @EnumSource(AppointmentStatus.class)
        @DisplayName("todo estado de cita está permitido como valor del tag status")
        void todo_estado_de_cita_esta_en_la_lista_blanca(AppointmentStatus status) {
            String valor = lower(status);

            assertThat(replyFor(BusinessMetricNames.APPOINTMENT_TRANSITIONS, "status", valor))
                    .withFailMessage(mensajeHuerfano("status", valor,
                            "MicrometerBusinessMetrics.transitioned con lower(AppointmentStatus."
                                    + status.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "StockMovementType.{0}")
        @EnumSource(StockMovementType.class)
        @DisplayName("todo tipo de movimiento de kardex está permitido como valor del tag movement.type")
        void todo_tipo_de_movimiento_esta_en_la_lista_blanca(StockMovementType movementType) {
            String valor = lower(movementType);

            assertThat(replyFor(BusinessMetricNames.INVENTORY_MOVEMENTS, "movement.type", valor))
                    .withFailMessage(mensajeHuerfano("movement.type", valor,
                            "MicrometerBusinessMetrics.movement con lower(StockMovementType."
                                    + movementType.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "StockMovementType.{0}")
        @EnumSource(StockMovementType.class)
        @DisplayName("el histograma de unidades también acepta todo tipo de movimiento")
        void todo_tipo_de_movimiento_esta_permitido_en_el_histograma_de_unidades(
                StockMovementType movementType) {
            String valor = lower(movementType);

            assertThat(replyFor(BusinessMetricNames.INVENTORY_UNITS, "movement.type", valor))
                    .withFailMessage(mensajeHuerfano("movement.type", valor,
                            "MicrometerBusinessMetrics.movement con lower(StockMovementType."
                                    + movementType.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "ElectronicDocumentType.{0}")
        @EnumSource(ElectronicDocumentType.class)
        @DisplayName("todo tipo de documento electrónico está permitido como valor del tag document.type")
        void todo_tipo_de_documento_esta_en_la_lista_blanca(ElectronicDocumentType documentType) {
            String valor = lower(documentType);

            assertThat(replyFor(BusinessMetricNames.SALES_OPERATIONS, "document.type", valor))
                    .withFailMessage(mensajeHuerfano("document.type", valor,
                            "MicrometerBusinessMetrics.documentType con lower(ElectronicDocumentType."
                                    + documentType.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @Test
        @DisplayName("el sustituto «unknown» de un tipo de documento nulo también está permitido")
        void el_sustituto_unknown_de_un_documento_nulo_esta_en_la_lista_blanca() {
            assertThat(replyFor(BusinessMetricNames.SALES_OPERATIONS, "document.type", "unknown"))
                    .withFailMessage(mensajeHuerfano("document.type", "unknown",
                            "MicrometerBusinessMetrics.documentType(null)"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }
    }

    @Nested
    @DisplayName("enums de los puertos de métricas cuyo value() viaja como valor de tag")
    class EnumsDeLosPuertos {

        @ParameterizedTest(name = "SalesMetrics.Channel.{0}")
        @EnumSource(SalesMetrics.Channel.class)
        @DisplayName("todo canal de venta está permitido como valor del tag channel")
        void todo_canal_de_venta_esta_en_la_lista_blanca(SalesMetrics.Channel channel) {
            String valor = channel.value();

            assertThat(replyFor(BusinessMetricNames.SALES_OPERATIONS, "channel", valor))
                    .withFailMessage(mensajeHuerfano("channel", valor,
                            "SalesMetrics.Channel." + channel.name() + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "SalesMetrics.Result.{0}")
        @EnumSource(SalesMetrics.Result.class)
        @DisplayName("todo resultado fallido de venta está permitido como valor del tag result")
        void todo_resultado_de_venta_esta_en_la_lista_blanca(SalesMetrics.Result result) {
            String valor = result.value();

            assertThat(replyFor(BusinessMetricNames.SALES_OPERATIONS, "result", valor))
                    .withFailMessage(mensajeHuerfano("result", valor,
                            "SalesMetrics.Result." + result.name() + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "BillingMetrics.Origin.{0}")
        @EnumSource(BillingMetrics.Origin.class)
        @DisplayName("todo origen de transmisión DIAN está permitido como valor del tag origin")
        void todo_origen_de_transmision_esta_en_la_lista_blanca(BillingMetrics.Origin origin) {
            String valor = origin.value();

            assertThat(replyFor(BusinessMetricNames.DIAN_TRANSMISSIONS, "origin", valor))
                    .withFailMessage(mensajeHuerfano("origin", valor,
                            "BillingMetrics.Origin." + origin.name() + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "InventoryMetrics.Result.{0}")
        @EnumSource(InventoryMetrics.Result.class)
        @DisplayName("todo resultado de movimiento de inventario está permitido como valor del tag result")
        void todo_resultado_de_inventario_esta_en_la_lista_blanca(InventoryMetrics.Result result) {
            String valor = result.value();

            assertThat(replyFor(BusinessMetricNames.INVENTORY_MOVEMENTS, "result", valor))
                    .withFailMessage(mensajeHuerfano("result", valor,
                            "InventoryMetrics.Result." + result.name() + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "AppointmentMetrics.Channel.{0}")
        @EnumSource(AppointmentMetrics.Channel.class)
        @DisplayName("todo canal de agendamiento está permitido como valor del tag channel")
        void todo_canal_de_agendamiento_esta_en_la_lista_blanca(
                AppointmentMetrics.Channel channel) {
            String valor = channel.value();

            assertThat(replyFor(BusinessMetricNames.APPOINTMENT_TRANSITIONS, "channel", valor))
                    .withFailMessage(mensajeHuerfano("channel", valor,
                            "AppointmentMetrics.Channel." + channel.name() + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }
    }

    /**
     * {@code DianStatus} no es paridad directa: {@code dianResult(...)} lo traduce
     * con un {@code switch} explícito. Aquí se comprueba lo que de verdad importa
     * —que el {@code switch} cubra el enum entero y que cada salida sobreviva al
     * filtro— ejercitando el emisor real contra un registro con el filtro
     * instalado.
     */
    @Nested
    @DisplayName("DianStatus, que pasa por el switch explícito de dianResult")
    class MapeoExplicitoDeDianStatus {

        private PrometheusMeterRegistry registry;
        private MicrometerBusinessMetrics metrics;

        @BeforeEach
        void setUp() {
            registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
            registry.config().meterFilter(new BusinessMetricCardinalityFilter());
            metrics = new MicrometerBusinessMetrics(registry, new AfterCommitMetricRecorder());
        }

        @ParameterizedTest(name = "DianStatus.{0}")
        @EnumSource(value = DianStatus.class, mode = EnumSource.Mode.EXCLUDE, names = "NO_ELECTRONICO")
        @DisplayName("cada estado DIAN transmisible tiene rama en el switch y su salida pasa el filtro")
        void cada_estado_dian_transmisible_produce_un_result_permitido(DianStatus status) {
            metrics.finished(status, BillingMetrics.Origin.INITIAL, ElectronicDocumentType.FE_VENTA,
                    Duration.ofMillis(10));

            assertThat(registry.find(BusinessMetricNames.DIAN_TRANSMISSIONS).counter())
                    .withFailMessage("DianStatus." + status.name()
                            + " no llego a registrar ningun contador en"
                            + " vetsoftware.business.dian.transmissions. O el switch de"
                            + " MicrometerBusinessMetrics.dianResult(...) no tiene rama para esta"
                            + " constante, o la cadena que devuelve no esta en ALLOWED_VALUES de"
                            + " BusinessMetricCardinalityFilter para el tag <result>. Las dos"
                            + " cosas se tragan la metrica en silencio. Anade la rama al switch"
                            + " y su salida al Map.entry(\"result\", Set.of(...)) del filtro.")
                    .isNotNull();
        }

        @Test
        @DisplayName("NO_ELECTRONICO no produce ningún valor de result y por eso no necesita entrada en la lista blanca")
        void no_electronico_no_registra_ninguna_transmision() {
            metrics.finished(DianStatus.NO_ELECTRONICO, BillingMetrics.Origin.INITIAL,
                    ElectronicDocumentType.FE_VENTA, Duration.ofMillis(10));

            assertThat(registry.find(BusinessMetricNames.DIAN_TRANSMISSIONS).counter())
                    .withFailMessage("NO_ELECTRONICO no representa una transmision DIAN:"
                            + " dianResult(...) lanza IllegalArgumentException a proposito y"
                            + " AfterCommitMetricRecorder la absorbe, asi que no debe existir"
                            + " ningun contador de transmision. Si aparece uno, alguien le dio"
                            + " rama al switch y ese valor hay que revisarlo antes de meterlo"
                            + " en la lista blanca.")
                    .isNull();
        }
    }

    @Nested
    @DisplayName("los tres enums del alta de superadministradores de plataforma")
    class EnumsDelAltaDePlataforma {

        @ParameterizedTest(name = "RequestResult.{0}")
        @EnumSource(PlatformAccessMetrics.RequestResult.class)
        @DisplayName("todo desenlace de la solicitud está permitido como valor del tag result")
        void todo_desenlace_de_solicitud_esta_en_la_lista_blanca(
                PlatformAccessMetrics.RequestResult result) {
            assertThat(replyFor(BusinessMetricNames.SYSTEM_USER_REQUESTS, "result", result.value()))
                    .withFailMessage(
                            mensajeHuerfano("result", result.value(),
                                    "MicrometerBusinessMetrics.requested con RequestResult."
                                            + result.name() + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "ApprovalResult.{0}")
        @EnumSource(PlatformAccessMetrics.ApprovalResult.class)
        @DisplayName("todo desenlace de la resolución está permitido como valor del tag result")
        void todo_desenlace_de_resolucion_esta_en_la_lista_blanca(
                PlatformAccessMetrics.ApprovalResult result) {
            // Este es el contador del que cuelga la vigilancia de fuerza bruta sobre
            // el codigo de 6 digitos: si un valor nuevo lo denegara, el panel
            // dejaria de ver los intentos fallidos y el hueco pareceria calma.
            assertThat(
                    replyFor(BusinessMetricNames.SYSTEM_USER_APPROVALS, "result", result.value()))
                    .withFailMessage(
                            mensajeHuerfano("result", result.value(),
                                    "MicrometerBusinessMetrics.resolved con ApprovalResult."
                                            + result.name() + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "InvitationResult.{0}")
        @EnumSource(PlatformAccessMetrics.InvitationResult.class)
        @DisplayName("todo desenlace de la invitación está permitido como valor del tag result")
        void todo_desenlace_de_invitacion_esta_en_la_lista_blanca(
                PlatformAccessMetrics.InvitationResult result) {
            assertThat(
                    replyFor(BusinessMetricNames.SYSTEM_USER_INVITATIONS, "result", result.value()))
                    .withFailMessage(mensajeHuerfano("result", result.value(),
                            "MicrometerBusinessMetrics.invitation con InvitationResult."
                                    + result.name() + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }
    }

    /**
     * Dinero de suscripciones (#606). Seis vocabularios nuevos, y todos con la
     * misma trampa: la lista blanca deniega el MEDIDOR ENTERO ante un valor sin
     * declarar, no esa serie suelta. Un {@code ChargeType} nuevo -o un
     * {@code ApplicationSourceKind}, que el modelo todavia puede ampliar- dejaria
     * el panel del cierre de mes con un hueco indistinguible de "no se facturo
     * nada".
     */
    @Nested
    @DisplayName("los seis vocabularios del dinero de suscripciones")
    class EnumsDelDineroDeSuscripciones {

        @ParameterizedTest(name = "ChargeType.{0}")
        @EnumSource(ChargeType.class)
        @DisplayName("toda clase de cargo pasa el filtro en el contador y en el histograma")
        void toda_clase_de_cargo_esta_en_la_lista_blanca(ChargeType chargeType) {
            String valor = lower(chargeType);

            assertThat(replyFor(BusinessMetricNames.SUBSCRIPTION_CHARGES, "charge.type", valor))
                    .withFailMessage(
                            mensajeHuerfano("charge.type", valor,
                                    "MicrometerBusinessMetrics.chargeAccrued con lower(ChargeType."
                                            + chargeType.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
            assertThat(
                    replyFor(BusinessMetricNames.SUBSCRIPTION_CHARGED_AMOUNT, "charge.type", valor))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "IssueStatus.{0}")
        @EnumSource(IssueStatus.class)
        @DisplayName("todo estado de emision de una cuenta de cobro esta permitido")
        void todo_estado_de_emision_esta_en_la_lista_blanca(IssueStatus issueStatus) {
            String valor = lower(issueStatus);

            assertThat(replyFor(BusinessMetricNames.SUBSCRIPTION_DOCUMENTS, "issue.status", valor))
                    .withFailMessage(mensajeHuerfano("issue.status", valor,
                            "MicrometerBusinessMetrics.documentIssued con lower(IssueStatus."
                                    + issueStatus.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "PaymentMethod.{0}")
        @EnumSource(PaymentMethod.class)
        @DisplayName("todo medio de pago esta permitido")
        void todo_medio_de_pago_esta_en_la_lista_blanca(PaymentMethod method) {
            String valor = lower(method);

            assertThat(replyFor(BusinessMetricNames.SUBSCRIPTION_PAYMENTS, "payment.method", valor))
                    .withFailMessage(mensajeHuerfano("payment.method", valor,
                            "MicrometerBusinessMetrics.paymentRegistered con lower(PaymentMethod."
                                    + method.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "ApplicationSourceKind.{0}")
        @EnumSource(ApplicationSourceKind.class)
        @DisplayName("toda clase de fuente de imputacion esta permitida")
        void toda_fuente_de_imputacion_esta_en_la_lista_blanca(ApplicationSourceKind sourceKind) {
            String valor = lower(sourceKind);

            assertThat(
                    replyFor(BusinessMetricNames.SUBSCRIPTION_APPLICATIONS, "source.kind", valor))
                    .withFailMessage(mensajeHuerfano("source.kind", valor,
                            "MicrometerBusinessMetrics.applicationRecorded con"
                                    + " lower(ApplicationSourceKind." + sourceKind.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "SubscriptionStatus.{0}")
        @EnumSource(SubscriptionStatus.class)
        @DisplayName("todo estado de contrato esta permitido como destino de la transicion")
        void todo_estado_de_contrato_esta_en_la_lista_blanca(SubscriptionStatus status) {
            String valor = lower(status);

            assertThat(replyFor(BusinessMetricNames.SUBSCRIPTION_STATUS_TRANSITIONS, "to.status",
                    valor))
                    .withFailMessage(mensajeHuerfano("to.status", valor,
                            "MicrometerBusinessMetrics.statusTransitioned con"
                                    + " lower(SubscriptionStatus." + status.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "Trigger.{0}")
        @EnumSource(SubscriptionEntitlementMetrics.Trigger.class)
        @DisplayName("todo disparador del recalculo esta permitido")
        void todo_disparador_esta_en_la_lista_blanca(
                SubscriptionEntitlementMetrics.Trigger trigger) {
            assertThat(
                    replyFor(BusinessMetricNames.SUBSCRIPTION_ENTITLEMENT_RECALCULATIONS,
                            "trigger.reason", trigger.value()))
                    .withFailMessage(mensajeHuerfano("trigger.reason", trigger.value(),
                            "SubscriptionEntitlementMetrics.Trigger." + trigger.name()
                                    + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "Rejection.{0}")
        @EnumSource(SubscriptionBillingMetrics.Rejection.class)
        @DisplayName("todo rechazo de emision reutiliza un valor de result ya declarado")
        void todo_rechazo_reutiliza_un_result_declarado(
                SubscriptionBillingMetrics.Rejection rejection) {
            assertThat(
                    replyFor(BusinessMetricNames.SUBSCRIPTION_DOCUMENTS, "result",
                            rejection.value()))
                    .withFailMessage(mensajeHuerfano("result", rejection.value(),
                            "SubscriptionBillingMetrics.Rejection." + rejection.name()
                                    + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "SubscriptionPaymentStatus.{0}")
        @EnumSource(SubscriptionPaymentStatus.class)
        @DisplayName("el switch de paymentResult cubre el enum entero y su salida pasa el filtro")
        void el_switch_de_payment_result_cubre_el_enum_entero(SubscriptionPaymentStatus status) {
            PrometheusMeterRegistry registro = new PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT);
            registro.config().meterFilter(new BusinessMetricCardinalityFilter());
            MicrometerBusinessMetrics emisor = new MicrometerBusinessMetrics(registro,
                    new AfterCommitMetricRecorder());

            emisor.paymentRegistered(PaymentMethod.TRANSFER, status);

            assertThat(registro.find(BusinessMetricNames.SUBSCRIPTION_PAYMENTS).counter())
                    .withFailMessage("SubscriptionPaymentStatus." + status.name()
                            + " no registro ningun contador. O el switch de"
                            + " MicrometerBusinessMetrics.paymentResult(...) no tiene rama para"
                            + " esta constante, o la cadena que devuelve no esta en ALLOWED_VALUES"
                            + " para el tag <result>. Las dos cosas se tragan la metrica en"
                            + " silencio, y esta es la del dinero que entra.")
                    .isNotNull();
        }
    }

    /**
     * Asistente comercial con IA. Cinco vocabularios, y el que mas duele si se
     * separa es {@code reason.rule}: es la unica serie que dice si el prompt esta
     * derivando -«los motivos salen genericos» tiene nueve causas posibles y sin
     * esta etiqueta no se distingue ninguna-. Si un valor nuevo la denegara, el
     * medidor entero desaparece y el panel se lee como «el modelo va bien».
     *
     * <p>
     * {@code retention.step} entra aqui porque los dos medidores del barrido se
     * movieron al prefijo de negocio en la misma fase: antes vivian fuera y su
     * vocabulario era cerrado por disciplina, sin ninguna barrera que lo
     * comprobara.
     */
    @Nested
    @DisplayName("los cinco vocabularios del asistente comercial con IA")
    class EnumsDelAsistenteConIa {

        @ParameterizedTest(name = "Operation.{0}")
        @EnumSource(AiProposalMetrics.Operation.class)
        @DisplayName("las dos operaciones del asistente estan permitidas")
        void toda_operacion_esta_en_la_lista_blanca(AiProposalMetrics.Operation operation) {
            assertThat(replyFor(BusinessMetricNames.AI_PROPOSAL_GENERATED, "ai.operation",
                    operation.value()))
                    .withFailMessage(mensajeHuerfano("ai.operation", operation.value(),
                            "AiProposalMetrics.Operation." + operation.name() + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "Outcome.{0}")
        @EnumSource(AiProposalMetrics.Outcome.class)
        @DisplayName("todo desenlace del asistente esta permitido")
        void todo_desenlace_esta_en_la_lista_blanca(AiProposalMetrics.Outcome outcome) {
            assertThat(replyFor(BusinessMetricNames.AI_PROPOSAL_GENERATED, "ai.outcome",
                    outcome.value()))
                    .withFailMessage(mensajeHuerfano("ai.outcome", outcome.value(),
                            "AiProposalMetrics.Outcome." + outcome.name() + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "GenerationOutcome.{0}")
        @EnumSource(GenerationOutcome.class)
        @DisplayName("todo GenerationOutcome del dominio tiene rama en Outcome.from y su salida pasa el filtro")
        void todo_generation_outcome_se_traduce(GenerationOutcome outcome) {
            String valor = AiProposalMetrics.Outcome.from(outcome).value();

            assertThat(replyFor(BusinessMetricNames.AI_PROPOSAL_GENERATED, "ai.outcome", valor))
                    .withFailMessage(
                            mensajeHuerfano("ai.outcome", valor,
                                    "AiProposalMetrics.Outcome.from(GenerationOutcome."
                                            + outcome.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        /**
         * &#9940; <b>Incluye {@code NONE}, y no es un descuido.</b> La etiqueta se
         * emite en <b>todas</b> las muestras del contador —Prometheus exige el mismo
         * juego de claves—, asi que el camino feliz tambien la lleva. Si {@code none}
         * faltara en la lista blanca, el filtro denegaria el <b>medidor entero</b> y el
         * panel se quedaria vacio de forma indistinguible de «no hubo prospectos»: el
         * fallo exacto contra el que existe esta clase.
         */
        @ParameterizedTest(name = "FailureKind.{0}")
        @EnumSource(AiProposalMetrics.FailureKind.class)
        @DisplayName("las dos clases de fallo, y el camino feliz, estan permitidas")
        void toda_clase_de_fallo_esta_en_la_lista_blanca(AiProposalMetrics.FailureKind kind) {
            assertThat(replyFor(BusinessMetricNames.AI_PROPOSAL_GENERATED, "ai.failure.kind",
                    kind.value()))
                    .withFailMessage(mensajeHuerfano("ai.failure.kind", kind.value(),
                            "AiProposalMetrics.FailureKind." + kind.name() + ".value()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "ProposalPresentation.{0}")
        @EnumSource(ProposalPresentation.class)
        @DisplayName("toda forma de presentar la propuesta esta permitida")
        void toda_presentacion_esta_en_la_lista_blanca(ProposalPresentation presentation) {
            String valor = lower(presentation);

            assertThat(
                    replyFor(BusinessMetricNames.AI_PROPOSAL_GENERATED, "ai.presentation", valor))
                    .withFailMessage(mensajeHuerfano("ai.presentation", valor,
                            "MicrometerAiProposalMetrics.lower(ProposalPresentation."
                                    + presentation.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "ReasonRejection.{0}")
        @EnumSource(ReasonRejection.class)
        @DisplayName("las nueve reglas del saneador del motivo estan permitidas")
        void toda_regla_del_saneador_esta_en_la_lista_blanca(ReasonRejection rule) {
            String valor = lower(rule);

            assertThat(
                    replyFor(BusinessMetricNames.AI_PROPOSAL_REASON_REJECTED, "reason.rule", valor))
                    .withFailMessage(
                            mensajeHuerfano("reason.rule", valor,
                                    "MicrometerAiProposalMetrics.lower(ReasonRejection."
                                            + rule.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "LineVerdict.{0}")
        @EnumSource(LineVerdict.class)
        @DisplayName("todo veredicto de linea esta permitido, incluido el aceptado que hoy no se emite")
        void todo_veredicto_esta_en_la_lista_blanca(LineVerdict verdict) {
            String valor = lower(verdict);

            assertThat(
                    replyFor(BusinessMetricNames.AI_PROPOSAL_INVALID_LINES, "line.verdict", valor))
                    .withFailMessage(
                            mensajeHuerfano("line.verdict", valor,
                                    "MicrometerAiProposalMetrics.lower(LineVerdict."
                                            + verdict.name() + ")"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }

        @ParameterizedTest(name = "Paso.{0}")
        @EnumSource(AiProposalRetentionMetrics.Paso.class)
        @DisplayName("los seis pasos del barrido de retencion estan permitidos")
        void todo_paso_de_retencion_esta_en_la_lista_blanca(AiProposalRetentionMetrics.Paso paso) {
            assertThat(replyFor(BusinessMetricNames.AI_PROPOSAL_RETENTION_ROWS, "retention.step",
                    paso.etiqueta()))
                    .withFailMessage(mensajeHuerfano("retention.step", paso.etiqueta(),
                            "AiProposalRetentionMetrics.Paso." + paso.name() + ".etiqueta()"))
                    .isEqualTo(MeterFilterReply.NEUTRAL);
        }
    }
}
