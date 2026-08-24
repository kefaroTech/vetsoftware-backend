package com.vetsoftware.app.subscription.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.stream.Stream;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Subscription - la carpeta del cliente")
class SubscriptionTest {

    private static final LocalDate INICIO = LocalDate.of(2026, 1, 1);
    private static final LocalDate FIN_PERIODO = LocalDate.of(2026, 1, 31);

    private static Subscription contrato(SubscriptionStatus status) {
        return Subscription.create("SUS-2026-00184", 42L, null, 3L, BillingCycle.MONTHLY, status,
                INICIO, status == SubscriptionStatus.TRIALING ? LocalDate.of(2026, 1, 15) : null,
                INICIO, FIN_PERIODO, FIN_PERIODO, null, 0, true);
    }

    @Nested
    @DisplayName("Cancelacion")
    class Cancelacion {

        @Test
        @DisplayName("cancelar el 10 no corta el servicio: sigue vigente hasta el 30")
        void cancelarNoCortaElServicio() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);
            LocalDateTime diez = LocalDateTime.of(2026, 1, 10, 9, 30);

            contrato.requestCancellation(diez, LocalDate.of(2026, 1, 30),
                    "Se paso a la competencia");

            // Lo que ya pago se disfruta: el estado no cambia y el contrato sigue
            // ocupando el marcador de vigente de su empresa.
            assertThat(contrato.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(contrato.isCurrent()).isTrue();
        }

