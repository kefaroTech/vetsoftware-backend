package com.vetsoftware.app.infrastructure.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.infrastructure.observability.DatabaseAvailabilityProbe;
import com.vetsoftware.app.infrastructure.observability.business.BusinessGaugeMetrics;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.Arrays;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * El pestillo de D-81: si alguien vuelve a dejar el reloj del negocio sin zona,
 * algo de aqui se pone rojo.
 *
 * <p>
 * Hace falta un test propio porque las pruebas de escenario de cada feature
 * —cotizaciones, ciclo de vida, permisos, cargos— <b>inyectan su reloj</b> con
 * {@code Clock.fixed(...)}, asi que seguirian verdes con el bean roto. Lo unico
 * que ata el bean de produccion a la decision correcta es esta clase.
 *
 * <p>
 * <b>Nota sobre el estado global.</b> La forma obvia de probar que la zona no
 * se hereda seria mover {@code TimeZone.setDefault(...)} y volver a construir
 * el bean. No se hace: el {@code pom.xml} documenta que el arbol de test se
 * auditó con <b>cero {@code Locale/TimeZone.setDefault}</b>, y eso es lo que
 * hace seguro el {@code forkCount=2}. En su lugar se combinan una comprobacion
 * de comportamiento —que basta en CI, donde el contenedor corre en UTC— y una
 * comprobacion de la fuente, que es la unica que tambien falla en el portatil
 * de un desarrollador cuya zona local ya es Bogota.
 */
@DisplayName("ClockConfig — el reloj del negocio lleva zona explicita (D-81)")
class ClockConfigTest {

    private final ClockConfig config = new ClockConfig();

    @Nested
    @DisplayName("La zona del reloj del negocio")
    class ZonaDelReloj {

        @Test
        @DisplayName("es America/Bogota, no la del contenedor")
        void elBeanDeclaraLaZonaDelNegocio() {
            assertThat(config.systemClock().getZone()).isEqualTo(ClockConfig.BUSINESS_ZONE)
                    .isEqualTo(ZoneId.of("America/Bogota"));
        }

        @Test
        @DisplayName("en CI (contenedor en UTC) el bean no puede coincidir con systemDefaultZone")
        void enUtcElBeanNoEsElRelojPorDefecto() {
            // En CI la zona por defecto es UTC, asi que volver a
            // Clock.systemDefaultZone() rompe esta comparacion. En una maquina
            // cuya zona ya es Bogota la comprobacion no discrimina: para ese caso
            // esta la de la fuente, mas abajo.
            Assumptions.assumeTrue(!ZoneId.systemDefault().equals(ClockConfig.BUSINESS_ZONE),
                    "La maquina ya corre en America/Bogota; este caso no discrimina aqui.");
            assertThat(config.systemClock()).isNotEqualTo(Clock.systemDefaultZone());
        }

        @Test
        @DisplayName("hoy vale -05:00 y no tiene ningun cambio de horario por delante")
        void colombiaNoTieneHorarioDeVeranoPorDelante() {
            // OJO con isFixedOffset(): devuelve FALSE para America/Bogota, porque la
            // zona arrastra tres transiciones historicas (Colombia tuvo horario de
            // verano en 1992-93). La premisa del diseno no es que la zona haya sido
            // siempre fija, sino que lo es de aqui en adelante: eso es lo que hace
            // que un instante guardado ya pruebe la hora local sin convertirlo.
            assertThat(ClockConfig.BUSINESS_ZONE.getRules().getOffset(Instant.now()))
                    .isEqualTo(ZoneOffset.of("-05:00"));
            assertThat(ClockConfig.BUSINESS_ZONE.getRules().nextTransition(Instant.now()))
                    .as("Si la base de datos de zonas anuncia un cambio de horario en "
                            + "Colombia, la premisa de desplazamiento fijo deja de valer y "
                            + "hay que revisar todo lo que derive una fecha del reloj.")
                    .isNull();
        }

        @Test
        @DisplayName("deriva 'hoy' en Bogota y no en UTC")
        void hoySeDerivaEnBogota() {
            Clock businessClock = config.systemClock();
            Instant instant = businessClock.instant();
            assertThat(LocalDate.now(businessClock))
                    .isEqualTo(LocalDate.ofInstant(instant, ClockConfig.BUSINESS_ZONE));
        }
    }

