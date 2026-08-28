package com.vetsoftware.app.entitlement.infrastructure.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionbilling.application.command.AccrueOverageChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.command.CreateSubscriptionChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.port.in.AccrueOverageChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.in.CreateSubscriptionChargeUseCase;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionItemValidationPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.application.usecase.AccrueOverageChargeService;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.DocumentKind;
import com.vetsoftware.app.subscriptionbilling.domain.ItemChargeMode;
import com.vetsoftware.app.subscriptionbilling.domain.NonBillableSubscriptionItemException;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionItemBillingProfile;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionRef;
import com.vetsoftware.app.subscriptionbilling.domain.TaxBreakdown;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>La costura</b> entre el contador de cupo de {@code entitlement} y el
 * devengo de {@code subscriptionbilling}: el cable por el que viajan el
 * tratamiento fiscal, los importes y los identificadores del excedente.
 *
 * <p>
 * <b>Por qué este test monta el caso de uso real y no un doble.</b> Los dos
 * extremos ya estaban probados —{@code AdjustCompanyCapacityUsageServiceTest}
 * contra un {@code OverageChargePort} mockeado,
 * {@code AccrueOverageChargeServiceTest} contra un command construido a mano— y
 * <b>los dos defectos reales de esta rodaja vivieron justo en medio</b>, con
 * los dos lados en verde: el excedente se facturaba sin IVA porque el par
 * fiscal no llegaba hasta el cargo, y el cargo se encaminaba por un caso de uso
 * reservado a plataforma, que habría dejado al cliente igual de bloqueado con
 * un 403 en vez de un 409. Un tercer test con el puerto de destino mockeado
 * habría pasado en verde con los dos defectos dentro. Aquí el adaptador habla
 * con el {@link AccrueOverageChargeService} de verdad y solo se doblan los
 * puertos de salida, que es donde termina la costura.
 *
 * <p>
 * <b>Los asertos de dinero pasan por el cálculo de producción.</b>
 * {@link TaxBreakdown} es el que emite el impuesto de la factura, así que el
 * caso pregunta cuántos pesos de IVA aportaría este cargo a su documento y no
 * qué enum lleva escrito. Es una distinción medida: {@code EXCLUDED} y
 * {@code EXEMPT} conviven los dos con tarifa cero, así que un aserto sobre el
 * enum pasa igual de verde cuando la tarifa sale mal — y con la constante vieja
 * ({@code EXCLUDED} + {@code 0.00}) estos 36.000 al 19 % daban <b>cero</b>
 * pesos de IVA en lugar de 6.840.
 *
 * <p>
 * Las cuatro cifras del fixture son distintas entre sí a propósito —3 unidades,
 * 12.000 de precio, 36.000 de base, 6.840 de IVA— y los tres identificadores
 * también (900 / 970 / 972): un cruce por el camino no puede compensarse solo.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("OverageChargeAdapter — la costura entre el cupo y el devengo")
class OverageChargeAdapterTest {

    private static final Long EMPRESA = 900L;
    private static final Long CONTRATO = 970L;
    private static final Long LINEA = 972L;
    private static final String DIMENSION = "PETS";

    /** Tres unidades de exceso a 12.000: 36.000 de base gravable. */
    private static final int UNIDADES = 3;
    private static final BigDecimal PRECIO_UNIDAD = new BigDecimal("12000.00");
    private static final BigDecimal SUBTOTAL = new BigDecimal("36000.00");
    private static final BigDecimal IVA_19 = new BigDecimal("19.00");

