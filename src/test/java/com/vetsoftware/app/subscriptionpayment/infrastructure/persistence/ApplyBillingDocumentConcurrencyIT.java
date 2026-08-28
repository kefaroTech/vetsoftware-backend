package com.vetsoftware.app.subscriptionpayment.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.subscriptionpayment.application.command.ApplyBillingDocumentCommand;
import com.vetsoftware.app.subscriptionpayment.application.usecase.ApplyBillingDocumentService;
import com.vetsoftware.app.testsupport.AbstractFullApplicationIT;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import javax.sql.DataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

/**
 * <b>El mismo defecto que costo el techo de las devoluciones, ahora en el techo
 * de R3.</b> Esta clase es la que lo fija: sin ella, quitar el aislamiento de
 * {@link ApplyBillingDocumentService#execute} no pone nada rojo.
 *
 * <h2>El defecto, medido</h2>
 *
 * <p>
 * {@code ApplyBillingDocumentService.execute} hace, por este orden: (1) bloquea
 * los documentos por id ascendente, (2) <b>resuelve la factura destino</b> con
 * una lectura normal, (3) resuelve y bloquea el origen y (4) suma lo ya
 * aplicado desde ese origen para comprobar R3 -que la suma de lo aplicado desde
 * un origen nunca supere ese origen-.
 *
 * <p>
 * MySQL corre por defecto en {@code REPEATABLE READ}, e InnoDB fija la foto de
 * lectura de la transaccion en su <b>primera lectura consistente</b>. Aqui esa
 * primera lectura es el paso (2), que va <em>antes</em> del candado del origen.
 * Con la foto congelada ahi, el paso (4) devuelve lo aplicado <b>antes</b> de
 * que la transaccion rival confirmara. El candado se toma y se respeta;
 * simplemente no refresca nada -y menos aun sobre
 * {@code billing_document_applications}, que es otra tabla-.
 *
 * <h2>Por que el saldo a favor es el caso peor</h2>
 *
 * <p>
 * El mismo lote se puede aplicar a <em>dos facturas distintas</em>, y esas dos
 * transacciones bloquean documentos distintos: el unico candado que las
 * serializa es el del lote, y es justo el que se toma tarde. Dos aplicaciones
 * concurrentes del lote 8916 -500.000 concedidos- a las facturas 8914 y 8915
 * leen las dos la misma suma vieja, las dos pasan R3, y entre las dos gastan
 * 1.000.000 de un lote de 500.000. Sin excepcion, sin log y sin 409: la cartera
 * cuadra y el saldo a favor queda sobregirado.
 *
 * <p>
 * {@code PAYMENT} tenia el mismo agujero por el mismo motivo -su candado
 * tambien se toma en {@code resolveAndLockSource}, despues de la primera
 * lectura- y por eso se cubre igual aqui. Solo {@code CREDIT_NOTE} se salvaba,
 * porque su candado se toma en {@code lockDocumentsInAscendingIdOrder}, antes
 * de cualquier lectura consistente.
 *
 * <h2>El arreglo, y por que esta clase lo sujeta</h2>
 *
 * <p>
 * La correccion fue anotar el caso de uso con
 * {@code @Transactional(isolation = READ_COMMITTED)}. {@link ElArreglo} lo
 * demuestra sobre el mismo entrelazado y ademas <b>fija la anotacion en
 * produccion</b>: quitarla -o devolverla al {@code @Transactional} pelado- pone
 * rojo {@link ElArreglo#el_caso_de_uso_declara_read_committed()}.
 *
 * <h2>Por que no hay dos hilos aqui</h2>
 *
 * <p>
 * Igual que en {@code PaymentRefundConcurrencyIT}: una carrera con hilos y
 * barrera <b>habria pasado por casualidad</b>, porque solo falla si los dos se
 * solapan justo en la ventana entre el paso (2) de uno y el commit del otro. Lo
 * que hay aqui es el entrelazado peligroso <b>ejecutado a mano, en dos
 * conexiones y en orden fijo</b>: sin hilos, sin barreras, sin esperas y sin
 * azar. Falla siempre que el codigo este mal y pasa siempre que este bien, que
 * es lo que se le pide a una prueba.
 *
 * <p>
 * Datos propios del rango 8910-8919, confirmados y borrados al terminar, como
 * exige el javadoc de {@link AbstractFullApplicationIT}. No chocan con los del
 * rango 8900 de {@code PaymentRefundConcurrencyIT}, que comparte base.
 */
