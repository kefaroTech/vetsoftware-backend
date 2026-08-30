package com.vetsoftware.app.aiproposal.infrastructure.ai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.vetsoftware.app.aiproposal.application.port.out.SpendGuardPort.SpendReservation;
import com.vetsoftware.app.infrastructure.observability.business.BusinessMetricNames;
import io.lettuce.core.RedisException;
import io.lettuce.core.api.StatefulRedisConnection;
import io.lettuce.core.api.sync.RedisCommands;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * El tope de gasto del modelo, contado una sola vez para toda la plataforma.
 *
 * <p>
 * &#9940; <b>Esta clase decide si el cien por cien de los prospectos ve o no ve
 * la lectura de su texto libre.</b> Es fail-closed: cualquier cosa que impida
 * afirmar que queda cupo devuelve vacio y el asistente degrada al camino
 * determinista. O sea que una caida de Valkey apaga la IA para todo el mundo,
 * en silencio y con la propuesta saliendo igual. Eso no se puede sostener sin
 * pruebas que lo digan.
 *
 * <p>
 * Lo que se afirma aqui son <b>los tres contratos del puerto</b>
 * ({@code SpendGuardPort}: reservar, reconciliar, devolver) y las dos
 * decisiones que ya mordieron en este proyecto:
 *
 * <ul>
 * <li><b>El orden reservar &rarr; comprobar &rarr; devolver.</b> Se suma
 * primero y se pregunta despues; al reves, N reservas concurrentes leen el
 * contador por debajo del tope y pasan todas. Se comprueba por la secuencia de
 * deltas, no por el resultado, porque el resultado es el mismo en ambos
 * ordenes.</li>
 * <li><b>Un tope de cero NO es «sin limite».</b> Cero es la clave mal escrita,
 * la variable de entorno vacia y el kill switch, y en los tres casos el techo
 * tiene que estar mas bajo que nunca, no apagado. Es el mismo error que
 * {@code LoginRateLimitFilter.limitesDePago} documenta para su cubo
 * diario.</li>
 * </ul>
 *
 * <p>
 * El {@link Clock} se fija: la clave lleva la fecha, y el corte de medianoche
 * solo aparece de noche y en CI.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ValkeyDailySpendGuard — el techo de gasto de TODA la plataforma")
class ValkeyDailySpendGuardTest {

    private static final BigDecimal TOPE_DEV = new BigDecimal("0.33");

    /** Una invocacion tipica: ~USD 0,0166. */
    private static final BigDecimal UNA_LLAMADA = new BigDecimal("0.0166");

    private static final String CLAVE_DE_HOY = "ai:spend:2026-08-30";

    private static final double EXACTO = 0.0000001;

    @Mock
    private StatefulRedisConnection<String, byte[]> connection;

    @Mock
    private RedisCommands<String, byte[]> comandos;

    private SimpleMeterRegistry registro;

    private static Clock a(String instante) {
        return Clock.fixed(Instant.parse(instante), ZoneOffset.UTC);
    }

    @BeforeEach
    void montar() {
        registro = new SimpleMeterRegistry();
    }

    private ValkeyDailySpendGuard guardConTope(BigDecimal tope) {
        return new ValkeyDailySpendGuard(connection, a("2026-08-30T10:00:00Z"), registro, tope);
    }

    private ValkeyDailySpendGuard guard() {
        return guardConTope(TOPE_DEV);
    }

    /** Valkey responde y el contador del dia queda en {@code despues}. */
    private void valkeyDevuelve(double despues) {
        when(connection.sync()).thenReturn(comandos);
        when(comandos.incrbyfloat(anyString(), anyDouble())).thenReturn(despues);
    }

    private void valkeyDevuelveNulo() {
        when(connection.sync()).thenReturn(comandos);
        when(comandos.incrbyfloat(anyString(), anyDouble())).thenReturn(null);
    }

    private void valkeyCaido() {
        when(connection.sync()).thenThrow(new RedisException("Valkey no responde"));
    }

    /** Los deltas que se mandaron a {@code INCRBYFLOAT}, en orden. */
    private List<Double> deltas() {
        ArgumentCaptor<Double> delta = ArgumentCaptor.forClass(Double.class);
        verify(comandos, Mockito.atLeastOnce()).incrbyfloat(anyString(), delta.capture());
        return delta.getAllValues();
    }

