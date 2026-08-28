package com.vetsoftware.app.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.scheduling.support.CronExpression;

/**
 * El gate de la convención que introduce el issue #609.
 *
 * <p>
 * <b>Sin esta prueba, {@link ScheduledJobCatalog} es una recomendación.</b> El
 * catálogo declara la cadencia de cada barrido y de ella cuelga el umbral de
 * {@code VetSoftwareScheduledJobOverdue}; la anotación {@code @Scheduled} de
 * cada job declara <em>otra vez</em> la misma cadena, porque el valor de una
 * anotación tiene que ser una constante de compilación y no puede leerse del
 * enum. Dos declaraciones del mismo hecho es exactamente la forma en que este
 * repositorio ha visto podrirse todo lo demás — y aquí la podredumbre no rompe
 * nada visiblemente: el barrido corre a su hora nueva, la alerta sigue
 * calculando su umbral con la vieja, y nadie se entera hasta que un barrido
 * deja de correr y la alerta no suena.
 *
 * <p>
 * De ahí que se comprueben las dos direcciones, como en
 * {@code AuditFieldsSurviveRedactionTest}: que ninguna anotación use una cadena
 * que el catálogo no declara, y que ninguna entrada del catálogo se quede sin
 * job. Una lista escrita a mano no se queja de lo que le falta.
 */
@DisplayName("Paridad entre ScheduledJobCatalog y las anotaciones @Scheduled")
class ScheduledJobCatalogParityTest {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");

    /** {@code @Scheduled(cron = "…", zone = …)} con su cadena literal. */
    private static final Pattern SCHEDULED_CRON = Pattern
            .compile("@Scheduled\\s*\\(\\s*cron\\s*=\\s*\"([^\"]+)\"([^)]*)\\)");

    /** {@code @Scheduled} con retardo fijo, que es lo que este issue retira. */
    private static final Pattern SCHEDULED_FIXED_DELAY = Pattern
            .compile("@Scheduled\\s*\\([^)]*fixedDelay");

    /**
     * Los dos que siguen —y deben seguir— con {@code fixedDelay}: son muestreo
     * continuo, no calendario. Escritos aquí para que añadir un tercero sea una
     * decisión y no un descuido.
     */
    private static final List<String> CONTINUOUS_SAMPLERS = List
            .of("DatabaseAvailabilityProbe.java", "BusinessGaugeMetrics.java");

    private record DeclaredSchedule(String cronPlaceholder, String arguments, Path source) {
    }

    private static List<DeclaredSchedule> scanCronSchedules() throws IOException {
        List<DeclaredSchedule> found = new ArrayList<>();
        try (var files = Files.walk(SOURCE_ROOT)) {
            for (Path source : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                Matcher matcher = SCHEDULED_CRON.matcher(Files.readString(source));
                while (matcher.find()) {
                    found.add(new DeclaredSchedule(matcher.group(1), matcher.group(2), source));
                }
            }
        }
        return found;
    }

    @Nested
    @DisplayName("cada @Scheduled de calendario cita el catálogo, literalmente")
    class AnotacionesContraCatalogo {

        @Test
        @DisplayName("toda expresión cron anotada coincide con el placeholder de una entrada")
        void toda_expresion_anotada_esta_en_el_catalogo() throws IOException {
            Map<String, ScheduledJobCatalog> byPlaceholder = java.util.Arrays
                    .stream(ScheduledJobCatalog.values())
                    .collect(Collectors.toMap(ScheduledJobCatalog::cronPlaceholder, job -> job));

            assertThat(scanCronSchedules()).isNotEmpty().allSatisfy(schedule -> assertThat(
                    byPlaceholder)
                    .as("la expresión cron de %s no coincide con ninguna entrada de"
                            + " ScheduledJobCatalog. Si se cambió la cadencia aquí y no allí, el"
                            + " barrido corre a una hora y VetSoftwareScheduledJobOverdue calcula"
                            + " su umbral con otra: la alerta deja de detectar el barrido que no"
                            + " corre, y no lo notará nadie", schedule.source())
                    .containsKey(schedule.cronPlaceholder()));
        }