@DisplayName("El techo de R3 aguanta dos aplicaciones concurrentes del mismo origen")
class ApplyBillingDocumentConcurrencyIT extends AbstractFullApplicationIT {

    private static final Long PAIS = 8910L;
    private static final Long DEPARTAMENTO = 8910L;
    private static final Long CIUDAD = 8910L;
    private static final Long EMPRESA = 8910L;
    private static final Long FIRMANTE = 8911L;
    private static final Long LISTA = 8912L;
    private static final Long CONTRATO = 8913L;

    /** Las dos facturas distintas a las que se aplica el MISMO origen. */
    private static final Long FACTURA_A = 8914L;
    private static final Long FACTURA_B = 8915L;

    private static final Long LOTE = 8916L;
    private static final Long PAGO = 8917L;

    /** Empresa que no existe: sirve para que el cero de la suma no sea vacuo. */
    private static final Long EMPRESA_VECINA = 8919L;

    /** Lo concedido por el lote y lo pagado: el techo de R3 en cada origen. */
    private static final BigDecimal CONCEDIDO = new BigDecimal("500000.00");
    private static final BigDecimal PAGADO = new BigDecimal("500000.00");

    private static final LocalDateTime CUANDO = LocalDateTime.of(2026, 3, 20, 12, 0, 0);

    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void sembrarLaCadenaPropia() {
        jdbcTemplate.update("INSERT INTO countries (id, name, created_date, enabled)"
                + " VALUES (?, 'Pais concurrencia imputacion', NOW(), true)", PAIS);
        jdbcTemplate.update(
                "INSERT INTO states (id, name, country_id, created_date, enabled)"
                        + " VALUES (?, 'Depto concurrencia imputacion', ?, NOW(), true)",
                DEPARTAMENTO, PAIS);
        jdbcTemplate.update(
                "INSERT INTO cities (id, name, state_id, created_date, enabled)"
                        + " VALUES (?, 'Ciudad concurrencia imputacion', ?, NOW(), true)",
                CIUDAD, DEPARTAMENTO);
        jdbcTemplate.update(
                "INSERT INTO companies (id, name, identifier, city_id)"
                        + " VALUES (?, 'Clinica concurrencia imputacion', 'NIT-CA-8910', ?)",
                EMPRESA, CIUDAD);
        jdbcTemplate.update(
                "INSERT INTO system_users (id, code, hash_password, created_date, enabled, version)"
                        + " VALUES (?, 'CONC-APPLY-8910', 'x', NOW(), true, 0)",
                FIRMANTE);
        // El contrato exige una lista PUBLICADA, y publicarla exige firmante
        // (chk_price_lists_published). Es andamio de FK, no parte del escenario.
        jdbcTemplate.update("INSERT INTO price_lists (id, code, name, currency, valid_from, status,"
                + " published_at, published_by_system_user_id, created_date, enabled, version)"
                + " VALUES (?, 'LISTA-CONC-8910', 'Lista concurrencia imputacion', 'COP',"
                + " '2026-01-01', 'PUBLISHED', '2026-01-01 00:00:00', ?, NOW(), true, 0)", LISTA,
                FIRMANTE);
        // active_marker es GENERATED ALWAYS: nombrarla en el INSERT da ERROR 3105.
        jdbcTemplate.update("INSERT INTO subscriptions (id, subscription_number, company_id,"
                + " quote_id, price_list_id, billing_cycle, status, start_date, trial_end_date,"
                + " current_period_start, current_period_end, next_billing_date,"
                + " commitment_end_date, grace_days, past_due_since, auto_renew, created_date,"
                + " enabled, version)"
                + " VALUES (?, 'SUS-CONC-8910', ?, NULL, ?, 'MONTHLY', 'ACTIVE', '2026-01-01',"
                + " NULL, '2026-01-01', '2026-01-31', '2026-02-01', NULL, 5, NULL, true, NOW(),"
                + " true, 0)", CONTRATO, EMPRESA, LISTA);
        factura(FACTURA_A, "CONC-8910-A");
        factura(FACTURA_B, "CONC-8910-B");
        // El lote: un GRANT de origen MANUAL, que es el unico que deja los tres ids de
        // origen nulos (chk_cce_origin_branch) y ademas no genera origin_marker, asi
        // que no compite por uq_cce_origin.
        jdbcTemplate.update("INSERT INTO customer_credit_entries (id, company_id, entry_kind,"
                + " amount, lot_entry_id, origin_kind, origin_payment_id, origin_document_id,"
                + " origin_subscription_id, occurred_at, value_date, expires_on,"
                + " client_request_id, created_date)"
                + " VALUES (?, ?, 'GRANT', ?, NULL, 'MANUAL', NULL, NULL, NULL, ?, ?, NULL,"
                + " 'lote-conc-8916', ?)", LOTE, EMPRESA, CONCEDIDO, CUANDO, CUANDO.toLocalDate(),
                CUANDO);
        jdbcTemplate.update("INSERT INTO subscription_payments (id, company_id, amount, currency,"
                + " payment_method, received_at, status, refunded_amount, created_date, version)"
                + " VALUES (?, ?, ?, 'COP', 'CARD', ?, 'CONFIRMED', 0.00, ?, 0)", PAGO, EMPRESA,
                PAGADO, CUANDO, CUANDO);
    }

