package com.vetsoftware.app.infrastructure.observability.business;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

import com.vetsoftware.app.appointment.application.port.out.AppointmentMetrics;
import com.vetsoftware.app.appointment.domain.AppointmentStatus;
import com.vetsoftware.app.electronicdocument.application.port.out.BillingMetrics;
import com.vetsoftware.app.electronicdocument.application.port.out.SalesMetrics;
import com.vetsoftware.app.electronicdocument.domain.DianStatus;
import com.vetsoftware.app.electronicdocument.domain.ElectronicDocumentType;
import com.vetsoftware.app.inventory.application.port.out.InventoryMetrics;
import com.vetsoftware.app.inventory.domain.StockMovementType;
import com.vetsoftware.app.platformaccess.application.port.out.PlatformAccessMetrics;
import io.micrometer.core.instrument.Counter;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class MicrometerBusinessMetricsTest {

    private PrometheusMeterRegistry registry;
    private MicrometerBusinessMetrics metrics;

    @BeforeEach
    void setUp() {
        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        registry.config().meterFilter(new BusinessMetricCardinalityFilter());
        metrics = new MicrometerBusinessMetrics(registry, new AfterCommitMetricRecorder());
    }

    @Test
    void exportsStablePrometheusNamesAndOnlyBoundedTags() {
        metrics.completed(SalesMetrics.Channel.POS, ElectronicDocumentType.DOC_EQUIV_POS,
                new BigDecimal("125000"), 3);
        metrics.finished(DianStatus.VALIDADO, BillingMetrics.Origin.INITIAL,
                ElectronicDocumentType.DOC_EQUIV_POS, Duration.ofMillis(850));
        metrics.movement(StockMovementType.SALE, InventoryMetrics.Result.SUCCESS, 2);
        metrics.transitioned(AppointmentStatus.CONFIRMED, AppointmentMetrics.Channel.STAFF);
        metrics.opened();
        metrics.closed(List.of(new BigDecimal("-10000"), BigDecimal.ZERO));

        String scrape = registry.scrape();

        assertThat(scrape).contains("vetsoftware_business_sales_operations_total")
                .contains("vetsoftware_business_sales_amount_cop_sum")
                .contains("vetsoftware_business_sales_lines_sum")
                .contains("vetsoftware_business_dian_transmissions_total")
                .contains("vetsoftware_business_dian_transmission_duration_seconds")
                .contains("vetsoftware_business_inventory_movements_total")
                .contains("vetsoftware_business_inventory_units_sum")
                .contains("vetsoftware_business_appointments_transitions_total")
                .contains("vetsoftware_business_cash_sessions_total")
                .contains("vetsoftware_business_cash_closing_difference_cop_sum")
                .contains("document_type=\"doc_equiv_pos\"").doesNotContain("companyId")
                .doesNotContain("productId").doesNotContain("traceId");
    }

    @Test
    void deniesUnknownBusinessTagsAndValues() {
        Counter.builder(BusinessMetricNames.PREFIX + "unsafe").tag("companyId", "123")
                .register(registry).increment();
        Counter.builder(BusinessMetricNames.PREFIX + "unsafe.result")
                .tag("result", "customer-provided-value").register(registry).increment();

        assertThat(registry.find(BusinessMetricNames.PREFIX + "unsafe").counter()).isNull();
        assertThat(registry.find(BusinessMetricNames.PREFIX + "unsafe.result").counter()).isNull();
    }

    @Test
    void doesNotPublishSuccessfulBusinessFactAfterRollback() {
        org.springframework.transaction.support.TransactionSynchronizationManager
                .setActualTransactionActive(true);
        org.springframework.transaction.support.TransactionSynchronizationManager
                .initSynchronization();
        try {
            metrics.completed(SalesMetrics.Channel.POS, ElectronicDocumentType.DOC_EQUIV_POS,
                    BigDecimal.TEN, 1);
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .getSynchronizations()
                    .forEach(synchronization -> synchronization.afterCompletion(
                            org.springframework.transaction.support.TransactionSynchronization.STATUS_ROLLED_BACK));

            assertThat(registry.find(BusinessMetricNames.SALES_OPERATIONS).counter()).isNull();
        } finally {
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .clearSynchronization();
            org.springframework.transaction.support.TransactionSynchronizationManager
                    .setActualTransactionActive(false);
        }
    }

    @Test
    @DisplayName("un intento de venta fallido se cuenta de inmediato, sin esperar al commit")
    void recordsFailedSalesAttemptImmediately() {
        metrics.failed(SalesMetrics.Channel.POS, ElectronicDocumentType.FE_VENTA,
                SalesMetrics.Result.REJECTED);

        assertThat(registry.get(BusinessMetricNames.SALES_OPERATIONS)
                .tags("result", "rejected", "channel", "pos", "document.type", "fe_venta").counter()
                .count()).isEqualTo(1);
    }

    @Test
    @DisplayName("un tipo de documento nulo se reporta como unknown en vez de fallar")
    void reportsUnknownDocumentTypeWhenMissing() {
        metrics.failed(SalesMetrics.Channel.POS, null, SalesMetrics.Result.ERROR);

        assertThat(registry.get(BusinessMetricNames.SALES_OPERATIONS)
                .tags("result", "error", "channel", "pos", "document.type", "unknown").counter()
                .count()).isEqualTo(1);
    }

    @Test
    @DisplayName("un fallo de transmisión DIAN se cuenta de inmediato con resultado error")
    void recordsFailedDianTransmissionImmediately() {
        metrics.failed(BillingMetrics.Origin.RETRY, ElectronicDocumentType.NOTA_CREDITO,
                Duration.ofMillis(500));

        assertThat(registry.get(BusinessMetricNames.DIAN_TRANSMISSIONS)
                .tags("result", "error", "origin", "retry", "document.type", "nota_credito")
                .counter().count()).isEqualTo(1);
    }

    @ParameterizedTest
    @MethodSource("dianStatusResultMapping")
    @DisplayName("cada estado DIAN transmisible mapea al resultado de negocio esperado")
    void mapsDianStatusToBusinessResult(DianStatus status, String expectedResult) {
        metrics.finished(status, BillingMetrics.Origin.INITIAL, ElectronicDocumentType.FE_VENTA,
                Duration.ofMillis(100));

        assertThat(registry.get(BusinessMetricNames.DIAN_TRANSMISSIONS)
                .tags("result", expectedResult, "origin", "initial", "document.type", "fe_venta")
                .counter().count()).isEqualTo(1);
    }

    private static Stream<Arguments> dianStatusResultMapping() {
        return Stream.of(Arguments.of(DianStatus.VALIDADO, "validated"),
                Arguments.of(DianStatus.RECHAZADO, "rejected"),
                Arguments.of(DianStatus.CONTINGENCIA, "contingency"),
                Arguments.of(DianStatus.PENDIENTE, "pending"));
    }

    @Test
    @DisplayName("un estado DIAN no electrónico no es una transmisión: el fallo de mapeo no se "
            + "propaga (las métricas son best-effort) ni deja una serie a medio registrar")
    void doesNotRecordAndDoesNotPropagateForANonElectronicDianStatus() {
        assertThatCode(
                () -> metrics.finished(DianStatus.NO_ELECTRONICO, BillingMetrics.Origin.INITIAL,
                        ElectronicDocumentType.FE_VENTA, Duration.ofMillis(100)))
                .doesNotThrowAnyException();

        assertThat(registry.find(BusinessMetricNames.DIAN_TRANSMISSIONS).counters()).isEmpty();
    }

    @Test
    @DisplayName("un movimiento de inventario fallido se cuenta de inmediato sin registrar unidades")
    void recordsFailedInventoryMovementImmediatelyWithoutUnits() {
        metrics.movement(StockMovementType.SALE, InventoryMetrics.Result.INSUFFICIENT_STOCK, 5);

        assertThat(registry.get(BusinessMetricNames.INVENTORY_MOVEMENTS)
                .tags("movement.type", "sale", "result", "insufficient_stock").counter().count())
                .isEqualTo(1);
        assertThat(registry.find(BusinessMetricNames.INVENTORY_UNITS).summary()).isNull();
    }

    @Test
    @DisplayName("un movimiento exitoso con cero unidades no registra unidades, solo el conteo")
    void recordsSuccessfulMovementWithZeroUnitsWithoutRecordingUnits() {
        metrics.movement(StockMovementType.ADJUSTMENT_IN, InventoryMetrics.Result.SUCCESS, 0);

        assertThat(registry.get(BusinessMetricNames.INVENTORY_MOVEMENTS)
                .tags("movement.type", "adjustment_in", "result", "success").counter().count())
                .isEqualTo(1);
        assertThat(registry.find(BusinessMetricNames.INVENTORY_UNITS).summary()).isNull();
    }

    @Test
    @DisplayName("un cierre de caja sin diferencias registra un único valor balanceado")
    void recordsBalancedCashClosingWhenThereAreNoDifferences() {
        metrics.closed(List.of());

        assertThat(registry.get(BusinessMetricNames.CASH_SESSIONS)
                .tags("event", "closed", "result", "success").counter().count()).isEqualTo(1);
        assertThat(registry.get(BusinessMetricNames.CASH_CLOSING_DIFFERENCE)
                .tag("direction", "balanced").summary().totalAmount()).isZero();
    }

    @Test
    @DisplayName("un excedente de caja se etiqueta como superávit, no como faltante")
    void tagsCashSurplusAsSurplus() {
        metrics.closed(List.of(new BigDecimal("5000")));

        assertThat(registry.get(BusinessMetricNames.CASH_CLOSING_DIFFERENCE)
                .tag("direction", "surplus").summary().totalAmount()).isEqualTo(5000);
    }

    @Test
    @DisplayName("los cuatro contadores del alta de plataforma se registran con su nombre estable")
    void registraLosCuatroContadoresDelAltaDePlataforma() {
        metrics.requested(PlatformAccessMetrics.RequestResult.SUCCESS);
        metrics.resolved(PlatformAccessMetrics.ApprovalResult.APPROVED);
        metrics.invitation(PlatformAccessMetrics.InvitationResult.SENT);
        metrics.provisioned();

        String scrape = registry.scrape();

        assertThat(scrape).contains("vetsoftware_business_system_user_requests_total")
                .contains("vetsoftware_business_system_user_approvals_total")
                .contains("vetsoftware_business_system_user_invitations_total")
                .contains("vetsoftware_business_system_user_provisioned_total");
    }

    @Test
    @DisplayName("cada desenlace de la solicitud viaja en su propio tag result")
    void cadaDesenlaceDeSolicitudViajaEnSuTag() {
        metrics.requested(PlatformAccessMetrics.RequestResult.FORM_CLOSED);
        metrics.requested(PlatformAccessMetrics.RequestResult.DUPLICATE_IGNORED);

        assertThat(registry.get(BusinessMetricNames.SYSTEM_USER_REQUESTS)
                .tag("result", "form_closed").counter().count()).isEqualTo(1);
        assertThat(registry.get(BusinessMetricNames.SYSTEM_USER_REQUESTS)
                .tag("result", "duplicate_ignored").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("los intentos fallidos del código se cuentan al instante, sin esperar a un commit que no habrá")
    void cuentaLosIntentosFallidosAlInstante() {
        // La denegacion viaja con una excepcion detras: la transaccion del caso de
        // uso revierte, asi que un recordAfterCommit no publicaria NUNCA este
        // contador — y es justo el que vigila la fuerza bruta sobre 10^6 codigos.
        metrics.resolved(PlatformAccessMetrics.ApprovalResult.CODE_MISMATCH);
        metrics.resolved(PlatformAccessMetrics.ApprovalResult.ATTEMPTS_EXHAUSTED);
        metrics.resolved(PlatformAccessMetrics.ApprovalResult.TOKEN_INVALID);

        assertThat(registry.get(BusinessMetricNames.SYSTEM_USER_APPROVALS)
                .tag("result", "code_mismatch").counter().count()).isEqualTo(1);
        assertThat(registry.get(BusinessMetricNames.SYSTEM_USER_APPROVALS)
                .tag("result", "attempts_exhausted").counter().count()).isEqualTo(1);
        assertThat(registry.get(BusinessMetricNames.SYSTEM_USER_APPROVALS)
                .tag("result", "token_invalid").counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("los desenlaces del correo de invitación se cuentan al instante: llegan del pool de correo, ya tras el commit")
    void cuentaLosDesenlacesDelCorreoAlInstante() {
        metrics.invitation(PlatformAccessMetrics.InvitationResult.SENT);
        metrics.invitation(PlatformAccessMetrics.InvitationResult.FAILED);
        metrics.invitation(PlatformAccessMetrics.InvitationResult.SKIPPED);

        // Alli no queda transaccion que esperar y recordAfterCommit tomaria la rama
        // equivocada: el envio perdido —el unico ERROR del flujo— se quedaria sin
        // contador.
        assertThat(registry.get(BusinessMetricNames.SYSTEM_USER_INVITATIONS).tag("result", "failed")
                .counter().count()).isEqualTo(1);
        assertThat(registry.get(BusinessMetricNames.SYSTEM_USER_INVITATIONS)
                .tag("result", "skipped").counter().count()).isEqualTo(1);
        assertThat(registry.get(BusinessMetricNames.SYSTEM_USER_INVITATIONS).tag("result", "sent")
                .counter().count()).isEqualTo(1);
    }

    @Test
    @DisplayName("el contador del alta no lleva ningún tag: es del que cuelga la única alerta del flujo")
    void elContadorDelAltaNoLlevaTags() {
        metrics.provisioned();

        assertThat(registry.get(BusinessMetricNames.SYSTEM_USER_PROVISIONED).counter().getId()
                .getTags()).isEmpty();
    }

    @Test
    @DisplayName("ningún contador del alta expone el correo, el token ni el id de la solicitud")
    void ningunContadorDelAltaExponeDatosDeAltaCardinalidad() {
        metrics.requested(PlatformAccessMetrics.RequestResult.SUCCESS);
        metrics.resolved(PlatformAccessMetrics.ApprovalResult.CODE_MISMATCH);
        metrics.invitation(PlatformAccessMetrics.InvitationResult.SENT);
        metrics.provisioned();

        // Un id de solicitud por peticion reventaria el numero de series. Ese dato
        // vive en la traza y en el MDC, que es donde la cardinalidad no cuesta.
        assertThat(registry.scrape()).doesNotContain("request_id").doesNotContain("email")
                .doesNotContain("token");
    }
}
