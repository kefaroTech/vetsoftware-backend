package com.vetsoftware.app.customercredit.infrastructure.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.customercredit.application.port.out.CustomerCreditBalanceRepository;
import com.vetsoftware.app.testsupport.AbstractFullApplicationIT;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * <b>Dos aplicaciones simultaneas del mismo saldo no pueden dejarlo bajo
 * cero</b>, y eso no lo puede demostrar ninguna rodaja transaccional.
 *
 * <h2>Por que no vale un {@code @DataJpaTest}</h2>
 *
 * <p>
 * Las rodajas de persistencia corren dentro de una transaccion que revierte al
 * terminar, asi que sus filas no existen para nadie mas: una segunda conexion
 * no las ve y no hay carrera posible. Esta clase levanta la aplicacion entera
 * sobre el MySQL de {@link AbstractFullApplicationIT}, siembra su cadena de
 * claves foraneas <b>confirmada</b> —con ids del rango 8800 que ninguna otra
 * prueba mira, y borrandola al terminar, como exige el javadoc de la clase
 * base— y lanza dos hilos de verdad.
 *
 * <h2>Que demuestra, y por que no puede pasar por suerte</h2>
 *
 * <p>
 * El saldo empieza en 100.000 y los dos hilos piden gastar 100.000 cada uno.
 * <b>El resultado correcto es el mismo con cualquier entrelazado</b>: el
 * {@code UPDATE} de la barandilla lleva su condicion dentro
 * ({@code WHERE company_id = ? AND balance_amount + ? >= 0}), asi que InnoDB
 * bloquea la fila para el primero y el segundo <em>reevalua la condicion contra
 * el valor ya confirmado</em> cuando el bloqueo se suelta. Uno mueve una fila y
 * el otro mueve cero. No hace falta forzar el solapamiento —la barrera solo lo
 * hace probable— porque no existe orden que produzca dos exitos.
 *
 * <p>
 * <b>Y falla de forma distinguible si alguien quita la condicion.</b> Sin ella
 * los dos {@code UPDATE} afectarian una fila, el saldo quedaria en -100.000 y
 * saltaria {@code chk_ccb_not_negative} desde el motor: por eso la asercion
 * mira los <em>dos</em> resultados —exactamente un 1 y exactamente un 0— y no
 * solo el saldo final. Un unico exito con el otro hilo muerto por excepcion no
 * colaria.
 *
 * <h2>Sin esperas artificiales</h2>
 *
 * <p>
 * No hay {@code Thread.sleep} ni tiempos: la sincronizacion es una
 * {@link CyclicBarrier} y el resto lo serializa el motor. Con reintentos, el
 * resultado seria el mismo.
 */
@DisplayName("Dos consumos simultaneos del mismo saldo a favor nunca lo dejan bajo cero")
class CustomerCreditBalanceConcurrencyIT extends AbstractFullApplicationIT {

    /** Cadena propia, en un rango que no mira ninguna otra prueba. */
    private static final Long PAIS = 8800L;
    private static final Long DEPARTAMENTO = 8800L;
    private static final Long CIUDAD = 8800L;
    private static final Long EMPRESA = 8800L;

    private static final BigDecimal SALDO_INICIAL = new BigDecimal("100000.00");
    private static final BigDecimal GASTO = new BigDecimal("-100000.00");
    private static final LocalDateTime CUANDO = LocalDateTime.of(2026, 3, 20, 12, 0, 0);

    @Autowired
    private CustomerCreditBalanceRepository repository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void sembrarLaCadenaPropia() {
        jdbcTemplate.update("INSERT INTO countries (id, name, created_date, enabled)"
                + " VALUES (?, 'Pais concurrencia saldo', NOW(), true)", PAIS);
        jdbcTemplate.update(
                "INSERT INTO states (id, name, country_id, created_date, enabled)"
                        + " VALUES (?, 'Depto concurrencia saldo', ?, NOW(), true)",
                DEPARTAMENTO, PAIS);
        jdbcTemplate.update(
                "INSERT INTO cities (id, name, state_id, created_date, enabled)"
                        + " VALUES (?, 'Ciudad concurrencia saldo', ?, NOW(), true)",
                CIUDAD, DEPARTAMENTO);
        jdbcTemplate.update(
                "INSERT INTO companies (id, name, identifier, city_id)"
                        + " VALUES (?, 'Clinica concurrencia saldo', 'NIT-CC-8800', ?)",
                EMPRESA, CIUDAD);
        jdbcTemplate.update("INSERT INTO customer_credit_balances"
                + " (company_id, balance_amount, next_expiry_on, recalculated_at, version)"
                + " VALUES (?, ?, NULL, ?, 0)", EMPRESA, SALDO_INICIAL, CUANDO);
    }