    private double contadorDeGasto() {
        return registro.get(BusinessMetricNames.AI_PROPOSAL_SPEND).counter().count();
    }

    private static ListAppender<ILoggingEvent> escuchar() {
        Logger guardia = (Logger) org.slf4j.LoggerFactory.getLogger(ValkeyDailySpendGuard.class);
        ListAppender<ILoggingEvent> eventos = new ListAppender<>();
        eventos.start();
        guardia.addAppender(eventos);
        return eventos;
    }

    private static void dejarDeEscuchar(ListAppender<ILoggingEvent> eventos) {
        Logger guardia = (Logger) org.slf4j.LoggerFactory.getLogger(ValkeyDailySpendGuard.class);
        guardia.detachAppender(eventos);
        eventos.stop();
    }

    @Nested
    @DisplayName("La reserva atomica")
    class Reserva {

        @Test
        @DisplayName("suma el estimado con INCRBYFLOAT sobre la clave del dia y devuelve la"
                + " reserva")
        void la_reserva_suma_sobre_la_clave_del_dia() {
            valkeyDevuelve(0.0166);

            SpendReservation reserva = guard().reserve(UNA_LLAMADA).orElseThrow();

            assertThat(reserva.reservedUsd()).isEqualByComparingTo(UNA_LLAMADA);
            assertThat(reserva.id()).isNotBlank();
            verify(comandos).incrbyfloat(CLAVE_DE_HOY, 0.0166);
        }

        /**
         * &#9940; <b>Leer-sumar-escribir reintroduciria la carrera que
         * {@code INCRBYFLOAT} evita.</b> Con {@code GET} + {@code SET}, N tareas de ECS
         * leen el mismo total por debajo del tope y todas pasan; el techo global
         * volveria a ser N veces el configurado, que es exactamente lo que esta clase
         * existe para arreglar. Se afirma por lo que NO se llama, porque el valor
         * devuelto seria identico en las dos implementaciones.
         */
        @Test
        @DisplayName("no lee la clave para decidir: solo incrementa y renueva el TTL")
        void no_hay_lectura_previa_que_abra_una_carrera() {
            valkeyDevuelve(0.0166);

            guard().reserve(UNA_LLAMADA);

            verify(connection).sync();
            verify(comandos).incrbyfloat(CLAVE_DE_HOY, 0.0166);
            verify(comandos).expire(CLAVE_DE_HOY, ValkeyDailySpendGuard.TTL_SEGUNDOS);
            verifyNoMoreInteractions(comandos);
        }

        /**
         * La fecha sale del {@link Clock} inyectado. Con {@code LocalDate.now()} el
         * corte de medianoche solo se veria de noche y en CI, que es como se llega a un
         * contador que rota un dia tarde.
         */
        @Test
        @DisplayName("la clave lleva la fecha del reloj inyectado, no la del sistema")
        void la_clave_lleva_la_fecha_del_reloj() {
            when(connection.sync()).thenReturn(comandos);
            when(comandos.incrbyfloat(anyString(), anyDouble())).thenReturn(0.01);

            new ValkeyDailySpendGuard(connection, a("2026-12-31T23:59:59Z"), registro, TOPE_DEV)
                    .reserve(UNA_LLAMADA);

            verify(comandos).incrbyfloat(eq("ai:spend:2026-12-31"), anyDouble());
        }

        @Test
        @DisplayName("justo en el tope todavia reserva: el corte es estrictamente por encima")
        void justo_en_el_tope_todavia_reserva() {
            valkeyDevuelve(0.33);

            assertThat(guard().reserve(UNA_LLAMADA)).isPresent();
            assertThat(deltas()).containsExactly(0.0166);
        }
    }

    @Nested
    @DisplayName("El agotamiento del tope")
    class Agotamiento {

        /**
         * &#9940; <b>El orden es la correccion, y el resultado no lo distingue.</b>
         * Comprobar-y-luego-sumar devuelve tambien vacio en este caso; lo unico que lo
         * separa es que la clave se toca dos veces, con {@code +estimado} primero y
         * {@code -estimado} despues. Si alguien reordena a comprobar-primero, esta
         * secuencia queda en un unico delta y el test se pone rojo.
         */
        @Test
        @DisplayName("pasarse del tope no reserva, y devuelve a la clave lo que acababa de"
                + " sumar")
        void al_pasarse_devuelve_lo_reservado() {
            valkeyDevuelve(0.40);

            assertThat(guard().reserve(UNA_LLAMADA)).isEmpty();

            assertThat(deltas()).as("reservar, comprobar y devolver, en ese orden")
                    .containsExactly(0.0166, -0.0166);
        }

