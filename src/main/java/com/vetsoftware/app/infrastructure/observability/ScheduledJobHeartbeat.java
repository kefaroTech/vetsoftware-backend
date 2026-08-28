package com.vetsoftware.app.infrastructure.observability;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.EnumMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

/**
 * Publica, por cada barrido de calendario, <b>cuándo terminó bien por última
 * vez</b> y <b>cada cuánto se espera que lo haga</b>.
 *
 * <p>
 * <b>El hueco que cierra (issue #609).</b> La única alerta que existía sobre
 * jobs, {@code VetSoftwareScheduledJobFailing}, cuenta ejecuciones con
 * {@code job_outcome} de fallo. Detecta que un job <b>falló</b>; no detecta que
 * <b>no se ejecutó</b>. Un barrido que dejó de programarse —una excepción en el
 * arranque del scheduler, un contenedor que se reinició dentro de su ventana,
 * un {@code @Scheduled} que alguien comentó— no incrementa ningún contador, no
 * escribe ningún log y no cambia ninguna serie. Es pérdida silenciosa en su
 * forma más pura: el sistema entero se ve igual de sano que el día anterior.
 *
 * <p>
 * La forma canónica de detectar eso es {@code time() - último_éxito > umbral}.
 * Ese umbral no se podía fijar mientras la hora de cada barrido fuera la del
 * último despliegue; con {@link ScheduledJobCatalog} ya es un contrato, así que
 * la alerta es posible.
 *
 * <p>
 * <b>Por qué el umbral también es una serie y no un número escrito en la
 * alerta.</b> {@code vetsoftware.scheduled.job.expected.interval} se calcula
 * <b>de la propia expresión cron</b>, tomando el mayor hueco entre las próximas
 * ocurrencias — que es lo que importa en un cron de dos pasadas diarias
 * asimétricas. Con el umbral publicado desde el proceso, cambiar la cadencia en
 * el catálogo mueve la alerta con ella; con el umbral escrito en el fichero de
 * reglas, cambiar la cadencia dejaría la alerta apuntando a una hora que ya no
 * existe y nadie se enteraría hasta el incidente. Es el mismo patrón que ya usa
 * {@code vetsoftware_security_tokens_growth_threshold}.
 *
 * <p>
 * <b>Por qué el valor inicial es el arranque del proceso y no cero.</b> Con
 * cero, {@code time() - 0} es la época Unix entera y las ocho alertas
 * dispararían en el primer scrape de cada despliegue. Con el instante de
 * arranque, la alerta guarda silencio exactamente hasta que pasa un intervalo
 * completo sin éxito — que es justo el hecho que hay que detectar, incluido el
 * caso «se desplegó y el job no llegó a programarse nunca».
 *
 * <p>
 * <b>Coste.</b> Ocho jobs por dos medidores, con dos etiquetas de conjunto
 * cerrado ({@code job.name}, {@code single.writer}) = 16 series constantes.
 * {@code job.name} es una etiqueta de baja cardinalidad ya probada y su
 * conjunto de valores lo cierra un enum. Ningún identificador de empresa entra
 * aquí ni podría entrar.
 */
@Component
public class ScheduledJobHeartbeat {

    /** Marca de tiempo del último final correcto, en segundos desde la época. */
    public static final String LAST_SUCCESS = "vetsoftware.scheduled.job.last.success.timestamp";

    /** Mayor hueco esperado entre dos ejecuciones, derivado del cron. */
    public static final String EXPECTED_INTERVAL = "vetsoftware.scheduled.job.expected.interval";

    static final String JOB_NAME_TAG = "job.name";

    /**
     * Si el barrido tolera N replicas o exige una sola. No anade ni una serie —es
     * una etiqueta mas sobre las mismas 14— y es lo que permite que
     * {@code VetSoftwareScheduledJobMultipleReplicas} avise solo por los cuatro que
     * de verdad duplicarian trabajo, en vez de por los siete.
     */
    static final String SINGLE_WRITER_TAG = "single.writer";

    /**
     * Cuántas ocurrencias futuras se recorren para hallar el mayor hueco. Doce
     * cubre con holgura el peor caso realista —un cron horario— y un cron de dos
     * pasadas diarias asimétricas queda cubierto de sobra.
     */
    private static final int OCCURRENCES_SAMPLED = 12;

    private static final Logger log = LoggerFactory.getLogger(ScheduledJobHeartbeat.class);