    private void factura(Long id, String numero) {
        jdbcTemplate.update("INSERT INTO subscription_billing_documents (id, document_number,"
                + " company_id, subscription_id, document_kind, billing_reason, period_start,"
                + " period_end, issue_status, subtotal_amount, tax_amount, total_amount,"
                + " settled_amount, created_date, version)"
                + " VALUES (?, ?, ?, ?, 'INVOICE', 'ONE_TIME', '2026-03-01', '2026-03-31',"
                + " 'DRAFT', ?, 0.00, ?, 0.00, ?, 0)", id, numero, EMPRESA, CONTRATO, CONCEDIDO,
                CONCEDIDO, CUANDO);
    }

    @AfterEach
    void borrarLaCadenaPropia() {
        jdbcTemplate.update("DELETE FROM billing_document_applications WHERE company_id = ?",
                EMPRESA);
        jdbcTemplate.update("DELETE FROM customer_credit_entries WHERE company_id = ?", EMPRESA);
        jdbcTemplate.update("DELETE FROM subscription_payments WHERE company_id = ?", EMPRESA);
        jdbcTemplate.update("DELETE FROM subscription_billing_documents WHERE company_id = ?",
                EMPRESA);
        jdbcTemplate.update("DELETE FROM subscriptions WHERE company_id = ?", EMPRESA);
        jdbcTemplate.update("DELETE FROM price_lists WHERE id = ?", LISTA);
        jdbcTemplate.update("DELETE FROM companies WHERE id = ?", EMPRESA);
        jdbcTemplate.update("DELETE FROM system_users WHERE id = ?", FIRMANTE);
        jdbcTemplate.update("DELETE FROM cities WHERE id = ?", CIUDAD);
        jdbcTemplate.update("DELETE FROM states WHERE id = ?", DEPARTAMENTO);
        jdbcTemplate.update("DELETE FROM countries WHERE id = ?", PAIS);
    }

    @Nested
    @DisplayName("El defecto que el candado no cubria")
    class ElDefecto {