        @Test
        @DisplayName("la devolucion del rechazo tambien renueva el TTL de la clave")
        void la_devolucion_renueva_el_ttl() {
            valkeyDevuelve(0.40);

            guard().reserve(UNA_LLAMADA);

            verify(comandos, Mockito.times(2)).expire(CLAVE_DE_HOY,
                    ValkeyDailySpendGuard.TTL_SEGUNDOS);
        }

        /**
         * &#9940; <b>Cero es el kill switch, no «sin limite».</b> Un tope a cero es la
         * clave mal escrita, la variable de entorno vacia o el apagado deliberado, y en
         * los tres casos no hay presupuesto: reservar aunque sea una llamada es gastar
         * dinero que nadie autorizo, sobre un endpoint anonimo. El defecto gemelo esta
         * escrito en {@code LoginRateLimitFilter.limitesDePago}, donde un cupo de cero
         * en el cubo diario si significa «sin limite» y por eso hay que evitarlo a
         * mano.
         */
        @Test
        @DisplayName("con el tope a cero no reserva NUNCA: cero es el kill switch, no «sin"
                + " limite»")
        void un_tope_de_cero_no_reserva_nunca() {
            valkeyDevuelve(0.000001);

            assertThat(guardConTope(BigDecimal.ZERO).reserve(new BigDecimal("0.000001")))
                    .as("con presupuesto cero el techo tiene que estar mas bajo que nunca,"
                            + " no apagado")
                    .isEmpty();
            assertThat(deltas()).as("y lo que sumo para preguntar lo devuelve")
                    .containsExactly(0.000001, -0.000001);
        }

        @Test
        @DisplayName("un tope negativo mal configurado se lee como cero, no como infinito")
        void un_tope_negativo_se_lee_como_cero() {
            valkeyDevuelve(0.0166);

            assertThat(guardConTope(new BigDecimal("-10")).reserve(UNA_LLAMADA)).isEmpty();
        }

        @Test
        @DisplayName("un tope nulo -propiedad ausente- tampoco es infinito")
        void un_tope_nulo_tampoco_es_infinito() {
            valkeyDevuelve(0.0166);

            assertThat(guardConTope(null).reserve(UNA_LLAMADA)).isEmpty();
        }

        /**
         * Fail-closed hasta el final: un coste que no se sabe calcular no se puede
         * acotar, asi que no se reserva y <b>ni siquiera se abre la conexion</b>.
         */
        @ParameterizedTest(name = "estimacion = {0}")
        @NullSource
        @ValueSource(strings = {"0", "0.00", "-1", "-0.0001"})
        @DisplayName("una estimacion que no se puede usar no reserva y no toca Valkey")
        void una_estimacion_inutilizable_no_toca_valkey(String estimacion) {
            BigDecimal estimado = estimacion == null ? null : new BigDecimal(estimacion);

            assertThat(guard().reserve(estimado)).isEmpty();

            verifyNoInteractions(connection);
        }

        /**
         * Una respuesta ilegible es indistinguible de no tener contador, y sin contador
         * no se puede afirmar que quede cupo.
         */
        @Test
        @DisplayName("si Valkey responde nulo no reserva y devuelve lo sumado")
        void una_respuesta_nula_no_reserva() {
            valkeyDevuelveNulo();

            assertThat(guard().reserve(UNA_LLAMADA)).isEmpty();
            assertThat(deltas()).containsExactly(0.0166, -0.0166);
        }