    @AfterEach
    void borrarLaCadenaPropia() {
        jdbcTemplate.update("DELETE FROM customer_credit_balances WHERE company_id = ?", EMPRESA);
        jdbcTemplate.update("DELETE FROM companies WHERE id = ?", EMPRESA);
        jdbcTemplate.update("DELETE FROM cities WHERE id = ?", CIUDAD);
        jdbcTemplate.update("DELETE FROM states WHERE id = ?", DEPARTAMENTO);
        jdbcTemplate.update("DELETE FROM countries WHERE id = ?", PAIS);
    }

    @Test
    @DisplayName("de dos consumos que agotan el mismo saldo solo uno mueve la fila")
    void de_dos_consumos_que_agotan_el_mismo_saldo_solo_uno_mueve_la_fila() throws Exception {
        List<Integer> resultados = enParalelo(() -> repository.applyDelta(EMPRESA, GASTO, CUANDO));

        // Exactamente un exito y exactamente un rechazo. Mirar solo el saldo final
        // dejaria pasar el escenario en que un hilo murio por excepcion.
        assertThat(resultados).containsExactlyInAnyOrder(1, 0);
        assertThat(saldoConfirmado()).isEqualByComparingTo("0.00");
        // Y la version se movio UNA sola vez: es lo que impide que un save que venga
        // de una lectura anterior deshaga el consumo que si paso (#53).
        assertThat(versionConfirmada()).isEqualTo(1L);
    }

    @Test
    @DisplayName("dos abonos simultaneos si se acumulan los dos: la barandilla solo mira el minimo")
    void dos_abonos_simultaneos_si_se_acumulan_los_dos() throws Exception {
        // La otra cara, y no es adorno: una barandilla que serializara de mas —un
        // bloqueo tomado antes de saber el signo, por ejemplo— haria perder abonos.
        // Aqui los dos tienen que entrar, y el saldo tiene que ser la suma exacta.
        List<Integer> resultados = enParalelo(
                () -> repository.applyDelta(EMPRESA, new BigDecimal("25000.00"), CUANDO));

        assertThat(resultados).containsExactly(1, 1);
        assertThat(saldoConfirmado()).isEqualByComparingTo("150000.00");
        assertThat(versionConfirmada()).isEqualTo(2L);
    }

    // --- andamio ------------------------------------------------------------

    /**
     * Ejecuta la misma operacion en dos hilos que salen a la vez de una barrera.
     * Sin esperas: la barrera solo hace probable el solapamiento y el resto lo
     * serializa el motor.
     */
    private List<Integer> enParalelo(Callable<Integer> operacion) throws Exception {
        ExecutorService hilos = Executors.newFixedThreadPool(2);
        CyclicBarrier salida = new CyclicBarrier(2);
        Callable<Integer> tarea = () -> {
            salida.await();
            return operacion.call();
        };
        Future<Integer> uno = hilos.submit(tarea);
        Future<Integer> otro = hilos.submit(tarea);
        List<Integer> resultados = List.of(uno.get(), otro.get());
        hilos.shutdown();
        return resultados;
    }

    /** Leido con JdbcTemplate, fuera de todo contexto de persistencia. */
    private BigDecimal saldoConfirmado() {
        return jdbcTemplate.queryForObject(
                "SELECT balance_amount FROM customer_credit_balances WHERE company_id = ?",
                BigDecimal.class, EMPRESA);
    }

    private long versionConfirmada() {
        return jdbcTemplate.queryForObject(
                "SELECT version FROM customer_credit_balances WHERE company_id = ?", Long.class,
                EMPRESA);
    }
}