        @Test
        @DisplayName("saldo a favor: bajo REPEATABLE READ el techo del lote se calcula con la"
                + " foto vieja y deja gastarlo dos veces")
        void saldo_a_favor_bajo_repeatable_read_deja_gastar_el_lote_dos_veces() throws Exception {
            BigDecimal yaAplicado = loQueVeElServicioTrasElCandadoDelLote(
                    Connection.TRANSACTION_REPEATABLE_READ);

            // Cero, con el lote ENTERO ya aplicado y CONFIRMADO por otra transaccion.
            // Este es el numero con el que requireWithinSource decidiria R3.
            //
            // El caso afirma el comportamiento del MOTOR, no el del servicio: existe
            // para que quede escrito por que la anotacion de aislamiento del caso de
            // uso no es decorativa. El dia que MySQL cambie esto, se pone rojo y hay
            // que releer la decision.
            assertThat(yaAplicado).as("lo ya aplicado desde el lote bajo REPEATABLE READ")
                    .isEqualByComparingTo("0.00");
            // Y esto es lo que cuesta: el servicio creeria tener los 500.000 enteros
            // disponibles sobre un lote que ya esta gastado del todo, asi que dejaria
            // pasar una segunda aplicacion completa a la OTRA factura. Un lote de
            // 500.000 pagando 1.000.000 de cartera.
            assertThat(CONCEDIDO.subtract(yaAplicado)).as("el techo que el servicio creeria tener")
                    .isEqualByComparingTo(CONCEDIDO);
        }

