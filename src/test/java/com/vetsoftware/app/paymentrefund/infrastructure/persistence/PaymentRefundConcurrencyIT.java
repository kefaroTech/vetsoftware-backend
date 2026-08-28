package com.vetsoftware.app.paymentrefund.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.paymentrefund.application.port.out.PaymentRefundRepository;
import com.vetsoftware.app.paymentrefund.application.port.out.SubscriptionPaymentQueryPort;
import com.vetsoftware.app.paymentrefund.application.usecase.RegisterPaymentRefundService;
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
 * <b>El candado pesimista, por si solo, NO hacia cumplir el tope de las
 * devoluciones.</b> Esta clase es la que lo descubrio, y ahora es la que impide
 * que vuelva.
 *
 * <h2>El defecto, medido</h2>
 *
 * <p>
 * {@code RegisterPaymentRefundService} hace, por este orden: (1) busca la
 * devolucion por llave de idempotencia, (2) toma el candado sobre la fila del
 * pago y (3) suma lo ya devuelto para comprobar el tope. Que (2) vaya antes que
 * (3) lo garantiza el {@code InOrder} de
 * {@code RegisterPaymentRefundServiceTest}, y durante un rato parecio
 * suficiente. No lo era.
 *
 * <p>
 * MySQL corre por defecto en {@code REPEATABLE READ}, e InnoDB fija la foto de
 * lectura de una transaccion en su <b>primera lectura consistente</b>. Aqui esa
 * primera lectura es el paso (1), que ocurre <em>antes</em> del candado. Con la
 * foto congelada ahi, el paso (3) devuelve lo devuelto <b>antes</b> de que la
 * devolucion rival se confirmara. El candado se toma y se respeta; simplemente
 * no refresca nada — y menos aun sobre {@code payment_refunds}, que es una
 * tabla distinta de la que se bloquea—. Resultado: dos devoluciones parciales
 * concurrentes leen la misma suma vieja, las dos pasan el tope, y entre las dos
 * sacan mas dinero del que entro. Sin excepcion, sin log y sin 409.
 *
 * <p>
 * {@link ElDefecto#bajo_repeatable_read_la_suma_se_queda_con_la_foto_vieja()}
 * lo reproduce: con 500.000 ya devueltos y confirmados, la suma vale
 * {@code 0.00}.
 *
 * <h2>El arreglo, y por que esta clase lo sujeta</h2>
 *
 * <p>
 * La correccion fue anotar el caso de uso con
 * {@code @Transactional(isolation = READ_COMMITTED)}: bajo ese nivel cada
 * lectura consistente toma foto nueva, asi que la suma posterior al candado ve
 * lo confirmado. {@link ElArreglo} lo demuestra sobre el mismo entrelazado y
 * ademas <b>fija la anotacion en produccion</b>: quitarla —o devolverla al
 * {@code @Transactional} pelado— pone rojo
 * {@link ElArreglo#el_caso_de_uso_declara_read_committed()}.
 *
 * <h2>Por que no hay dos hilos aqui</h2>
 *
 * <p>
 * Una carrera con hilos y barrera <b>habria pasado por casualidad</b>: solo
 * falla si los dos se solapan justo en la ventana entre el paso (1) de uno y el
 * commit del otro, y nada garantiza que eso ocurra en una ejecucion dada. Lo
 * que hay aqui es el entrelazado peligroso <b>ejecutado a mano, en dos
 * conexiones y en orden fijo</b>: sin hilos, sin barreras, sin esperas y sin
 * azar. Falla siempre que el codigo este mal y pasa siempre que este bien, que
 * es lo que se le pide a una prueba.
 *
 * <p>
 * Datos propios del rango 8900, confirmados y borrados al terminar, como exige
 * el javadoc de {@link AbstractFullApplicationIT}.
 */
@DisplayName("El tope de devoluciones aguanta dos devoluciones concurrentes")
class PaymentRefundConcurrencyIT extends AbstractFullApplicationIT {

    private static final Long PAIS = 8900L;
    private static final Long DEPARTAMENTO = 8900L;
    private static final Long CIUDAD = 8900L;
    private static final Long EMPRESA = 8900L;
    private static final Long FIRMANTE = 8900L;
    private static final Long PAGO = 8901L;

    /** Empresa que no existe: sirve para que el cero de la suma no sea vacuo. */
    private static final Long EMPRESA_VECINA = 8999L;

    private static final BigDecimal IMPORTE_DEL_PAGO = new BigDecimal("500000.00");
    private static final LocalDateTime CUANDO = LocalDateTime.of(2026, 3, 20, 12, 0, 0);

    @Autowired
    private PaymentRefundRepository refundRepository;
    @Autowired
    private SubscriptionPaymentQueryPort paymentQueryPort;
    @Autowired
    private JdbcTemplate jdbcTemplate;
    @Autowired
    private DataSource dataSource;

    @BeforeEach
    void sembrarLaCadenaPropia() {
        jdbcTemplate.update("INSERT INTO countries (id, name, created_date, enabled)"
                + " VALUES (?, 'Pais concurrencia devolucion', NOW(), true)", PAIS);
        jdbcTemplate.update(
                "INSERT INTO states (id, name, country_id, created_date, enabled)"
                        + " VALUES (?, 'Depto concurrencia devolucion', ?, NOW(), true)",
                DEPARTAMENTO, PAIS);
        jdbcTemplate.update(
                "INSERT INTO cities (id, name, state_id, created_date, enabled)"
                        + " VALUES (?, 'Ciudad concurrencia devolucion', ?, NOW(), true)",
                CIUDAD, DEPARTAMENTO);
        jdbcTemplate.update(
                "INSERT INTO companies (id, name, identifier, city_id)"
                        + " VALUES (?, 'Clinica concurrencia devolucion', 'NIT-CR-8900', ?)",
                EMPRESA, CIUDAD);
        jdbcTemplate.update(
                "INSERT INTO system_users (id, code, hash_password, created_date, enabled, version)"
                        + " VALUES (?, 'CONC-REFUND-8900', 'x', NOW(), true, 0)",
                FIRMANTE);
        jdbcTemplate.update("INSERT INTO subscription_payments (id, company_id, amount, currency,"
                + " payment_method, received_at, status, refunded_amount, created_date, version)"
                + " VALUES (?, ?, ?, 'COP', 'CARD', ?, 'CONFIRMED', 0.00, ?, 0)", PAGO, EMPRESA,
                IMPORTE_DEL_PAGO, CUANDO, CUANDO);
    }

    @AfterEach
    void borrarLaCadenaPropia() {
        jdbcTemplate.update("DELETE FROM payment_refunds WHERE company_id = ?", EMPRESA);
        jdbcTemplate.update("DELETE FROM subscription_payments WHERE company_id = ?", EMPRESA);
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
        @DisplayName("bajo REPEATABLE READ la suma posterior al candado se queda con la foto vieja")
        void bajo_repeatable_read_la_suma_se_queda_con_la_foto_vieja() throws Exception {
            // Cero, con 500.000 ya devueltos y CONFIRMADOS por otra transaccion. Este
            // es el numero con el que el servicio decidiria el tope: creeria que el
            // pago esta intacto y dejaria pasar una segunda devolucion completa.
            //
            // El caso afirma el comportamiento del MOTOR, no el del servicio: existe
            // para que quede escrito por que la anotacion de aislamiento del caso de
            // uso no es decorativa. El dia que MySQL cambie esto, se pone rojo y hay
            // que releer la decision.
            assertThat(loQueVeElServicioTrasElCandado(Connection.TRANSACTION_REPEATABLE_READ))
                    .as("lo ya devuelto que se ve bajo REPEATABLE READ")
                    .isEqualByComparingTo("0.00");
        }
    }

    @Nested
    @DisplayName("El arreglo")
    class ElArreglo {

        @Test
        @DisplayName("bajo READ COMMITTED la suma ve la devolucion que otra transaccion confirmo")
        void bajo_read_committed_la_suma_ve_la_devolucion_confirmada() throws Exception {
            // Mismo entrelazado, mismo orden, misma tabla: lo unico que cambia es el
            // nivel de aislamiento, y con el la suma pasa de 0 a los 500.000 reales.
            // Eso es lo que hace que el tope se cumpla de verdad.
            assertThat(loQueVeElServicioTrasElCandado(Connection.TRANSACTION_READ_COMMITTED))
                    .as("lo ya devuelto que se ve bajo READ COMMITTED")
                    .isEqualByComparingTo(IMPORTE_DEL_PAGO);
        }

        @Test
        @DisplayName("el caso de uso declara READ_COMMITTED, que es lo que sujeta el tope")
        void el_caso_de_uso_declara_read_committed() throws Exception {
            Transactional anotacion = RegisterPaymentRefundService.class.getMethod("execute",
                    com.vetsoftware.app.paymentrefund.application.command.RegisterPaymentRefundCommand.class)
                    .getAnnotation(Transactional.class);

            // Sin esto, los dos casos de arriba seguirian pasando —describen al motor,
            // no al servicio— y produccion podria volver al @Transactional pelado sin
            // que nada se pusiera rojo. Esta es la unica asercion que ata el hallazgo
            // al codigo que lo sufre.
            assertThat(anotacion).as("@Transactional de RegisterPaymentRefundService.execute")
                    .isNotNull();
            assertThat(anotacion.isolation())
                    .as("el aislamiento del que depende el tope de las devoluciones")
                    .isEqualTo(Isolation.READ_COMMITTED);
        }
    }

    @Nested
    @DisplayName("Tenancy del tope")
    class TenancyDelTope {

        @Test
        @DisplayName("las dos mitades del tope llevan la empresa")
        void las_dos_mitades_del_tope_llevan_la_empresa() {
            devolucionConfirmadaPorOtraTransaccion();

            assertThat(paymentQueryPort.findByIdAndCompanyId(PAGO, EMPRESA)).get().satisfies(
                    pago -> assertThat(pago.amount()).isEqualByComparingTo(IMPORTE_DEL_PAGO));
            assertThat(refundRepository.sumRefundedByPaymentAndCompanyId(PAGO, EMPRESA))
                    .isEqualByComparingTo(IMPORTE_DEL_PAGO);
            // El cero no es vacuo: hay 500.000 devueltos sobre ESE pago, y lo unico
            // que los esconde es el filtro por empresa.
            assertThat(refundRepository.sumRefundedByPaymentAndCompanyId(PAGO, EMPRESA_VECINA))
                    .isEqualByComparingTo(BigDecimal.ZERO);
        }
    }

    // --- andamio ------------------------------------------------------------

    /**
     * Reproduce a mano, en orden fijo y sin hilos, el entrelazado peligroso que el
     * servicio tiene que sobrevivir, y devuelve la suma que su paso (3) leeria.
     *
     * <p>
     * Va en un metodo aparte para que el cuerpo de cada caso sea una sola asercion:
     * la conexion y su cierre son andamio, no parte de lo que se afirma.
     */
    private BigDecimal loQueVeElServicioTrasElCandado(int nivelDeAislamiento) throws Exception {
        try (Connection a = dataSource.getConnection()) {
            a.setAutoCommit(false);
            a.setTransactionIsolation(nivelDeAislamiento);
            // PASO 1 del servicio: la busqueda por llave de idempotencia. Es la
            // primera lectura consistente de esta transaccion y, bajo REPEATABLE
            // READ, la que le fija la foto.
            leerDevolucionPorLlave(a, "llave-de-A");
            // La transaccion rival devuelve el pago ENTERO y confirma. En produccion
            // seria la que llego antes al candado.
            devolucionConfirmadaPorOtraTransaccion();
            // PASO 2: el candado sobre la fila del pago. Nadie lo retiene ya, asi que
            // no espera — y por eso se ve que el problema no es el candado.
            bloquearElPago(a);
            // PASO 3: la suma con la que se decide el tope.
            BigDecimal yaDevuelto = sumaDeLoDevuelto(a);
            a.rollback();
            return yaDevuelto;
        }
    }

    /**
     * Escribe una devolucion del pago entero y la <b>confirma</b>. Esta clase no es
     * transaccional —{@link AbstractFullApplicationIT} no lo es—, asi que el
     * {@code JdbcTemplate} toma su propia conexion en autocommit: es otra
     * transaccion, distinta de la del caso, y confirma sola.
     */
    private void devolucionConfirmadaPorOtraTransaccion() {
        jdbcTemplate.update(
                "INSERT INTO payment_refunds (company_id, payment_id, amount, method,"
                        + " destination_reference, refunded_at, value_date, reason_code, reason,"
                        + " authorized_by_system_user_id, client_request_id, created_date)"
                        + " VALUES (?, ?, ?, 'BANK_TRANSFER', 'CTA-RIVAL', ?, ?, 'WITHDRAWAL',"
                        + " 'Devolucion completa de la transaccion rival', ?, 'llave-de-B', ?)",
                EMPRESA, PAGO, IMPORTE_DEL_PAGO, CUANDO, CUANDO.toLocalDate(), FIRMANTE, CUANDO);
    }

    private void leerDevolucionPorLlave(Connection conexion, String llave) throws Exception {
        try (PreparedStatement consulta = conexion.prepareStatement(
                "SELECT id FROM payment_refunds WHERE company_id = ? AND client_request_id = ?")) {
            consulta.setLong(1, EMPRESA);
            consulta.setString(2, llave);
            try (ResultSet filas = consulta.executeQuery()) {
                filas.next();
            }
        }
    }

    private void bloquearElPago(Connection conexion) throws Exception {
        try (PreparedStatement candado = conexion.prepareStatement(
                "SELECT id FROM subscription_payments WHERE id = ? AND company_id = ? FOR UPDATE")) {
            candado.setLong(1, PAGO);
            candado.setLong(2, EMPRESA);
            try (ResultSet filas = candado.executeQuery()) {
                filas.next();
            }
        }
    }

    private BigDecimal sumaDeLoDevuelto(Connection conexion) throws Exception {
        try (PreparedStatement suma = conexion
                .prepareStatement("SELECT COALESCE(SUM(amount), 0) FROM payment_refunds"
                        + " WHERE payment_id = ? AND company_id = ?")) {
            suma.setLong(1, PAGO);
            suma.setLong(2, EMPRESA);
            try (ResultSet filas = suma.executeQuery()) {
                filas.next();
                return filas.getBigDecimal(1);
            }
        }
    }
}
