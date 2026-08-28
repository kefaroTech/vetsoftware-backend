package com.vetsoftware.app.configurator.domain;

import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_BASE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_CAJA;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.ITEM_POS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O11_SI_VENDE;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.O21_SI_MOSTRADOR;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.Q3_CUANTAS_CAJAS;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoDeshabilitado;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorOpcion;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.efectoPorPregunta;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.marcadas;
import static com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.respuestas;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * La pieza donde un error no da excepción: da una cotización equivocada,
 * firmada.
 *
 * <p>
 * Lo que estas pruebas defienden por encima de todo es el
 * <strong>orden</strong>. Los efectos no conmutan, así que el contrato dice que
 * se aplican por {@code priority} ascendente —con desempate por id— y no en el
 * orden en que la base los devuelva. Un test que solo comprobase «ADD mete el
 * artículo» pasaría igual con el orden roto, y el defecto solo se vería en una
 * cotización ya firmada.
 *
 * <p>
 * <strong>Este javadoc decía «se aplican por id ascendente», y eso era el
 * defecto escrito como si fuera el contrato.</strong> La columna
 * {@code priority} existía en el esquema desde el changeset 238 —con su
 * {@code CHECK}, su índice {@code (priority, id)} y la siembra repartiendo
 * decenas por pregunta— y no estaba mapeada en Java: el resolvedor ordenaba por
 * el orden en que alguien insertó las filas. Los casos del bloque
 * {@link PrioridadSobreId} son los que discriminan de verdad, porque llevan los
 * ids <em>al revés</em> que las prioridades; los del bloque {@link Orden} usan
 * todos la misma prioridad y por tanto lo que ejercitan es el desempate.
 */
@DisplayName("ConfiguratorResolver — de respuestas a carrito")
class ConfiguratorResolverTest {

    @Nested
    @DisplayName("el orden de aplicacion es (priority, id), y es parte del contrato")
    class Orden {
        // Todos estos casos usan la prioridad por defecto, asi que lo que
        // ejercitan es el DESEMPATE por id. Quien anada aqui un caso que crea
        // estar probando la prioridad tiene que fijarla: ver PrioridadSobreId.

        @Test
        @DisplayName("ADD antes que REMOVE sobre el mismo articulo deja el carrito sin el")
        void add_antes_que_remove_deja_el_carrito_sin_el_articulo() {
            List<ConfiguratorEffect> efectos = List.of(
                    efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null),
                    efectoPorOpcion(2L, O11_SI_VENDE, ITEM_POS, EffectType.REMOVE, null));

            assertThat(ConfiguratorResolver.resolve(efectos, marcadas(O11_SI_VENDE))).isEmpty();
        }

        @Test
        @DisplayName("REMOVE antes que ADD sobre el mismo articulo lo deja dentro: no conmutan")
        void remove_antes_que_add_deja_el_articulo_dentro() {
            List<ConfiguratorEffect> efectos = List.of(
                    efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.REMOVE, null),
                    efectoPorOpcion(2L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null));