    private final Map<ScheduledJobCatalog, AtomicLong> lastSuccess = new EnumMap<>(
            ScheduledJobCatalog.class);
    private final Clock clock;

    public ScheduledJobHeartbeat(MeterRegistry registry, Environment environment, Clock clock) {
        this.clock = clock;
        long startup = clock.instant().getEpochSecond();
        ZoneId zone = ZoneId.of(ScheduledJobCatalog.ZONE);
        for (ScheduledJobCatalog job : ScheduledJobCatalog.values()) {
            AtomicLong holder = new AtomicLong(startup);
            lastSuccess.put(job, holder);
            Gauge.builder(LAST_SUCCESS, holder, AtomicLong::get).baseUnit("s")
                    .description("Instante del último final correcto del barrido; se inicializa al"
                            + " arranque del proceso para que la alerta de retraso guarde silencio"
                            + " durante el primer intervalo")
                    .tag(JOB_NAME_TAG, job.jobName())
                    .tag(SINGLE_WRITER_TAG, Boolean.toString(job.requiresSingleWriter()))
                    .strongReference(true).register(registry);
            long interval = expectedIntervalSeconds(job, environment, zone, this.clock);
            Gauge.builder(EXPECTED_INTERVAL, () -> interval).baseUnit("s")
                    .description("Mayor hueco esperado entre dos ejecuciones, derivado de la"
                            + " expresión cron declarada en ScheduledJobCatalog")
                    .tag(JOB_NAME_TAG, job.jobName())
                    .tag(SINGLE_WRITER_TAG, Boolean.toString(job.requiresSingleWriter()))
                    .register(registry);
        }
    }

    /**
     * Sella el final correcto. Lo llama {@link ScheduledJobTelemetry} y nadie más:
     * el heartbeat tiene que contar lo mismo que la observación, y dos emisores
     * para el mismo hecho es cómo se llega a que la métrica y la traza se
     * contradigan.
     */
    void recordSuccess(ScheduledJobCatalog job) {
        lastSuccess.get(job).set(clock.instant().getEpochSecond());
    }

    /**
     * Mayor hueco entre las próximas {@value #OCCURRENCES_SAMPLED} ocurrencias.
     *
     * <p>
     * Si la expresión no se puede parsear se degrada a 26 horas y se registra a
     * {@code ERROR}: un umbral tosco que dispara tarde sigue siendo detección, y un
     * arranque caído por una alerta mal calibrada es peor que la alerta mal
     * calibrada. El {@code ERROR} es correcto según el criterio del repositorio —
     * nadie ni nada lo recupera sin que una persona corrija la propiedad.
     */
    private static long expectedIntervalSeconds(ScheduledJobCatalog job, Environment environment,
            ZoneId zone, Clock clock) {
        String expression = environment.getProperty(job.cronProperty(), job.defaultCron());
        try {
            CronExpression cron = CronExpression.parse(expression);
            // El reloj inyectado y no LocalDateTime.now(zone): un now() que lee el reloj
            // de la maquina no se puede fijar desde un test, y aqui la consecuencia no es
            // teorica -el intervalo derivado de un cron de dos pasadas asimetricas depende
            // de en cual de las dos caiga el instante de partida-. Lo comprueba la regla
            // RELOJ_INYECTADO_EN_VEZ_DE_NOW de HexagonalArchitectureTest.
            LocalDateTime cursor = LocalDateTime.ofInstant(clock.instant(), zone);
            long widest = 0L;
            LocalDateTime previous = cron.next(cursor);
            if (previous == null) {
                throw new IllegalArgumentException("la expresión no vuelve a disparar nunca");
            }
            for (int i = 0; i < OCCURRENCES_SAMPLED; i++) {
                LocalDateTime next = cron.next(previous);
                if (next == null) {
                    break;
                }
                widest = Math.max(widest, Duration.between(previous, next).toSeconds());
                previous = next;
            }
            return widest > 0 ? widest : Duration.ofHours(26).toSeconds();
        } catch (IllegalArgumentException exception) {
            log.error(
                    "La expresión cron \"{}\" del barrido {} no se pudo interpretar; su alerta de"
                            + " retraso usará un umbral tosco de 26 h en vez del intervalo real."
                            + " Corregir la propiedad {} y redesplegar.",
                    expression, job.jobName(), job.cronProperty(), exception);
            return Duration.ofHours(26).toSeconds();
        }
    }
}
