package com.vetsoftware.app.subscriptionbilling.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.subscriptionbilling.application.command.AccrueOverageChargeCommand;
import com.vetsoftware.app.subscriptionbilling.application.dto.SubscriptionChargeDto;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingAuditPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionBillingMetrics;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionChargeRepository;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionItemValidationPort;
import com.vetsoftware.app.subscriptionbilling.application.port.out.SubscriptionQueryPort;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeStatus;
import com.vetsoftware.app.subscriptionbilling.domain.ChargeType;
import com.vetsoftware.app.subscriptionbilling.domain.ItemChargeMode;
import com.vetsoftware.app.subscriptionbilling.domain.NonBillableSubscriptionItemException;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionCharge;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionItemBillingProfile;
import com.vetsoftware.app.subscriptionbilling.domain.SubscriptionRef;
import com.vetsoftware.app.subscriptionbilling.domain.TaxTreatment;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Devengar el excedente, y sobre todo <b>con que impuesto</b>.
 *
 * <p>
 * <b>Este archivo nace de un defecto fiscal, no de una laguna de cobertura.</b>
 * El cargo por excedente se construia con dos constantes escritas en el
 * service: {@code TaxTreatment.EXCLUDED} y tarifa {@code 0.00}. El tratamiento
 * fiscal de la linea no llegaba hasta el, asi que <b>una clinica con su
 * suscripcion gravada al 19 % veia su excedente facturado sin IVA</b>. El daño
 * cae del lado que duele: una factura emitida de menos ante la DIAN, con la
 * responsabilidad en la empresa que emite, un impuesto que se dejo de cobrar y
 * que la administracion reclama igual. Y sin ningun sintoma: el cargo se
 * guarda, la factura sale, todo parece bien hasta la fiscalizacion.
 *
 * <p>
 * <b>Por eso los casos de {@link ImpuestoHeredadoDeLaLinea} asertan sobre el
 * importe del impuesto y no solo sobre el enum</b>, y la diferencia esta
 * medida: un aserto sobre {@code getTaxTreatment()} pasa igual de verde cuando
 * la tarifa sale mal, porque {@code EXCLUDED} y {@code EXEMPT} conviven los dos
 * con tarifa cero. Lo unico que no se puede fingir es el dinero: 20.000 al 19 %
 * son 3.800 pesos de IVA, y con la constante vieja salian cero.
 *
 * <p>
 * El caso de la linea <b>exenta</b> es el que muerde a un regresor. Con la
 * constante vieja el excedente salia {@code EXCLUDED} + {@code 0.00}; una linea
 * exenta tambien lleva tarifa cero, asi que solo el <b>tratamiento</b> los
 * distingue —exento esta gravado a tarifa cero y conserva el derecho a
 * descontar el IVA soportado; excluido esta fuera del impuesto y no lo
 * conserva—. Quien vuelva a escribir un valor fijo lo rompe aunque acierte la
 * tarifa.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AccrueOverageChargeService — el excedente hereda el impuesto de su linea")
class AccrueOverageChargeServiceTest {

