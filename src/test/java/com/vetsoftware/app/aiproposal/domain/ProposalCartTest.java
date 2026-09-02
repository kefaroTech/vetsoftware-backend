package com.vetsoftware.app.aiproposal.domain;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * El motor determinista, que es donde la feature deja de depender del modelo.
 *
 * <p>
 * Todo lo que se comprueba aqui son afirmaciones del plan que un lector no
 * puede verificar leyendo el codigo: que la cadena de {@code REQUIRES} se
 * cierra entera y no un salto, que {@code recomendados} no entra al carrito,
 * que las tres formas de "no se puede cotizar" producen tres veredictos
 * distintos hacia dentro y un solo entero hacia fuera, y que la caja arrastra
 * su terminal.
 */
@DisplayName("ProposalCart — el motor determinista")
class ProposalCartTest {

    private static final SellableCatalog CATALOGO = SellableCatalogMother.completo();

    private static final String MOTIVO = "Porque atiendes consultas medicas.";

    private static CartResult carrito(List<String> necesarios, List<String> recomendados) {
        Map<String, String> motivos = java.util.stream.Stream
                .concat(necesarios.stream(), recomendados.stream()).distinct()
                .collect(java.util.stream.Collectors.toMap(c -> c, c -> MOTIVO));
        return ProposalCart.build(necesarios, recomendados, motivos, CATALOGO);
    }

    private static List<String> codigos(List<CartLine> lineas) {
        return lineas.stream().map(CartLine::code).toList();
    }

    @Nested
    @DisplayName("Validacion")
    class Validacion {

        /**
         * <b>Los tres rechazos son distinguibles hacia dentro y solo hacia dentro.</b>
         * El veredicto es la senal con la que se mide si el modelo sirve; hacia fuera
         * los tres son el mismo entero, porque cinco veredictos serializados serian un
         * oraculo de cinco valores sobre el catalogo interno, y el texto de entrada lo
         * escribe el atacante.
         */
        @Test
        @DisplayName("distingue codigo inventado, no publicado y no contratable")
        void los_tres_rechazos_tienen_veredicto_propio() {
            CartResult resultado = carrito(
                    List.of("PACK_ENTERPRISE_2027", "DRAFT_MODULE", "EXTRA_USER"), List.of());

            assertThat(resultado.lineas()).filteredOn(l -> !l.verdict().esAceptado())
                    .extracting(CartLine::code, CartLine::verdict).containsExactly(
                            org.assertj.core.groups.Tuple.tuple("PACK_ENTERPRISE_2027",
                                    LineVerdict.UNKNOWN_CODE),
                            org.assertj.core.groups.Tuple.tuple("DRAFT_MODULE",
                                    LineVerdict.NOT_SELLABLE),
                            org.assertj.core.groups.Tuple.tuple("EXTRA_USER",
                                    LineVerdict.NOT_SELF_SERVICE));
            assertThat(resultado.descartadas()).isEqualTo(3);
            assertThat(codigos(resultado.aceptadas())).containsExactly("CORE");
        }

        /**
         * <b>El codigo inventado se guarda verbatim.</b> Es el dato que mide la
         * alucinacion del modelo: normalizarlo o sustituirlo por un centinela
         * destruiria la unica senal de calidad que sobrevive a la anonimizacion.
         */
        @Test
        @DisplayName("el codigo inventado se conserva tal como lo dijo el modelo")
        void el_codigo_inventado_se_conserva_verbatim() {
            CartResult resultado = carrito(List.of("pack_enterprise_2027"), List.of());

            assertThat(resultado.lineas()).filteredOn(l -> !l.verdict().esAceptado())
                    .singleElement().extracting(CartLine::code).isEqualTo("pack_enterprise_2027");
        }

        @Test
        @DisplayName("un codigo repetido se marca DUPLICATE y no se cotiza dos veces")
        void el_repetido_no_se_cotiza_dos_veces() {
            CartResult resultado = carrito(List.of("SCHEDULING", "SCHEDULING"), List.of());

            assertThat(codigos(resultado.aceptadas())).containsExactly("SCHEDULING", "CORE");
            assertThat(resultado.lineas()).filteredOn(l -> l.verdict() == LineVerdict.DUPLICATE)
                    .hasSize(1);
        }
    }

    @Nested
    @DisplayName("Cierre de dependencias")
    class CierreDeDependencias {