        @Test
        @DisplayName("se guardan las dos fechas por separado y el motivo")
        void guardaLasDosFechasYElMotivo() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);
            LocalDateTime diez = LocalDateTime.of(2026, 1, 10, 9, 30);

            contrato.requestCancellation(diez, LocalDate.of(2026, 1, 30), "Cerro la clinica");

            CancellationRequest cancelacion = contrato.getCancellation();
            assertThat(cancelacion.requestedAt()).isEqualTo(diez);
            assertThat(cancelacion.effectiveDate()).isEqualTo(LocalDate.of(2026, 1, 30));
            assertThat(cancelacion.reason()).isEqualTo("Cerro la clinica");
        }

        @Test
        @DisplayName("cancelar apaga la renovacion automatica")
        void cancelarApagaLaRenovacion() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            contrato.requestCancellation(LocalDateTime.of(2026, 1, 10, 9, 30),
                    LocalDate.of(2026, 1, 30), null);

            assertThat(contrato.isAutoRenew()).isFalse();
        }

        @Test
        @DisplayName("la fecha efectiva no puede ser anterior a la peticion")
        void efectivaAnteriorALaPeticion() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            assertThatThrownBy(() -> contrato.requestCancellation(
                    LocalDateTime.of(2026, 1, 10, 9, 30), LocalDate.of(2026, 1, 5), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("la baja surte efecto el propio dia efectivo, no el siguiente")
        void surteEfectoElDiaEfectivo() {
            CancellationRequest cancelacion = new CancellationRequest(
                    LocalDateTime.of(2026, 1, 10, 9, 30), LocalDate.of(2026, 1, 30), null);

            assertThat(cancelacion.hasTakenEffectOn(LocalDate.of(2026, 1, 29))).isFalse();
            assertThat(cancelacion.hasTakenEffectOn(LocalDate.of(2026, 1, 30))).isTrue();
        }

        @Test
        @DisplayName("un contrato terminal ya no se puede cancelar")
        void contratoTerminal() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);
            contrato.changeStatus(SubscriptionStatus.CANCELLED, null, "SYSTEM",
                    LocalDateTime.now());

            assertThatThrownBy(() -> contrato.requestCancellation(LocalDateTime.now(),
                    LocalDate.of(2026, 3, 1), null))
                    .isInstanceOf(InvalidSubscriptionStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Transiciones de estado")
    class Transiciones {

        @Test
        @DisplayName("la transicion devuelve su fila de bitacora")
        void devuelveLaBitacora() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);
            LocalDateTime cuando = LocalDateTime.of(2026, 2, 6, 8, 0);

            SubscriptionStatusChange cambio = contrato.changeStatus(SubscriptionStatus.PAST_DUE,
                    "Factura FE-1043 vencida hace 6 dias", "billing-job", cuando);

            assertThat(cambio.getFromStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
            assertThat(cambio.getToStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
            assertThat(cambio.getReason()).isEqualTo("Factura FE-1043 vencida hace 6 dias");
            assertThat(cambio.getActor()).isEqualTo("billing-job");
            assertThat(cambio.getOccurredAt()).isEqualTo(cuando);
        }

        @Test
        @DisplayName("pasar a PAST_DUE apunta desde cuando debe")
        void pastDueApuntaDesdeCuando() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            contrato.changeStatus(SubscriptionStatus.PAST_DUE, null, "billing-job",
                    LocalDateTime.of(2026, 2, 6, 8, 0));

            assertThat(contrato.getPastDueSince()).isEqualTo(LocalDate.of(2026, 2, 6));
        }

        @Test
        @DisplayName("volver a ACTIVE al pagar limpia la mora")
        void volverAActiveLimpiaLaMora() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);
            contrato.changeStatus(SubscriptionStatus.PAST_DUE, null, "billing-job",
                    LocalDateTime.of(2026, 2, 6, 8, 0));

            contrato.changeStatus(SubscriptionStatus.ACTIVE, "Pago recibido", "billing-job",
                    LocalDateTime.of(2026, 2, 10, 8, 0));

            assertThat(contrato.getPastDueSince()).isNull();
        }

        @Test
        @DisplayName("de READ_ONLY se puede volver: es restriccion, no un callejon sin salida")
        void deReadOnlySeVuelve() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);
            contrato.changeStatus(SubscriptionStatus.READ_ONLY, null, "SYSTEM",
                    LocalDateTime.now());

            contrato.changeStatus(SubscriptionStatus.ACTIVE, "Pago recibido", "SYSTEM",
                    LocalDateTime.now());

            assertThat(contrato.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("de ACTIVE a ACTIVE se rechaza: la fila de ruido ensucia la pelicula")
        void mismoEstado() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            assertThatThrownBy(() -> contrato.changeStatus(SubscriptionStatus.ACTIVE, null,
                    "SYSTEM", LocalDateTime.now()))
                    .isInstanceOf(InvalidSubscriptionStatusTransitionException.class);
        }

        @Test
        @DisplayName("de CANCELLED no se sale")
        void cancelledEsTerminal() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);
            contrato.changeStatus(SubscriptionStatus.CANCELLED, null, "SYSTEM",
                    LocalDateTime.now());

            assertThatThrownBy(() -> contrato.changeStatus(SubscriptionStatus.ACTIVE, null,
                    "SYSTEM", LocalDateTime.now()))
                    .isInstanceOf(InvalidSubscriptionStatusTransitionException.class);
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("un contrato no puede nacer cancelado")
        void noNaceCancelado() {
            assertThatThrownBy(() -> contrato(SubscriptionStatus.CANCELLED))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("born");
        }

        @Test
        @DisplayName("TRIALING exige fecha de fin de prueba")
        void trialingExigeFecha() {
            assertThatThrownBy(() -> Subscription.create("SUS-1", 42L, null, 3L,
                    BillingCycle.MONTHLY, SubscriptionStatus.TRIALING, INICIO, null, INICIO,
                    FIN_PERIODO, null, null, 0, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("trialEndDate");
        }

        @Test
        @DisplayName("el periodo no puede terminar antes de empezar")
        void periodoInvertido() {
            assertThatThrownBy(() -> Subscription.create("SUS-1", 42L, null, 3L,
                    BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, INICIO, null, FIN_PERIODO,
                    INICIO, null, null, 0, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currentPeriodEnd");
        }

        @Test
        @DisplayName("los dias de gracia no pueden ser negativos")
        void graciaNegativa() {
            assertThatThrownBy(() -> Subscription.create("SUS-1", 42L, null, 3L,
                    BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, INICIO, null, INICIO,
                    FIN_PERIODO, null, null, -1, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("graceDays");
        }

        @Test
        @DisplayName("sin empresa no hay contrato")
        void sinEmpresa() {
            assertThatThrownBy(() -> Subscription.create("SUS-1", null, null, 3L,
                    BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, INICIO, null, INICIO,
                    FIN_PERIODO, null, null, 0, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("companyId");
        }
    }

    private static Subscription contrato(String numero, Long companyId, Long priceListId,
            BillingCycle ciclo, SubscriptionStatus status, LocalDate inicio, LocalDate periodoDesde,
            LocalDate periodoHasta, int diasDeGracia, LocalDate compromisoHasta) {
        return new Subscription(1L, numero, companyId, null, priceListId, ciclo, status, inicio,
                status == SubscriptionStatus.TRIALING ? LocalDate.of(2026, 1, 15) : null,
                periodoDesde, periodoHasta, periodoHasta, compromisoHasta, diasDeGracia, null, true,
                null, null, 0L, true);
    }

    @Nested
    @DisplayName("Invariantes de la cabecera")
    class InvariantesDeLaCabecera {

        static Stream<Arguments> cabecerasInvalidas() {
            return Stream.of(
                    Arguments.of("numero en blanco",
                            (ThrowingCallable) () -> contrato("  ", 42L, 3L, BillingCycle.MONTHLY,
                                    SubscriptionStatus.ACTIVE, INICIO, INICIO, FIN_PERIODO, 0,
                                    null),
                            "subscriptionNumber is required"),
                    Arguments.of("numero mas largo que la columna",
                            (ThrowingCallable) () -> contrato("S".repeat(31), 42L, 3L,
                                    BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, INICIO, INICIO,
                                    FIN_PERIODO, 0, null),
                            "subscriptionNumber must be 30"),
                    Arguments.of("sin lista de precios",
                            (ThrowingCallable) () -> contrato("SUS-2026-00184", 42L, null,
                                    BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, INICIO, INICIO,
                                    FIN_PERIODO, 0, null),
                            "priceListId"),
                    Arguments.of("sin ciclo de facturacion",
                            (ThrowingCallable) () -> contrato("SUS-2026-00184", 42L, 3L, null,
                                    SubscriptionStatus.ACTIVE, INICIO, INICIO, FIN_PERIODO, 0,
                                    null),
                            "billingCycle"),
                    Arguments.of("sin estado",
                            (ThrowingCallable) () -> contrato("SUS-2026-00184", 42L, 3L,
                                    BillingCycle.MONTHLY, null, INICIO, INICIO, FIN_PERIODO, 0,
                                    null),
                            "status"),
                    Arguments.of("sin fecha de inicio",
                            (ThrowingCallable) () -> contrato("SUS-2026-00184", 42L, 3L,
                                    BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, null, INICIO,
                                    FIN_PERIODO, 0, null),
                            "startDate"),
                    Arguments.of("sin periodo facturable",
                            (ThrowingCallable) () -> contrato("SUS-2026-00184", 42L, 3L,
                                    BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, INICIO, null,
                                    FIN_PERIODO, 0, null),
                            "current period"),
                    Arguments.of("compromiso anterior al inicio",
                            (ThrowingCallable) () -> contrato("SUS-2026-00184", 42L, 3L,
                                    BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, INICIO, INICIO,
                                    FIN_PERIODO, 0, LocalDate.of(2025, 12, 1)),
                            "commitmentEndDate"));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("cabecerasInvalidas")
        @DisplayName("la cabecera rechaza lo mismo que rechazaria la base")
        void cabecerasInvalidas(String caso, ThrowingCallable creacion, String fragmento) {
            assertThatThrownBy(creacion).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining(fragmento);
        }

        @Test
        @DisplayName("la mora no puede ser anterior al inicio del contrato")
        void moraAnteriorAlInicio() {
            // Un past_due_since anterior al alta hace que cualquier calculo de dias de
            // mora salga inflado, y la mora es lo que dispara el paso a READ_ONLY.
            assertThatThrownBy(() -> new Subscription(1L, "SUS-2026-00184", 42L, null, 3L,
                    BillingCycle.MONTHLY, SubscriptionStatus.PAST_DUE, INICIO, null, INICIO,
                    FIN_PERIODO, FIN_PERIODO, null, 0, LocalDate.of(2025, 12, 31), true, null, null,
                    0L, true)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("pastDueSince");
        }

        @Test
        @DisplayName("un contrato deshabilitado no ocupa el marcador de vigente de su empresa")
        void deshabilitadoNoEsVigente() {
            // enabled entra en isCurrent por el mismo motivo que en active_marker: si el
            // dominio dijera que si y la columna generada dijera que no, la empresa
            // podria firmar un segundo contrato sin que nada lo impidiera.
            Subscription deshabilitado = new Subscription(1L, "SUS-2026-00184", 42L, null, 3L,
                    BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE, INICIO, null, INICIO,
                    FIN_PERIODO, FIN_PERIODO, null, 0, null, true, null, null, 0L, false);

            assertThat(deshabilitado.isCurrent()).isFalse();
            assertThat(deshabilitado.getStatus().isCurrent()).isTrue();
        }
    }

    @Nested
    @DisplayName("Lo que cambia sin tocar lo firmado")
    class CambiosDeCabecera {

        @Test
        @DisplayName("renovar el periodo no toca ni el estado ni lo contratado")
        void renovarNoTocaLoContratado() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            contrato.renewPeriod(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 2, 28),
                    LocalDate.of(2026, 3, 1));

            assertThat(contrato.getCurrentPeriodStart()).isEqualTo(LocalDate.of(2026, 2, 1));
            assertThat(contrato.getCurrentPeriodEnd()).isEqualTo(LocalDate.of(2026, 2, 28));
            assertThat(contrato.getNextBillingDate()).isEqualTo(LocalDate.of(2026, 3, 1));
            assertThat(contrato.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        }

        @Test
        @DisplayName("renovar sin periodo completo falla")
        void renovarSinPeriodo() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            assertThatThrownBy(() -> contrato.renewPeriod(LocalDate.of(2026, 2, 1), null, null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("current period");
        }

        @Test
        @DisplayName("renovar con el periodo invertido falla")
        void renovarConPeriodoInvertido() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            assertThatThrownBy(() -> contrato.renewPeriod(LocalDate.of(2026, 2, 28),
                    LocalDate.of(2026, 2, 1), null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("currentPeriodEnd");
        }

        @Test
        @DisplayName("migrar de tarifa cambia la lista y nada mas")
        void migrarDeTarifa() {
            // Los precios firmados viven congelados en cada linea, no en la lista: por
            // eso cambiar de lista no puede mover ni un importe de lo ya contratado.
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            contrato.migrateToPriceList(99L);

            assertThat(contrato.getPriceListId()).isEqualTo(99L);
        }

        @Test
        @DisplayName("migrar a una tarifa nula falla")
        void migrarANadie() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            assertThatThrownBy(() -> contrato.migrateToPriceList(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("priceListId");
        }

        @Test
        @DisplayName("cambiar el ciclo de facturacion no toca el periodo en curso")
        void cambiarElCiclo() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            contrato.changeBillingCycle(BillingCycle.ANNUAL);

            assertThat(contrato.getBillingCycle()).isEqualTo(BillingCycle.ANNUAL);
            assertThat(contrato.getCurrentPeriodEnd()).isEqualTo(FIN_PERIODO);
        }

        @Test
        @DisplayName("cambiar a un ciclo nulo falla")
        void cambiarANingunCiclo() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            assertThatThrownBy(() -> contrato.changeBillingCycle(null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("billingCycle");
        }

        @Test
        @DisplayName("cambiar a un estado nulo falla en vez de dejar el contrato como estaba")
        void cambiarANingunEstado() {
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            assertThatThrownBy(() -> contrato.changeStatus(null, "motivo", "cobranza",
                    LocalDateTime.of(2026, 1, 20, 9, 0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("target status");
        }

        @Test
        @DisplayName("sin momento falla, y el contrato queda ya mutado sin su fila de bitacora")
        void cambiar_sin_momento_deja_el_contrato_mutado() {
            // Este caso fija un defecto, no un comportamiento deseado: ver #430.
            //
            // changeStatus asigna `status = target` ANTES de construir la anotacion de
            // bitacora, y es esa construccion la que exige occurredAt. Resultado: la
            // llamada revienta con el agregado ya cambiado y sin ninguna fila que lo
            // explique, que es literalmente lo que el javadoc del metodo declara
            // imposible. De paso, la guarda `occurredAt != null` de la linea 147 es
            // inalcanzable: si occurredAt es nulo nunca se llega a devolver nada.
            //
            // Hoy no hay dano en base porque la excepcion sube y la transaccion
            // revierte. Cuando se corrija el orden, este caso fallara: hay que
            // reescribirlo en el mismo PR.
            Subscription contrato = contrato(SubscriptionStatus.ACTIVE);

            assertThatThrownBy(() -> contrato.changeStatus(SubscriptionStatus.PAST_DUE,
                    "Cuota vencida", "cobranza", null)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("occurredAt");

            assertThat(contrato.getStatus()).isEqualTo(SubscriptionStatus.PAST_DUE);
            assertThat(contrato.getPastDueSince()).isNull();
        }
    }
}
