package com.vetsoftware.app.aiproposal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.domain.SellableItem;
import com.vetsoftware.app.aiproposal.testsupport.CatalogoComercial2026;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/**
 * &#9940; <b>EL TEST QUE FALTABA, Y EL QUE HABRIA EVITADO EL DEFECTO
 * ENTERO.</b>
 *
 * <p>
 * {@link CatalogoComercial2026} <b>declara por escrito</b> que copia de la
 * semilla 308 «los codigos, cuales son modulos y cuales capacidades, quien es
 * {@code is_core}…». Durante toda su vida tuvo <b>un</b> articulo
 * {@code is_core} cuando la semilla marca <b>tres</b>, y esa divergencia estaba
 * justo en el campo que decidia el defecto. El golden set pasaba en verde
 * mientras produccion devolvia un carrito vacio a quien escribia «tengo una
 * veterinaria»: el fixture no reproducia la unica condicion que rompia.
 *
 * <p>
 * <b>Por eso esto no compara contra una lista escrita a mano.</b> Una constante
 * con los tres codigos seria otra copia que se puede quedar atras igual —el
 * mismo error una capa mas arriba—. Se lee el changeset <b>del classpath</b> y
 * se parsea su tabla en linea, de modo que el dia que alguien anada, retire o
 * remarque un articulo en la semilla, el fixture tenga que enterarse o el build
 * se cae.
 *
 * <p>
 * <b>Lo que NO comprueba</b>, a proposito: los importes (viven en la 310, con
 * escalera por tramos), los arcos {@code REQUIRES} (309 y 380) y los
 * componentes de los paquetes. Este test cubre lo que la 308 fija y el fixture
 * afirma copiar.
 */
@DisplayName("El catalogo de pruebas no puede divergir de la semilla 308")
class CatalogoDePruebasNoDivergeDeLaSemillaTest {

    private static final String RECURSO = "db/changelog/migrations/308_seed_commercial_catalog_items.xml";

    /**
     * Cuantos articulos siembra la 308. No es una asercion de negocio: es el seguro
     * contra un parser que se rompa en silencio y deje el resto del test pasando
     * sobre un mapa vacio, que es la forma clasica de que una medicion mienta.
     */
    private static final int ARTICULOS_SEMBRADOS = 26;

    private static Map<String, ArticuloSembrado> semilla;

