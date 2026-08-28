package com.vetsoftware.app.paymentrefund.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.paymentrefund.domain.PaymentRefund;
import com.vetsoftware.app.paymentrefund.domain.RefundExceedsPaymentAmountException;
import com.vetsoftware.app.paymentrefund.domain.RefundMethod;
import com.vetsoftware.app.paymentrefund.domain.RefundReasonCode;
import com.vetsoftware.app.paymentrefund.domain.SubscriptionPaymentRef;
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
 * Rodaja de {@code JpaPaymentRefundRepository} contra MySQL real.
 *
 * <p>
 * <b>Lo que esta clase existe para vigilar es el tope de las devoluciones</b>,
 * y lo interesante es que la base <b>no lo cuida</b>: MySQL prohibe
 * subconsultas dentro de un {@code CHECK}, asi que «la suma de devoluciones no
 * supera el pago» no se puede escribir en el esquema y el changeset 320 lo
 * declara por escrito. Aqui se comprueban las dos mitades de esa afirmacion:
 * {@link TopeDeDevoluciones#el_motor_no_impide_pasarse_del_pago()} demuestra
 * que el motor deja pasar la suma excedida —si algun dia deja de dejarla, este
 * caso se pone rojo y hay que reescribir el comentario del changeset—, y
 * {@link TopeDeDevoluciones#el_dominio_si_lo_impide_con_la_suma_leida_de_la_base()}
 * demuestra que la unica barandilla real es
 * {@link PaymentRefund#register(SubscriptionPaymentRef, BigDecimal, Long, BigDecimal, RefundMethod, String, LocalDateTime, LocalDate, RefundReasonCode, String, Long, String, LocalDateTime)}
 * alimentada por la consulta de este adaptador.
 *
 * <p>
 * <b>El seed no trae tablas de dinero.</b> {@code SchemaSeed} satisface claves
 * foraneas y se detiene antes de los pagos, a proposito, para que nadie pueda
 * afirmar «la cartera de esta clinica es cero» sobre un escenario prefabricado.
 * Los dos pagos de aqui se insertan con SQL nativo y con ids del rango 8100,
 * que ninguna otra rodaja usa.
 */
@Import(PersistenceSliceConfig.class)
@DisplayName("JpaPaymentRefundRepository — devoluciones contra MySQL real")
class PaymentRefundPersistenceIT extends AbstractDataJpaTest {

    /** Pago de la empresa propia. Importe con centavos: un truncado se ve. */
    private static final Long PAGO_PROPIO = 8100L;
    private static final BigDecimal IMPORTE_DEL_PAGO = new BigDecimal("500000.00");

    /** Pago de la empresa ajena, con el mismo importe para que no lo desempate. */
    private static final Long PAGO_AJENO = 8101L;

    /**
     * Cuatro instantes deliberadamente distintos entre si. Si el mapper cruza
     * {@code refundedAt} con {@code createdDate} o {@code valueDate} con la fecha
     * de {@code refundedAt}, la asercion cae; con la misma fecha en los tres, no.
     */
    private static final LocalDateTime DEVUELTO_EL = LocalDateTime.of(2026, 3, 5, 14, 30, 15);
    private static final LocalDate FECHA_VALOR = LocalDate.of(2026, 3, 9);
    private static final LocalDateTime CREADO_EL = LocalDateTime.of(2026, 3, 7, 8, 45, 0);

    @Autowired
    private JpaPaymentRefundRepository repository;
    @PersistenceContext
    private EntityManager entityManager;

    @BeforeEach
    void seed() {
        SchemaSeed.seed(entityManager);
        pago(PAGO_PROPIO, SchemaSeed.COMPANY_ID);
        pago(PAGO_AJENO, SchemaSeed.OTRA_COMPANY_ID);
        entityManager.flush();
    }

    @Nested
    @DisplayName("Ida y vuelta")
    class IdaYVuelta {

        @Test
        @DisplayName("guarda la devolucion y la recupera con cada fecha e importe en su sitio")
        void guarda_la_devolucion_y_la_recupera_campo_a_campo() {
            PaymentRefund guardada = repository.save(devolucion(new BigDecimal("217345.61"),
                    RefundMethod.BANK_TRANSFER, "CTA-AHORROS-0099", "req-ida-y-vuelta"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.COMPANY_ID))
                    .get().satisfies(recuperada -> {
                        assertThat(recuperada.getCompanyId()).isEqualTo(SchemaSeed.COMPANY_ID);
                        assertThat(recuperada.getPaymentId()).isEqualTo(PAGO_PROPIO);
                        assertThat(recuperada.getAmount()).isEqualByComparingTo("217345.61");
                        assertThat(recuperada.getMethod()).isEqualTo(RefundMethod.BANK_TRANSFER);
                        assertThat(recuperada.getDestinationReference())
                                .isEqualTo("CTA-AHORROS-0099");
                        assertThat(recuperada.getRefundedAt()).isEqualTo(DEVUELTO_EL);
                        assertThat(recuperada.getValueDate()).isEqualTo(FECHA_VALOR);
                        assertThat(recuperada.getCreatedDate()).isEqualTo(CREADO_EL);
                        assertThat(recuperada.getReasonCode())
                                .isEqualTo(RefundReasonCode.BILLING_ERROR);
                        assertThat(recuperada.getReason()).isEqualTo("Cobro duplicado de febrero");
                        assertThat(recuperada.getAuthorizedBySystemUserId())
                                .isEqualTo(SchemaSeed.SYSTEM_USER_ID);
                    });
        }

        @Test
        @DisplayName("la llave de idempotencia recupera la devolucion ya registrada")
        void la_llave_de_idempotencia_recupera_la_ya_registrada() {
            PaymentRefund guardada = repository.save(devolucion(new BigDecimal("12500.00"),
                    RefundMethod.PSE, "PSE-REF-771", "doble-clic-del-operador"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByCompanyIdAndClientRequestId(SchemaSeed.COMPANY_ID,
                    "doble-clic-del-operador")).get()
                    .satisfies(ya -> assertThat(ya.getId()).isEqualTo(guardada.getId()));
            assertThat(repository.findByCompanyIdAndClientRequestId(SchemaSeed.COMPANY_ID,
                    "una-llave-que-nadie-uso")).isEmpty();
        }
    }

    @Nested
    @DisplayName("Tope de devoluciones")
    class TopeDeDevoluciones {

        @Test
        @DisplayName("sin devoluciones previas la suma vale cero y no nulo")
        void sin_devoluciones_previas_la_suma_vale_cero() {
            // Un null aqui viajaria hasta la suma del tope y la convertiria en un
            // NullPointerException justo en el camino que devuelve dinero.
            assertThat(
                    repository.sumRefundedByPaymentAndCompanyId(PAGO_PROPIO, SchemaSeed.COMPANY_ID))
                    .isNotNull().isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("suma exactamente las parciales de ese pago y de esa empresa")
        void suma_exactamente_las_parciales_de_ese_pago() {
            repository.save(devolucion(new BigDecimal("120000.00"), RefundMethod.CARD, "TARJ-1",
                    "parcial-1"));
            repository.save(devolucion(new BigDecimal("35500.25"), RefundMethod.CARD, "TARJ-2",
                    "parcial-2"));
            entityManager.flush();
            entityManager.clear();

            assertThat(
                    repository.sumRefundedByPaymentAndCompanyId(PAGO_PROPIO, SchemaSeed.COMPANY_ID))
                    .isEqualByComparingTo("155500.25");
        }

        @Test
        @DisplayName("el motor NO impide que dos parciales sumen mas que el pago")
        void el_motor_no_impide_pasarse_del_pago() {
            // 200000 + 350000 = 550000 sobre un pago de 500000. MySQL prohibe
            // subconsultas en un CHECK, asi que no hay forma de expresar el tope en
            // el esquema y estas dos filas entran. Este caso congela esa realidad:
            // el dia que el motor empiece a rechazarlo, el comentario del changeset
            // 320 habra dejado de ser cierto y hay que reescribirlo.
            repository.save(devolucion(new BigDecimal("200000.00"), RefundMethod.CARD, "TARJ-A",
                    "excede-1"));
            repository.save(devolucion(new BigDecimal("350000.00"), RefundMethod.CARD, "TARJ-B",
                    "excede-2"));
            entityManager.flush();
            entityManager.clear();

            assertThat(
                    repository.sumRefundedByPaymentAndCompanyId(PAGO_PROPIO, SchemaSeed.COMPANY_ID))
                    .isEqualByComparingTo("550000.00").isGreaterThan(IMPORTE_DEL_PAGO);
        }

        @Test
        @DisplayName("el dominio si lo impide, alimentado por la suma leida de la base")
        void el_dominio_si_lo_impide_con_la_suma_leida_de_la_base() {
            repository.save(devolucion(new BigDecimal("400000.00"), RefundMethod.CARD, "TARJ-C",
                    "ya-devuelto"));
            entityManager.flush();
            entityManager.clear();

            BigDecimal yaDevuelto = repository.sumRefundedByPaymentAndCompanyId(PAGO_PROPIO,
                    SchemaSeed.COMPANY_ID);
            SubscriptionPaymentRef pago = new SubscriptionPaymentRef(PAGO_PROPIO,
                    SchemaSeed.COMPANY_ID, IMPORTE_DEL_PAGO);

            // 400000 ya devueltos + 100001 pedidos = 500001 sobre un pago de 500000.
            // Un solo centavo de mas: si alguien cambiara el > por un >=, o comparara
            // solo la parte entera, este caso lo caza.
            assertThatThrownBy(() -> PaymentRefund.register(pago, yaDevuelto, null,
                    new BigDecimal("100001.00"), RefundMethod.CARD, "TARJ-D", DEVUELTO_EL,
                    FECHA_VALOR, RefundReasonCode.WITHDRAWAL, "Se pasa por un centavo",
                    SchemaSeed.SYSTEM_USER_ID, "excedida", CREADO_EL))
                    .isInstanceOf(RefundExceedsPaymentAmountException.class).hasMessageContaining(
                            "Refund exceeds payment amount for payment " + PAGO_PROPIO);

            // Y el centavo justo por debajo si pasa: el tope es exacto, no aproximado.
            assertThat(PaymentRefund.register(pago, yaDevuelto, null, new BigDecimal("100000.00"),
                    RefundMethod.CARD, "TARJ-E", DEVUELTO_EL, FECHA_VALOR,
                    RefundReasonCode.WITHDRAWAL, "Cierra el pago exacto", SchemaSeed.SYSTEM_USER_ID,
                    "exacta", CREADO_EL).getAmount()).isEqualByComparingTo("100000.00");
        }
    }

    @Nested
    @DisplayName("Tenancy")
    class Tenancy {

        @Test
        @DisplayName("la carga por id no cruza de empresa")
        void la_carga_por_id_no_cruza_de_empresa() {
            PaymentRefund guardada = repository.save(devolucion(new BigDecimal("9900.00"),
                    RefundMethod.PSE, "PSE-AJENO", "tenancy-carga"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.COMPANY_ID))
                    .isPresent();
            assertThat(
                    repository.findByIdAndCompanyId(guardada.getId(), SchemaSeed.OTRA_COMPANY_ID))
                    .isEmpty();
        }

        @Test
        @DisplayName("la suma del tope esta acotada por empresa aunque el pago ya la identifique")
        void la_suma_del_tope_esta_acotada_por_empresa() {
            repository.save(devolucion(new BigDecimal("77000.00"), RefundMethod.CARD, "TARJ-T",
                    "tenancy-suma"));
            entityManager.flush();
            entityManager.clear();

            // Hay 77000 devueltos sobre PAGO_PROPIO, asi que este cero no es vacuo:
            // sale del filtro por empresa y de ningun otro sitio. Sin el, escribir el
            // id de un pago vecino bastaria para leer su cartera.
            assertThat(
                    repository.sumRefundedByPaymentAndCompanyId(PAGO_PROPIO, SchemaSeed.COMPANY_ID))
                    .isEqualByComparingTo("77000.00");
            assertThat(repository.sumRefundedByPaymentAndCompanyId(PAGO_PROPIO,
                    SchemaSeed.OTRA_COMPANY_ID)).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    @Nested
    @DisplayName("Restricciones del motor")
    class RestriccionesDelMotor {

        @Test
        @DisplayName("la misma llave de idempotencia dos veces la para uq_payment_refunds_client_request")
        void la_misma_llave_dos_veces_la_para_la_unicidad() {
            repository.save(devolucion(new BigDecimal("1000.00"), RefundMethod.PSE, "PSE-U1",
                    "llave-repetida"));
            entityManager.flush();

            EngineConstraint.assertViolates("uq_payment_refunds_client_request", () -> {
                // Importe y referencia distintos: lo unico repetido es la llave, asi
                // que no hay otra constraint que pueda saltar antes.
                repository.save(devolucion(new BigDecimal("2000.00"), RefundMethod.PSE, "PSE-U2",
                        "llave-repetida"));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("una devolucion no puede colgar del pago de otra empresa: fk_payment_refunds_payment")
        void una_devolucion_no_cuelga_del_pago_de_otra_empresa() {
            // PAGO_AJENO existe de verdad, solo que bajo OTRA_COMPANY_ID. La empresa,
            // el usuario que firma y el documento de origen son validos, asi que la
            // unica barandilla que puede pararlo es la FK COMPUESTA (company_id,
            // payment_id). Si manana alguien la degradara a una FK simple sobre
            // payment_id, esta fila entraria y el caso se pondria rojo.
            EngineConstraint.assertViolates("fk_payment_refunds_payment", () -> {
                repository.save(new PaymentRefund(null, SchemaSeed.COMPANY_ID, PAGO_AJENO, null,
                        new BigDecimal("5000.00"), RefundMethod.CARD, "TARJ-CRUZADA", DEVUELTO_EL,
                        FECHA_VALOR, RefundReasonCode.OTHER, "Pago de otra clinica",
                        SchemaSeed.SYSTEM_USER_ID, "fk-cruzada", CREADO_EL));
                entityManager.flush();
            });
        }

        @Test
        @DisplayName("devolver al saldo a favor con cuenta destino lo para chk_payment_refunds_destination")
        void devolver_al_saldo_a_favor_con_cuenta_destino_lo_para_el_check() {
            // El dominio ya rechaza esta combinacion, asi que la unica forma de
            // comprobar que la base tambien la rechaza —el cinturon bajo el tirante—
            // es escribir la fila por SQL nativo, saltandose el agregado.
            EngineConstraint.assertViolates("chk_payment_refunds_destination",
                    () -> insertarDevolucionCruda("CUSTOMER_CREDIT", "NO-DEBERIA-TENER-CUENTA",
                            "check-destino-1"));
        }

        @Test
        @DisplayName("devolver por transferencia sin cuenta destino lo para el mismo check")
        void devolver_por_transferencia_sin_cuenta_destino_lo_para_el_mismo_check() {
            // La otra mitad del CHECK. Sin este caso, un CHECK que solo mirara la
            // rama del saldo a favor pasaria por bueno y el dinero podria salir sin
            // rastro de adonde fue.
            EngineConstraint.assertViolates("chk_payment_refunds_destination",
                    () -> insertarDevolucionCruda("BANK_TRANSFER", null, "check-destino-2"));
        }
    }

    @Nested
    @DisplayName("Listados")
    class Listados {

        @Test
        @DisplayName("ordena por fecha de devolucion descendente y desempata por id")
        void ordena_por_fecha_descendente_y_desempata_por_id() {
            PaymentRefund antigua = repository.save(devolucionEn(new BigDecimal("100.00"),
                    LocalDateTime.of(2026, 3, 1, 9, 0), "orden-antigua"));
            // Las dos siguientes comparten instante EXACTO: sin el id de desempate el
            // orden entre ellas seria el que quiera el motor y la paginacion podria
            // repetir o perder una.
            PaymentRefund empateA = repository.save(devolucionEn(new BigDecimal("200.00"),
                    LocalDateTime.of(2026, 3, 20, 9, 0), "orden-empate-a"));
            PaymentRefund empateB = repository.save(devolucionEn(new BigDecimal("300.00"),
                    LocalDateTime.of(2026, 3, 20, 9, 0), "orden-empate-b"));
            entityManager.flush();
            entityManager.clear();

            PageResult<PaymentRefund> pagina = repository.findAllByCompanyId(SchemaSeed.COMPANY_ID,
                    0, 20);

            assertThat(pagina.content()).extracting(PaymentRefund::getId)
                    .containsExactly(empateB.getId(), empateA.getId(), antigua.getId());
            assertThat(pagina.totalElements()).isEqualTo(3L);
        }

        @Test
        @DisplayName("el listado por pago solo trae las de ese pago y de esa empresa")
        void el_listado_por_pago_solo_trae_las_de_ese_pago() {
            repository.save(devolucion(new BigDecimal("400.00"), RefundMethod.PSE, "PSE-P1",
                    "listado-propio"));
            entityManager.flush();
            entityManager.clear();

            assertThat(repository
                    .findAllByCompanyIdAndPaymentId(SchemaSeed.COMPANY_ID, PAGO_PROPIO, 0, 20)
                    .content()).singleElement()
                    .satisfies(fila -> assertThat(fila.getAmount()).isEqualByComparingTo("400.00"));
            assertThat(repository
                    .findAllByCompanyIdAndPaymentId(SchemaSeed.OTRA_COMPANY_ID, PAGO_PROPIO, 0, 20)
                    .content()).isEmpty();
        }
    }

    // --- andamio ------------------------------------------------------------

    private PaymentRefund devolucion(BigDecimal importe, RefundMethod medio, String destino,
            String llave) {
        return new PaymentRefund(null, SchemaSeed.COMPANY_ID, PAGO_PROPIO, null, importe, medio,
                destino, DEVUELTO_EL, FECHA_VALOR, RefundReasonCode.BILLING_ERROR,
                "Cobro duplicado de febrero", SchemaSeed.SYSTEM_USER_ID, llave, CREADO_EL);
    }

    private PaymentRefund devolucionEn(BigDecimal importe, LocalDateTime devueltoEl, String llave) {
        return new PaymentRefund(null, SchemaSeed.COMPANY_ID, PAGO_PROPIO, null, importe,
                RefundMethod.CARD, "TARJ-" + llave, devueltoEl, FECHA_VALOR,
                RefundReasonCode.WITHDRAWAL, "Retracto", SchemaSeed.SYSTEM_USER_ID, llave,
                CREADO_EL);
    }

    /**
     * Escritura cruda que se salta el agregado. Solo para los CHECK que el dominio
     * ya replica: sin ella no habria forma de comprobar que la base tambien los
     * cuida.
     */
    private void insertarDevolucionCruda(String medio, String destino, String llave) {
        entityManager.createNativeQuery("""
                INSERT INTO payment_refunds (company_id, payment_id, amount, method,
                                             destination_reference, refunded_at, value_date,
                                             reason_code, reason, authorized_by_system_user_id,
                                             client_request_id, created_date)
                VALUES (:companyId, :paymentId, 1000.00, :medio, :destino, :devueltoEl,
                        :fechaValor, 'OTHER', 'Escritura cruda de prueba', :usuario, :llave,
                        :creadoEl)
                """).setParameter("companyId", SchemaSeed.COMPANY_ID)
                .setParameter("paymentId", PAGO_PROPIO).setParameter("medio", medio)
                .setParameter("destino", destino).setParameter("devueltoEl", DEVUELTO_EL)
                .setParameter("fechaValor", FECHA_VALOR)
                .setParameter("usuario", SchemaSeed.SYSTEM_USER_ID).setParameter("llave", llave)
                .setParameter("creadoEl", CREADO_EL).executeUpdate();
    }

    /**
     * Pago confirmado minimo. {@code gateway} y {@code gateway_reference} van los
     * dos o ninguno ({@code chk_subscription_payments_gateway_pair}); aqui ninguno.
     */
    private void pago(Long id, Long companyId) {
        entityManager.createNativeQuery("""
                INSERT INTO subscription_payments (id, company_id, amount, currency,
                                                   payment_method, received_at, status,
                                                   refunded_amount, created_date, version)
                VALUES (:id, :companyId, :importe, 'COP', 'CARD', :recibidoEl, 'CONFIRMED',
                        0.00, :recibidoEl, 0)
                """).setParameter("id", id).setParameter("companyId", companyId)
                .setParameter("importe", IMPORTE_DEL_PAGO)
                .setParameter("recibidoEl", LocalDateTime.of(2026, 2, 1, 10, 0)).executeUpdate();
    }
}
