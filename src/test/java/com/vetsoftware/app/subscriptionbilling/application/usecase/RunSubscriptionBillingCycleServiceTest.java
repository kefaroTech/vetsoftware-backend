package com.vetsoftware.app.subscriptionbilling.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionbilling.application.command.GenerateBillingDocumentCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionBillingBatchResult;
import com.vetsoftware.app.subscriptionbilling.application.port.in.GenerateBillingDocumentUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.BillableSubscriptionItemPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.DueSubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionPeriodAdvancePort;
import com.vetsoftware.app.subscriptionbilling.domain.BillableSubscriptionItem;
import com.vetsoftware.app.subscriptionbilling.domain.BillingCycleSubscription;
import com.vetsoftware.app.subscriptionbilling.domain.BillingPeriodicity;
import com.vetsoftware.app.subscriptionbilling.domain.BillingReason;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.DuplicateBillingCycleException;
import com.vetsoftware.app.subscriptionbilling.domain.EmptyBillingDocumentException;
import com.vetsoftware.app.subscriptionbilling.domain.ItemChargeMode;
import com.vetsoftware.app.subscriptionbilling.domain.RecurringChargeKey;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El motor de facturacion recurrente.
 *
 * <p>
 * <b>Tres de las reglas que se comprueban aqui no las sostiene ninguna
 * restriccion de la base</b> —la llave antiduplicados del reinicio, el avance
 * del periodo y el filtro por modo de cobro de cada linea—, asi que este test
 * es la unica red que tienen.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("RunSubscriptionBillingCycleService — el barrido que factura solo")
class RunSubscriptionBillingCycleServiceTest {

    private static final Long EMPRESA = 42L;
    private static final Long CONTRATO = 7L;
    private static final LocalDate HOY = LocalDate.of(2026, 3, 2);
    private static final Clock RELOJ = Clock.fixed(HOY.atTime(4, 40).toInstant(ZoneOffset.UTC),
            ZoneOffset.UTC);

    @Mock
    private DueSubscriptionQueryPort dueSubscriptionQueryPort;
    @Mock
    private BillableSubscriptionItemPort itemPort;
    @Mock
    private SubscriptionChargeRepository chargeRepository;
    @Mock
    private GenerateBillingDocumentUseCase generateUseCase;
    @Mock
    private SubscriptionPeriodAdvancePort periodAdvancePort;

    private RunSubscriptionBillingCycleService service;

    @BeforeEach
    void setUp() {
        service = new RunSubscriptionBillingCycleService(dueSubscriptionQueryPort, itemPort,
                chargeRepository, generateUseCase, periodAdvancePort, RELOJ);
    }

    // ------------------------------------------------------------------ fixtures

