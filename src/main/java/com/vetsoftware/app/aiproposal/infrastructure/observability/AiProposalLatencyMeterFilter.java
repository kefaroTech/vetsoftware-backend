package com.vetsoftware.app.aiproposal.infrastructure.observability;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.config.MeterFilter;
import io.micrometer.core.instrument.distribution.DistributionStatisticConfig;
import java.time.Duration;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Los bordes del histograma de latencia del asistente.
 *
 * <p>
 * &#9940; <strong>Sin esto el indicador de latencia no existe, y el que hay
 * miente.</strong> El {@code Timer} de {@code aiproposal.generate} lo crea la
 * anotacion {@code @Observed} y nace <em>sin un solo borde finito</em>: publica
 * {@code le="+Inf"} y nada mas, asi que {@code histogram_quantile} devuelve
 * {@code NaN} y cualquier alerta sobre el p95 no puede dispararse jamas. Y si
 * se heredaran por jerarquia los bordes de {@code http.server.requests} —250
 * ms…5 s— el techo finito seria 5 s, con lo que <strong>el p95 de una operacion
 * que tarda 3-8 segundos de manera normal valdria 5000 exactos
 * siempre</strong>: una serie plana que parece medida y no mide nada. Cambiar
 * un {@code NaN} por un techo constante es cambiar una alerta muerta por otra
 * alerta muerta.
 *
 * <p>
 * <strong>Por que estos seis valores.</strong> Una generacion real tarda 3-8 s
 * —el modelo, no el backend—, asi que la poblacion interesante vive entre 2 y
 * 12 segundos y ahi van cuatro bordes. Los dos extremos no son relleno:
 * <ul>
 * <li><strong>2 s</strong> separa las degradaciones que <em>no</em> llegan a
 * invocar al modelo —tope de gasto, palanca apagada, sin hints: responden en
 * milisegundos mas el suelo de latencia— de las que si invocan. Es el borde con
 * el que se ve, sin credenciales de negocio, que la plataforma esta degradando.
 * <li><strong>20 s</strong> es el techo, y es el borde que hace posible la
 * alerta. {@code histogram_quantile} devuelve el borde finito mas alto cuando
 * el cuantil cae en el cubo {@code +Inf}: con 8 s como maximo, un p95 de verdad
 * de 15 s se publicaria como 8000 y la alerta {@code > 8000} <strong>nunca
 * podria cumplirse</strong>. El umbral y el techo tienen que ser bordes
 * distintos.
 * </ul>
 *
 * <p>
 * <strong>Bordes SLO y NO {@code percentilesHistogram(true)}</strong>, mismo
 * criterio —y misma medida— que la seccion
 * {@code management.metrics.distribution} de {@code application.yml}: el
 * histograma completo genera decenas de bordes por combinacion de etiquetas y
 * el plan de Grafana Cloud rechaza la ingesta al 100 %, perdiendo TODA la
 * telemetria en silencio. Seis bordes es lo que cuesta poder responder la
 * pregunta.
 *
 * <p>
 * <strong>En nanosegundos, aunque se publiquen en milisegundos.</strong>
 * {@code DistributionStatisticConfig} recibe los bordes de un {@code Timer} en
 * la unidad interna de Micrometer, que es el nanosegundo —es exactamente lo que
 * hace {@code PropertiesMeterFilter} con los {@code slo:} del YAML—. Pasar
 * {@code 2000} a secas serian dos microsegundos y las seis mediciones caerian
 * en el primer cubo. Se declaran como {@link Duration} y se convierten aqui,
 * para que el valor escrito sea el que se lee. En el registro OTLP, cuya unidad
 * base es el milisegundo, se publican como
 * {@code le="2000"}…{@code le="20000"}.
 *
 * <p>
 * <strong>{@code @Component} y no {@code @Bean} en una configuracion</strong>:
 * Spring Boot recoge todo bean de tipo {@link MeterFilter} y lo aplica al
 * registro. Como los filtros se aplican <em>mientras</em> se construye el
 * registro, esta clase no depende de {@code MeterRegistry} ni de nada que lo
 * requiera —la misma restriccion que documenta
 * {@code BusinessMetricsConfiguration}—.
 */
@Component
public class AiProposalLatencyMeterFilter implements MeterFilter {

    /**
     * Los dos {@code Timer} que crean las anotaciones {@code @Observed} de los
     * casos de uso que invocan al modelo. El refinamiento paga la misma llamada y
     * tarda lo mismo, asi que comparte bordes: dos escalas distintas para la misma
     * poblacion harian incomparables las dos series.
     *
     * <p>
     * {@code aiproposal.suppression} queda fuera a proposito —es una escritura de
     * base de datos de milisegundos, no una invocacion— y los {@code LongTaskTimer}
     * {@code *.active} tambien, porque la comparacion es por nombre exacto.
     */
    static final List<String> MEDIDORES = List.of("aiproposal.generate", "aiproposal.refine");

    static final List<Duration> BORDES = List.of(Duration.ofSeconds(2), Duration.ofSeconds(4),
            Duration.ofSeconds(6), Duration.ofSeconds(8), Duration.ofSeconds(12),
            Duration.ofSeconds(20));

    private static final double[] BORDES_EN_NANOS = BORDES.stream().mapToDouble(Duration::toNanos)
            .toArray();

    @Override
    public DistributionStatisticConfig configure(Meter.Id id, DistributionStatisticConfig config) {
        if (!MEDIDORES.contains(id.getName()))
            return config;
        return DistributionStatisticConfig.builder().serviceLevelObjectives(BORDES_EN_NANOS.clone())
                .percentilesHistogram(false).build().merge(config);
    }
}
