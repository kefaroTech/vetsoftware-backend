package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.classic.spi.IThrowableProxy;
import ch.qos.logback.core.UnsynchronizedAppenderBase;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

/**
 * Concurrencia real sobre {@link RedactingAppender} (incidencia #92).
 *
 * <p>
 * El appender pasó de {@code AppenderBase} —cuyo {@code doAppend} es
 * {@code synchronized}— a {@link UnsynchronizedAppenderBase}. Como por este
 * decorador pasa <b>todo</b> lo que emite el proceso, el cambio afecta al
 * camino que atraviesan todos los hilos de request, y hay que demostrar las dos
 * mitades:
 *
 * <ol>
 * <li>que los hilos <b>de verdad</b> dejaron de serializarse —lo verifica
 * {@link #varios_hilos_atraviesan_la_redaccion_a_la_vez_sin_serializarse()},
 * que cita a los hilos dentro de {@code append(...)} en una
 * {@link CyclicBarrier}: con el monitor de {@code AppenderBase} solo entraría
 * uno y la cita expiraría;
 * <li>que quitar la exclusión mutua <b>no</b> introdujo una condición de
 * carrera —lo verifica
 * {@link #ninguna_linea_sale_sin_redactar_corrupta_ni_mezclada_entre_hilos()},
 * que carga las cuatro superficies redactadas desde varios hilos a la vez y
 * afirma que cada evento sigue siendo internamente coherente.
 * </ol>
 *
 * <p>
 * Los sumideros de esta prueba extienden {@link UnsynchronizedAppenderBase} a
 * propósito: un {@code ListAppender} —que es {@code AppenderBase}—
 * reintroduciría por debajo el mismo monitor que se acaba de quitar y taparía
 * justo lo que se quiere medir.
 *
 * @see RedactingAppender
 */
class RedactingAppenderConcurrencyTest {

    /** Hilos simultáneos. Es también el número de partes de la cita. */
    private static final int HILOS = 8;

    /** Eventos que emite cada hilo en la prueba de carga. */
    private static final int EVENTOS_POR_HILO = 150;

    /**
     * Margen de la cita. Si el appender serializa, ningún hilo coincide con otro y
     * la espera agota este plazo; si no serializa, la cita se cierra de inmediato y
     * el plazo no se llega a consumir.
     */
    private static final int ESPERA_CITA_SEGUNDOS = 10;

    /**
     * Valor señuelo que no puede sobrevivir por ninguna de las cuatro superficies.
     */
    private static final String DECOY = "SENUELO-Concurrencia-9c1d";

    private LoggerContext context;
    private RedactingAppender redacting;
    private Logger logger;

    @BeforeEach
    void wirePipeline() {
        context = new LoggerContext();
        // Un LoggerContext construido a mano no trae adaptador de MDC y
        // LoggingEvent.getMDCPropertyMap() reventaría con NPE. Mismo apaño que en
        // LogRedactionPipelineTest.
        context.setMDCAdapter(MDC.getMDCAdapter());
        context.start();

        redacting = new RedactingAppender();
        redacting.setContext(context);
        redacting.setName("REDACTED_CONCURRENT_SINK");

        logger = context.getLogger("redaction-concurrency-test");
        logger.setLevel(Level.INFO);
    }

    @AfterEach
    void tearDown() {
        redacting.stop();
        context.stop();
    }

    // ---------------------------------------------------------------------------------------------
    // 1. El appender ya no serializa
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("varios hilos atraviesan la redacción a la vez: el appender no los serializa")
    void varios_hilos_atraviesan_la_redaccion_a_la_vez_sin_serializarse() throws Exception {
        SumideroConCita sumidero = new SumideroConCita(new CyclicBarrier(HILOS));
        sumidero.setContext(context);
        sumidero.setName("CITA");
        sumidero.start();
        redacting.addAppender(sumidero);
        redacting.start();
        logger.addAppender(redacting);

        lanzarEnParalelo(hilo -> logger.info("evento del hilo {}", hilo));

        assertThat(sumidero.coincidieron.get()).as(
                "solo %d de %d hilos coincidieron dentro de append(...): el appender los está "
                        + "serializando, que es exactamente lo que #92 quitó",
                sumidero.coincidieron.get(), HILOS).isEqualTo(HILOS);
    }

    // ---------------------------------------------------------------------------------------------
    // 2. Sin exclusión mutua, ninguna carrera
    // ---------------------------------------------------------------------------------------------

    @Test
    @DisplayName("ninguna línea sale sin redactar, corrupta ni mezclada entre hilos")
    void ninguna_linea_sale_sin_redactar_corrupta_ni_mezclada_entre_hilos() throws Exception {
        SumideroConcurrente sumidero = new SumideroConcurrente();
        sumidero.setContext(context);
        sumidero.setName("CARGA");
        sumidero.start();
        redacting.addAppender(sumidero);
        redacting.start();
        logger.addAppender(redacting);

        lanzarEnParalelo(this::emitirCargaDelHilo);

        assertThat(sumidero.eventos).as("se perdieron o duplicaron eventos bajo concurrencia")
                .hasSize(HILOS * EVENTOS_POR_HILO)
                .allSatisfy(RedactingAppenderConcurrencyTest::assertCoherenteYRedactado);
    }