    private static final LocalDate INICIO = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 3, 31);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 15, 9, 30);
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    /** El gate de {@code RecordLimitEventUseCase}: un efecto del sistema. */
    private static final String GATE_DEL_EFECTO_DEL_SISTEMA = "hasRole('SYSTEM') or @authz.isMyCompany(#command.companyId)";

    @Mock
    private SubscriptionChargeRepository repository;
    @Mock
    private SubscriptionQueryPort subscriptionQueryPort;
    @Mock
    private SubscriptionItemValidationPort itemValidationPort;
    @Mock
    private SubscriptionBillingMetrics metrics;
    @Mock
    private SubscriptionBillingAuditPort audit;
    @Captor
    private ArgumentCaptor<SubscriptionCharge> guardado;

    private OverageChargeAdapter adapter;

    @BeforeEach
    void montarLaCostura() {
        adapter = new OverageChargeAdapter(new AccrueOverageChargeService(repository,
                subscriptionQueryPort, itemValidationPort, metrics, audit, RELOJ));
    }

    @Nested
    @DisplayName("El impuesto que cruza la costura")
    class ImpuestoQueCruzaLaCostura {

        @Test
        @DisplayName("una linea gravada al 19 % produce un excedente que aporta 6.840 pesos de"
                + " IVA a su factura, no cero")
        void una_linea_gravada_produce_un_excedente_con_iva_real() {
            contratoPropio();
            linea(ItemChargeMode.PAID, IVA_19, TaxTreatment.TAXED);
            devuelveLoQueGuarda();

            adapter.chargeOverage(EMPRESA, CONTRATO, LINEA, DIMENSION, UNIDADES, PRECIO_UNIDAD,
                    INICIO, FIN);

            // El aserto con dientes: no el enum, sino los pesos que el desglose de
            // produccion declararia ante la DIAN por este cargo.
            TaxBreakdown desglose = desgloseDe(cargoGuardado());
            assertThat(desglose.taxAmount()).isEqualByComparingTo("6840.00");
            assertThat(desglose.totalAmount()).isEqualByComparingTo("42840.00");
        }

        @Test
        @DisplayName("la tarifa de la linea llega intacta al cargo: 19,00, ni redondeada ni"
                + " sustituida por la constante cero de antes")
        void la_tarifa_de_la_linea_llega_intacta() {
            contratoPropio();
            linea(ItemChargeMode.PAID, IVA_19, TaxTreatment.TAXED);
            devuelveLoQueGuarda();

            adapter.chargeOverage(EMPRESA, CONTRATO, LINEA, DIMENSION, UNIDADES, PRECIO_UNIDAD,
                    INICIO, FIN);

            SubscriptionCharge cargo = cargoGuardado();
            assertThat(cargo.getTaxRate()).isEqualByComparingTo(IVA_19);
            assertThat(cargo.getTaxTreatment()).isEqualTo(TaxTreatment.TAXED);
        }

        @Test
        @DisplayName("una linea exenta produce un excedente EXENTO y no excluido: los dos van a"
                + " cero pesos de IVA y solo el tratamiento los distingue")
        void una_linea_exenta_produce_un_excedente_exento_y_no_excluido() {
            contratoPropio();
            linea(ItemChargeMode.PAID, BigDecimal.ZERO, TaxTreatment.EXEMPT);
            devuelveLoQueGuarda();

            adapter.chargeOverage(EMPRESA, CONTRATO, LINEA, DIMENSION, UNIDADES, PRECIO_UNIDAD,
                    INICIO, FIN);

            // Aqui el importe no separa exento de excluido --los dos dan cero-- y el
            // que separa es el derecho a descontar el IVA soportado, que es lo que la
            // linea del desglose declara. La constante vieja acertaba los pesos y
            // fallaba el derecho.
            assertThat(desgloseDe(cargoGuardado()).lineas()).singleElement().satisfies(fiscal -> {
                assertThat(fiscal.taxTreatment()).isEqualTo(TaxTreatment.EXEMPT);
                assertThat(fiscal.taxableBase()).isEqualByComparingTo(SUBTOTAL);
                assertThat(fiscal.taxAmount()).isEqualByComparingTo("0.00");
            });
        }

        @Test
        @DisplayName("una linea excluida produce un excedente excluido: heredar tambien vale"
                + " cuando coincide con lo que habia escrito a mano")
        void una_linea_excluida_produce_un_excedente_excluido() {
            contratoPropio();
            linea(ItemChargeMode.PAID, BigDecimal.ZERO, TaxTreatment.EXCLUDED);
            devuelveLoQueGuarda();

            adapter.chargeOverage(EMPRESA, CONTRATO, LINEA, DIMENSION, UNIDADES, PRECIO_UNIDAD,
                    INICIO, FIN);

            SubscriptionCharge cargo = cargoGuardado();
            assertThat(cargo.getTaxTreatment()).isEqualTo(TaxTreatment.EXCLUDED);
            assertThat(desgloseDe(cargo).taxAmount()).isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("Identificadores e importes que no se cruzan")
    class IdentificadoresQueNoSeCruzan {

        @Test
        @DisplayName("cada identificador y cada importe cae en su sitio: empresa 900, contrato"
                + " 970, linea 972, 3 unidades a 12.000 y 36.000 de base")
        void cada_identificador_y_cada_importe_cae_en_su_sitio() {
            contratoPropio();
            linea(ItemChargeMode.PAID, IVA_19, TaxTreatment.TAXED);
            devuelveLoQueGuarda();

            adapter.chargeOverage(EMPRESA, CONTRATO, LINEA, DIMENSION, UNIDADES, PRECIO_UNIDAD,
                    INICIO, FIN);

            assertThat(cargoGuardado()).satisfies(cargo -> {
                assertThat(cargo.getCompanyId()).isEqualTo(EMPRESA);
                assertThat(cargo.getSubscriptionId()).isEqualTo(CONTRATO);
                assertThat(cargo.getSubscriptionItemId()).isEqualTo(LINEA);
                assertThat(cargo.getChargeType()).isEqualTo(ChargeType.OVERAGE);
                assertThat(cargo.getStatus()).isEqualTo(ChargeStatus.PENDING);
                assertThat(cargo.getQuantity()).isEqualByComparingTo("3.00");
                assertThat(cargo.getUnitAmount()).isEqualByComparingTo(PRECIO_UNIDAD);
                assertThat(cargo.getSubtotalAmount()).isEqualByComparingTo(SUBTOTAL);
                assertThat(cargo.getServicePeriod().start()).isEqualTo(INICIO);
                assertThat(cargo.getServicePeriod().end()).isEqualTo(FIN);
                assertThat(cargo.getCreatedDate()).isEqualTo(AHORA);
            });
        }

        @Test
        @DisplayName("el contrato se resuelve acotado por la empresa y la linea tambien: el"
                + " cargo de una clinica no puede colgar del contrato de otra")
        void las_dos_lecturas_van_acotadas_por_la_empresa() {
            contratoPropio();
            linea(ItemChargeMode.PAID, IVA_19, TaxTreatment.TAXED);
            devuelveLoQueGuarda();

            adapter.chargeOverage(EMPRESA, CONTRATO, LINEA, DIMENSION, UNIDADES, PRECIO_UNIDAD,
                    INICIO, FIN);

            // Los stubs de arriba estan acotados a (CONTRATO, EMPRESA) y (LINEA,
            // EMPRESA): bajo STRICT_STUBS, cruzar el contrato con la linea o perder el
            // companyId por el camino no devolveria vacio, reventaria el test.
            assertThat(cargoGuardado().getCompanyId()).isEqualTo(EMPRESA);
        }

        @ParameterizedTest(name = "{0} → \"{1}\"")
        @CsvSource({"1, Excedente de 1 unidad sobre el cupo contratado de PETS",
                "3, Excedente de 3 unidades sobre el cupo contratado de PETS"})
        @DisplayName("la descripcion nombra el eje y concuerda en numero: es lo que el cliente"
                + " lee en su cuenta de cobro")
        void la_descripcion_nombra_el_eje_y_concuerda_en_numero(int unidades, String esperada) {
            contratoPropio();
            linea(ItemChargeMode.PAID, IVA_19, TaxTreatment.TAXED);
            devuelveLoQueGuarda();

            adapter.chargeOverage(EMPRESA, CONTRATO, LINEA, DIMENSION, unidades, PRECIO_UNIDAD,
                    INICIO, FIN);

            assertThat(cargoGuardado().getDescription()).isEqualTo(esperada);
        }
    }

    /**
     * El segundo defecto de la noche: el cargo se encaminaba por
     * {@link CreateSubscriptionChargeUseCase}, cerrado a {@code hasRole('SYSTEM')}
     * a secas, cuando quien dispara el consumo es un empleado del tenant. El
     * cliente quedaba igual de bloqueado, con un 403 en vez de un 409.
     */
    @Nested
    @DisplayName("El gate por el que se encamina")
    class Gate {

        @Test
        @DisplayName("el adaptador solo depende del puerto dedicado, nunca del de alta general")
        void el_adaptador_solo_depende_del_puerto_dedicado() {
            assertThat(OverageChargeAdapter.class.getDeclaredConstructors()).hasSize(1);
            assertThat(OverageChargeAdapter.class.getDeclaredConstructors()[0].getParameterTypes())
                    .containsExactly(AccrueOverageChargeUseCase.class);
        }

        @Test
        @DisplayName("el puerto de destino lleva el gate del efecto del sistema, el mismo de"
                + " RecordLimitEventUseCase, y no una capacidad concedible al tenant")
        void el_puerto_de_destino_lleva_el_gate_del_efecto_del_sistema() throws Exception {
            assertThat(gateDe(AccrueOverageChargeUseCase.class, AccrueOverageChargeCommand.class))
                    .isEqualTo(GATE_DEL_EFECTO_DEL_SISTEMA);
        }

        @Test
        @DisplayName("el puerto de alta general sigue cerrado al empleado: reencaminar el"
                + " excedente por alli volveria a devolver 403")
        void el_puerto_de_alta_general_sigue_cerrado_al_empleado() throws Exception {
            assertThat(gateDe(CreateSubscriptionChargeUseCase.class,
                    CreateSubscriptionChargeCommand.class)).doesNotContain("isMyCompany");
        }

        @Test
        @DisplayName("el parametro que resuelve el SpEL sigue llamandose command: si dejan de"
                + " coincidir, la comprobacion evalua a null en silencio y niega siempre")
        void el_parametro_que_resuelve_el_spel_sigue_llamandose_command() throws Exception {
            Method destino = AccrueOverageChargeUseCase.class.getMethod("execute",
                    AccrueOverageChargeCommand.class);

            assertThat(destino.getParameterCount()).isEqualTo(1);
            assertThat(destino.getParameters()[0].getName()).isEqualTo("command");
        }
    }

    /**
     * Aquí es al revés que en {@link LimitDenialAdapter}: aquel se traga las
     * excepciones a propósito, porque la negación ya estaba decidida y al usuario
     * le tiene que llegar «se te acabó el cupo» y no un 500. Este no puede tragarse
     * ninguna: si el cargo no se escribe, el consumo por encima del techo tampoco
     * puede quedar.
     */
    @Nested
    @DisplayName("No se traga las excepciones")
    class NoSeTragaLasExcepciones {

        @Test
        @DisplayName("una linea en prueba que declara excedente tumba la operacion entera y no"
                + " escribe ningun cargo: el exceso no se regala")
        void una_linea_en_prueba_tumba_la_operacion_entera() {
            contratoPropio();
            linea(ItemChargeMode.TRIAL, BigDecimal.ZERO, TaxTreatment.EXCLUDED);

            assertThatThrownBy(() -> adapter.chargeOverage(EMPRESA, CONTRATO, LINEA, DIMENSION,
                    UNIDADES, PRECIO_UNIDAD, INICIO, FIN))
                    .isInstanceOf(NonBillableSubscriptionItemException.class);

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un contrato que no es de la empresa sale por excepcion y sin escribir:"
                + " tragarsela seria el defecto anterior con otra cara")
        void un_contrato_ajeno_sale_por_excepcion_y_sin_escribir() {
            when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> adapter.chargeOverage(EMPRESA, CONTRATO, LINEA, DIMENSION,
                    UNIDADES, PRECIO_UNIDAD, INICIO, FIN))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subscription not found");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("un fallo al guardar el cargo sube hasta el llamador para que la"
                + " transaccion del contador se vaya atras con el")
        void un_fallo_al_guardar_sube_hasta_el_llamador() {
            contratoPropio();
            linea(ItemChargeMode.PAID, IVA_19, TaxTreatment.TAXED);
            when(repository.save(any()))
                    .thenThrow(new IllegalStateException("could not execute statement"));

            assertThatThrownBy(() -> adapter.chargeOverage(EMPRESA, CONTRATO, LINEA, DIMENSION,
                    UNIDADES, PRECIO_UNIDAD, INICIO, FIN)).isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("could not execute statement");
        }

        @Test
        @DisplayName("el devengo no abre transaccion propia: se une a la del contador para que"
                + " consumo y cargo vivan o mueran juntos")
        void el_devengo_no_abre_transaccion_propia() throws Exception {
            assertThat(AccrueOverageChargeService.class.getAnnotation(Transactional.class))
                    .isNull();
            assertThat(AccrueOverageChargeService.class
                    .getMethod("execute", AccrueOverageChargeCommand.class)
                    .getAnnotation(Transactional.class)).isNull();
        }
    }

    /**
     * El IVA que este cargo aportaria a su factura, por el calculo de produccion.
     */
    private static TaxBreakdown desgloseDe(SubscriptionCharge cargo) {
        return TaxBreakdown.of(List.of(cargo), DocumentKind.INVOICE, EMPRESA, AHORA);
    }

    private static String gateDe(Class<?> puerto, Class<?> comando) throws NoSuchMethodException {
        return puerto.getMethod("execute", comando).getAnnotation(PreAuthorize.class).value();
    }

    private SubscriptionCharge cargoGuardado() {
        verify(repository).save(guardado.capture());
        return guardado.getValue();
    }

    private void contratoPropio() {
        when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                .thenReturn(Optional.of(new SubscriptionRef(CONTRATO, EMPRESA)));
    }

    private void linea(ItemChargeMode modo, BigDecimal tarifa, TaxTreatment tratamiento) {
        when(itemValidationPort.findBillingProfileInCompany(LINEA, EMPRESA)).thenReturn(
                Optional.of(new SubscriptionItemBillingProfile(modo, tarifa, tratamiento)));
    }

    private void devuelveLoQueGuarda() {
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
    }
}