        /**
         * Con el tope de dev bastan veinte llamadas para agotarlo; a partir de ahi
         * <em>cada</em> peticion de un endpoint publico escribiria el mismo WARN hasta
         * medianoche, y un canal que grita todos los dias se deja de mirar.
         */
        @Test
        @DisplayName("el aviso de cupo agotado sale UNA vez al dia, no una por peticion")
        void el_aviso_no_es_una_tormenta() {
            valkeyDevuelve(0.40);
            ValkeyDailySpendGuard guard = guard();
            ListAppender<ILoggingEvent> eventos = escuchar();
            try {
                guard.reserve(UNA_LLAMADA);
                guard.reserve(UNA_LLAMADA);
                guard.reserve(UNA_LLAMADA);

                assertThat(eventos.list).filteredOn(evento -> evento.getLevel() == Level.WARN)
                        .hasSize(1);
            } finally {
                dejarDeEscuchar(eventos);
            }
        }

        /**
         * ERROR y no WARN: no es una anomalia de una peticion sino un defecto
         * determinista del llamante que rechaza el 100 % de las reservas hasta que
         * alguien cambie el codigo. Nada lo reintenta y nada lo recupera.
         */
        @Test
        @DisplayName("una estimacion rota se registra como ERROR: apaga la IA entera y nadie la"
                + " recupera")
        void una_estimacion_rota_es_error() {
            ValkeyDailySpendGuard guard = guard();
            ListAppender<ILoggingEvent> eventos = escuchar();
            try {
                guard.reserve(null);
                guard.reserve(BigDecimal.ZERO);

                assertThat(eventos.list).hasSize(2)
                        .allMatch(evento -> evento.getLevel() == Level.ERROR);
            } finally {
                dejarDeEscuchar(eventos);
            }
        }
    }

    @Nested
    @DisplayName("La reconciliacion y la devolucion")
    class ReconciliacionYDevolucion {

        /**
         * Lo que viaja es la <b>diferencia</b> entre lo real y lo reservado, no el
         * total: el estimado ya esta en la clave desde la reserva, asi que sumar el
         * real entero contaria la llamada dos veces.
         */
        @Test
        @DisplayName("reconciliar carga la diferencia contra la clave, no el coste entero")
        void reconciliar_carga_la_diferencia() {
            valkeyDevuelve(0.0176);
            ValkeyDailySpendGuard guard = guard();
            SpendReservation reserva = guard.reserve(new BigDecimal("0.0176")).orElseThrow();

            guard.reconcile(reserva, new BigDecimal("0.0154"));

            List<Double> movimientos = deltas();
            assertThat(movimientos).hasSize(2);
            assertThat(movimientos.get(1)).isCloseTo(-0.0022, Offset.offset(1e-9));
        }

        @Test
        @DisplayName("si el coste real supera al estimado se carga el exceso: el gasto ocurrio")
        void el_exceso_se_carga() {
            valkeyDevuelve(0.0100);
            ValkeyDailySpendGuard guard = guard();
            SpendReservation reserva = guard.reserve(new BigDecimal("0.0100")).orElseThrow();

            guard.reconcile(reserva, new BigDecimal("0.0500"));

            assertThat(deltas().get(1)).isCloseTo(0.0400, Offset.offset(1e-9));
        }

        /**
         * Un ajuste de cero no vale una ida a Valkey: la estimacion clavada es un caso
         * normal, no una excepcion.
         */
        @Test
        @DisplayName("si el real coincide con el estimado no se toca la clave otra vez")
        void un_ajuste_de_cero_no_toca_la_clave() {
            valkeyDevuelve(0.0166);
            ValkeyDailySpendGuard guard = guard();
            SpendReservation reserva = guard.reserve(UNA_LLAMADA).orElseThrow();

            guard.reconcile(reserva, UNA_LLAMADA);

            assertThat(deltas()).containsExactly(0.0166);
        }

        @Test
        @DisplayName("devolver una reserva sin usar resta el estimado entero")
        void devolver_resta_el_estimado_entero() {
            valkeyDevuelve(0.0166);
            ValkeyDailySpendGuard guard = guard();

            guard.release(guard.reserve(UNA_LLAMADA).orElseThrow());

            assertThat(deltas()).containsExactly(0.0166, -0.0166);
        }

        @Test
        @DisplayName("devolver no cuenta gasto: no hubo llamada al modelo")
        void devolver_no_cuenta_gasto() {
            valkeyDevuelve(0.0166);
            ValkeyDailySpendGuard guard = guard();

            guard.release(guard.reserve(UNA_LLAMADA).orElseThrow());

            assertThat(contadorDeGasto()).isZero();
        }