    /**
     * El contrato del enunciado: firmado el 31 de enero con treinta dias de prueba,
     * asi que la prueba vence el 1 de marzo y devenga desde el 2. Su ancla es el 2.
     */
    private static BillingCycleSubscription contratoQueSaleDePrueba() {
        return new BillingCycleSubscription(CONTRATO, EMPRESA, BillingPeriodicity.MONTHLY,
                LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 1), LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 3, 1), null);
    }

    /** Contrato anclado al 31 que acaba de facturar febrero y vuelve a su dia. */
    private static BillingCycleSubscription contratoAncladoAl31() {
        return new BillingCycleSubscription(CONTRATO, EMPRESA, BillingPeriodicity.MONTHLY,
                LocalDate.of(2026, 1, 31), null, LocalDate.of(2026, 1, 31),
                LocalDate.of(2026, 2, 27), LocalDate.of(2026, 2, 28));
    }

    private static BillableSubscriptionItem linea(Long id, ItemChargeMode modo, int cantidad,
            int incluido, String tarifa) {
        return new BillableSubscriptionItem(id, EMPRESA, CONTRATO, 500L, "Modulo agenda", modo,
                cantidad, incluido, new BigDecimal(tarifa), new BigDecimal("19.00"),
                TaxTreatment.TAXED, LocalDate.of(2026, 1, 1), null);
    }

    private static SubscriptionCharge cargoGuardado(SubscriptionCharge entrada) {
        return entrada;
    }

    // ------------------------------------------------------------------ pruebas

    @Nested
    @DisplayName("El ancla y el primer periodo")
    class AnclaYPrimerPeriodo {

        @Test
        @DisplayName("el primer periodo completo arranca al dia siguiente del fin de la prueba,"
                + " sin prorrateo de entrada")
        void primer_periodo_completo_sin_prorrateo() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> cargoGuardado(inv.getArgument(0)));

            service.processBatchAfter(0L, 100);

            ArgumentCaptor<SubscriptionCharge> cargo = ArgumentCaptor
                    .forClass(SubscriptionCharge.class);
            verify(chargeRepository).save(cargo.capture());
            // Mes completo del 2 de marzo al 1 de abril: ni un dia prorrateado.
            assertThat(cargo.getValue().getServicePeriod().start())
                    .isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(cargo.getValue().getServicePeriod().end())
                    .isEqualTo(LocalDate.of(2026, 4, 1));
            assertThat(cargo.getValue().getChargeType()).isEqualTo(ChargeType.RECURRING);
            assertThat(cargo.getValue().getSubtotalAmount()).isEqualByComparingTo("59000.00");
        }

        @Test
        @DisplayName("un contrato anclado al 31 que facturo el 28 de febrero vuelve al 31")
        void el_ancla_no_se_degrada() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoAncladoAl31()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 2, 28)))
                    .thenReturn(List.of());

            service.processBatchAfter(0L, 100);

            // Periodo 28-feb..30-mar y proximo cobro el 31 de marzo: el ancla recupera su
            // dia en vez de quedarse en el 28 para siempre.
            verify(periodAdvancePort).advanceTo(CONTRATO, EMPRESA, LocalDate.of(2026, 2, 28),
                    LocalDate.of(2026, 3, 30), LocalDate.of(2026, 3, 31));
        }

        @Test
        @DisplayName("un contrato cuyo primer dia devengable aun no llego no factura ni avanza")
        void contrato_en_prueba_no_factura_ni_avanza() {
            BillingCycleSubscription enPrueba = new BillingCycleSubscription(CONTRATO, EMPRESA,
                    BillingPeriodicity.MONTHLY, LocalDate.of(2026, 1, 31),
                    LocalDate.of(2026, 4, 30), LocalDate.of(2026, 1, 31), LocalDate.of(2026, 4, 30),
                    null);
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(enPrueba));

            SubscriptionBillingBatchResult resultado = service.processBatchAfter(0L, 100);

            assertThat(resultado.processed()).isEqualTo(1);
            assertThat(resultado.skipped()).isEqualTo(1);
            verifyNoInteractions(itemPort, chargeRepository, generateUseCase, periodAdvancePort);
        }
    }

    @Nested
    @DisplayName("El periodo tiene que avanzar")
    class AvanceDelPeriodo {

        @Test
        @DisplayName("avanza el periodo despues de emitir, con las tres fechas del ancla")
        void avanza_despues_de_emitir() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> cargoGuardado(inv.getArgument(0)));

            service.processBatchAfter(0L, 100);

            verify(periodAdvancePort).advanceTo(CONTRATO, EMPRESA, LocalDate.of(2026, 3, 2),
                    LocalDate.of(2026, 4, 1), LocalDate.of(2026, 4, 2));
        }

        /**
         * <b>Sin esto el contrato factura una vez y nunca mas.</b> Un periodo que no
         * avanza deja al barrido de manana mirando el mismo tramo, encontrando sus
         * cargos ya sellados y sin nada que emitir.
         */
        @Test
        @DisplayName("avanza el periodo tambien cuando no habia nada que emitir")
        void avanza_aunque_no_se_emita() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.TRIAL, 1, 0, "59000.00")));
            // Sin ni un cargo devengado, la emision falla con su excepcion propia. Eso no
            // es un fallo del contrato y no puede impedir que el periodo avance.
            when(generateUseCase.execute(any()))
                    .thenThrow(new EmptyBillingDocumentException(CONTRATO));

            SubscriptionBillingBatchResult resultado = service.processBatchAfter(0L, 100);

            verify(periodAdvancePort).advanceTo(eq(CONTRATO), eq(EMPRESA), any(), any(), any());
            assertThat(resultado.skipped()).isEqualTo(1);
        }

        @Test
        @DisplayName("no avanza el periodo del contrato que fallo, para que manana se reintente")
        void no_avanza_si_fallo() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenThrow(new IllegalStateException("base caida"));

            SubscriptionBillingBatchResult resultado = service.processBatchAfter(0L, 100);

            assertThat(resultado.failures()).isEqualTo(1);
            verifyNoInteractions(periodAdvancePort);
        }
    }

    @Nested
    @DisplayName("Se factura por linea en modo de pago, nunca por estado del contrato")
    class PorLineaNoPorContrato {

        /**
         * <b>El caso de los cincuenta y nueve mil al mes.</b> El contrato sigue en
         * prueba —{@code trial_end_date} en el futuro no es lo que decide— y una de sus
         * lineas es de pago obligatorio: la facturacion electronica DIAN se cobra desde
         * el dia 0. Filtrar por estado del contrato la dejaria sin cobrar.
         */
        @Test
        @DisplayName("una linea PAID de un contrato que viene de prueba si devenga")
        void una_linea_paid_devenga_aunque_el_contrato_venga_de_prueba() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.TRIAL, 1, 0, "179000.00"),
                            linea(901L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> cargoGuardado(inv.getArgument(0)));

            SubscriptionBillingBatchResult resultado = service.processBatchAfter(0L, 100);

            ArgumentCaptor<SubscriptionCharge> cargo = ArgumentCaptor
                    .forClass(SubscriptionCharge.class);
            verify(chargeRepository).save(cargo.capture());
            assertThat(cargo.getValue().getSubscriptionItemId()).isEqualTo(901L);
            assertThat(cargo.getValue().getSubtotalAmount()).isEqualByComparingTo("59000.00");
            assertThat(resultado.chargesAccrued()).isEqualTo(1);
        }

        /**
         * R-TRIAL-14: la linea gratuita <b>conserva su tarifa real</b>, asi que
         * olvidarse del modo de cobro no produce ceros — produce la tarifa completa
         * cobrada a todos los clientes en prueba.
         */
        @Test
        @DisplayName("una linea TRIAL no devenga aunque lleve tarifa completa guardada")
        void una_linea_trial_no_devenga() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.TRIAL, 1, 0, "179000.00")));

            service.processBatchAfter(0L, 100);

            verify(chargeRepository, never()).save(any());
        }

        @Test
        @DisplayName("una linea consumida entera dentro de lo incluido no genera cargo")
        void todo_incluido_no_genera_cargo() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 3, 3, "59000.00")));

            service.processBatchAfter(0L, 100);

            verify(chargeRepository, never()).save(any());
        }

        @Test
        @DisplayName("cobra solo lo que pasa de lo incluido")
        void cobra_solo_el_exceso() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 5, 3, "10000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> cargoGuardado(inv.getArgument(0)));

            service.processBatchAfter(0L, 100);

            ArgumentCaptor<SubscriptionCharge> cargo = ArgumentCaptor
                    .forClass(SubscriptionCharge.class);
            verify(chargeRepository).save(cargo.capture());
            assertThat(cargo.getValue().getQuantity()).isEqualByComparingTo("2");
            assertThat(cargo.getValue().getSubtotalAmount()).isEqualByComparingTo("20000.00");
        }
    }

    @Nested
    @DisplayName("Un reinicio a mitad no duplica")
    class ReinicioIdempotente {

        @Test
        @DisplayName("la llave se calcula con la LINEA, no con el articulo")
        void la_llave_lleva_la_linea() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> cargoGuardado(inv.getArgument(0)));

            service.processBatchAfter(0L, 100);

            ArgumentCaptor<RecurringChargeKey> llave = ArgumentCaptor
                    .forClass(RecurringChargeKey.class);
            verify(chargeRepository).existsRecurringCharge(llave.capture());
            assertThat(llave.getValue()).isEqualTo(new RecurringChargeKey(EMPRESA, CONTRATO, 900L,
                    LocalDate.of(2026, 3, 2), LocalDate.of(2026, 4, 1)));
        }

        @Test
        @DisplayName("el segundo arranque no vuelve a devengar la linea que ya tiene su cargo")
        void el_segundo_arranque_no_devenga_dos_veces() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(true);

            SubscriptionBillingBatchResult resultado = service.processBatchAfter(0L, 100);

            verify(chargeRepository, never()).save(any());
            assertThat(resultado.chargesAccrued()).isZero();
        }

        /**
         * El barrido murio despues de emitir la factura: al volver, la barandilla de
         * periodo duplicado lo rechaza, el contrato cuenta como {@code skipped} y —esto
         * es lo importante— <b>el periodo si avanza</b>, que era el paso que faltaba.
         */
        @Test
        @DisplayName("un periodo ya facturado se salta y aun asi avanza el periodo")
        void periodo_ya_facturado_se_salta_pero_avanza() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(true);
            when(generateUseCase.execute(any())).thenThrow(new DuplicateBillingCycleException(
                    CONTRATO, LocalDate.of(2026, 3, 2), LocalDate.of(2026, 4, 1)));

            SubscriptionBillingBatchResult resultado = service.processBatchAfter(0L, 100);

            assertThat(resultado.documentsIssued()).isZero();
            assertThat(resultado.skipped()).isEqualTo(1);
            assertThat(resultado.failures()).isZero();
            verify(periodAdvancePort).advanceTo(eq(CONTRATO), eq(EMPRESA), any(), any(), any());
        }

        @Test
        @DisplayName("un periodo sin cargos pendientes se salta y no cuenta como fallo")
        void periodo_sin_cargos_no_es_fallo() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(true);
            when(generateUseCase.execute(any()))
                    .thenThrow(new EmptyBillingDocumentException(CONTRATO));

            SubscriptionBillingBatchResult resultado = service.processBatchAfter(0L, 100);

            assertThat(resultado.skipped()).isEqualTo(1);
            assertThat(resultado.failures()).isZero();
        }
    }

    @Nested
    @DisplayName("El lote y su cursor")
    class LoteYCursor {

        @Test
        @DisplayName("emite el documento como factura del ciclo, con el periodo exacto")
        void emite_como_factura_del_ciclo() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> cargoGuardado(inv.getArgument(0)));

            service.processBatchAfter(0L, 100);

            ArgumentCaptor<GenerateBillingDocumentCommand> comando = ArgumentCaptor
                    .forClass(GenerateBillingDocumentCommand.class);
            verify(generateUseCase).execute(comando.capture());
            assertThat(comando.getValue().billingReason()).isEqualTo(BillingReason.RECURRING_CYCLE);
            assertThat(comando.getValue().periodStart()).isEqualTo(LocalDate.of(2026, 3, 2));
            assertThat(comando.getValue().periodEnd()).isEqualTo(LocalDate.of(2026, 4, 1));
        }

        @Test
        @DisplayName("un lote vacio devuelve el mismo cursor y no toca nada")
        void lote_vacio() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 15L, 100)).thenReturn(List.of());

            SubscriptionBillingBatchResult resultado = service.processBatchAfter(15L, 100);

            assertThat(resultado.processed()).isZero();
            assertThat(resultado.lastId()).isEqualTo(15L);
            verifyNoInteractions(itemPort, chargeRepository, generateUseCase, periodAdvancePort);
        }

        @Test
        @DisplayName("el cursor avanza al mayor id visto")
        void el_cursor_avanza() {
            BillingCycleSubscription otro = new BillingCycleSubscription(31L, EMPRESA,
                    BillingPeriodicity.MONTHLY, LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 1), null);
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba(), otro));
            when(itemPort.findCurrentOn(eq(EMPRESA), any(), any())).thenReturn(List.of());

            SubscriptionBillingBatchResult resultado = service.processBatchAfter(0L, 100);

            assertThat(resultado.processed()).isEqualTo(2);
            assertThat(resultado.lastId()).isEqualTo(31L);
        }

        @Test
        @DisplayName("un contrato que revienta no se lleva por delante a los demas")
        void un_fallo_no_para_el_lote() {
            BillingCycleSubscription otro = new BillingCycleSubscription(31L, EMPRESA,
                    BillingPeriodicity.MONTHLY, LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 1),
                    LocalDate.of(2026, 1, 31), LocalDate.of(2026, 3, 1), null);
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba(), otro));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenThrow(new IllegalStateException("linea ilegible"));
            when(itemPort.findCurrentOn(EMPRESA, 31L, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of());

            SubscriptionBillingBatchResult resultado = service.processBatchAfter(0L, 100);

            assertThat(resultado.processed()).isEqualTo(2);
            assertThat(resultado.failures()).isEqualTo(1);
            // El segundo llego hasta el final: el fallo del primero no lo arrastro.
            assertThat(resultado.documentsIssued()).isEqualTo(1);
            verify(periodAdvancePort).advanceTo(eq(31L), eq(EMPRESA), any(), any(), any());
        }

        @Test
        @DisplayName("rechaza un tamano de lote no positivo")
        void rechaza_lote_no_positivo() {
            assertThatThrownBy(() -> service.processBatchAfter(0L, 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("batchSize must be positive");
        }

        @Test
        @DisplayName("rechaza un cursor negativo")
        void rechaza_cursor_negativo() {
            assertThatThrownBy(() -> service.processBatchAfter(-1L, 10))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("afterId must not be negative");
        }
    }

    @Nested
    @DisplayName("El reloj lo pone la inyeccion, no el llamador")
    class RelojInyectado {

        @Test
        @DisplayName("consulta los vencidos con la fecha del reloj inyectado")
        void usa_el_reloj_inyectado() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 50)).thenReturn(List.of());

            service.processBatchAfter(0L, 50);

            verify(dueSubscriptionQueryPort).dueForBillingAfter(HOY, 0L, 50);
        }

        @Test
        @DisplayName("el cargo devengado nace con la fecha del reloj inyectado")
        void el_cargo_usa_el_reloj() {
            when(dueSubscriptionQueryPort.dueForBillingAfter(HOY, 0L, 100))
                    .thenReturn(List.of(contratoQueSaleDePrueba()));
            when(itemPort.findCurrentOn(EMPRESA, CONTRATO, LocalDate.of(2026, 3, 2)))
                    .thenReturn(List.of(linea(900L, ItemChargeMode.PAID, 1, 0, "59000.00")));
            when(chargeRepository.existsRecurringCharge(any())).thenReturn(false);
            when(chargeRepository.save(any())).thenAnswer(inv -> cargoGuardado(inv.getArgument(0)));

            service.processBatchAfter(0L, 100);

            ArgumentCaptor<SubscriptionCharge> cargo = ArgumentCaptor
                    .forClass(SubscriptionCharge.class);
            verify(chargeRepository).save(cargo.capture());
            assertThat(cargo.getValue().getCreatedDate())
                    .isEqualTo(LocalDateTime.of(2026, 3, 2, 4, 40));
        }
    }
}
