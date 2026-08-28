package com.vetsoftware.app.gatewaysettlement.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.gatewaysettlement.domain.GatewaySettlement;
import com.vetsoftware.app.gatewaysettlement.domain.PaymentCountReconciliation;
import com.vetsoftware.app.gatewaysettlement.domain.SettlementAmounts;
import com.vetsoftware.app.gatewaysettlement.testsupport.GatewaySettlementMother;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.testsupport.AbstractDataJpaTest;
import com.vetsoftware.app.testsupport.EngineConstraint;
import com.vetsoftware.app.testsupport.PersistenceSliceConfig;
import com.vetsoftware.app.testsupport.SchemaSeed;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Rodaja de {@code JpaGatewaySettlementRepository} contra MySQL real.
 *
 * <p>
 * <b>Tres cosas de esta feature no se pueden comprobar sin el motor</b>, y son
 * exactamente las tres que esta clase vigila:
 *
 * <ol>
 * <li><b>El contraste de cobros.</b> El contador es una consulta
 * <em>nativa</em> contra {@code subscription_payments}, y es nativa porque
 * {@code settlement_reference} existe en el esquema pero <b>no esta mapeada</b>
 * en {@code SubscriptionPaymentJpaEntity}. El SQL nativo se queda fuera de la
 * validacion de arranque de Hibernate: si esa columna se renombra, lo unico que
 * lo denuncia antes de produccion es este caso.</li>
 * <li><b>La comparacion exacta de la referencia del lote</b>, que vive en la
 * colacion {@code ascii_bin} de las columnas y no en una linea de Java.</li>
 * <li><b>Que los cuatro {@code CHECK} del changeset 326 estan puestos</b> — el
 * cinturon bajo el tirante de las invariantes del dominio.</li>
 * </ol>
 *
 * <p>
 * <b>Por que el adaptador se construye a mano.</b>
 * {@code PersistenceSliceConfig} reune los adaptadores para que todas las
 * rodajas compartan una unica clave de {@code MergedContextConfiguration} y,
 * con ella, un unico contexto cacheado. Declarar aqui un {@code @Import} propio
 * volveria a darle a esta clase una clave unica y un arranque de contexto
 * entero para ella sola.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaGatewaySettlementRepository — las liquidaciones contra MySQL real")
class GatewaySettlementPersistenceIT extends AbstractDataJpaTest {

    /** Ids del rango reservado a esta rodaja; ninguna otra usa el 875x. */
    private static final Long LOTE_CRUDO = 8750L;
    private static final Long PRIMER_PAGO = 8755L;

    private static final LocalDate DIA = LocalDate.of(2026, 3, 12);
    private static final LocalDateTime CREADA_EL = LocalDateTime.of(2026, 3, 14, 9, 30, 15);

    @Autowired
    private GatewaySettlementJpaRepository springDataRepository;
    @PersistenceContext
    private EntityManager entityManager;

    private JpaGatewaySettlementRepository repository;

    /**
     * Contador propio en vez de ids derivados del texto de la referencia: un
     * {@code hashCode} puede ser negativo y puede colisionar, y una colision aqui
     * fallaria con un {@code Duplicate entry} de la clave primaria que no tiene
     * nada que ver con lo que el caso quiere probar.
     */
    private long siguienteIdDePago = PRIMER_PAGO;