            assertThat(ConfiguratorResolver.resolve(efectos, marcadas(O11_SI_VENDE)))
                    .containsExactly(new SelectedItem(ITEM_POS, 1));
        }

        @Test
        @DisplayName("el orden en que llega la lista es irrelevante: manda (priority, id), no la BD")
        void el_orden_de_llegada_es_irrelevante() {
            ConfiguratorEffect add = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null);
            ConfiguratorEffect remove = efectoPorOpcion(2L, O11_SI_VENDE, ITEM_POS,
                    EffectType.REMOVE, null);

            List<SelectedItem> enUnOrden = ConfiguratorResolver.resolve(List.of(add, remove),
                    marcadas(O11_SI_VENDE));
            List<SelectedItem> enElOtro = ConfiguratorResolver.resolve(List.of(remove, add),
                    marcadas(O11_SI_VENDE));

            assertThat(enUnOrden).isEqualTo(enElOtro).isEmpty();
        }

        @Test
        @DisplayName("no muta la lista de efectos que recibe: ordena sobre una copia")
        void no_muta_la_lista_de_efectos_recibida() {
            ConfiguratorEffect segundo = efectoPorOpcion(2L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null);
            ConfiguratorEffect primero = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_CAJA,
                    EffectType.ADD, null);
            List<ConfiguratorEffect> recibida = new ArrayList<>(List.of(segundo, primero));

            ConfiguratorResolver.resolve(recibida, marcadas(O11_SI_VENDE));

            assertThat(recibida).containsExactly(segundo, primero);
        }

        @Test
        @DisplayName("SET_QUANTITY pisa lo que hubiera puesto un ADD anterior")
        void set_quantity_pisa_el_add_anterior() {
            List<ConfiguratorEffect> efectos = List.of(
                    efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null),
                    efectoPorOpcion(2L, O11_SI_VENDE, ITEM_POS, EffectType.SET_QUANTITY, 4));

            assertThat(ConfiguratorResolver.resolve(efectos, marcadas(O11_SI_VENDE)))
                    .containsExactly(new SelectedItem(ITEM_POS, 4));
        }

        @Test
        @DisplayName("un ADD posterior no pisa la cantidad que fijo un SET_QUANTITY")
        void un_add_posterior_no_pisa_el_set_quantity() {
            List<ConfiguratorEffect> efectos = List.of(
                    efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.SET_QUANTITY, 4),
                    efectoPorOpcion(2L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null));

            assertThat(ConfiguratorResolver.resolve(efectos, marcadas(O11_SI_VENDE)))
                    .containsExactly(new SelectedItem(ITEM_POS, 4));
        }

        @Test
        @DisplayName("un efecto sin id todavia se aplica el ultimo, no en medio")
        void un_efecto_sin_id_se_aplica_el_ultimo() {
            ConfiguratorEffect sinId = new ConfiguratorEffect(null, O11_SI_VENDE, null, ITEM_POS,
                    EffectType.REMOVE, null, 0,
                    com.vetsoftware.app.configurator.testsupport.ConfiguratorMother.CREADA_EL, null,
                    true);
            List<ConfiguratorEffect> efectos = List.of(sinId,
                    efectoPorOpcion(9L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null));

            assertThat(ConfiguratorResolver.resolve(efectos, marcadas(O11_SI_VENDE))).isEmpty();
        }
    }

    @Nested
    @DisplayName("QUANTITY_FROM_ANSWER — la cantidad la escribe el cliente")
    class CantidadDeLaRespuesta {

        @ParameterizedTest(name = "responder {0} deja {0} unidades")
        @DisplayName("la cantidad del carrito es el numero respondido")
        @CsvSource({"1", "2", "17", "999"})
        void la_cantidad_es_el_numero_respondido(int respondido) {
            List<ConfiguratorEffect> efectos = List.of(efectoPorPregunta(1L, Q3_CUANTAS_CAJAS,
                    ITEM_CAJA, EffectType.QUANTITY_FROM_ANSWER, null));

            List<SelectedItem> seleccion = ConfiguratorResolver.resolve(efectos,
                    respuestas(Set.of(), Map.of(Q3_CUANTAS_CAJAS, respondido)));

            assertThat(seleccion).containsExactly(new SelectedItem(ITEM_CAJA, respondido));
        }

        @Test
        @DisplayName("responder cero saca el articulo del carrito en vez de dejar una linea de cero")
        void responder_cero_saca_el_articulo_del_carrito() {
            List<ConfiguratorEffect> efectos = List.of(efectoPorPregunta(1L, Q3_CUANTAS_CAJAS,
                    ITEM_CAJA, EffectType.QUANTITY_FROM_ANSWER, null));

            List<SelectedItem> seleccion = ConfiguratorResolver.resolve(efectos,
                    respuestas(Set.of(), Map.of(Q3_CUANTAS_CAJAS, 0)));

            assertThat(seleccion).isEmpty();
        }

        @Test
        @DisplayName("responder cero tambien retira lo que habia metido un ADD anterior")
        void responder_cero_retira_lo_que_metio_un_add_anterior() {
            List<ConfiguratorEffect> efectos = List.of(
                    efectoPorOpcion(1L, O21_SI_MOSTRADOR, ITEM_CAJA, EffectType.ADD, null),
                    efectoPorPregunta(2L, Q3_CUANTAS_CAJAS, ITEM_CAJA,
                            EffectType.QUANTITY_FROM_ANSWER, null));

            List<SelectedItem> seleccion = ConfiguratorResolver.resolve(efectos,
                    respuestas(Set.of(O21_SI_MOSTRADOR), Map.of(Q3_CUANTAS_CAJAS, 0)));

            assertThat(seleccion).isEmpty();
        }

        @Test
        @DisplayName("con el orden inverso el cero se aplica antes y el ADD vuelve a meterlo: no conmutan")
        void con_el_orden_inverso_el_add_vuelve_a_meterlo() {
            List<ConfiguratorEffect> efectos = List.of(
                    efectoPorPregunta(1L, Q3_CUANTAS_CAJAS, ITEM_CAJA,
                            EffectType.QUANTITY_FROM_ANSWER, null),
                    efectoPorOpcion(2L, O21_SI_MOSTRADOR, ITEM_CAJA, EffectType.ADD, null));

            List<SelectedItem> seleccion = ConfiguratorResolver.resolve(efectos,
                    respuestas(Set.of(O21_SI_MOSTRADOR), Map.of(Q3_CUANTAS_CAJAS, 0)));

            assertThat(seleccion).containsExactly(new SelectedItem(ITEM_CAJA, 1));
        }

        @Test
        @DisplayName("sin respuesta numerica el efecto ni se dispara: el articulo anterior sigue")
        void sin_respuesta_numerica_el_efecto_no_se_dispara() {
            List<ConfiguratorEffect> efectos = List.of(
                    efectoPorOpcion(1L, O21_SI_MOSTRADOR, ITEM_CAJA, EffectType.ADD, null),
                    efectoPorPregunta(2L, Q3_CUANTAS_CAJAS, ITEM_CAJA,
                            EffectType.QUANTITY_FROM_ANSWER, null));

            List<SelectedItem> seleccion = ConfiguratorResolver.resolve(efectos,
                    marcadas(O21_SI_MOSTRADOR));

            assertThat(seleccion).containsExactly(new SelectedItem(ITEM_CAJA, 1));
        }

        @Test
        @DisplayName("un QUANTITY_FROM_ANSWER disparado por opcion se ignora en vez de reventar")
        void un_quantity_from_answer_disparado_por_opcion_se_ignora() {
            List<ConfiguratorEffect> efectos = List.of(efectoPorOpcion(1L, O21_SI_MOSTRADOR,
                    ITEM_CAJA, EffectType.QUANTITY_FROM_ANSWER, null));

            List<SelectedItem> seleccion = ConfiguratorResolver.resolve(efectos,
                    marcadas(O21_SI_MOSTRADOR));

            assertThat(seleccion).isEmpty();
        }
    }

    @Nested
    @DisplayName("que dispara y que no")
    class Disparadores {

        @Test
        @DisplayName("un efecto dado de baja no se aplica nunca")
        void un_efecto_dado_de_baja_no_se_aplica() {
            List<ConfiguratorEffect> efectos = List
                    .of(efectoDeshabilitado(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD));

            assertThat(ConfiguratorResolver.resolve(efectos, marcadas(O11_SI_VENDE))).isEmpty();
        }

        @Test
        @DisplayName("un REMOVE dado de baja no retira lo que un ADD activo metio")
        void un_remove_dado_de_baja_no_retira_nada() {
            List<ConfiguratorEffect> efectos = List.of(
                    efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null),
                    efectoDeshabilitado(2L, O11_SI_VENDE, ITEM_POS, EffectType.REMOVE));

            assertThat(ConfiguratorResolver.resolve(efectos, marcadas(O11_SI_VENDE)))
                    .containsExactly(new SelectedItem(ITEM_POS, 1));
        }

        @Test
        @DisplayName("una opcion no marcada no dispara su efecto")
        void una_opcion_no_marcada_no_dispara_su_efecto() {
            List<ConfiguratorEffect> efectos = List
                    .of(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null));

            assertThat(ConfiguratorResolver.resolve(efectos, marcadas(O21_SI_MOSTRADOR))).isEmpty();
        }

        @Test
        @DisplayName("un REMOVE de un articulo que nadie metio no rompe nada")
        void un_remove_de_un_articulo_ausente_no_rompe_nada() {
            List<ConfiguratorEffect> efectos = List.of(
                    efectoPorOpcion(1L, O11_SI_VENDE, ITEM_CAJA, EffectType.REMOVE, null),
                    efectoPorOpcion(2L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null));

            assertThat(ConfiguratorResolver.resolve(efectos, marcadas(O11_SI_VENDE)))
                    .containsExactly(new SelectedItem(ITEM_POS, 1));
        }
    }

    @Nested
    @DisplayName("la seleccion que sale")
    class Salida {

        @Test
        @DisplayName("va ordenada por id de articulo, no por el orden en que entro al carrito")
        void va_ordenada_por_id_de_articulo() {
            List<ConfiguratorEffect> efectos = List.of(
                    efectoPorOpcion(1L, O11_SI_VENDE, ITEM_BASE, EffectType.ADD, null),
                    efectoPorOpcion(2L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null),
                    efectoPorOpcion(3L, O11_SI_VENDE, ITEM_CAJA, EffectType.ADD, null));

            assertThat(ConfiguratorResolver.resolve(efectos, marcadas(O11_SI_VENDE)))
                    .extracting(SelectedItem::catalogItemId)
                    .containsExactly(ITEM_POS, ITEM_CAJA, ITEM_BASE);
        }

        @Test
        @DisplayName("dos ejecuciones con los mismos datos dan exactamente la misma lista")
        void dos_ejecuciones_dan_la_misma_lista() {
            List<ConfiguratorEffect> efectos = new ArrayList<>(
                    List.of(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_BASE, EffectType.ADD, null),
                            efectoPorOpcion(2L, O11_SI_VENDE, ITEM_POS, EffectType.SET_QUANTITY, 3),
                            efectoPorOpcion(3L, O11_SI_VENDE, ITEM_CAJA, EffectType.ADD, null)));

            List<SelectedItem> primera = ConfiguratorResolver.resolve(efectos,
                    marcadas(O11_SI_VENDE));
            Collections.shuffle(efectos, new java.util.Random(42));
            List<SelectedItem> segunda = ConfiguratorResolver.resolve(efectos,
                    marcadas(O11_SI_VENDE));

            assertThat(segunda).isEqualTo(primera);
        }

        @Test
        @DisplayName("sin efectos devuelve vacio sin mirar las respuestas")
        void sin_efectos_devuelve_vacio() {
            assertThat(ConfiguratorResolver.resolve(List.of(), marcadas(O11_SI_VENDE))).isEmpty();
        }

        @Test
        @DisplayName("con efectos null devuelve vacio en vez de reventar")
        void con_efectos_null_devuelve_vacio() {
            assertThat(ConfiguratorResolver.resolve(null, marcadas(O11_SI_VENDE))).isEmpty();
        }

        @Test
        @DisplayName("con respuestas null no dispara nada, pero tampoco revienta")
        void con_respuestas_null_no_dispara_nada() {
            List<ConfiguratorEffect> efectos = List
                    .of(efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null));

            assertThat(ConfiguratorResolver.resolve(efectos, null)).isEmpty();
        }
    }

    /**
     * El caso que da nombre al defecto: <em>marcar más servicios produce un carrito
     * más pequeño</em>.
     *
     * <p>
     * Un efecto añade Inventario por «vendo productos» y otro lo quita por «soy
     * solo estética». Quien marca las dos cosas debería quedarse con Inventario —la
     * pregunta posterior corrige a la anterior, nunca al revés—, y con el orden por
     * {@code id} eso dependía de en qué orden se hubieran insertado las filas.
     * Nadie lo lee como un error de datos: se lee como que el producto no funciona.
     *
     * <p>
     * Los ids de estos casos van <strong>al revés</strong> que las prioridades a
     * propósito. Si el resolvedor volviera a ordenar por {@code id}, estos casos se
     * ponen rojos; con ids alineados pasarían igual con el defecto vivo, que es
     * exactamente lo que hizo que el defecto sobreviviera.
     */
    @Nested
    @DisplayName("la prioridad manda sobre el id")
    class PrioridadSobreId {

        @Test
        @DisplayName("el REMOVE de la pregunta anterior no deshace el ADD de la posterior")
        void el_remove_anterior_no_deshace_el_add_posterior() {
            ConfiguratorEffect remove = efectoPorOpcion(99L, O21_SI_MOSTRADOR, ITEM_POS,
                    EffectType.REMOVE, null, 10);
            ConfiguratorEffect add = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null, 20);

            assertThat(ConfiguratorResolver.resolve(List.of(remove, add),
                    marcadas(O11_SI_VENDE, O21_SI_MOSTRADOR)))
                    .containsExactly(new SelectedItem(ITEM_POS, 1));
        }

        @Test
        @DisplayName("con las prioridades invertidas el articulo si desaparece: la columna es la que decide")
        void con_las_prioridades_invertidas_el_articulo_desaparece() {
            ConfiguratorEffect add = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null, 10);
            ConfiguratorEffect remove = efectoPorOpcion(99L, O21_SI_MOSTRADOR, ITEM_POS,
                    EffectType.REMOVE, null, 20);

            assertThat(ConfiguratorResolver.resolve(List.of(add, remove),
                    marcadas(O11_SI_VENDE, O21_SI_MOSTRADOR))).isEmpty();
        }

        @Test
        @DisplayName("a igualdad de prioridad desempata el id, para que el orden sea total")
        void a_igualdad_de_prioridad_desempata_el_id() {
            ConfiguratorEffect remove = efectoPorOpcion(2L, O11_SI_VENDE, ITEM_POS,
                    EffectType.REMOVE, null, 30);
            ConfiguratorEffect add = efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD,
                    null, 30);

            assertThat(ConfiguratorResolver.resolve(List.of(remove, add), marcadas(O11_SI_VENDE)))
                    .isEmpty();
        }

        @Test
        @DisplayName("entre dos SET_QUANTITY gana el de prioridad mayor, no el del id mayor")
        void entre_dos_set_quantity_gana_el_de_prioridad_mayor() {
            // Discrimina de verdad: por id ganaria el 9 (id 99); por prioridad gana
            // el 4 (prioridad 50). Los dos ordenes dan resultados distintos, que es
            // lo que un caso de orden tiene que conseguir para valer algo.
            // Dos opciones distintas, que es lo unico representable: las dos claves
            // unicas de la tabla prohiben repetir (option_id, catalog_item_id,
            // effect), asi que un caso con la misma opcion dos veces probaria algo
            // que la base no admite.
            ConfiguratorEffect cuatro = efectoPorOpcion(10L, O21_SI_MOSTRADOR, ITEM_POS,
                    EffectType.SET_QUANTITY, 4, 50);
            ConfiguratorEffect nueve = efectoPorOpcion(99L, O11_SI_VENDE, ITEM_POS,
                    EffectType.SET_QUANTITY, 9, 20);

            assertThat(ConfiguratorResolver.resolve(List.of(cuatro, nueve),
                    marcadas(O11_SI_VENDE, O21_SI_MOSTRADOR)))
                    .containsExactly(new SelectedItem(ITEM_POS, 4));
        }
    }

    /**
     * <strong>La invariante de negocio real, que hasta hoy no sostenía
     * nadie.</strong> Marcar un servicio más nunca puede quitar artículos del
     * carrito: cada pregunta corrige a las anteriores y ninguna puede corregir a
     * las siguientes. Es lo que la siembra consigue reservando una decena por
     * pregunta, y lo que el orden por {@code id} rompía.
     *
     * <p>
     * El síntoma no se lee como un error de datos, se lee como que el producto no
     * funciona: el prospecto marca «vendo productos» <em>además de</em> «hago
     * estética» y ve <em>menos</em> módulos en su cotización que si hubiera marcado
     * solo uno.
     *
     * <p>
     * Los ids van deliberadamente al revés que las prioridades. Si alguien
     * devolviera el resolvedor a ordenar por {@code id}, estos casos se ponen rojos
     * — que es exactamente lo que no pasaba antes.
     */
    @Nested
    @DisplayName("marcar mas servicios nunca produce un carrito mas pequeno")
    class MasServiciosNuncaEsMenosCarrito {

        /**
         * El {@code REMOVE} lo dispara la pregunta anterior (decena 10) y el
         * {@code ADD} la posterior (decena 20), que es el reparto que la siembra hace
         * por construcción. Los ids —50 y 1— dicen lo contrario a propósito.
         */
        private final List<ConfiguratorEffect> catalogo = List.of(
                efectoPorOpcion(50L, O21_SI_MOSTRADOR, ITEM_POS, EffectType.REMOVE, null, 10),
                efectoPorOpcion(1L, O11_SI_VENDE, ITEM_POS, EffectType.ADD, null, 20));

        @Test
        @DisplayName("marcar las dos cosas da al menos lo que da marcar solo una")
        void marcar_las_dos_cosas_da_al_menos_lo_que_da_marcar_una() {
            List<SelectedItem> soloEstetica = ConfiguratorResolver.resolve(catalogo,
                    marcadas(O21_SI_MOSTRADOR));
            List<SelectedItem> soloVende = ConfiguratorResolver.resolve(catalogo,
                    marcadas(O11_SI_VENDE));
            List<SelectedItem> lasDos = ConfiguratorResolver.resolve(catalogo,
                    marcadas(O11_SI_VENDE, O21_SI_MOSTRADOR));

            assertThat(lasDos).containsAll(soloEstetica).containsAll(soloVende);
            assertThat(lasDos.size()).isGreaterThanOrEqualTo(soloEstetica.size())
                    .isGreaterThanOrEqualTo(soloVende.size());
        }

        @Test
        @DisplayName("y el articulo que se juega la corrección sigue dentro, no fuera")
        void el_articulo_en_disputa_sigue_dentro() {
            // La asercion de conjunto de arriba pasaria con los dos carritos vacios.
            // Esta nombra el articulo: sin ella, «no encoge» se cumpliria encogiendo
            // los dos a la vez.
            assertThat(ConfiguratorResolver.resolve(catalogo,
                    marcadas(O11_SI_VENDE, O21_SI_MOSTRADOR)))
                    .containsExactly(new SelectedItem(ITEM_POS, 1));
        }

        @Test
        @DisplayName("marcar solo la opcion que quita deja el carrito vacio, que es lo correcto")
        void marcar_solo_la_que_quita_deja_el_carrito_vacio() {
            assertThat(ConfiguratorResolver.resolve(catalogo, marcadas(O21_SI_MOSTRADOR)))
                    .isEmpty();
        }
    }
}