        /**
         * <b>La cadena entera, no un salto.</b> {@code LAB_IMAGING} arrastra
         * {@code CLINICAL_HISTORY} y este a su vez {@code SCHEDULING}: un cierre de un
         * solo nivel dejaria al cliente contratando algo que no puede funcionar, y lo
         * descubriria en el paso 6, despues de verificar el correo.
         */
        @Test
        @DisplayName("cierra REQUIRES en cadena hasta el final")
        void cierra_la_cadena_completa() {
            CartResult resultado = carrito(List.of("LAB_IMAGING"), List.of());

            assertThat(codigos(resultado.aceptadas())).containsExactlyInAnyOrder("LAB_IMAGING",
                    "CORE", "CLINICAL_HISTORY", "SCHEDULING");
            assertThat(resultado.aceptadas())
                    .filteredOn(l -> l.source() == LineSource.DEPENDENCY_CLOSURE)
                    .extracting(CartLine::code)
                    .containsExactlyInAnyOrder("CORE", "CLINICAL_HISTORY", "SCHEDULING");
        }

        @Test
        @DisplayName("el nucleo entra aunque el modelo no lo pida")
        void el_nucleo_entra_siempre() {
            assertThat(codigos(carrito(List.of(), List.of()).aceptadas())).containsExactly("CORE");
        }

        /**
         * <b>La regla 3 de S2.3.</b> Quien contrata la caja sin
         * {@code CAPACITY_TERMINAL} se queda con techo cero de terminales y no puede
         * abrir la primera. Aqui la caja llega <em>pedida</em> por el modelo.
         */
        @Test
        @DisplayName("la caja arrastra su terminal cuando la pide el modelo")
        void la_caja_arrastra_el_terminal() {
            assertThat(codigos(carrito(List.of("CASH_REGISTER"), List.of()).aceptadas()))
                    .contains("CASH_REGISTER", "CAPACITY_TERMINAL");
        }

        /**
         * <b>Y tambien cuando la caja llega por el cierre, no por el modelo.</b> Es la
         * razon por la que la regla del terminal viaja dentro del BFS y no como un paso
         * posterior: un paso al final solo cubriria el primer caso, y el defecto no se
         * veria hasta que un catalogo distinto hiciera de la caja una dependencia.
         */
        @Test
        @DisplayName("la caja arrastra su terminal tambien si llega por dependencia")
        void la_caja_arrastra_el_terminal_tambien_por_cierre() {
            SellableCatalog conCajaDependiente = new SellableCatalog(CATALOGO.items(),
                    Map.of("LAB_IMAGING", List.of("CASH_REGISTER")), List.of(), CATALOGO.nucleo());

            CartResult resultado = ProposalCart.build(List.of("LAB_IMAGING"), List.of(),
                    Map.of("LAB_IMAGING", MOTIVO), conCajaDependiente);

            assertThat(codigos(resultado.aceptadas())).contains("CASH_REGISTER",
                    "CAPACITY_TERMINAL");
        }
    }

    /**
     * &#9940; <b>El paso 3 no puede volver a quedarse callado.</b> El defecto de
     * produccion no fue «eligio mal el nucleo»: fue que, al elegir uno que no se
     * podia cotizar, <b>no dejo rastro de nada</b> —ni linea aceptada, ni linea de
     * rechazo, ni log—. El carrito salia vacio con {@code discardedLines = 0}, que
     * es peor que un error: afirma que el modelo no descarto nada cuando lo que
     * paso es que el backend no llego a mirar.
     */
    @Nested
    @DisplayName("El nucleo no puede desaparecer en silencio")
    class ElNucleoNoDesaparece {

        /**
         * El motor recibe un nucleo <b>garantizado cotizable</b> por el constructor de
         * {@link SellableCatalog}, asi que el paso 3 siempre escribe su linea. Este
         * test fija esa consecuencia sobre el catalogo de laboratorio: sin lineas del
         * modelo, el carrito no puede salir vacio.
         */
        @Test
        @DisplayName("un borrador sin lineas produce el nucleo, nunca un carrito mudo")
        void un_borrador_sin_lineas_no_produce_un_carrito_mudo() {
            CartResult resultado = carrito(List.of(), List.of());

            assertThat(resultado.lineas()).isNotEmpty();
            assertThat(codigos(resultado.aceptadas())).containsExactly("CORE");
        }

        /**
         * &#9940; <b>La forma exacta del defecto, cerrada por tipo.</b> Un catalogo
         * cuyo nucleo no se puede cotizar —una capacidad del minimo estructural que no
         * cuelga de ningun {@code BUNDLE}, o el modulo retirado— ya no existe: lo
         * rechaza el constructor. El motor no necesita defenderse de un estado que no
         * puede recibir, que es la diferencia entre una guarda repartida por los
         * consumidores y una invariante.
         */
        @Test
        @DisplayName("un catalogo cuyo nucleo no se puede cotizar no llega al motor")
        void un_nucleo_no_cotizable_no_llega_al_motor() {
            SellableItem nucleoRetirado = new SellableItem("CORE", "Nucleo",
                    "Retirado del catalogo", SellableItemKind.MODULE, false, true, 30,
                    new java.math.BigDecimal("69000.00"), new java.math.BigDecimal("19.00"), "COP");

            org.assertj.core.api.Assertions
                    .assertThatThrownBy(() -> new SellableCatalog(Map.of("CORE", nucleoRetirado),
                            Map.of(), List.of(), nucleoRetirado))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("the catalog core must be quotable: CORE");
        }
    }