        @Test
        @DisplayName("pago: bajo REPEATABLE READ el techo del pago tiene el mismo agujero")
        void pago_bajo_repeatable_read_tiene_el_mismo_agujero() throws Exception {
            // El candado del pago se toma en resolveAndLockSource, igual que el del
            // lote: DESPUES de resolver la factura destino. Mismo orden, mismo defecto.
            assertThat(
                    loQueVeElServicioTrasElCandadoDelPago(Connection.TRANSACTION_REPEATABLE_READ))
                    .as("lo ya aplicado desde el pago bajo REPEATABLE READ")
                    .isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("El arreglo")
    class ElArreglo {

        @Test
        @DisplayName("saldo a favor: bajo READ COMMITTED la suma ve la aplicacion confirmada y el"
                + " techo queda en cero")
        void saldo_a_favor_bajo_read_committed_ve_la_aplicacion_confirmada() throws Exception {
            BigDecimal yaAplicado = loQueVeElServicioTrasElCandadoDelLote(
                    Connection.TRANSACTION_READ_COMMITTED);

            // Mismo entrelazado, mismo orden, mismas tablas: lo unico que cambia es el
            // nivel de aislamiento, y con el la suma pasa de 0 a los 500.000 reales.
            // Eso es lo que hace que R3 se cumpla de verdad.
            assertThat(yaAplicado).as("lo ya aplicado desde el lote bajo READ COMMITTED")
                    .isEqualByComparingTo(CONCEDIDO);
            assertThat(CONCEDIDO.subtract(yaAplicado)).as("el techo que queda del lote")
                    .isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("pago: bajo READ COMMITTED la suma ve la aplicacion confirmada")
        void pago_bajo_read_committed_ve_la_aplicacion_confirmada() throws Exception {
            assertThat(loQueVeElServicioTrasElCandadoDelPago(Connection.TRANSACTION_READ_COMMITTED))
                    .as("lo ya aplicado desde el pago bajo READ COMMITTED")
                    .isEqualByComparingTo(PAGADO);
        }

        @Test
        @DisplayName("el caso de uso declara READ_COMMITTED, que es lo que sujeta el techo de R3")
        void el_caso_de_uso_declara_read_committed() throws Exception {
            Transactional anotacion = ApplyBillingDocumentService.class
                    .getMethod("execute", ApplyBillingDocumentCommand.class)
                    .getAnnotation(Transactional.class);

            // Sin esto, los cuatro casos de arriba seguirian pasando -describen al
            // motor, no al servicio- y produccion podria volver al @Transactional pelado
            // sin que nada se pusiera rojo. Esta es la unica asercion que ata el
            // hallazgo al codigo que lo sufre.
            assertThat(anotacion).as("@Transactional de ApplyBillingDocumentService.execute")
                    .isNotNull();
            assertThat(anotacion.isolation())
                    .as("el aislamiento del que depende el techo de R3 en los origenes cuyo"
                            + " candado se toma despues de resolver la factura destino")
                    .isEqualTo(Isolation.READ_COMMITTED);
        }
    }

    @Nested
    @DisplayName("Tenancy del techo")
    class TenancyDelTecho {

        @Test
        @DisplayName("las dos mitades del techo llevan la empresa")
        void las_dos_mitades_del_techo_llevan_la_empresa() throws Exception {
            aplicacionDelLoteConfirmadaPorOtraTransaccion();

            try (Connection a = dataSource.getConnection()) {
                assertThat(sumaAplicadaDesdeElLote(a, EMPRESA)).isEqualByComparingTo(CONCEDIDO);
                // El cero no es vacuo: hay 500.000 aplicados desde ESE lote, y lo unico
                // que los esconde es el filtro por empresa. Sin el, la aplicacion de
                // otra clinica entraria en el techo de esta.
                assertThat(sumaAplicadaDesdeElLote(a, EMPRESA_VECINA))
                        .isEqualByComparingTo(BigDecimal.ZERO);
            }
        }
    }

    // --- andamio ------------------------------------------------------------

    /**
     * Reproduce a mano, en orden fijo y sin hilos, el entrelazado peligroso que
     * {@code execute} tiene que sobrevivir con un origen {@code CUSTOMER_CREDIT}, y
     * devuelve la suma que su comprobacion de R3 leeria.
     *
     * <p>
     * Va en un metodo aparte para que el cuerpo de cada caso sea asercion pura: la
     * conexion y su cierre son andamio, no parte de lo que se afirma.
     */
    private BigDecimal loQueVeElServicioTrasElCandadoDelLote(int nivelDeAislamiento)
            throws Exception {
        try (Connection a = dataSource.getConnection()) {
            a.setAutoCommit(false);
            a.setTransactionIsolation(nivelDeAislamiento);
            // PASO 1: lockDocumentsInAscendingIdOrder sobre la factura destino de ESTA
            // transaccion. La rival apunta a la OTRA factura, asi que este candado no
            // las serializa -y ese es justo el punto del caso peor-.
            bloquearDocumento(a, FACTURA_B);
            // PASO 2: resolveDocument sobre la factura destino. Es la primera lectura
            // consistente y, bajo REPEATABLE READ, la que le fija la foto.
            leerDocumento(a, FACTURA_B);
            // La transaccion rival aplica el lote ENTERO a la otra factura y confirma.
            // En produccion seria la que llego antes al candado del lote.
            aplicacionDelLoteConfirmadaPorOtraTransaccion();
            // PASO 3: resolveAndLockSource. Nadie retiene ya el candado del lote, asi
            // que no espera - y por eso se ve que el problema no es el candado.
            bloquearElLote(a);
            // PASO 4: la suma con la que se decide R3.
            BigDecimal yaAplicado = sumaAplicadaDesdeElLote(a, EMPRESA);
            a.rollback();
            return yaAplicado;
        }
    }

    /** El mismo entrelazado con un origen {@code PAYMENT}. */
    private BigDecimal loQueVeElServicioTrasElCandadoDelPago(int nivelDeAislamiento)
            throws Exception {
        try (Connection a = dataSource.getConnection()) {
            a.setAutoCommit(false);
            a.setTransactionIsolation(nivelDeAislamiento);
            bloquearDocumento(a, FACTURA_B);
            leerDocumento(a, FACTURA_B);
            aplicacionDelPagoConfirmadaPorOtraTransaccion();
            bloquearElPago(a);
            BigDecimal yaAplicado = sumaAplicadaDesdeElPago(a);
            a.rollback();
            return yaAplicado;
        }
    }

    /**
     * Aplica el lote entero a la factura A y <b>confirma</b>. Esta clase no es
     * transaccional -{@link AbstractFullApplicationIT} no lo es-, asi que el
     * {@code JdbcTemplate} toma su propia conexion en autocommit: es otra
     * transaccion, distinta de la del caso, y confirma sola.
     */
    private void aplicacionDelLoteConfirmadaPorOtraTransaccion() {
        jdbcTemplate.update("INSERT INTO billing_document_applications (company_id,"
                + " target_document_id, source_kind, credit_entry_id, applied_amount, applied_at,"
                + " value_date, client_request_id, created_date)"
                + " VALUES (?, ?, 'CUSTOMER_CREDIT', ?, ?, ?, ?, 'llave-de-B-lote', ?)", EMPRESA,
                FACTURA_A, LOTE, CONCEDIDO, CUANDO, CUANDO.toLocalDate(), CUANDO);
    }

    private void aplicacionDelPagoConfirmadaPorOtraTransaccion() {
        jdbcTemplate.update("INSERT INTO billing_document_applications (company_id,"
                + " target_document_id, source_kind, payment_id, applied_amount, applied_at,"
                + " value_date, client_request_id, created_date)"
                + " VALUES (?, ?, 'PAYMENT', ?, ?, ?, ?, 'llave-de-B-pago', ?)", EMPRESA, FACTURA_A,
                PAGO, PAGADO, CUANDO, CUANDO.toLocalDate(), CUANDO);
    }

    private void bloquearDocumento(Connection conexion, Long documentoId) throws Exception {
        try (PreparedStatement candado = conexion.prepareStatement(
                "SELECT id FROM subscription_billing_documents WHERE id = ? AND company_id = ?"
                        + " FOR UPDATE")) {
            candado.setLong(1, documentoId);
            candado.setLong(2, EMPRESA);
            try (ResultSet filas = candado.executeQuery()) {
                filas.next();
            }
        }
    }

    private void leerDocumento(Connection conexion, Long documentoId) throws Exception {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT total_amount," + " settled_amount FROM subscription_billing_documents"
                        + " WHERE id = ? AND company_id = ?")) {
            consulta.setLong(1, documentoId);
            consulta.setLong(2, EMPRESA);
            try (ResultSet filas = consulta.executeQuery()) {
                filas.next();
            }
        }
    }

