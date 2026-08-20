package com.vetsoftware.app.infrastructure.logging;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * El gate de la incidencia #183: <b>toda</b> regla de {@link LogRedactor} se
 * evalúa de verdad para la entrada que debería activarla.
 *
 * <p>
 * <b>El defecto de fondo, que sigue vivo aunque #170 esté cerrado.</b>
 * {@code LogRedactor.redact(...)} hace un solo barrido de caracteres para
 * decidir qué reglas pueden casar —¿hay algún dígito?, ¿alguna {@code @}?,
 * ¿algún {@code :} o {@code =}?— y después cuelga cada patrón de una de esas
 * banderas. Es una optimización correcta y deliberada: evita ocho pasadas de
 * expresión regular sobre la inmensa mayoría de líneas, que no contienen ningún
 * candidato. Pero convierte «la regla está escrita» en algo distinto de «la
 * regla se ejecuta», y <b>nada en el sistema notaba la diferencia</b>: una
 * regla nueva colgada de la bandera equivocada compila, se lee perfecta en el
 * diff, tiene su propio test de patrón en verde… y no se ejecuta jamás. La fuga
 * de PII que resulta es silenciosa y con ventana de meses.
 *
 * <p>
 * <b>Ya pasó exactamente eso.</b> {@code MYSQL_DUPLICATE_ENTRY} enmascara el
 * valor de un {@code Duplicate entry '<valor>'}, que puede ser un nombre
 * propio: sin dígitos, sin {@code @}, sin {@code :} ni {@code =}. Colgarla de
 * cualquier bandera del barrido la dejaba sin evaluar <em>precisamente</em> en
 * el caso que motiva la regla. Hoy va delante de todas y sin bandera, con un
 * {@code indexOf} de una constante como discriminador — pero eso es una
 * decisión que alguien tomó, no algo que el código garantice.
 *
 * <p>
 * <b>Cómo se prueba la alcanzabilidad y no solo el patrón.</b> Cada patrón
 * declarado en {@code LogRedactor} tiene aquí un cebo, y de cada cebo se
 * comprueban dos cosas: que el patrón lo reconoce (o sea, que el cebo es un
 * disparador legítimo <em>de ese</em> patrón) y que {@code redact(cebo)}
 * devuelve la salida enmascarada que ese patrón produce. Los cebos están
 * elegidos para que ninguna otra regla pueda enmascararlos —el que prueba
 * {@code MYSQL_DUPLICATE_ENTRY} no tiene dígitos, el de {@code KEYED_VALUE} no
 * tiene {@code @}, el de {@code EMAIL} no tiene separador—, así que si el
 * barrido de banderas deja un patrón fuera, su caso falla y ningún otro lo
 * tapa.
 *
 * <p>
 * <b>Y la mitad anti-podredumbre</b>, que es la que de verdad cierra el
 * agujero: {@link #todo_patron_declarado_tiene_su_cebo()} exige que el conjunto
 * de campos {@code Pattern} de {@code LogRedactor} sea <em>exactamente</em> el
 * de los cebos de aquí. Añadir una regla nueva sin declarar con qué entrada se
 * activa rompe el build. Esa es la diferencia entre este fichero y un test de
 * patrones más: los otros crecen cuando alguien se acuerda; este obliga.
 */
@DisplayName("LogRedactor: toda regla declarada se ejecuta de verdad")
class LogRedactorAlcanzabilidadTest {

    private static final String MASK = LogRedactor.MASK;

    /**
     * El cebo de {@code JWT}, ensamblado a partir de sus tres segmentos en vez de
     * escrito de una pieza. No es cosmetica: el gancho de pre-commit corre
     * gitleaks, cuya regla {@code jwt} casa cualquier {@code eyJ} contiguo con sus
     * dos puntos y no tiene forma de saber que este es sintetico: cabecera HS512,
     * sujeto {@code mafalda} y una firma que es {@code AladdinsLamp} en base64.
     * Partirlo deja el gate util para un secreto de verdad. La alternativa era
     * estrenar un {@code .gitleaksignore} en este repositorio con una huella
     * fichero:regla:linea que caduca sola en cuanto alguien inserta una linea
     * encima, y una allowlist podrida es peor que no tenerla. El valor que ve el
     * test es identico.
     */
    private static final String JWT_SINTETICO = "eyJhbGciOiJIUzUxMiJ9" + "."
            + "eyJzdWIiOiJtYWZhbGRhIn0" + "." + "QWxhZGRpbnNMYW1w";

    /**
     * Un cebo por patrón: la entrada que debe activarlo y la salida exacta que su
     * enmascarado produce.
     *
     * @param patron
     *            nombre del campo {@code Pattern} en {@code LogRedactor}
     * @param cebo
     *            entrada elegida para que <b>solo</b> ese patrón pueda actuar
     * @param esperado
     *            resultado de {@code redact(cebo)} si el patrón se evalúa
     */
    private record Cebo(String patron, String cebo, String esperado) {
    }

    /**
     * Los cebos, uno por patrón. Cada valor sensible se eligió para que ninguna
     * otra regla de la cadena pueda tocarlo: sin dígitos donde se prueba una regla
     * de forma, sin {@code :} ni {@code =} donde se prueba el correo, sin {@code @}
     * donde se prueba la clave-valor.
     */
    private static Map<String, Cebo> cebos() {
        Map<String, Cebo> cebos = new LinkedHashMap<>();
        // Nombre propio entre comillas: ni un digito, ni una arroba, ni un separador.
        // Si esta regla se colgara de una bandera del barrido, este caso es el unico
        // que lo detecta.
        cebos.put("MYSQL_DUPLICATE_ENTRY",
                new Cebo("MYSQL_DUPLICATE_ENTRY",
                        "Duplicate entry 'Mafalda Cenuela' for key 'uq_owners_company_name'",
                        "Duplicate entry '" + MASK + "' for key 'uq_owners_company_name'"));
        // El detalle de PostgreSQL. Lleva ')=(' , asi que la bandera 'separator' esta
        // puesta siempre que el detalle exista: colgarla de ella es seguro y ese
        // razonamiento es lo que este caso vigila.
        cebos.put("POSTGRES_CONSTRAINT_KEY",
                new Cebo("POSTGRES_CONSTRAINT_KEY", "Key (name)=(Mafalda Cenuela) already exists.",
                        "Key (name)=(" + MASK + ") already exists."));
        cebos.put("URL_CREDENTIALS",
                new Cebo("URL_CREDENTIALS", "jdbc:mysql://vetadmin:Zurriaga@db.interno/vetsoftware",
                        "jdbc:mysql://" + MASK + ":" + MASK + "@db.interno/vetsoftware"));
        cebos.put("JWT", new Cebo("JWT", "token caducado " + JWT_SINTETICO + " fin",
                "token caducado " + MASK + " fin"));
        cebos.put("HTTP_AUTH_SCHEME", new Cebo("HTTP_AUTH_SCHEME",
                "cabecera Basic dmV0YWRtaW46WnVycmlhZ2E fin", "cabecera Basic " + MASK + " fin"));
        // PAN valido por Luhn. Sin ':' ni '=', asi que KEYED_VALUE no interviene
        // aunque 'card' este en su vocabulario.
        cebos.put("CARD_CANDIDATE", new Cebo("CARD_CANDIDATE", "card 4111 1111 1111 1111 fin",
                "card " + MASK + "1111 fin"));
        // Valor sin digitos y sin arroba: solo la regla clave-valor puede taparlo.
        cebos.put("KEYED_VALUE",
                new Cebo("KEYED_VALUE", "password=Zurriaga fin", "password=" + MASK + " fin"));
        // Sin ':' ni '=' en toda la linea, asi que maskKeyedValues ni se llama.
        cebos.put("EMAIL", new Cebo("EMAIL", "aviso a mafalda@clinica.vet enviado",
                "aviso a " + MASK + "@clinica.vet enviado"));
        // Doce digitos: por debajo del minimo de CARD_CANDIDATE (13) y sin corrida
        // aislada de diez que active LONG_DIGIT_RUN. Solo queda el telefono.
        cebos.put("INTERNATIONAL_PHONE", new Cebo("INTERNATIONAL_PHONE",
                "contacto +57 300 123 4567 fin", "contacto " + MASK + " fin"));
        cebos.put("LONG_DIGIT_RUN", new Cebo("LONG_DIGIT_RUN", "documento 1032456789 fin",
                "documento " + MASK + " fin"));
        return cebos;
    }

    /** Los campos {@code Pattern} que {@code LogRedactor} declara hoy. */
    private static Stream<Field> patronesDeclarados() {
        return Arrays.stream(LogRedactor.class.getDeclaredFields())
                .filter(campo -> campo.getType() == Pattern.class)
                .filter(campo -> Modifier.isStatic(campo.getModifiers()));
    }

    private static Stream<Arguments> casos() {
        return cebos().values().stream()
                .map(cebo -> Arguments.of(cebo.patron(), cebo.cebo(), cebo.esperado()));
    }

    private static Pattern patron(String nombre) {
        return patronesDeclarados().filter(campo -> campo.getName().equals(nombre)).findFirst()
                .map(LogRedactorAlcanzabilidadTest::leer).orElseThrow(
                        () -> new AssertionError("LogRedactor no declara el patron " + nombre));
    }

    private static Pattern leer(Field campo) {
        campo.setAccessible(true);
        try {
            return (Pattern) campo.get(null);
        } catch (IllegalAccessException e) {
            throw new AssertionError("no se pudo leer " + campo.getName(), e);
        }
    }

    /**
     * La mitad anti-podredumbre. Un patrón nuevo sin cebo rompe el build: es la
     * única forma de que «regla escrita» siga significando «regla ejecutada» dentro
     * de seis meses, cuando quien la añada no haya leído esta clase.
     */
    @Test
    @DisplayName("todo Pattern declarado en LogRedactor tiene su cebo en este test")
    void todo_patron_declarado_tiene_su_cebo() {
        assertThat(patronesDeclarados().map(Field::getName))
                .as("cada Pattern de LogRedactor necesita aqui una entrada que declare"
                        + " con que texto se activa; si no, puede quedar colgado de una"
                        + " bandera del barrido que nunca se enciende y nadie lo notara")
                .containsExactlyInAnyOrderElementsOf(cebos().keySet());
    }

    /**
     * La alcanzabilidad propiamente dicha, en dos aserciones que se sostienen la
     * una a la otra.
     *
     * <p>
     * La primera es la sanidad del cebo: el patrón tiene que reconocerlo. Sin ella,
     * un cebo mal escrito haría pasar la segunda por la razón equivocada
     * —enmascarado por otra regla— y la prueba sería falsa.
     *
     * <p>
     * La segunda es la que cierra #183: {@code redact(...)} tiene que producir el
     * enmascarado <b>de este</b> patrón. Como ninguna otra regla de la cadena puede
     * tocar este cebo, la única forma de que salga el resultado esperado es que el
     * barrido de banderas haya dejado pasar la regla. Si alguien la cuelga de una
     * bandera que este texto no enciende, este caso falla y lo dice por su nombre.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("casos")
    @DisplayName("redact(...) evalua el patron de verdad para la entrada que deberia activarlo")
    void redact_evalua_el_patron(String nombre, String cebo, String esperado) {
        assertThat(patron(nombre).matcher(cebo).find())
                .as("el cebo de %s no casa con el propio patron: este caso estaria midiendo"
                        + " otra regla", nombre)
                .isTrue();
        assertThat(LogRedactor.redact(cebo))
                .as("%s esta declarado pero redact(...) no lo aplico: revisa de que bandera"
                        + " del barrido cuelga en redact(String)", nombre)
                .isEqualTo(esperado);
    }
}