    @Nested
    @DisplayName("La zona esta escrita, no heredada")
    class ZonaExplicitaEnLaFuente {

        private static final Path SOURCE = Path
                .of("src/main/java/com/vetsoftware/app/infrastructure/config/ClockConfig.java");

        /**
         * La unica comprobacion de este fichero que falla en <b>cualquier</b> maquina,
         * incluida una cuya zona por defecto ya sea Bogota —donde la version rota se
         * comporta igual que la buena y ningun assert de comportamiento la distingue—.
         */
        @Test
        @DisplayName("systemClock() no vuelve a Clock.systemDefaultZone()")
        void laFuenteNoUsaLaZonaPorDefecto() throws IOException {
            Assumptions.assumeTrue(Files.exists(SOURCE),
                    "Fuera de Maven no se resuelve la ruta del modulo; en el build siempre existe.");
            // Se miran las INSTRUCCIONES, no el fichero entero: el javadoc de
            // ClockConfig explica el defecto y por tanto nombra
            // `systemDefaultZone` a proposito. Sin quitar los comentarios, esta
            // comprobacion fallaria por la propia documentacion del arreglo.
            String code = Files.readString(SOURCE, StandardCharsets.UTF_8)
                    .replaceAll("(?s)/\\*.*?\\*/", "").replaceAll("(?m)//.*$", "");
            assertThat(code)
                    .as("El reloj del negocio no puede heredar la zona del contenedor (D-81): "
                            + "la imagen no declara zona y heredarla significa decidir en UTC.")
                    .doesNotContain("systemDefaultZone");
            assertThat(code).contains("Clock.system(BUSINESS_ZONE)");
        }
    }

    @Nested
    @DisplayName("Las dos medidas que usan horario universal a proposito siguen intactas")
    class MedidasEnHorarioUniversal {

        /**
         * D-81 avisa de que el atajo —{@code TZ} en el contenedor— corregiria los
         * puntos de decision de golpe y <b>romperia estas dos</b>. Zonar el bean no las
         * toca porque construyen su propio {@code Clock.systemUTC()}. Esta comprobacion
         * es la que impide que alguien las "arregle" cableandoles el bean del negocio,
         * que es la forma en que este cuidado se perderia.
         */
        @Test
        @DisplayName("BusinessGaugeMetrics no recibe el reloj del negocio por inyeccion")
        void elMedidorDeNegocioSigueEnUtc() {
            assertThat(clockParametersOfSpringConstructor(BusinessGaugeMetrics.class))
                    .as("BusinessGaugeMetrics#loadContingencyExhausted calcula su umbral con "
                            + "ZoneId.systemDefault() a proposito, para contar la misma poblacion "
                            + "que descarta ContingencyRetryJob. Inyectarle el reloj del negocio "
                            + "desplazaria el umbral cinco horas respecto de las filas ya escritas.")
                    .isZero();
        }

        @Test
        @DisplayName("DatabaseAvailabilityProbe no recibe el reloj del negocio por inyeccion")
        void laSondaDeBaseDeDatosSigueEnUtc() {
            assertThat(clockParametersOfSpringConstructor(DatabaseAvailabilityProbe.class))
                    .as("La sonda mide segundos transcurridos de una racha de caida: es "
                            + "aritmetica de instantes, no de calendario, y Clock.systemUTC() es "
                            + "la eleccion correcta.")
                    .isZero();
        }

        /**
         * Cuenta los parametros {@link Clock} del constructor que usa el contenedor: el
         * anotado con {@link Autowired} si lo hay, y si no el unico publico.
         */
        private static long clockParametersOfSpringConstructor(Class<?> type) {
            Constructor<?> springConstructor = Arrays.stream(type.getDeclaredConstructors())
                    .filter(c -> c.isAnnotationPresent(Autowired.class)).findFirst()
                    .orElseGet(() -> Arrays.stream(type.getConstructors()).findFirst()
                            .orElseThrow(() -> new AssertionError(
                                    "Sin constructor publico: " + type.getName())));
            return Arrays.stream(springConstructor.getParameterTypes()).filter(Clock.class::equals)
                    .count();
        }
    }
}