    /**
     * Las dos mitades de la afirmación, sobre un evento cualquiera de la carga.
     *
     * <p>
     * <b>Coherencia:</b> mensaje, MDC y cadena de causas tienen que llevar el sello
     * del <em>mismo</em> hilo. Si un buffer compartido mezclara líneas de dos
     * hilos, es aquí donde se vería.
     *
     * <p>
     * <b>Redacción:</b> las cuatro superficies siguen enmascaradas igual que en el
     * caso de un solo hilo, incluida la clave que #216 movió a {@code SCANNED}.
     */
    private static void assertCoherenteYRedactado(ILoggingEvent evento) {
        Map<String, String> mdc = evento.getMDCPropertyMap();
        String sello = mdc.get(MdcKeys.HTTP_METHOD);
        IThrowableProxy causa = evento.getThrowableProxy().getCause();

        assertThat(sello).as("el sello VERBATIM del hilo no llegó al evento").isNotNull()
                .startsWith("H");
        assertThat(evento.getFormattedMessage())
                .as("el mensaje no corresponde al hilo que lo emitió (líneas mezcladas)")
                .contains("hilo=" + sello);
        assertThat(causa.getMessage())
                .as("la causa no corresponde al hilo que la emitió (cadenas mezcladas)")
                .contains("contexto del hilo " + sello);

        assertThat(evento.getFormattedMessage()).as("el señuelo sobrevivió en el mensaje")
                .doesNotContain(DECOY).contains("token=" + LogRedactor.MASK);
        assertThat(evento.getThrowableProxy().getMessage())
                .as("el señuelo sobrevivió en el mensaje de la excepción").doesNotContain(DECOY)
                .contains("token=" + LogRedactor.MASK);
        assertThat(causa.getMessage()).as("la cédula sobrevivió en la causa")
                .doesNotContain("1098765432").contains("cedula " + LogRedactor.MASK);
        assertThat(mdc.get("probe.no.declarada"))
                .as("una clave fuera de la allowlist salió sin enmascarar")
                .isEqualTo(LogRedactor.MASK);
        assertThat(mdc.get("actor.identifier")).as("se reabrió la fuga de correo que cerró #216")
                .isEqualTo(LogRedactor.MASK + "@clinica.test");
    }

    /**
     * Carga de un hilo: MDC en los tres niveles de {@link LogFieldPolicy} y un
     * evento con mensaje, señuelo por clave y cadena de excepciones, repetido
     * {@value #EVENTOS_POR_HILO} veces.
     */
    private void emitirCargaDelHilo(String sello) {
        MDC.put(MdcKeys.HTTP_METHOD, sello);
        MDC.put("actor.identifier", "dueno." + sello + "@clinica.test");
        MDC.put("probe.no.declarada", DECOY);
        try {
            for (int i = 0; i < EVENTOS_POR_HILO; i++) {
                Throwable causa = new IllegalStateException(
                        "contexto del hilo " + sello + " cedula 1098765432");
                Throwable fallo = new IllegalArgumentException("fallo token=" + DECOY, causa);
                logger.info("hilo={} intento={} token={}", sello, i, DECOY, fallo);
            }
        } finally {
            MDC.clear();
        }
    }

    // ---------------------------------------------------------------------------------------------
    // Utilidades
    // ---------------------------------------------------------------------------------------------

    /**
     * Arranca {@value #HILOS} hilos de plataforma que salen a la vez —una barrera
     * de salida evita que el primero termine antes de que arranque el último— y
     * espera a que todos acaben. El sello de cada hilo ({@code H0}, {@code H1}, …)
     * es su identidad a lo largo de toda la prueba.
     */
    private void lanzarEnParalelo(Consumer<String> trabajo) throws InterruptedException {
        CountDownLatch salida = new CountDownLatch(1);
        List<Thread> hilos = new ArrayList<>(HILOS);
        for (int i = 0; i < HILOS; i++) {
            String sello = "H" + i;
            Thread hilo = new Thread(() -> {
                try {
                    salida.await();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    return;
                }
                trabajo.accept(sello);
            }, "redaccion-" + sello);
            hilos.add(hilo);
            hilo.start();
        }
        salida.countDown();
        for (Thread hilo : hilos) {
            hilo.join();
        }
    }

    /**
     * Sumidero que cita a todos los hilos <b>dentro</b> de {@code append(...)}. Es
     * la sonda de la primera prueba: solo se cierra la cita si el appender de
     * arriba permite que varios hilos estén dentro a la vez.
     */
    private static final class SumideroConCita extends UnsynchronizedAppenderBase<ILoggingEvent> {

        private final CyclicBarrier cita;
        private final AtomicInteger coincidieron = new AtomicInteger();

        private SumideroConCita(CyclicBarrier cita) {
            this.cita = cita;
        }

        @Override
        protected void append(ILoggingEvent event) {
            try {
                cita.await(ESPERA_CITA_SEGUNDOS, TimeUnit.SECONDS);
                coincidieron.incrementAndGet();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (BrokenBarrierException | TimeoutException ignored) {
                // El hilo llegó solo a la cita: queda reflejado en el contador.
            }
        }
    }

    /**
     * Sumidero sin lock propio, para no tapar la concurrencia que se está midiendo.
     */
    private static final class SumideroConcurrente
            extends
                UnsynchronizedAppenderBase<ILoggingEvent> {

        private final Queue<ILoggingEvent> eventos = new ConcurrentLinkedQueue<>();

        @Override
        protected void append(ILoggingEvent event) {
            eventos.add(event);
        }
    }
}