        @Test
        @DisplayName("una reserva nula no mueve nada, no revienta y no abre conexion")
        void una_reserva_nula_no_mueve_nada() {
            ValkeyDailySpendGuard guard = guard();

            guard.reconcile(null, new BigDecimal("9"));
            guard.release(null);

            verifyNoInteractions(connection);
            assertThat(contadorDeGasto()).isZero();
        }

        @ParameterizedTest(name = "coste real = {0}")
        @NullSource
        @ValueSource(strings = {"-1", "-0.5"})
        @DisplayName("un coste real nulo o negativo cuenta como cero, no como credito")
        void un_coste_real_no_utilizable_cuenta_como_cero(String real) {
            valkeyDevuelve(0.0166);
            ValkeyDailySpendGuard guard = guard();
            SpendReservation reserva = guard.reserve(UNA_LLAMADA).orElseThrow();

            guard.reconcile(reserva, real == null ? null : new BigDecimal(real));

            assertThat(deltas()).containsExactly(0.0166, -0.0166);
            assertThat(contadorDeGasto()).isZero();
        }

        /**
         * &#9940; <b>El contador de gasto se emite aqui y en ningun otro sitio.</b>
         * {@link ValkeyDailySpendGuard#reconcile} es el punto por el que pasan
         * <em>todos</em> los cargos, incluido el del intento que fallo despues de
         * pagar, donde no hay {@code ModelUsage} que leer. Emitirlo desde el caso de
         * uso dejaria fuera precisamente ese, y la metrica diria menos que la factura
         * de AWS.
         */
        @Test
        @DisplayName("el contador cuenta el coste REAL de la llamada, no la reserva pesimista")
        void el_contador_cuenta_el_coste_real() {
            valkeyDevuelve(0.0166);
            ValkeyDailySpendGuard guard = guard();
            SpendReservation reserva = guard.reserve(UNA_LLAMADA).orElseThrow();

            guard.reconcile(reserva, new BigDecimal("0.0042"));

            assertThat(contadorDeGasto()).isEqualTo(0.0042,
                    org.assertj.core.data.Offset.offset(EXACTO));
        }
    }

    @Nested
    @DisplayName("Valkey caido")
    class ValkeyCaido {

        /**
         * &#9940; <b>Esta es la decision incomoda y es la correcta.</b> Degradar de mas
         * cuesta una propuesta determinista -que es una propuesta correcta-; degradar
         * de menos cuesta dinero real y sin techo sobre un endpoint anonimo. Es la
         * politica opuesta a la de {@code ValkeyProposalEmailThrottle}, que es
         * fail-open porque al otro lado no hay dinero.
         */
        @Test
        @DisplayName("reservar con Valkey caido no reserva: fail-closed, y apaga la IA para"
                + " todos")
        void reservar_con_valkey_caido_no_reserva() {
            valkeyCaido();

            assertThat(guard().reserve(UNA_LLAMADA)).isEmpty();
        }

        @Test
        @DisplayName("el fallo de reserva se registra en WARN, sin el importe ni el tope")
        void el_fallo_de_reserva_se_registra() {
            valkeyCaido();
            ValkeyDailySpendGuard guard = guard();
            ListAppender<ILoggingEvent> eventos = escuchar();
            try {
                guard.reserve(UNA_LLAMADA);

                assertThat(eventos.list).hasSize(1).allMatch(e -> e.getLevel() == Level.WARN);
                assertThat(eventos.list.get(0).getFormattedMessage()).contains("determinista")
                        .doesNotContain("0.33");
            } finally {
                dejarDeEscuchar(eventos);
            }
        }

        /**
         * Si {@code EXPIRE} revienta despues de que {@code INCRBYFLOAT} ya haya sumado,
         * el contador queda alto y no se sabe si hay cupo. La clave rota sola por el
         * TTL anterior o por el barrido del dia siguiente; lo que no puede pasar es
         * reservar sin poder afirmarlo.
         */
        @Test
        @DisplayName("si falla el EXPIRE despues del incremento tampoco reserva")
        void un_expire_que_revienta_tampoco_reserva() {
            when(connection.sync()).thenReturn(comandos);
            when(comandos.incrbyfloat(anyString(), anyDouble())).thenReturn(0.0166);
            when(comandos.expire(anyString(), anyLong()))
                    .thenThrow(new RedisException("Valkey no responde"));

            assertThat(guard().reserve(UNA_LLAMADA)).isEmpty();
        }