    @Nested
    @DisplayName("Recomendados")
    class Recomendados {

        /**
         * <b>La decision de S4.4, escrita como test.</b> Un recomendado se sirve
         * aparte, sin sumar al total y sin cerrar sus dependencias: cerrar dependencias
         * de algo que nadie pidio es como un carrito de 6 lineas se convierte en uno de
         * 10, y deshacerlo despues es una migracion de datos y una conversacion con un
         * cliente que pago lo que no queria.
         */
        @Test
        @DisplayName("no entran al carrito, no suman y no cierran dependencias")
        void los_recomendados_no_entran_al_carrito() {
            CartResult resultado = carrito(List.of("VACCINATION"), List.of("LAB_IMAGING"));

            assertThat(codigos(resultado.aceptadas())).containsExactlyInAnyOrder("VACCINATION",
                    "CORE");
            assertThat(codigos(resultado.recomendaciones())).containsExactly("LAB_IMAGING");
            assertThat(codigos(resultado.aceptadas())).doesNotContain("CLINICAL_HISTORY",
                    "SCHEDULING");
            assertThat(resultado.subtotal()).isEqualByComparingTo("94000.00");
        }

        @Test
        @DisplayName("un recomendado que no se puede cotizar se descarta igual")
        void el_recomendado_pasa_la_misma_validacion() {
            CartResult resultado = carrito(List.of("VACCINATION"), List.of("EXTRA_USER"));

            assertThat(resultado.recomendaciones()).isEmpty();
            assertThat(resultado.descartadas()).isEqualTo(1);
        }
    }

    /**
     * &#9940; <b>La invariante que sostiene la escritura del turno, y que no se ve
     * mirando el carrito.</b> {@code ai_proposal_lines} tiene
     * {@code uq_ai_proposal_lines_code} sobre {@code (turn_id, item_code)} y
     * {@code ProposalTurnWriter} persiste <b>todas</b> las lineas del carrito en un
     * solo {@code saveLines}, rechazos incluidos. Dos lineas con el mismo codigo no
     * son una fealdad del resultado: son un {@code DataIntegrityViolationException}
     * que revierte la transaccion, deja el turno {@code PENDING} para siempre y
     * devuelve un 500 por una propuesta que ya se cobro al modelo.
     *
     * <p>
     * El defecto estaba en que las guardas de cierre preguntaban por
     * {@code enCarrito} -los codigos aceptados- en vez de por {@code vistos} -los
     * evaluados-. Un requisito <b>rechazado</b> no esta en el primero y si en el
     * segundo, asi que se volvia a evaluar y la segunda evaluacion escribia una
     * linea {@code DUPLICATE} con el mismo codigo.
     */
    @Nested
    @DisplayName("Ninguna linea repite codigo: es la clave unica de la tabla")
    class SinCodigosRepetidos {

        /**
         * Un modulo publicado que <b>requiere</b> uno sin publicar. No hace falta un
         * modelo estropeado ni una alucinacion: basta con despublicar un articulo del
         * que otro depende, que es una edicion normal de catalogo.
         */
        private static final SellableCatalog CON_REQUISITO_RECHAZADO = new SellableCatalog(
                Map.of("CORE", SellableCatalogMother.nucleo(), "PADRE",
                        SellableCatalogMother.modulo("PADRE", "Modulo padre", 30_000, 0),
                        "DRAFT_MODULE", SellableCatalogMother.moduloEnBorrador()),
                Map.of("PADRE", List.of("DRAFT_MODULE")), List.of(),
                SellableCatalogMother.nucleo());

        @Test
        @DisplayName("un requisito rechazado que ademas pidio el modelo no genera una segunda"
                + " linea con el mismo codigo")
        void un_requisito_rechazado_no_se_evalua_dos_veces() {
            CartResult resultado = ProposalCart.build(List.of("DRAFT_MODULE", "PADRE"), List.of(),
                    Map.of("DRAFT_MODULE", MOTIVO, "PADRE", MOTIVO), CON_REQUISITO_RECHAZADO);

            assertThat(codigos(resultado.lineas())).doesNotHaveDuplicates();
            assertThat(resultado.lineas()).filteredOn(linea -> "DRAFT_MODULE".equals(linea.code()))
                    .singleElement().satisfies(linea -> assertThat(linea.verdict())
                            .isEqualTo(LineVerdict.NOT_SELLABLE));
        }