    @BeforeAll
    static void leerLaSemilla() throws IOException {
        try (InputStream entrada = CatalogoDePruebasNoDivergeDeLaSemillaTest.class.getClassLoader()
                .getResourceAsStream(RECURSO)) {
            assertThat(entrada).as("la semilla 308 tiene que estar en el classpath de test")
                    .isNotNull();
            semilla = parsear(new String(entrada.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    /**
     * &#9888; <b>Se lee la tabla del INSERT y solo esa.</b> El {@code UPDATE} de
     * convergencia que viene despues en el mismo fichero lleva las columnas en otro
     * orden —sin {@code short_description}, asi que {@code is_core} cae en la
     * posicion 4 y no en la 5— y parsearlo con este mismo mapa daria valores
     * desplazados. Se acota al primer {@code FROM (…) seed} del fichero.
     */
    private static Map<String, ArticuloSembrado> parsear(String xml) {
        int abre = xml.indexOf("FROM (");
        int cierra = xml.indexOf(") seed", abre);
        assertThat(abre).as("no se encontro la tabla en linea del INSERT").isNotNegative();
        assertThat(cierra).as("no se encontro el cierre de la tabla en linea").isGreaterThan(abre);

        Map<String, ArticuloSembrado> filas = new LinkedHashMap<>();
        for (String fila : xml.substring(abre + "FROM (".length(), cierra)
                .split("UNION ALL SELECT")) {
            String cuerpo = fila.trim();
            if (cuerpo.startsWith("SELECT"))
                cuerpo = cuerpo.substring("SELECT".length());
            if (cuerpo.isBlank())
                continue;
            List<String> campos = campos(cuerpo);
            String code = primerLiteral(campos.get(0));
            filas.put(code, new ArticuloSembrado(code, primerLiteral(campos.get(3)),
                    campos.get(5).startsWith("TRUE")));
        }
        return filas;
    }

    /**
     * Trocea por las comas de nivel cero. Ni las comas dentro de un literal —«Spa,
     * estetica y guarderia»— ni las de un {@code CAST(NULL AS CHAR(30))} separan
     * campos.
     */
    private static List<String> campos(String fila) {
        List<String> campos = new ArrayList<>();
        StringBuilder actual = new StringBuilder();
        boolean enComillas = false;
        int parentesis = 0;
        for (char caracter : fila.toCharArray()) {
            if (caracter == '\'')
                enComillas = !enComillas;
            if (!enComillas) {
                if (caracter == '(')
                    parentesis++;
                else if (caracter == ')')
                    parentesis--;
                else if (caracter == ',' && parentesis == 0) {
                    campos.add(actual.toString().trim());
                    actual.setLength(0);
                    continue;
                }
            }
            actual.append(caracter);
        }
        campos.add(actual.toString().trim());
        return campos;
    }

    /**
     * El contenido del primer literal, ignorando el {@code AS nombre} que sigue.
     */
    private static String primerLiteral(String campo) {
        int abre = campo.indexOf('\'');
        int cierra = campo.indexOf('\'', abre + 1);
        return campo.substring(abre + 1, cierra);
    }

    private static List<String> nucleosDeLaSemilla() {
        return semilla.values().stream().filter(ArticuloSembrado::core).map(ArticuloSembrado::code)
                .sorted().toList();
    }

    /**
     * El seguro del instrumento: si el parser se rompe, este test cae <b>antes</b>
     * que los otros dos y dice que el problema es la medicion, no el fixture. Sin
     * el, un parser roto devolveria un mapa vacio y los {@code allSatisfy} de abajo
     * pasarian por vacuidad.
     */
    @Test
    @DisplayName("la semilla se leyo de verdad: 26 articulos y mas de un is_core")
    void la_semilla_se_leyo_de_verdad() {
        assertThat(semilla).hasSize(ARTICULOS_SEMBRADOS).containsKey("CORE");
        assertThat(semilla.get("CORE").itemType()).isEqualTo("MODULE");
        assertThat(nucleosDeLaSemilla()).as(
                "si la 308 solo marcara UN is_core, este test dejaria de vigilar lo que importa")
                .hasSizeGreaterThan(1);
    }

    /**
     * &#9940; <b>La asercion que fallaba antes del arreglo.</b> La semilla marca
     * {@code is_core} en tres articulos porque
     * {@code PlatformCatalogTemplateJpaRepository.findInitialCapacityTemplates} lee
     * esa columna como predicado de conjunto; un fixture con uno solo no reproduce
     * la unica condicion capaz de romper el motor.
     */
    @Test
    @DisplayName("ningun articulo is_core de la semilla puede faltar en el catalogo de pruebas")
    void ningun_nucleo_de_la_semilla_falta_en_el_fixture() {
        SellableCatalog catalogo = CatalogoComercial2026.catalogo();

        assertThat(nucleosDeLaSemilla()).allSatisfy(code -> assertThat(catalogo.find(code))
                .as("la semilla marca %s como is_core y el catalogo de pruebas no lo tiene", code)
                .isPresent());
    }

    /**
     * Y la otra direccion: lo que el fixture si trae tiene que decir lo mismo que
     * la semilla en los dos campos que declara copiar —el tipo y la pertenencia al
     * minimo estructural—. La pertenencia se comprueba por su efecto observable,
     * que es lo unico que el dominio conserva: <b>quien es el nucleo</b>. Del resto
     * de {@code is_core} el dominio ya no sabe nada, y esa es justamente la
     * correccion.
     */
    @Test
    @DisplayName("todo articulo del catalogo de pruebas existe en la semilla y copia su tipo")
    void el_fixture_copia_el_tipo_de_cada_articulo() {
        SellableCatalog catalogo = CatalogoComercial2026.catalogo();
        assertThat(catalogo.items()).isNotEmpty();

        assertThat(catalogo.items().values()).allSatisfy(item -> {
            ArticuloSembrado sembrado = semilla.get(item.code());
            assertThat(sembrado).as("la semilla 308 no tiene ningun articulo %s", item.code())
                    .isNotNull();
            assertThat(item.kind().name()).as("item_type de %s", item.code())
                    .isEqualTo(sembrado.itemType());
        });
    }

    /**
     * El nucleo que el fixture resuelve tiene que ser un {@code is_core} de la
     * semilla <b>y</b> un modulo: es la misma regla que aplica la capa
     * anticorrupcion del adaptador, comprobada aqui sobre el catalogo real para que
     * el golden set no pueda fijarse contra otro nucleo.
     */
    @Test
    @DisplayName("el nucleo del catalogo de pruebas es un is_core de la semilla y es un MODULE")
    void el_nucleo_del_fixture_sale_de_la_semilla() {
        SellableItem nucleo = CatalogoComercial2026.catalogo().nucleo();

        assertThat(nucleosDeLaSemilla()).contains(nucleo.code());
        assertThat(semilla.get(nucleo.code()).itemType()).isEqualTo("MODULE");
        assertThat(nucleo.esCotizable()).isTrue();
    }

    /** Lo que la 308 fija de cada articulo y este test vigila. */
    private record ArticuloSembrado(String code, String itemType, boolean core) {
    }
}