        /**
         * &#9940; <b>Reconciliar y devolver NUNCA lanzan.</b> Corren cuando la llamada
         * ya se hizo y el gasto ya ocurrio: convertir su fallo en un error del usuario
         * no devolveria el dinero y si tumbaria una respuesta buena que ya esta pagada
         * y guardada.
         */
        @Test
        @DisplayName("reconciliar con Valkey caido no lanza y sigue contando el gasto real")
        void reconciliar_con_valkey_caido_no_lanza() {
            valkeyCaido();
            ValkeyDailySpendGuard guard = guard();
            SpendReservation reserva = new SpendReservation("r-1", UNA_LLAMADA);

            assertThatCode(() -> guard.reconcile(reserva, new BigDecimal("0.0042")))
                    .doesNotThrowAnyException();

            assertThat(contadorDeGasto())
                    .as("la factura de AWS llego igual; el contador no puede callarselo")
                    .isEqualTo(0.0042, Offset.offset(EXACTO));
        }

        @Test
        @DisplayName("devolver con Valkey caido no lanza: el contador queda alto y degrada de"
                + " mas, que es el lado seguro")
        void devolver_con_valkey_caido_no_lanza() {
            valkeyCaido();
            ValkeyDailySpendGuard guard = guard();

            assertThatCode(() -> guard.release(new SpendReservation("r-1", UNA_LLAMADA)))
                    .doesNotThrowAnyException();
        }

        /**
         * El medidor no puede tumbar un scrape entero de Prometheus. Cero es «no se
         * sabe», y quien corta es {@code reserve}, que si es fail-closed.
         */
        @Test
        @DisplayName("el medidor del dia con Valkey caido lee cero y no tumba el scrape")
        void el_medidor_con_valkey_caido_lee_cero() {
            valkeyCaido();

            assertThat(guard().spentToday()).isEqualByComparingTo("0");
        }
    }

    @Nested
    @DisplayName("La caducidad de las claves")
    class Caducidad {

        /**
         * Dos dias: la clave del dia rota sola -no hace falta ningun barrido- y queda
         * margen para consultar la vispera. Menos de un dia arriesga expirar el
         * contador en curso si el reloj del servidor y el de la aplicacion no coinciden
         * del todo, y eso reabriria el techo a mitad de dia.
         */
        @Test
        @DisplayName("el TTL declarado son exactamente dos dias en segundos")
        void el_ttl_son_dos_dias() {
            assertThat(ValkeyDailySpendGuard.TTL_SEGUNDOS).isEqualTo(2L * 24 * 60 * 60)
                    .isEqualTo(172_800L);
        }

        @Test
        @DisplayName("la reserva renueva el TTL en la misma ida que el incremento")
        void la_reserva_renueva_el_ttl() {
            valkeyDevuelve(0.0166);

            guard().reserve(UNA_LLAMADA);

            verify(comandos).expire(CLAVE_DE_HOY, 172_800L);
        }

        /**
         * Sin esto, una clave creada al final del dia anterior podria caducar mientras
         * el ajuste la esta corrigiendo, y el contador perderia el gasto ya hecho.
         */
        @Test
        @DisplayName("el ajuste posterior a la llamada tambien renueva el TTL")
        void el_ajuste_tambien_renueva_el_ttl() {
            when(connection.sync()).thenReturn(comandos);
            when(comandos.incrbyfloat(anyString(), anyDouble())).thenReturn(0.0166);

            guard().release(new SpendReservation("r-1", UNA_LLAMADA));

            verify(comandos).expire(CLAVE_DE_HOY, ValkeyDailySpendGuard.TTL_SEGUNDOS);
        }

        @Test
        @DisplayName("el prefijo de la clave es el declarado, y lleva la fecha detras")
        void el_prefijo_es_el_declarado() {
            assertThat(CLAVE_DE_HOY).isEqualTo(ValkeyDailySpendGuard.PREFIJO + "2026-08-30");
        }
    }

    @Nested
    @DisplayName("Lo gastado hoy")
    class GastadoHoy {

