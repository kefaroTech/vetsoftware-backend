package com.vetsoftware.app.aiproposal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * &#9940; <b>El estado que ya no se puede construir.</b>
 *
 * <p>
 * Un catalogo sin nucleo cotizable no es un catalogo pobre: es uno con el que
 * <b>no se puede cotizar nada</b>. Antes se representaba como un
 * {@code Optional} vacio que devolvia {@code core()}, el motor lo filtraba por
 * {@code esCotizable} y el resultado se ignoraba <em>en silencio</em>: ni linea
 * de rechazo, ni log, ni contador. El prospecto que escribia «tengo una
 * veterinaria» recibia un 200 con {@code lines: []}, {@code discardedLines: 0}
 * y todos los importes a cero, y el estado se curaba solo al reiniciar —el
 * nucleo salia de un {@code findFirst()} sobre un mapa cuyo orden aleatoriza la
 * JVM— para volver despues.
 *
 * <p>
 * Lo que se ata aqui es que ese estado <b>no llega a existir</b>. Una
 * invariante en el constructor no se puede olvidar en un consumidor nuevo; una
 * guarda repartida por los llamantes, si.
 */
@DisplayName("SellableCatalog — un catalogo que no puede cotizar no existe")
class SellableCatalogTest {

    private static final BigDecimal IVA = new BigDecimal("19.00");

    private static SellableItem articulo(String code, SellableItemKind kind, boolean autoservicio,
            String divisa) {
        return new SellableItem(code, "Articulo " + code, "Descripcion de " + code, kind, true,
                autoservicio, 0, new BigDecimal("1000.00"), IVA, divisa);
    }

    private static SellableItem nucleoValido() {
        return articulo("CORE", SellableItemKind.MODULE, true, "COP");
    }

    private static Map<String, SellableItem> mapaDe(SellableItem... articulos) {
        Map<String, SellableItem> items = new LinkedHashMap<>();
        for (SellableItem articulo : articulos)
            items.put(articulo.code(), articulo);
        return items;
    }

    @Nested
    @DisplayName("La invariante del nucleo")
    class InvarianteDelNucleo {

        @Test
        @DisplayName("sin nucleo no hay catalogo, y lo dice con el mismo lenguaje que la divisa")
        void sin_nucleo_no_hay_catalogo() {
            Map<String, SellableItem> items = mapaDe(nucleoValido());

            assertThatThrownBy(() -> new SellableCatalog(items, Map.of(), List.of(), null))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "a catalog without a quotable core cannot price a proposal");
        }

        /**
         * &#9940; <b>La forma exacta del defecto de produccion, cerrada en el
         * constructor.</b> Un articulo del minimo estructural que no se puede cotizar
         * -una capacidad que no cuelga de ningun {@code BUNDLE}, o el propio modulo
         * retirado del catalogo- ya no puede colarse como nucleo. Antes se colaba, el
         * {@code filter(esCotizable)} del motor lo tiraba y nadie se enteraba.
         */
        @Test
        @DisplayName("un nucleo que no se puede cotizar se rechaza al construir, no al usar")
        void el_nucleo_tiene_que_ser_cotizable() {
            SellableItem noCotizable = articulo("CORE", SellableItemKind.MODULE, false, "COP");
            Map<String, SellableItem> items = mapaDe(noCotizable);

            assertThatThrownBy(() -> new SellableCatalog(items, Map.of(), List.of(), noCotizable))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("the catalog core must be quotable: CORE");
        }

        /**
         * El nucleo tiene que ser <b>uno de los articulos de este catalogo</b>, y no
         * una copia parecida: el motor lo busca por codigo en {@code items} durante el
         * cierre de dependencias, asi que un nucleo suelto daria una linea con otro
         * precio que el de la foto que se firmo en
         * {@code ai_proposals.catalog_snapshot_hash}.
         */
        @Test
        @DisplayName("el nucleo tiene que ser uno de los articulos del propio catalogo")
        void el_nucleo_pertenece_al_catalogo() {
            SellableItem forastero = articulo("OTRO_CORE", SellableItemKind.MODULE, true, "COP");
            Map<String, SellableItem> items = mapaDe(nucleoValido());

            assertThatThrownBy(() -> new SellableCatalog(items, Map.of(), List.of(), forastero))
                    .isInstanceOf(IllegalArgumentException.class).hasMessageContaining(
                            "the catalog core must be one of its own items: OTRO_CORE");
        }

        @Test
        @DisplayName("con nucleo cotizable y propio, el catalogo se construye")
        void el_catalogo_valido_se_construye() {
            SellableItem nucleo = nucleoValido();

            SellableCatalog catalogo = new SellableCatalog(
                    mapaDe(nucleo, articulo("SCHEDULING", SellableItemKind.MODULE, true, "COP")),
                    Map.of(), List.of(), nucleo);

            assertThat(catalogo.nucleo()).isEqualTo(nucleo);
        }
    }

    @Nested
    @DisplayName("La divisa")
    class Divisa {

        /**
         * &#9940; <b>Total, y sin sorteo.</b> Esto devolvia un {@code Optional} y, sin
         * nucleo, caia en un {@code findFirst()} sobre {@code items} —cuyo orden
         * aleatoriza la JVM en cada arranque—. Hoy todo el catalogo es {@code COP} y
         * ese sorteo acertaba por casualidad; con dos divisas conviviendo, el mismo
         * despliegue habria cotizado en una moneda distinta despues de reiniciar.
         */
        @Test
        @DisplayName("es la del nucleo, y no la de un articulo cualquiera del mapa")
        void la_divisa_es_la_del_nucleo() {
            SellableItem nucleo = nucleoValido();

            SellableCatalog catalogo = new SellableCatalog(
                    mapaDe(articulo("AAA_PRIMERO", SellableItemKind.MODULE, true, "USD"), nucleo,
                            articulo("ZZZ_ULTIMO", SellableItemKind.MODULE, true, "EUR")),
                    Map.of(), List.of(), nucleo);

            assertThat(catalogo.currency()).isEqualTo("COP");
        }
    }
}
