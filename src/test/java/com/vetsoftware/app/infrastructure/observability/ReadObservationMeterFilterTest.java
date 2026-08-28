package com.vetsoftware.app.infrastructure.observability;

import static org.assertj.core.api.Assertions.assertThat;

import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.config.MeterFilterReply;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Red de la retirada de las 152 series de lectura.
 *
 * <p>
 * Dos riesgos, y son opuestos. Denegar <b>de menos</b> —olvidar el
 * {@code LongTaskTimer} con infijo {@code .active}— deja vivas cuatro de las
 * ocho series de cada observación y el ahorro es la mitad del anunciado, sin
 * que nada lo delate. Denegar <b>de más</b> es mucho peor: una mutación de
 * dinero cuya latencia deje de publicarse produce exactamente el hueco
 * indistinguible de la ausencia de actividad contra el que existe el resto de
 * este paquete.
 */
@DisplayName("Filtro de medidores de las observaciones de lectura")
class ReadObservationMeterFilterTest {

    private static final Path SOURCE_ROOT = Path.of("src", "main", "java");
    private static final Pattern OBSERVED_NAME = Pattern
            .compile("@Observed\\s*\\(\\s*name\\s*=\\s*\"([^\"]+)\"");

    private final ReadObservationMeterFilter filter = new ReadObservationMeterFilter();

    private MeterFilterReply replyFor(String name) {
        return filter.accept(new Meter.Id(name, Tags.empty(), null, null, Meter.Type.TIMER));
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"subscription.find", "subscription.list.all",
            "subscription.billing.document.list", "subscription.billing.charge.list",
            "subscription.billing.sequence.list", "subscription.payment.find",
            "subscription.payment.list.all", "subscription.payment.application.list.by.document"})
    @DisplayName("una lectura declarada no publica su Timer")
    void una_lectura_declarada_no_publica_su_timer(String name) {
        assertThat(replyFor(name)).isEqualTo(MeterFilterReply.DENY);
    }

    @ParameterizedTest(name = "{0}.active")
    @ValueSource(strings = {"subscription.find", "subscription.billing.document.list"})
    @DisplayName("tampoco publica el LongTaskTimer que la acompaña")
    void tampoco_publica_el_long_task_timer(String name) {
        assertThat(replyFor(name + ".active"))
                .as("DefaultMeterObservationHandler crea un LongTaskTimer <nombre>.active por cada"
                        + " observación. Sin denegarlo, cuatro de las ocho series de cada lectura"
                        + " siguen vivas y el ahorro es la mitad del que dice el javadoc")
                .isEqualTo(MeterFilterReply.DENY);
    }

    @ParameterizedTest(name = "{0}")
    @ValueSource(strings = {"subscription.create", "subscription.item.add",
            "subscription.item.remove", "subscription.item.quantity.change", "subscription.cancel",
            "subscription.status.change", "subscription.billing.charge.create",
            "subscription.billing.charge.void", "subscription.billing.document.generate",
            "subscription.billing.document.void", "subscription.payment.register",
            "subscription.payment.apply", "subscription.payment.application.reverse",
            "entitlement.recalculate", "http.server.requests",
            "vetsoftware.business.subscription.charges"})
    @DisplayName("ninguna mutación de dinero pierde su latencia")
    void ninguna_mutacion_pierde_su_latencia(String name) {
        assertThat(replyFor(name))
                .as("denegar de más es peor que no denegar: una mutación sin latencia deja un"
                        + " hueco indistinguible de «no hubo actividad» justo en el bloque de"
                        + " dinero")
                .isEqualTo(MeterFilterReply.NEUTRAL);
    }

    @Test
    @DisplayName("cada nombre denegado corresponde a un @Observed que existe de verdad")
    void cada_nombre_denegado_existe_en_el_codigo() throws IOException {
        List<String> declarados = new ArrayList<>();
        try (var files = Files.walk(SOURCE_ROOT)) {
            for (Path source : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                Matcher matcher = OBSERVED_NAME.matcher(Files.readString(source));
                while (matcher.find()) {
                    declarados.add(matcher.group(1));
                }
            }
        }

        assertThat(declarados)
                .as("la lista de denegados nombra una observación que ya no existe. Una entrada"
                        + " podrida no rompe nada hoy, pero enseña a no leer la lista — y el día"
                        + " que alguien renombre una lectura de verdad, su Timer vuelve a"
                        + " publicarse en silencio y las 152 series regresan sin que nadie lo"
                        + " decida")
                .containsAll(ReadObservationMeterFilter.DENIED_READ_OBSERVATIONS);
    }

    @Test
    @DisplayName("la lista solo contiene lecturas, comprobado por el verbo del nombre")
    void la_lista_solo_contiene_lecturas() {
        assertThat(ReadObservationMeterFilter.DENIED_READ_OBSERVATIONS)
                .as("un verbo de escritura en esta lista silencia la latencia de una operación"
                        + " que mueve dinero")
                .allSatisfy(name -> assertThat(name)
                        .matches(".*\\.(find|find\\.current|find\\.overlapping|list|list\\.all"
                                + "|list\\.awaiting|list\\.overdue|list\\.by\\.company"
                                + "|list\\.by\\.document|history\\.list)$"));
    }
}