        @Test
        @DisplayName("toda entrada del catálogo tiene un @Scheduled que la usa")
        void toda_entrada_del_catalogo_tiene_job() throws IOException {
            List<String> anotadas = scanCronSchedules().stream()
                    .map(DeclaredSchedule::cronPlaceholder).toList();

            assertThat(anotadas)
                    .as("hay entradas de ScheduledJobCatalog sin ningún @Scheduled que las use."
                            + " El heartbeat publica igualmente sus dos gauges, así que"
                            + " VetSoftwareScheduledJobOverdue empezará a disparar por un barrido"
                            + " que no existe — una alerta que nadie puede resolver es peor que"
                            + " no tenerla")
                    .containsAll(java.util.Arrays.stream(ScheduledJobCatalog.values())
                            .map(ScheduledJobCatalog::cronPlaceholder).toList());
        }

        @Test
        @DisplayName("toda expresión cron declara la zona explícita del catálogo")
        void toda_expresion_declara_la_zona() throws IOException {
            assertThat(scanCronSchedules()).allSatisfy(schedule -> assertThat(schedule.arguments())
                    .as("el @Scheduled de %s no declara zone. ECS corre en UTC, así que un cron"
                            + " sin zona pone el barrido de las 03:10 a las 22:10 de Bogotá —"
                            + " dentro del horario de atención", schedule.source())
                    .contains("zone = ScheduledJobCatalog.ZONE"));
        }

        @Test
        @DisplayName("solo las dos sondas de muestreo continuo conservan fixedDelay")
        void solo_las_sondas_conservan_fixed_delay() throws IOException {
            List<String> conRetardoFijo = new ArrayList<>();
            try (var files = Files.walk(SOURCE_ROOT)) {
                for (Path source : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    if (SCHEDULED_FIXED_DELAY.matcher(Files.readString(source)).find()) {
                        conRetardoFijo.add(source.getFileName().toString());
                    }
                }
            }

            assertThat(conRetardoFijo)
                    .as("un barrido de calendario volvió a fixedDelay. Su hora pasa a ser la del"
                            + " último despliegue, y con la hora moviéndose sola no hay umbral"
                            + " que fijar para «este job no corrió»")
                    .containsExactlyInAnyOrderElementsOf(CONTINUOUS_SAMPLERS);
        }
    }

    @Nested
    @DisplayName("el propio catálogo es coherente")
    class CoherenciaDelCatalogo {

        @ParameterizedTest(name = "{0}")
        @EnumSource(ScheduledJobCatalog.class)
        @DisplayName("la expresión por defecto se puede interpretar y vuelve a disparar")
        void la_expresion_por_defecto_es_valida(ScheduledJobCatalog job) {
            CronExpression cron = CronExpression.parse(job.defaultCron());

            assertThat(cron.next(java.time.LocalDateTime.now(ZoneId.of(ScheduledJobCatalog.ZONE))))
                    .as("la expresión de %s no vuelve a disparar nunca; el heartbeat caería al"
                            + " umbral tosco de 26 h y la alerta detectaría tarde", job.jobName())
                    .isNotNull();
        }

        @ParameterizedTest(name = "{0}")
        @EnumSource(ScheduledJobCatalog.class)
        @DisplayName("el nombre usa lowercase.dot.notation, que es lo que exige la observación")
        void el_nombre_usa_la_notacion_de_la_convencion(ScheduledJobCatalog job) {
            assertThat(job.jobName()).matches("^[a-z][a-z0-9]*(?:\\.[a-z][a-z0-9]*)+$");
        }

        @Test
        @DisplayName("no hay dos entradas con el mismo nombre ni con la misma clave de propiedad")
        void no_hay_entradas_duplicadas() {
            assertThat(java.util.Arrays.stream(ScheduledJobCatalog.values())
                    .map(ScheduledJobCatalog::jobName).toList()).doesNotHaveDuplicates();
            assertThat(java.util.Arrays.stream(ScheduledJobCatalog.values())
                    .map(ScheduledJobCatalog::cronProperty).toList()).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("la zona es una zona real y no una cadena cualquiera")
        void la_zona_existe() {
            assertThat(ZoneId.of(ScheduledJobCatalog.ZONE).getId()).isEqualTo("America/Bogota");
        }
    }
}