        @Test
        @DisplayName("lee la clave del dia y la interpreta como decimal")
        void lee_la_clave_del_dia() {
            when(connection.sync()).thenReturn(comandos);
            when(comandos.get(CLAVE_DE_HOY)).thenReturn("0.1234".getBytes(StandardCharsets.UTF_8));

            assertThat(guard().spentToday()).isEqualByComparingTo("0.1234");
        }

        @Test
        @DisplayName("una clave que todavia no existe es cero gastado, no un fallo")
        void una_clave_ausente_es_cero() {
            when(connection.sync()).thenReturn(comandos);
            when(comandos.get(CLAVE_DE_HOY)).thenReturn(null);

            assertThat(guard().spentToday()).isEqualByComparingTo("0");
        }

        @Test
        @DisplayName("una clave vacia tambien es cero y no revienta el parseo")
        void una_clave_vacia_es_cero() {
            when(connection.sync()).thenReturn(comandos);
            when(comandos.get(CLAVE_DE_HOY)).thenReturn(new byte[0]);

            assertThat(guard().spentToday()).isEqualByComparingTo("0");
        }

        /**
         * El medidor responde «cuanto queda del cupo de hoy» y se reinicia solo al
         * rotar el dia, que es justo lo que el contador acumulativo no puede hacer. Son
         * dos preguntas distintas y ninguna sustituye a la otra.
         */
        @Test
        @DisplayName("el medidor del dia publica lo que dice la clave, en toda la plataforma")
        void el_medidor_publica_lo_que_dice_la_clave() {
            when(connection.sync()).thenReturn(comandos);
            when(comandos.get(CLAVE_DE_HOY)).thenReturn("0.2500".getBytes(StandardCharsets.UTF_8));
            guard();

            assertThat(registro.get(BusinessMetricNames.AI_PROPOSAL_SPEND_TODAY).gauge().value())
                    .isEqualTo(0.25, Offset.offset(EXACTO));
        }

        /**
         * Pre-registrado a cero para que una alerta {@code increase(...) > 0} funcione
         * desde el primer scrape, en vez de depender de que la serie nazca justo
         * durante el incidente.
         */
        @Test
        @DisplayName("el contador de gasto nace a cero, sin esperar a la primera llamada")
        void el_contador_nace_a_cero() {
            guard();

            assertThat(contadorDeGasto()).isZero();
        }
    }

    @Nested
    @DisplayName("El tope por defecto")
    class TopePorDefecto {

        /**
         * {@code LoginRateLimitFilter} deriva de este mismo tope cuantas peticiones de
         * pago permite por IP y lee la clave con su propio {@code @Value}: si los dos
         * defectos se separan, el filtro calibra su limite contra un presupuesto que no
         * es el que se aplica.
         */
        @Test
        @DisplayName("el defecto es el de dev, que es el lado seguro de un despliegue sin"
                + " configurar")
        void el_defecto_es_el_de_dev() {
            assertThat(new BigDecimal(ValkeyDailySpendGuard.DEFECTO_TOPE_DIARIO_USD))
                    .isEqualByComparingTo("0.33");
        }
    }

    @Nested
    @DisplayName("El orden completo de una invocacion")
    class OrdenCompleto {

        /**
         * El ciclo de vida real de una llamada que si se hizo: reservar el estimado,
         * invocar, y reconciliar con lo que costo de verdad. La clave acaba valiendo el
         * coste real y el contador acumulado tambien.
         */
        @Test
        @DisplayName("reservar e invocar deja en la clave el coste real, no el estimado")
        void reservar_e_invocar_deja_el_coste_real() {
            valkeyDevuelve(0.0176);
            ValkeyDailySpendGuard guard = guard();

            SpendReservation reserva = guard.reserve(new BigDecimal("0.0176")).orElseThrow();
            guard.reconcile(reserva, new BigDecimal("0.0154"));

            InOrder enOrden = Mockito.inOrder(comandos);
            enOrden.verify(comandos).incrbyfloat(eq(CLAVE_DE_HOY), anyDouble());
            enOrden.verify(comandos).expire(CLAVE_DE_HOY, ValkeyDailySpendGuard.TTL_SEGUNDOS);
            enOrden.verify(comandos).incrbyfloat(eq(CLAVE_DE_HOY), anyDouble());
            assertThat(contadorDeGasto()).isEqualTo(0.0154, Offset.offset(EXACTO));
        }
    }
}