    @BeforeEach
    void adaptador() {
        repository = new JpaGatewaySettlementRepository(springDataRepository,
                new GatewaySettlementJpaMapper());
        siguienteIdDePago = PRIMER_PAGO;
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda el lote y recupera los CINCO importes cada uno en su columna")
        void guarda_el_lote_y_recupera_los_cinco_importes() {
            GatewaySettlement guardado = repository.save(GatewaySettlementMother.reciencargada());
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(recuperado -> {
                assertThat(recuperado.getGateway()).isEqualTo(GatewaySettlementMother.PASARELA);
                assertThat(recuperado.getSettlementReference())
                        .isEqualTo(GatewaySettlementMother.REFERENCIA);
                SettlementAmounts importes = recuperado.getAmounts();
                assertThat(importes.gross()).isEqualByComparingTo("12450800.00");
                assertThat(importes.fee()).isEqualByComparingTo("373524.00");
                // Las dos columnas que existen para no quedar sumadas dentro de otra:
                // si el mapper las cruzara, estas dos aserciones caerian.
                assertThat(importes.feeTax()).isEqualByComparingTo("70969.56");
                assertThat(importes.gmf()).isEqualByComparingTo("46423.10");
                assertThat(importes.net()).isEqualByComparingTo("11959883.34");
                assertThat(recuperado.getPaymentCount()).isEqualTo(37);
                assertThat(recuperado.getSettledOn()).isEqualTo(DIA);
                assertThat(recuperado.getCreatedDate()).isEqualTo(CREADA_EL);
                assertThat(recuperado.getProviderInvoiceRef()).isNull();
                assertThat(recuperado.getBankReceiptId()).isNull();
                assertThat(recuperado.getVersion()).isZero();
            });
        }

        @Test
        @DisplayName("el ciclo completo: llega la factura del proveedor y la version se mueve")
        void el_ciclo_completo_mueve_la_version() {
            GatewaySettlement guardado = repository.save(GatewaySettlementMother.reciencargada());
            entityManager.flush();
            entityManager.clear();

            GatewaySettlement cargado = repository.findById(guardado.getId()).orElseThrow();
            cargado.attachProviderInvoice(GatewaySettlementMother.FACTURA_DEL_PROVEEDOR,
                    GatewaySettlementMother.NIT_DEL_PROVEEDOR);
            repository.save(cargado);
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findById(guardado.getId())).get().satisfies(conSoporte -> {
                assertThat(conSoporte.getProviderInvoiceRef())
                        .isEqualTo(GatewaySettlementMother.FACTURA_DEL_PROVEEDOR);
                assertThat(conSoporte.getProviderTaxId())
                        .isEqualTo(GatewaySettlementMother.NIT_DEL_PROVEEDOR);
                // El UPDATE paso por el ciclo de Hibernate y no por una escritura masiva
                // que dejaria la version intacta.
                assertThat(conSoporte.getVersion()).isEqualTo(1L);
            });
        }
    }

    @Nested
    @DisplayName("Unicidad de la referencia")
    class UnicidadDeLaReferencia {

        @Test
        @DisplayName("la misma pasarela con la misma referencia la para la unicidad")
        void la_misma_pasarela_con_la_misma_referencia_la_para_la_unicidad() {
            repository.save(GatewaySettlementMother.conReferencia("LOTE-DUPLICADO"));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_gateway_settlements_reference", () -> {
                repository.save(GatewaySettlementMother.conReferencia("LOTE-DUPLICADO"));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("la misma referencia en OTRA pasarela si entra: la unicidad es del par")
        void la_misma_referencia_en_otra_pasarela_si_entra() {
            // Dos pasarelas distintas numeran sus lotes como quieren. Una unicidad solo
            // por referencia rechazaria al segundo proveedor el dia que se contrate.
            repository.save(GatewaySettlementMother.deLaPasarela("WOMPI", "LOTE-0001"));
            repository.save(GatewaySettlementMother.deLaPasarela("PAYU", "LOTE-0001"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAll(0, 20).totalElements()).isEqualTo(2L);
        }

        @Test
        @DisplayName("dos capitalizaciones de la misma referencia son DOS lotes distintos")
        void dos_capitalizaciones_son_dos_lotes() {
            // Las columnas son ascii_bin. Bajo la colacion heredada del esquema estas dos
            // filas serian la misma y el segundo lote se descartaria como duplicado: un
            // abono real entero fuera del cuadre. Si alguien devuelve las columnas a
            // utf8mb4_unicode_ci, este caso se pone rojo.
            GatewaySettlement minusculas = repository
                    .save(GatewaySettlementMother.conReferencia("lote-9f2a"));
            GatewaySettlement mayusculas = repository
                    .save(GatewaySettlementMother.conReferencia("LOTE-9F2A"));
            entityManager.flush();
            entityManager.clear();

            assertThat(minusculas.getId()).isNotEqualTo(mayusculas.getId());
            assertThat(repository.existsByGatewayAndSettlementReference("WOMPI", "lote-9f2a"))
                    .isTrue();
            assertThat(repository.existsByGatewayAndSettlementReference("WOMPI", "LOTE-9F2A"))
                    .isTrue();
            assertThat(repository.existsByGatewayAndSettlementReference("PAYU", "lote-9f2a"))
                    .isFalse();
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("un neto que no cuadra con los otros cuatro lo para chk_gateway_settlements_net")
        void un_neto_que_no_cuadra_lo_para_el_check() {
            // El dominio ya rechaza esta combinacion, asi que la unica forma de
            // comprobar que la base tambien la rechaza —el cinturon bajo el tirante— es
            // escribir la fila por SQL nativo, saltandose el agregado.
            EngineConstraint.assertViolates("chk_gateway_settlements_net",
                    () -> insertarCrudo(LOTE_CRUDO, "LOTE-CHK-1", new BigDecimal("1000.00"),
                            new BigDecimal("100.00"), new BigDecimal("19.00"),
                            new BigDecimal("4.00"), new BigDecimal("900.00"), 5));
        }

        @Test
        @DisplayName("un neto de cero lo para chk_gateway_settlements_amounts")
        void un_neto_de_cero_lo_para_el_check_de_importes() {
            // Congela que hoy un lote cuyo neto no es positivo NO se puede guardar: el
            // contracargo que se lleva por delante el abono entero es inexpresable
            // mientras el CHECK exija net > 0. Si algun dia el negocio lo necesita, la
            // salida es un changeset, y este caso se pondra rojo para avisarlo.
            EngineConstraint.assertViolates("chk_gateway_settlements_amounts",
                    () -> insertarCrudo(LOTE_CRUDO + 1, "LOTE-CHK-2", new BigDecimal("1000.00"),
                            new BigDecimal("1000.00"), BigDecimal.ZERO, BigDecimal.ZERO,
                            BigDecimal.ZERO, 5));
        }

        @Test
        @DisplayName("un lote de cero cobros lo para chk_gateway_settlements_payment_count")
        void un_lote_de_cero_cobros_lo_para_el_check() {
            EngineConstraint.assertViolates("chk_gateway_settlements_payment_count",
                    () -> insertarCrudo(LOTE_CRUDO + 2, "LOTE-CHK-3", new BigDecimal("1000.00"),
                            new BigDecimal("100.00"), new BigDecimal("19.00"),
                            new BigDecimal("4.00"), new BigDecimal("877.00"), 0));
        }

        @Test
        @DisplayName("media factura del proveedor la para chk_gateway_settlements_provider_invoice")
        void media_factura_la_para_el_check() {
            // El bicondicional del soporte: sin el NIT no hay reporte de terceros.
            EngineConstraint.assertViolates("chk_gateway_settlements_provider_invoice",
                    () -> entityManager.createNativeQuery("""
                            INSERT INTO gateway_settlements (id, gateway, settlement_reference,
                                    provider_invoice_ref, provider_tax_id, gross_amount,
                                    fee_amount, fee_tax_amount, gmf_amount, net_amount,
                                    payment_count, settled_on, created_date, version)
                            VALUES (:id, 'WOMPI', 'LOTE-CHK-4', 'FE-SOLA', NULL, 1000.00,
                                    100.00, 19.00, 4.00, 877.00, 5, :dia, :creado, 0)
                            """).setParameter("id", LOTE_CRUDO + 3).setParameter("dia", DIA)
                            .setParameter("creado", CREADA_EL).executeUpdate());
        }
    }

    @Nested
    @DisplayName("El contraste de cobros")
    class ElContrasteDeCobros {

        @BeforeEach
        void sembrarLaEmpresa() {
            SchemaSeed.seed(entityManager);
        }

        @Test
        @DisplayName("una liquidacion que declara 37 cobros y tiene 36 pagos enlazados dispara la vigilancia")
        void una_liquidacion_que_declara_37_cobros_y_tiene_36_pagos_enlazados_dispara_la_vigilancia() {
            // R-MONEY-51 del documento maestro, y la razon de ser de payment_count. El
            // cobro que la pasarela liquido pero que nunca se ato a su lote no deja
            // ningun otro rastro: el dinero cuadra —entro en el bruto— pero el cliente
            // aparece debiendo lo que ya pago.
            GatewaySettlement lote = repository
                    .save(GatewaySettlementMother.conCobros(37, "LOTE-CONTRASTE"));
            entityManager.flush();
            atarPagos("LOTE-CONTRASTE", 36);

            long enlazados = springDataRepository.countSettledPayments("WOMPI", "LOTE-CONTRASTE");
            PaymentCountReconciliation contraste = lote.reconcileWith(enlazados);

            assertThat(enlazados).isEqualTo(36L);
            assertThat(contraste.isBalanced()).isFalse();
            assertThat(contraste.difference()).isEqualTo(1L);
        }

        @Test
        @DisplayName("el contador solo cuenta los pagos de SU lote, no los del de al lado")
        void el_contador_solo_cuenta_los_pagos_de_su_lote() {
            // Si el WHERE se dejara la referencia, todos los lotes de la pasarela darian
            // el mismo numero y la vigilancia entera seria ruido.
            repository.save(GatewaySettlementMother.conCobros(2, "LOTE-A"));
            repository.save(GatewaySettlementMother.conCobros(9, "LOTE-B"));
            entityManager.flush();
            atarPagos("LOTE-A", 2);
            atarPagos("LOTE-B", 3);

            assertThat(springDataRepository.countSettledPayments("WOMPI", "LOTE-A")).isEqualTo(2L);
            assertThat(springDataRepository.countSettledPayments("WOMPI", "LOTE-B")).isEqualTo(3L);
        }

        @Test
        @DisplayName("un lote sin un solo pago atado cuenta cero, no falla")
        void un_lote_sin_pagos_cuenta_cero() {
            repository.save(GatewaySettlementMother.conCobros(4, "LOTE-HUERFANO"));
            entityManager.flush();

            assertThat(springDataRepository.countSettledPayments("WOMPI", "LOTE-HUERFANO"))
                    .isZero();
        }
    }

    @Nested
    @DisplayName("El listado")
    class ElListado {

        @Test
        @DisplayName("lo ultimo que liquido la pasarela sale primero")
        void lo_ultimo_liquidado_sale_primero() {
            GatewaySettlement viejo = repository.save(
                    GatewaySettlementMother.liquidadaEl(LocalDate.of(2026, 1, 10), "L-VIEJO"));
            GatewaySettlement reciente = repository.save(
                    GatewaySettlementMother.liquidadaEl(LocalDate.of(2026, 3, 20), "L-NUEVO"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAll(0, 20).content()).extracting(GatewaySettlement::getId)
                    .containsExactly(reciente.getId(), viejo.getId());
        }

        @Test
        @DisplayName("dos lotes del mismo dia desempatan por id descendente")
        void dos_lotes_del_mismo_dia_desempatan_por_id() {
            // La pasarela liquida varios lotes con la MISMA fecha —uno por cuenta, uno
            // por moneda—. Sin desempate el orden lo decide el motor y dos paginas
            // consecutivas repiten u omiten filas: aqui una fila omitida son sesenta
            // cobros que nadie concilia.
            GatewaySettlement primero = repository
                    .save(GatewaySettlementMother.liquidadaEl(DIA, "L-EMPATE-A"));
            GatewaySettlement segundo = repository
                    .save(GatewaySettlementMother.liquidadaEl(DIA, "L-EMPATE-B"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findAll(0, 20).content()).extracting(GatewaySettlement::getId)
                    .containsExactly(segundo.getId(), primero.getId());
        }

        @Test
        @DisplayName("la pagina respeta el tope del kernel de paginacion")
        void la_pagina_respeta_el_tope() {
            repository.save(GatewaySettlementMother.conReferencia("LOTE-TOPE"));
            entityManager.flush();
            entityManager.clear();

            // 100000 no llega a la consulta: Pages.request lo acota a MAX_SIZE.
            PageResult<GatewaySettlement> pagina = repository.findAll(0, 100000);
            assertThat(pagina.pageSize()).isEqualTo(200);
        }
    }

    /**
     * Cobros atados al lote por la clave hacia atras. El {@code gateway_reference}
     * es distinto en cada uno porque {@code uq_subscription_payments_gateway} es
     * sobre {@code (gateway, gateway_reference)}: es el aviso de la pasarela, no el
     * lote.
     */
    private void atarPagos(String referenciaDelLote, int cuantos) {
        for (int i = 0; i < cuantos; i++) {
            entityManager.createNativeQuery("""
                    INSERT INTO subscription_payments (id, company_id, amount, currency,
                            payment_method, gateway, gateway_reference, received_at, status,
                            settlement_reference, settled_on, refunded_amount, created_date,
                            version)
                    VALUES (:id, :empresa, 120000.00, 'COP', 'CARD', 'WOMPI', :aviso,
                            :recibido, 'CONFIRMED', :lote, :dia, 0.00, :recibido, 0)
                    """).setParameter("id", siguienteIdDePago++)
                    .setParameter("empresa", SchemaSeed.COMPANY_ID)
                    .setParameter("aviso", referenciaDelLote + "-AV-" + i)
                    .setParameter("recibido", CREADA_EL).setParameter("lote", referenciaDelLote)
                    .setParameter("dia", DIA).executeUpdate();
        }
        entityManager.flush();
    }

    /**
     * La fila escrita <b>sin pasar por el agregado</b>, que es la unica forma de
     * llegar a los {@code CHECK} del changeset 326: las invariantes de
     * {@code GatewaySettlement} y de {@code SettlementAmounts} rechazan estos
     * valores mucho antes.
     */
    private void insertarCrudo(Long id, String referencia, BigDecimal bruto, BigDecimal comision,
            BigDecimal impuesto, BigDecimal gmf, BigDecimal neto, int cobros) {
        entityManager.createNativeQuery("""
                INSERT INTO gateway_settlements (id, gateway, settlement_reference, gross_amount,
                        fee_amount, fee_tax_amount, gmf_amount, net_amount, payment_count,
                        settled_on, created_date, version)
                VALUES (:id, 'WOMPI', :referencia, :bruto, :comision, :impuesto, :gmf, :neto,
                        :cobros, :dia, :creado, 0)
                """).setParameter("id", id).setParameter("referencia", referencia)
                .setParameter("bruto", bruto).setParameter("comision", comision)
                .setParameter("impuesto", impuesto).setParameter("gmf", gmf)
                .setParameter("neto", neto).setParameter("cobros", cobros).setParameter("dia", DIA)
                .setParameter("creado", CREADA_EL).executeUpdate();
    }
}
