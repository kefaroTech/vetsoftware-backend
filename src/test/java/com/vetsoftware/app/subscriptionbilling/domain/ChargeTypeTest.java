package com.vetsoftware.app.subscriptionbilling.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.File;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * La convención de signos del cargo, declarada en código — <b>y contrastada
 * contra la que el motor impone</b>.
 *
 * <p>
 * <b>Por qué este fichero existe.</b>
 * {@link ChargeType#exigeSubtotalNoNegativo} es la mitad aplicativa de
 * {@code chk_subscription_charges_sign}. Las dos mitades tienen que decir lo
 * mismo, y no hay nada que lo garantice: son un método de Java y un
 * {@code CHECK} de MySQL escritos en ficheros distintos por personas distintas.
 * Cuando dejan de coincidir el síntoma no es un test rojo, es <b>una violación
 * de constraint convertida en un 500 a mitad de una operación de dinero</b> —el
 * cargo se construye, el {@code INSERT} muere— y el mensaje señala la
 * restricción, no la línea de Java que la contradijo. El changeset 374 lo
 * cuenta con detalle: ampliar {@code chk_..._type} y olvidar
 * {@code chk_..._sign} deja el {@code INSERT} muriendo igual, apuntando a la
 * restricción equivocada.
 *
 * <p>
 * <b>Los tres bloques hacen tres cosas distintas y ninguno sustituye a los
 * otros.</b> {@link Clasificacion} fija qué dice el enum; {@link ReglaAplicada}
 * comprueba que decirlo <em>sirve de algo</em> construyendo cargos reales que
 * la regla tiene que rechazar —sin esto el enum podría devolver lo que fuera
 * sin que nadie lo notase—; y {@link EspejoDelEsquema} lee la definición viva
 * de la restricción en {@code src/main/resources/db/changelog/migrations} y la
 * compara literal a literal contra el enum. Ese tercero es el que caza el
 * escenario caro: alguien añade un {@code ChargeType} nuevo, lo clasifica aquí
 * y no toca el changeset.
 *
 * <p>
 * <b>El espejo lee la migración de número más alto que declare la
 * restricción</b>, no el 374 por su nombre: así el día que un changeset 4xx
 * vuelva a redefinirla, este test pasa a contrastar contra <em>esa</em> y no
 * contra una foto vieja que ya no está en la base.
 */
@DisplayName("ChargeType — la convención de signos, y que el esquema diga lo mismo")
class ChargeTypeTest {

    private static final Path MIGRACIONES = Path.of("src/main/resources/db/changelog/migrations");

    /** El bloque de vuelta atrás declara la definición ANTERIOR: no es la viva. */
    private static final Pattern BLOQUE_ROLLBACK = Pattern.compile("<rollback>.*?</rollback>",
            Pattern.DOTALL);
    private static final Pattern COMENTARIO = Pattern.compile("<!--.*?-->", Pattern.DOTALL);
    private static final Pattern PREFIJO_NUMERICO = Pattern.compile("^(\\d+)_");

    /** La rama de {@code chk_subscription_charges_sign} que exige subtotal ≥ 0. */
    private static final Pattern RAMA_NO_NEGATIVA = Pattern
            .compile("charge_type IN \\(([^)]*)\\)\\s*AND subtotal_amount >= 0", Pattern.DOTALL);

    /** La rama que exige subtotal ≤ 0, la de los cargos que restan. */
    private static final Pattern RAMA_NO_POSITIVA = Pattern
            .compile("charge_type IN \\(([^)]*)\\)\\s*AND voids_charge_id IS NULL\\s*"
                    + "AND subtotal_amount <= 0", Pattern.DOTALL);

    /** La enumeración de tipos válidos de {@code chk_subscription_charges_type}. */
    private static final Pattern TIPOS_VALIDOS = Pattern
            .compile("ADD CONSTRAINT chk_subscription_charges_type\\s*CHECK \\(charge_type IN\\s*"
                    + "\\(([^)]*)\\)\\)", Pattern.DOTALL);

    private static final Long EMPRESA = 900L;
    private static final Long CONTRATO = 970L;
    private static final LocalDateTime AHORA = LocalDateTime.of(2026, 3, 15, 9, 30);
    private static final Clock RELOJ = Clock.fixed(AHORA.toInstant(ZoneOffset.UTC), ZoneOffset.UTC);
    private static final ServicePeriod MARZO = new ServicePeriod(LocalDate.of(2026, 3, 1),
            LocalDate.of(2026, 3, 31));

    @Nested
    @DisplayName("Clasificacion")
    class Clasificacion {

        @ParameterizedTest
        @EnumSource(value = ChargeType.class, names = {"RECURRING", "ONE_TIME", "OVERAGE"})
        @DisplayName("los tipos que cobran exigen subtotal no negativo y no exigen lo contrario")
        void los_tipos_que_cobran_exigen_subtotal_no_negativo(ChargeType tipo) {
            assertThat(tipo.exigeSubtotalNoNegativo()).isTrue();
            assertThat(tipo.exigeSubtotalNoPositivo()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(value = ChargeType.class, names = {"CREDIT", "DISCOUNT"})
        @DisplayName("los tipos que devuelven exigen subtotal no positivo y no exigen lo contrario")
        void los_tipos_que_devuelven_exigen_subtotal_no_positivo(ChargeType tipo) {
            assertThat(tipo.exigeSubtotalNoPositivo()).isTrue();
            assertThat(tipo.exigeSubtotalNoNegativo()).isFalse();
        }

        @Test
        @DisplayName("PRORATION queda libre de signo: una ampliacion cobra y una reduccion"
                + " acredita, y las dos son normales")
        void proration_queda_libre_de_signo() {
            assertThat(ChargeType.PRORATION.exigeSubtotalNoNegativo()).isFalse();
            assertThat(ChargeType.PRORATION.exigeSubtotalNoPositivo()).isFalse();
        }

        @ParameterizedTest
        @EnumSource(ChargeType.class)
        @DisplayName("ningun tipo exige las dos cosas a la vez: eso solo admitiria el cargo de"
                + " cero pesos, que no es una clase de devengo")
        void ningun_tipo_exige_las_dos_cosas_a_la_vez(ChargeType tipo) {
            assertThat(tipo.exigeSubtotalNoNegativo() && tipo.exigeSubtotalNoPositivo()).isFalse();
        }
    }

    /**
     * Que el enum lo declare no basta: lo que protege el dinero es que
     * {@code SubscriptionCharge} lo aplique. Cada caso construye un cargo de verdad
     * con el signo prohibido, que es la única forma de que la regla esté
     * <em>ejercitada</em> y no solo consultada.
     */
    @Nested
    @DisplayName("La regla aplicada al cargo")
    class ReglaAplicada {

        @Test
        @DisplayName("un OVERAGE negativo se rechaza nombrando el tipo: el excedente es servicio"
                + " prestado de mas, nunca una devolucion")
        void un_overage_negativo_se_rechaza() {
            assertThatThrownBy(() -> cargo(ChargeType.OVERAGE, new BigDecimal("-36000.00")))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("OVERAGE");
        }

        @Test
        @DisplayName("un OVERAGE positivo se construye: la regla rechaza el signo, no el tipo")
        void un_overage_positivo_se_construye() {
            assertThat(cargo(ChargeType.OVERAGE, new BigDecimal("36000.00")).getSubtotalAmount())
                    .isEqualByComparingTo("36000.00");
        }

        @Test
        @DisplayName("un PRORATION negativo si se construye: sin este caso, el anterior pasaria"
                + " igual con una regla que rechazara todo negativo")
        void un_proration_negativo_se_construye() {
            assertThat(cargo(ChargeType.PRORATION, new BigDecimal("-36000.00")).signo())
                    .isEqualTo(-1);
        }

        @Test
        @DisplayName("un CREDIT positivo se rechaza: la otra mitad de la convencion sigue viva")
        void un_credit_positivo_se_rechaza() {
            assertThatThrownBy(() -> cargo(ChargeType.CREDIT, new BigDecimal("36000.00")))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("CREDIT");
        }
    }

    /**
     * El contraste contra el motor. Si estas tres aserciones y el {@code CHECK}
     * dejan de coincidir, lo que se pierde no es cobertura: es la única señal antes
     * de que un {@code INSERT} muera en producción.
     */
    @Nested
    @DisplayName("Espejo del esquema")
    class EspejoDelEsquema {

        @Test
        @DisplayName("la rama de subtotal >= 0 de chk_subscription_charges_sign enumera"
                + " exactamente los tipos que exigeSubtotalNoNegativo declara")
        void la_rama_no_negativa_del_check_coincide_con_el_enum() {
            assertThat(literales(RAMA_NO_NEGATIVA, definicionVigenteDelSigno()))
                    .containsExactlyInAnyOrderElementsOf(nombresQueCumplen(true));
        }

        @Test
        @DisplayName("la rama de subtotal <= 0 enumera exactamente los tipos que"
                + " exigeSubtotalNoPositivo declara")
        void la_rama_no_positiva_del_check_coincide_con_el_enum() {
            assertThat(literales(RAMA_NO_POSITIVA, definicionVigenteDelSigno()))
                    .containsExactlyInAnyOrderElementsOf(nombresQueCumplen(false));
        }

        @Test
        @DisplayName("chk_subscription_charges_type admite exactamente los valores del enum: un"
                + " tipo nuevo sin changeset muere en el motor, no aqui")
        void el_check_de_tipos_admite_exactamente_los_valores_del_enum() {
            assertThat(literales(TIPOS_VALIDOS,
                    definicionVigenteDe("ADD CONSTRAINT chk_subscription_charges_type")))
                    .containsExactlyInAnyOrderElementsOf(
                            Arrays.stream(ChargeType.values()).map(Enum::name).toList());
        }
    }

    private static SubscriptionCharge cargo(ChargeType tipo, BigDecimal subtotal) {
        return SubscriptionCharge.create(EMPRESA, CONTRATO, null, tipo, "Excedente de marzo", MARZO,
                BigDecimal.ONE, new BigDecimal("36000.00"), subtotal, BigDecimal.ZERO,
                TaxTreatment.EXCLUDED, ProrationBasis.of(null, null), null, RELOJ);
    }

    private static List<String> nombresQueCumplen(boolean noNegativo) {
        return Arrays.stream(ChargeType.values())
                .filter(tipo -> noNegativo
                        ? tipo.exigeSubtotalNoNegativo()
                        : tipo.exigeSubtotalNoPositivo())
                .map(Enum::name).toList();
    }

    private static String definicionVigenteDelSigno() {
        return definicionVigenteDe("ADD CONSTRAINT chk_subscription_charges_sign");
    }

    /**
     * El SQL de ida de la migración <b>de número más alto</b> que declara esa
     * restricción, que es la definición que hay hoy en la base. Buscar por nombre
     * de fichero fijaría el test a una foto que un changeset posterior invalidaría
     * en silencio.
     */
    private static String definicionVigenteDe(String declaracion) {
        return Arrays.stream(ficherosDeMigracion())
                .sorted(Comparator.comparingInt(ChargeTypeTest::numeroDe).reversed())
                .map(ChargeTypeTest::sqlDeIda).filter(sql -> sql.contains(declaracion)).findFirst()
                .orElseThrow(() -> new AssertionError(
                        "ninguna migracion declara «" + declaracion + "»: la restriccion que este"
                                + " test contrasta ya no existe en el changelog"));
    }

    private static File[] ficherosDeMigracion() {
        return Objects.requireNonNull(
                MIGRACIONES.toFile().listFiles((directorio, nombre) -> nombre.endsWith(".xml")),
                "no hay migraciones en " + MIGRACIONES.toAbsolutePath()
                        + ": el test se ejecuta desde otro basedir");
    }

    private static int numeroDe(File migracion) {
        Matcher prefijo = PREFIJO_NUMERICO.matcher(migracion.getName());
        return prefijo.find() ? Integer.parseInt(prefijo.group(1)) : -1;
    }

    /** El fichero sin comentarios y sin el bloque de vuelta atrás. */
    private static String sqlDeIda(File migracion) {
        String contenido = leer(migracion);
        return BLOQUE_ROLLBACK.matcher(COMENTARIO.matcher(contenido).replaceAll("")).replaceAll("");
    }

    private static String leer(File migracion) {
        try {
            return Files.readString(migracion.toPath(), StandardCharsets.UTF_8);
        } catch (IOException noSePudoLeer) {
            throw new UncheckedIOException(noSePudoLeer);
        }
    }

    /** Los literales {@code 'X','Y'} del primer grupo que capture el patrón. */
    private static List<String> literales(Pattern patron, String sql) {
        Matcher rama = patron.matcher(sql);
        return Arrays.stream(rama.find() ? rama.group(1).split(",") : new String[0])
                .map(literal -> literal.trim().replace("'", ""))
                .filter(literal -> !literal.isEmpty()).toList();
    }
}