    private void bloquearElLote(Connection conexion) throws Exception {
        try (PreparedStatement candado = conexion
                .prepareStatement("SELECT id FROM customer_credit_entries"
                        + " WHERE id = ? AND company_id = ? AND entry_kind = 'GRANT'"
                        + " FOR UPDATE")) {
            candado.setLong(1, LOTE);
            candado.setLong(2, EMPRESA);
            try (ResultSet filas = candado.executeQuery()) {
                filas.next();
            }
        }
    }

    private void bloquearElPago(Connection conexion) throws Exception {
        try (PreparedStatement candado = conexion.prepareStatement(
                "SELECT id FROM subscription_payments WHERE id = ? AND company_id = ?"
                        + " FOR UPDATE")) {
            candado.setLong(1, PAGO);
            candado.setLong(2, EMPRESA);
            try (ResultSet filas = candado.executeQuery()) {
                filas.next();
            }
        }
    }

    /**
     * Espejo literal de {@code sumAppliedFromCreditEntry}, filtro de tipo de origen
     * incluido.
     */
    private BigDecimal sumaAplicadaDesdeElLote(Connection conexion, Long companyId)
            throws Exception {
        try (PreparedStatement suma = conexion.prepareStatement(
                "SELECT COALESCE(SUM(applied_amount), 0)" + " FROM billing_document_applications"
                        + " WHERE credit_entry_id = ? AND company_id = ?"
                        + " AND source_kind = 'CUSTOMER_CREDIT'")) {
            suma.setLong(1, LOTE);
            suma.setLong(2, companyId);
            try (ResultSet filas = suma.executeQuery()) {
                filas.next();
                return filas.getBigDecimal(1);
            }
        }
    }

    /** Espejo literal de {@code sumAppliedFromPayment}. */
    private BigDecimal sumaAplicadaDesdeElPago(Connection conexion) throws Exception {
        try (PreparedStatement suma = conexion.prepareStatement(
                "SELECT COALESCE(SUM(applied_amount), 0)" + " FROM billing_document_applications"
                        + " WHERE payment_id = ? AND company_id = ? AND source_kind = 'PAYMENT'")) {
            suma.setLong(1, PAGO);
            suma.setLong(2, EMPRESA);
            try (ResultSet filas = suma.executeQuery()) {
                filas.next();
                return filas.getBigDecimal(1);
            }
        }
    }
}