        /**
         * La tercera guarda del cierre, y la que mas facil se arma sola: la regla del
         * terminal de caja. Basta con que {@code CAPACITY_TERMINAL} deje de estar
         * publicado —o de ser autoservicio— para que la caja lo arrastre por segunda
         * vez despues de que el modelo ya lo hubiera propuesto.
         */
        @Test
        @DisplayName("la terminal que la caja arrastra no genera una segunda linea si ya se"
                + " evaluo y quedo rechazada")
        void la_terminal_rechazada_no_se_evalua_dos_veces() {
            SellableCatalog conTerminalNoPublicada = new SellableCatalog(
                    Map.of("CORE", SellableCatalogMother.nucleo(), "CASH_REGISTER",
                            SellableCatalogMother.modulo("CASH_REGISTER", "Caja", 46_000, 14),
                            "CAPACITY_TERMINAL",
                            new SellableItem("CAPACITY_TERMINAL", "Terminal", "Un punto de venta",
                                    SellableItemKind.CAPACITY, false, true, 0,
                                    new java.math.BigDecimal("0.00"),
                                    new java.math.BigDecimal("0.19"), "COP")),
                    Map.of(), List.of(), SellableCatalogMother.nucleo());

            CartResult resultado = ProposalCart.build(List.of("CAPACITY_TERMINAL", "CASH_REGISTER"),
                    List.of(), Map.of("CAPACITY_TERMINAL", MOTIVO, "CASH_REGISTER", MOTIVO),
                    conTerminalNoPublicada);

            assertThat(codigos(resultado.lineas())).doesNotHaveDuplicates();
            assertThat(resultado.lineas())
                    .filteredOn(linea -> "CAPACITY_TERMINAL".equals(linea.code())).singleElement()
                    .satisfies(linea -> assertThat(linea.verdict())
                            .isEqualTo(LineVerdict.NOT_SELLABLE));
        }

        @Test
        @DisplayName("un codigo que el modelo pone a la vez en necesarios y en recomendados"
                + " produce UNA linea, no dos")
        void un_codigo_en_las_dos_listas_produce_una_linea() {
            CartResult resultado = carrito(List.of("SCHEDULING"), List.of("SCHEDULING"));

            assertThat(codigos(resultado.lineas())).doesNotHaveDuplicates();
        }

        @Test
        @DisplayName("y el carrito normal, con cierre de dependencias y terminal, tampoco repite")
        void el_carrito_normal_no_repite_codigos() {
            assertThat(
                    codigos(carrito(List.of("LAB_IMAGING", "CASH_REGISTER"), List.of("VACCINATION"))
                            .lineas()))
                    .doesNotHaveDuplicates();
        }
    }

    @Nested
    @DisplayName("Precio")
    class Precio {

        /**
         * <b>El IVA se calcula por articulo y no se cablea al 19 %.</b> Aqui todos
         * tributan igual, asi que lo que este caso fija es la suma; lo que impide
         * cablear la tasa es que viaja en cada linea.
         */
        @Test
        @DisplayName("suma base, IVA por linea y total, con la divisa dentro")
        void suma_base_impuesto_y_total() {
            CartResult resultado = carrito(List.of("CLINICAL_HISTORY"), List.of());

            assertThat(codigos(resultado.aceptadas())).containsExactlyInAnyOrder("CLINICAL_HISTORY",
                    "SCHEDULING", "CORE");
            assertThat(resultado.subtotal()).isEqualByComparingTo("153000.00");
            assertThat(resultado.impuestos()).isEqualByComparingTo("29070.00");
            assertThat(resultado.total()).isEqualByComparingTo("182070.00");
            assertThat(resultado.currency()).isEqualTo("COP");
        }

        /**
         * <b>La dimension que la sustitucion silenciosa de la v1 se llevaba por
         * delante.</b> Con tres modulos que dan prueba, el primer periodo no se cobra
         * nada; el dia que entre uno sin prueba, la diferencia se ve.
         */
        @Test
        @DisplayName("el primer periodo no cobra las lineas con prueba gratis")
        void el_primer_periodo_descuenta_las_pruebas() {
            CartResult conPrueba = carrito(List.of("CLINICAL_HISTORY"), List.of());
            assertThat(conPrueba.totalPrimerPeriodo()).isEqualByComparingTo("0.00");

            CartResult conTerminal = carrito(List.of("CASH_REGISTER"), List.of());
            assertThat(conTerminal.aceptadas()).anyMatch(l -> !l.gratisElPrimerPeriodo());
            assertThat(conTerminal.totalPrimerPeriodo()).isEqualByComparingTo("0.00");
        }

        @Test
        @DisplayName("un carrito solo con el nucleo sigue teniendo divisa")
        void el_carrito_minimo_tiene_divisa() {
            assertThat(carrito(List.of(), List.of()).currency()).isEqualTo("COP");
        }
    }
}
