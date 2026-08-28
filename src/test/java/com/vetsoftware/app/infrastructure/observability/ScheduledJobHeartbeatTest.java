package com.vetsoftware.app.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.mock.env.MockEnvironment;

/**
 * El heartbeat es lo único que permite alertar sobre un barrido que <b>no
 * corrió</b>, así que sus dos propiedades no obvias necesitan red: que las
 * series existan desde el arranque —si nacieran con el primer éxito, la alerta
 * no podría detectar el barrido que nunca corre, que es justo el caso— y que el
 * umbral que publica sea el <b>mayor</b> hueco del cron y no el primero.
 */
@DisplayName("Heartbeat de los barridos programados")
class ScheduledJobHeartbeatTest {

    private static final Instant ARRANQUE = Instant.parse("2026-08-27T08:00:00Z");

    private SimpleMeterRegistry registry;
    private MockEnvironment environment;

    @BeforeEach
    void setUp() {
        registry = new SimpleMeterRegistry();
        environment = new MockEnvironment();
    }

    private ScheduledJobHeartbeat heartbeatEn(Instant instante) {
        return new ScheduledJobHeartbeat(registry, environment,
                Clock.fixed(instante, ZoneOffset.UTC));
    }

    private Gauge gauge(String name, ScheduledJobCatalog job) {
        return registry.find(name).tag(ScheduledJobHeartbeat.JOB_NAME_TAG, job.jobName()).gauge();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ScheduledJobCatalog.class)
    @DisplayName("cada barrido publica sus dos medidores desde el arranque, sin haber corrido")
    void cada_barrido_publica_sus_medidores_desde_el_arranque(ScheduledJobCatalog job) {
        heartbeatEn(ARRANQUE);

        assertThat(gauge(ScheduledJobHeartbeat.LAST_SUCCESS, job))
                .as("sin la serie pre-registrada, VetSoftwareScheduledJobOverdue no puede"
                        + " detectar el barrido que NUNCA corre: la serie nacería con el primer"
                        + " éxito, que es precisamente lo que no ocurre")
                .isNotNull();
        assertThat(gauge(ScheduledJobHeartbeat.EXPECTED_INTERVAL, job)).isNotNull();
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ScheduledJobCatalog.class)
    @DisplayName("el valor inicial es el arranque del proceso y no cero")
    void el_valor_inicial_es_el_arranque_y_no_cero(ScheduledJobCatalog job) {
        heartbeatEn(ARRANQUE);

        assertThat(gauge(ScheduledJobHeartbeat.LAST_SUCCESS, job).value())
                .as("con cero, time() - 0 es la época Unix entera y las ocho alertas dispararían"
                        + " en el primer scrape de cada despliegue")
                .isEqualTo(ARRANQUE.getEpochSecond());
    }

    @ParameterizedTest(name = "{0}")
    @EnumSource(ScheduledJobCatalog.class)
    @DisplayName("la etiqueta single.writer distingue los que exigen una réplica")
    void la_etiqueta_single_writer_refleja_el_catalogo(ScheduledJobCatalog job) {
        heartbeatEn(ARRANQUE);

        assertThat(gauge(ScheduledJobHeartbeat.EXPECTED_INTERVAL, job).getId()
                .getTag(ScheduledJobHeartbeat.SINGLE_WRITER_TAG))
                .as("VetSoftwareScheduledJobMultipleReplicas filtra por esta etiqueta; si mintiera,"
                        + " avisaría por los DIAN —que sí toleran N réplicas— o callaría por los"
                        + " que duplicarían los cargos del cierre de mes")
                .isEqualTo(Boolean.toString(job.requiresSingleWriter()));
    }

    @Test
    @DisplayName("el intervalo esperado es el MAYOR hueco del cron, no el primero")
    void el_intervalo_esperado_es_el_mayor_hueco() {
        // Dos pasadas al día a las 02:15 y 14:15: los huecos alternan 12 h y 12 h. Se
        // fuerza uno asimétrico -02:15 y 03:15- donde el primer hueco es de 1 h y el
        // segundo de 23 h. Tomar el primero pondría el umbral en 1,25 h y la alerta
        // sonaría todas las noches sin que pase nada.
        environment.setProperty(ScheduledJobCatalog.DIAN_CONTINGENCY_RETRY.cronProperty(),
                "0 15 2,3 * * *");
        heartbeatEn(ARRANQUE);

        assertThat(gauge(ScheduledJobHeartbeat.EXPECTED_INTERVAL,
                ScheduledJobCatalog.DIAN_CONTINGENCY_RETRY).value())
                .isEqualTo(Duration.ofHours(23).toSeconds());
    }

    @Test
    @DisplayName("una expresión ilegible degrada a un umbral tosco en vez de tumbar el arranque")
    void una_expresion_ilegible_degrada_en_vez_de_romper_el_arranque() {
        environment.setProperty(ScheduledJobCatalog.QUOTE_EXPIRATION.cronProperty(), "no es cron");

        heartbeatEn(ARRANQUE);

        assertThat(
                gauge(ScheduledJobHeartbeat.EXPECTED_INTERVAL, ScheduledJobCatalog.QUOTE_EXPIRATION)
                        .value())
                .as("un umbral tosco que dispara tarde sigue siendo detección; un arranque caído"
                        + " por una alerta mal calibrada se lleva por delante el servicio entero")
                .isEqualTo(Duration.ofHours(26).toSeconds());
    }

    @Test
    @DisplayName("sellar el final correcto mueve la marca de tiempo al instante actual")
    void sellar_mueve_la_marca_de_tiempo() {
        ScheduledJobHeartbeat heartbeat = new ScheduledJobHeartbeat(registry, environment,
                Clock.fixed(ARRANQUE.plus(Duration.ofHours(4)), ZoneOffset.UTC));

        heartbeat.recordSuccess(ScheduledJobCatalog.SUBSCRIPTION_LIFECYCLE);

        assertThat(gauge(ScheduledJobHeartbeat.LAST_SUCCESS,
                ScheduledJobCatalog.SUBSCRIPTION_LIFECYCLE).value())
                .isEqualTo(ARRANQUE.plus(Duration.ofHours(4)).getEpochSecond());
    }
}