    private static final Long EMPRESA = 900L;
    private static final Long OTRA_EMPRESA = 901L;
    private static final Long CONTRATO = 970L;
    private static final Long LINEA = 972L;
    private static final LocalDate INICIO = LocalDate.of(2026, 3, 1);
    private static final LocalDate FIN = LocalDate.of(2026, 3, 31);
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 15, 9, 30);
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);

    /** Cinco unidades de exceso a 4.000 cada una: 20.000 de base gravable. */
    private static final int UNIDADES = 5;
    private static final BigDecimal PRECIO_UNIDAD = new BigDecimal("4000.00");
    private static final BigDecimal SUBTOTAL = new BigDecimal("20000.00");
    private static final BigDecimal IVA_19 = new BigDecimal("19.00");

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

    private AccrueOverageChargeService service;

    @BeforeEach
    void montar() {
        service = new AccrueOverageChargeService(repository, subscriptionQueryPort,
                itemValidationPort, metrics, audit, RELOJ);
    }

    @Nested
    @DisplayName("Impuesto heredado de la linea")
    class ImpuestoHeredadoDeLaLinea {

        @Test
        @DisplayName("una linea gravada al 19 % devenga un excedente gravado al 19 %, y el IVA"
                + " que aporta son 3.800 pesos y no cero")
        void una_linea_gravada_devenga_un_excedente_gravado_con_iva_real() {
            contratoPropio();
            linea(ItemChargeMode.PAID, IVA_19, TaxTreatment.TAXED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando());

            verify(repository).save(guardado.capture());
            SubscriptionCharge cargo = guardado.getValue();
            assertThat(cargo.getTaxTreatment()).isEqualTo(TaxTreatment.TAXED);
            assertThat(cargo.getTaxRate()).isEqualByComparingTo(IVA_19);
            assertThat(cargo.getSubtotalAmount()).isEqualByComparingTo(SUBTOTAL);
            // El aserto con dientes: el enum solo dice como se declara, el importe dice
            // cuanto se cobra. Con la constante vieja (EXCLUDED, 0.00) esto daba 0.00.
            assertThat(impuestoDe(cargo)).isEqualByComparingTo("3800.00");
        }

        @Test
        @DisplayName("una linea exenta devenga un excedente EXENTO, no excluido: los dos llevan"
                + " tarifa cero y solo el tratamiento los distingue")
        void una_linea_exenta_devenga_un_excedente_exento_y_no_excluido() {
            contratoPropio();
            linea(ItemChargeMode.PAID, BigDecimal.ZERO, TaxTreatment.EXEMPT);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando());

            verify(repository).save(guardado.capture());
            // Este es el caso que caza al regresor: la constante vieja acertaba la
            // tarifa y fallaba el derecho a descuento, que es lo que separa exento de
            // excluido y lo que la DIAN mira.
            assertThat(guardado.getValue().getTaxTreatment()).isEqualTo(TaxTreatment.EXEMPT);
            assertThat(guardado.getValue().getTaxRate()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("una linea excluida devenga un excedente excluido: heredar tambien vale"
                + " cuando coincide con lo que habia escrito a mano")
        void una_linea_excluida_devenga_un_excedente_excluido() {
            contratoPropio();
            linea(ItemChargeMode.PAID, BigDecimal.ZERO, TaxTreatment.EXCLUDED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando());

            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue().getTaxTreatment()).isEqualTo(TaxTreatment.EXCLUDED);
            assertThat(impuestoDe(guardado.getValue())).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("el impuesto se lee de la linea acotada por la empresa del comando, que es"
                + " la unica consulta fiscal que hace el caso de uso")
        void el_impuesto_se_lee_de_la_linea_de_la_empresa_del_comando() {
            contratoPropio();
            linea(ItemChargeMode.PAID, IVA_19, TaxTreatment.TAXED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando());

            verify(itemValidationPort).findBillingProfileInCompany(LINEA, EMPRESA);
        }

        @Test
        @DisplayName("el impuesto viaja al DTO que devuelve el caso de uso: quien lo consume"
                + " ve la misma tarifa que se guardo")
        void el_impuesto_viaja_al_dto() {
            contratoPropio();
            linea(ItemChargeMode.PAID, IVA_19, TaxTreatment.TAXED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            SubscriptionChargeDto dto = service.execute(comando());

            assertThat(dto.taxTreatment()).isEqualTo(TaxTreatment.TAXED);
            assertThat(dto.taxRate()).isEqualByComparingTo(IVA_19);
        }
    }

    @Nested
    @DisplayName("Creacion")
    class Creacion {

        @Test
        @DisplayName("guarda un OVERAGE PENDING con la cantidad, el precio del limite y la fecha"
                + " del reloj inyectado")
        void guarda_un_overage_pendiente_con_el_precio_del_limite() {
            contratoPropio();
            linea(ItemChargeMode.PAID, IVA_19, TaxTreatment.TAXED);
            when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

            service.execute(comando());

            verify(repository).save(guardado.capture());
            assertThat(guardado.getValue()).satisfies(cargo -> {
                assertThat(cargo.getCompanyId()).isEqualTo(EMPRESA);
                assertThat(cargo.getSubscriptionId()).isEqualTo(CONTRATO);
                assertThat(cargo.getSubscriptionItemId()).isEqualTo(LINEA);
                assertThat(cargo.getChargeType()).isEqualTo(ChargeType.OVERAGE);
                assertThat(cargo.getStatus()).isEqualTo(ChargeStatus.PENDING);
                assertThat(cargo.getQuantity()).isEqualByComparingTo("5.00");
                assertThat(cargo.getUnitAmount()).isEqualByComparingTo(PRECIO_UNIDAD);
                assertThat(cargo.getSubtotalAmount()).isEqualByComparingTo(SUBTOTAL);
                assertThat(cargo.getCreatedDate()).isEqualTo(AHORA);
            });
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("cero unidades de exceso no es un cargo de cero: se rechaza sin consultar"
                + " nada ni escribir nada")
        void cero_unidades_de_exceso_se_rechaza() {
            assertThatThrownBy(() -> service.execute(comandoCon(0)))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("overageUnits");

            verifyNoInteractions(repository, subscriptionQueryPort, itemValidationPort);
        }

        @Test
        @DisplayName("sin linea de contrato no hay excedente que devengar: el permiso cuelga"
                + " de una linea concreta")
        void sin_linea_de_contrato_se_rechaza() {
            contratoPropio();

            assertThatThrownBy(() -> service.execute(comandoSinLinea()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("subscriptionItemId");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("una linea en prueba que declara excedente es una configuracion"
                + " contradictoria: 409 y no se regala el exceso")
        void una_linea_en_prueba_no_devenga_excedente() {
            contratoPropio();
            when(itemValidationPort.findBillingProfileInCompany(LINEA, EMPRESA))
                    .thenReturn(Optional.of(new SubscriptionItemBillingProfile(ItemChargeMode.TRIAL,
                            BigDecimal.ZERO, TaxTreatment.EXCLUDED)));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(NonBillableSubscriptionItemException.class);

            verifyNoInteractions(repository);
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("un contrato que no es de la empresa no existe para este caso de uso, y no"
                + " se escribe ningun cargo")
        void un_contrato_ajeno_no_existe_y_no_escribe_nada() {
            when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subscription not found");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("una linea que no es de la empresa se rechaza nombrando el campo y no"
                + " escribe nada: el impuesto de otra clinica no puede colarse")
        void una_linea_ajena_se_rechaza_nombrando_el_campo() {
            contratoPropio();
            when(itemValidationPort.findBillingProfileInCompany(LINEA, EMPRESA))
                    .thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("Subscription item not found for company");

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("si el puerto devolviera un contrato de otra empresa, la comprobacion del"
                + " propio SubscriptionRef lo caza antes de construir el cargo")
        void un_contrato_de_otra_empresa_devuelto_por_el_puerto_se_caza() {
            when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                    .thenReturn(Optional.of(new SubscriptionRef(CONTRATO, OTRA_EMPRESA)));

            assertThatThrownBy(() -> service.execute(comando()))
                    .isInstanceOf(IllegalArgumentException.class);

            verifyNoInteractions(repository);
        }
    }

    /**
     * El IVA que este cargo aporta a la factura: base por tarifa. Es la cuenta que
     * hace el desglose del documento, escrita aqui para que el caso pueda asertar
     * sobre pesos y no sobre un enum.
     */
    private static BigDecimal impuestoDe(SubscriptionCharge cargo) {
        return cargo.getSubtotalAmount().multiply(cargo.getTaxRate()).movePointLeft(2);
    }

    private void contratoPropio() {
        when(subscriptionQueryPort.findByIdAndCompanyId(CONTRATO, EMPRESA))
                .thenReturn(Optional.of(new SubscriptionRef(CONTRATO, EMPRESA)));
    }

    private void linea(ItemChargeMode modo, BigDecimal tarifa, TaxTreatment tratamiento) {
        when(itemValidationPort.findBillingProfileInCompany(LINEA, EMPRESA)).thenReturn(
                Optional.of(new SubscriptionItemBillingProfile(modo, tarifa, tratamiento)));
    }

    private static AccrueOverageChargeCommand comando() {
        return comandoCon(UNIDADES);
    }

    private static AccrueOverageChargeCommand comandoCon(int unidades) {
        return new AccrueOverageChargeCommand(EMPRESA, CONTRATO, LINEA,
                "Excedente de " + unidades + " unidades sobre el cupo contratado de PETS", INICIO,
                FIN, unidades, PRECIO_UNIDAD);
    }

    private static AccrueOverageChargeCommand comandoSinLinea() {
        return new AccrueOverageChargeCommand(EMPRESA, CONTRATO, null, "Excedente sin linea",
                INICIO, FIN, UNIDADES, PRECIO_UNIDAD);
    }
}
