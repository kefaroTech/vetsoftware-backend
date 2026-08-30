package com.vetsoftware.app.aiproposal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;

import com.vetsoftware.app.aiproposal.domain.CartLine;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.LineSource;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalCart;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalOutputValidator;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.infrastructure.ai.ModelAccessNotEnabledInvoker;
import com.vetsoftware.app.aiproposal.testsupport.CasoDorado;
import com.vetsoftware.app.aiproposal.testsupport.CatalogoComercial2026;
import com.vetsoftware.app.aiproposal.testsupport.GoldenSetDeClinicasColombianas;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * El <b>golden set</b> del embudo publico: doce clinicas veterinarias
 * colombianas y la propuesta que cada una tiene que recibir.
 *
 * <p>
 * &#9940; <b>Lo que fija cada caso es la propuesta, no el camino por el que se
 * llega.</b> Hoy el acceso al modelo no esta habilitado —
 * {@link ModelAccessNotEnabledInvoker} devuelve {@code isAvailable() == false}
 * y todo borrador sale sin lineas—, asi que la lectura del texto libre la
 * aporta el propio caso, como dato. El dia que se encienda Bedrock esa lectura
 * la producira el modelo de verdad y <b>este test no cambia</b>: lo que compara
 * sigue siendo la propuesta esperada contra la que sale del motor determinista.
 * Un golden set escrito al reves —fijando la llamada al modelo— habria que
 * tirarlo entero ese dia, que es justo cuando mas falta hace.
 *
 * <p>
 * <b>Se ejercita la misma maquina que ejecuta produccion, y en el mismo
 * orden</b> ({@link GenerateProposalService#generate}): validar la salida del
 * modelo, y despues armar el carrito —o dejarlo vacio si el negocio esta fuera
 * de dominio—. Ver {@link #propuesta(CasoDorado)}, que es una copia deliberada
 * de esas cuatro lineas: si el caso de uso cambia el orden, este test deja de
 * hablar del mismo sistema y hay que moverlo con el.
 *
 * <p>
 * <b>Contra el catalogo real</b> ({@link CatalogoComercial2026}) y no contra el
 * de laboratorio: en aquel {@code CLINICAL_HISTORY} exige {@code SCHEDULING} y
 * en el real solo lo recomienda, asi que media docena de estas propuestas
 * saldrian con una linea de mas que ningun prospecto va a ver.
 */
@DisplayName("Golden set — doce clinicas colombianas y la propuesta que reciben")
class PropuestaGoldenSetTest {

    private static final SellableCatalog CATALOGO = CatalogoComercial2026.catalogo();

    static List<CasoDorado> casos() {
        return GoldenSetDeClinicasColombianas.casos();
    }

    /**
     * Las cuatro lineas de {@link GenerateProposalService#generate} que van del
     * texto a la propuesta. La rama de {@code outOfDomain} es la del caso de uso y
     * no una simplificacion: un negocio fuera de dominio no recibe ni el nucleo
     * como punto de partida.
     */
    private static CartResult propuesta(CasoDorado caso) {
        ProposalDraft draft = ProposalOutputValidator.validate(caso.lectura(), CATALOGO);
        return draft.outOfDomain()
                ? ProposalAssembler.vacio(CATALOGO)
                : ProposalCart.build(draft.necessaryCodes(), draft.recommendedCodes(),
                        draft.textosDeMotivo(), CATALOGO);
    }

    private static ProposalPresentation pantalla(CasoDorado caso) {
        return ProposalAssembler.presentacion(GenerationOutcome.SUCCEEDED,
                ProposalOutputValidator.validate(caso.lectura(), CATALOGO));
    }

    private static List<String> codigos(List<CartLine> lineas) {
        return lineas.stream().map(CartLine::code).toList();
    }

    private static CartLine linea(CartResult carrito, String code) {
        return carrito.lineas().stream().filter(l -> l.code().equals(code)).findFirst().orElseThrow(
                () -> new AssertionError("el carrito no trae ninguna linea con el codigo " + code));
    }

    @Nested
    @DisplayName("La propuesta esperada")
    class PropuestaEsperada {

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.aiproposal.application.usecase."
                + "PropuestaGoldenSetTest#casos")
        @DisplayName("el carrito cotiza exactamente los codigos que el caso declara")
        void el_carrito_cotiza_los_codigos_del_caso(CasoDorado caso) {
            assertThat(codigos(propuesta(caso).aceptadas()))
                    .as("propuesta esperada para: %s", caso.texto())
                    .containsExactlyInAnyOrderElementsOf(caso.aceptados());
        }

        /**
         * Un recomendado pasa el mismo filtro de catalogo que un necesario, pero no
         * entra al carrito ni suma al total: fundirlos convierte un carrito de seis
         * lineas en uno de diez, y la interfaz las presentaria ya elegidas.
         */
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.aiproposal.application.usecase."
                + "PropuestaGoldenSetTest#casos")
        @DisplayName("lo recomendado sale aparte y no se cuenta como aceptado")
        void lo_recomendado_sale_aparte(CasoDorado caso) {
            assertThat(codigos(propuesta(caso).recomendaciones()))
                    .containsExactlyInAnyOrderElementsOf(caso.recomendados());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.aiproposal.application.usecase."
                + "PropuestaGoldenSetTest#casos")
        @DisplayName("lo descartado conserva su codigo y su veredicto")
        void lo_descartado_conserva_su_veredicto(CasoDorado caso) {
            Map<String, LineVerdict> rechazadas = propuesta(caso).lineas().stream()
                    .filter(l -> !l.verdict().esAceptado())
                    .collect(Collectors.toMap(CartLine::code, CartLine::verdict));

            assertThat(rechazadas).containsExactlyInAnyOrderEntriesOf(caso.rechazados());
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.aiproposal.application.usecase."
                + "PropuestaGoldenSetTest#casos")
        @DisplayName("la pantalla que ve el prospecto es la que el caso declara")
        void la_pantalla_es_la_que_declara_el_caso(CasoDorado caso) {
            assertThat(pantalla(caso)).isEqualTo(caso.pantalla());
        }
    }

    @Nested
    @DisplayName("Invariantes de todo el conjunto")
    class Invariantes {

        @Test
        @DisplayName("son doce casos y ninguno repite nombre")
        void son_doce_casos_sin_nombres_repetidos() {
            assertThat(casos()).hasSize(12).extracting(CasoDorado::nombre).doesNotHaveDuplicates();
        }

        /**
         * &#9940; Una linea aceptada sin importe o sin divisa es el defecto que dejo
         * mudos sobre la moneda a cincuenta y dos de los cincuenta y tres DTO de dinero
         * de este backend. Aqui no es construible a proposito, y esto lo comprueba
         * sobre las doce propuestas a la vez.
         */
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.aiproposal.application.usecase."
                + "PropuestaGoldenSetTest#casos")
        @DisplayName("ninguna linea cotizada sale sin importe ni sin divisa")
        void ninguna_linea_cotizada_sale_muda(CasoDorado caso) {
            assertThat(propuesta(caso).aceptadas()).allSatisfy(linea -> {
                assertThat(linea.unitAmount()).as("importe de %s", linea.code()).isNotNull();
                assertThat(linea.currency()).as("divisa de %s", linea.code())
                        .isEqualTo(CatalogoComercial2026.COP);
            });
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.aiproposal.application.usecase."
                + "PropuestaGoldenSetTest#casos")
        @DisplayName("el importe de cada linea es el del catalogo publicado, nunca otro")
        void el_importe_es_el_del_catalogo(CasoDorado caso) {
            assertThat(propuesta(caso).aceptadas())
                    .allSatisfy(linea -> assertThat(linea.unitAmount())
                            .as("importe de %s", linea.code()).isEqualByComparingTo(
                                    CATALOGO.find(linea.code()).orElseThrow().unitAmount()));
        }

        /**
         * El nucleo entra siempre, lo pidiera el modelo o no —es la administracion de
         * la propia cuenta y sin el no hay producto—. La unica excepcion es el negocio
         * fuera de dominio, que no recibe ni una linea.
         */
        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.aiproposal.application.usecase."
                + "PropuestaGoldenSetTest#casos")
        @DisplayName("el nucleo entra en todas las propuestas salvo la que esta fuera de dominio")
        void el_nucleo_entra_siempre_salvo_fuera_de_dominio(CasoDorado caso) {
            boolean fueraDeDominio = caso.pantalla() == ProposalPresentation.OUT_OF_DOMAIN;

            assertThat(codigos(propuesta(caso).aceptadas()).contains("CORE"))
                    .isEqualTo(!fueraDeDominio);
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.vetsoftware.app.aiproposal.application.usecase."
                + "PropuestaGoldenSetTest#casos")
        @DisplayName("la propuesta se cotiza en pesos colombianos")
        void la_propuesta_se_cotiza_en_pesos(CasoDorado caso) {
            assertThat(propuesta(caso).currency()).isEqualTo(CatalogoComercial2026.COP);
        }
    }

    @Nested
    @DisplayName("Las dos reglas de negocio que el golden set fija")
    class ReglasDeNegocio {

        /**
         * &#9940; <b>REGLA 1: la peluqueria exige servicios y tarifas.</b> Una estetica
         * cobra por tamano y raza, y el unico sitio del catalogo donde vive un
         * tarifario por dimension es {@code SERVICES}. Vender {@code GROOMING} suelto
         * entrega un modulo que no se puede tarifar: la pantalla de servicios y tarifas
         * no esta. El modelo <b>no</b> lo pide en ninguno de los dos casos; lo arrastra
         * el cierre del arco sembrado en el changeset 380.
         */
        @Test
        @DisplayName("la peluqueria arrastra servicios y tarifas aunque el modelo no los pida")
        void la_peluqueria_arrastra_servicios_y_tarifas() {
            CasoDorado caso = GoldenSetDeClinicasColombianas.clinicaConPeluqueria();
            assertThat(caso.lectura().necessaryCodes())
                    .as("el modelo no pidio SERVICES: tiene que entrar por el cierre")
                    .doesNotContain("SERVICES");

            CartResult carrito = propuesta(caso);

            assertThat(codigos(carrito.aceptadas())).contains("GROOMING", "SERVICES");
            assertThat(linea(carrito, "SERVICES").source())
                    .as("lo arrastro el grafo, no la persona")
                    .isEqualTo(LineSource.DEPENDENCY_CLOSURE);
        }

        @Test
        @DisplayName("la peluqueria sin veterinario tambien arrastra tarifas, y NO historia"
                + " clinica")
        void la_peluqueria_sola_arrastra_tarifas_y_no_historia_clinica() {
            CartResult carrito = propuesta(
                    GoldenSetDeClinicasColombianas.peluqueriaSinVeterinario());

            assertThat(codigos(carrito.aceptadas())).containsExactlyInAnyOrder("CORE", "GROOMING",
                    "SERVICES");
        }

        /**
         * &#9940; <b>REGLA 2: el modulo de laboratorio e imagen tambien sirve a quien
         * manda las muestras fuera.</b> Lo que resuelve es guardar el resultado y la
         * imagen dentro del expediente del paciente, no procesar la muestra. Leerlo
         * como "solo para quien tiene laboratorio propio" deja fuera a la mayor parte
         * de las clinicas pequenas del pais.
         */
        @Test
        @DisplayName("el modulo de laboratorio e imagen entra aunque la muestra se procese fuera")
        void el_laboratorio_entra_aunque_la_muestra_se_procese_fuera() {
            CasoDorado caso = GoldenSetDeClinicasColombianas.mandaLasMuestrasFuera();
            assertThat(caso.texto()).contains("no tengo laboratorio propio");

            CartResult carrito = propuesta(caso);

            assertThat(codigos(carrito.aceptadas())).contains("LAB_IMAGING");
            assertThat(linea(carrito, "CLINICAL_HISTORY").source())
                    .as("un resultado sin expediente donde colgarlo no resuelve nada")
                    .isEqualTo(LineSource.DEPENDENCY_CLOSURE);
        }
    }

    @Nested
    @DisplayName("Los dos casos adversarios")
    class Adversarios {

        /**
         * &#9940; El modelo elige codigos; el catalogo decide cuales existen. Un codigo
         * alucinado se conserva <b>verbatim</b> en su linea rechazada —es el dato que
         * mide la calidad del modelo— y no lleva precio, porque no se cotiza.
         */
        @Test
        @DisplayName("un codigo que no esta en el catalogo no se cotiza y se conserva verbatim")
        void un_codigo_inventado_no_se_cotiza() {
            CartResult carrito = propuesta(
                    GoldenSetDeClinicasColombianas.pideModulosQueNoExisten());

            assertThat(codigos(carrito.aceptadas())).containsExactly("CORE");
            assertThat(linea(carrito, "TELEMEDICINE").verdict())
                    .isEqualTo(LineVerdict.UNKNOWN_CODE);
            assertThat(linea(carrito, "TELEMEDICINE").unitAmount()).isNull();
            assertThat(linea(carrito, "EXTRA_USER").verdict())
                    .as("existe en el catalogo pero no se contrata por autoservicio")
                    .isEqualTo(LineVerdict.NOT_SELF_SERVICE);
        }

        /**
         * &#9940; <b>El precio no sale del modelo.</b> El texto ordena descuento y
         * total en cero; la propuesta se cotiza contra el catalogo publicado y el
         * motivo con cifras lo sustituye el saneador por la descripcion del articulo,
         * que es lo que impide que la prosa del atacante llegue a la pantalla del
         * prospecto.
         */
        @Test
        @DisplayName("el descuento que exige el prospecto no cambia ni un peso del catalogo")
        void el_descuento_exigido_no_cambia_el_precio() {
            CartResult carrito = propuesta(GoldenSetDeClinicasColombianas.pideDescuento());

            assertThat(linea(carrito, "CORE").unitAmount()).isEqualByComparingTo("69000.00");
            assertThat(linea(carrito, "CLINICAL_HISTORY").unitAmount())
                    .isEqualByComparingTo("49000.00");
            assertThat(carrito.subtotal()).isEqualByComparingTo("118000.00");
            assertThat(carrito.total()).as("el total lo calcula el motor, no el texto")
                    .isGreaterThan(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("el motivo con cifras y dinero se sustituye por la descripcion del catalogo")
        void el_motivo_con_dinero_se_sustituye() {
            CartResult carrito = propuesta(GoldenSetDeClinicasColombianas.pideDescuento());

            assertThat(linea(carrito, "CLINICAL_HISTORY").reason())
                    .isEqualTo(CATALOGO.find("CLINICAL_HISTORY").orElseThrow().shortDescription())
                    .doesNotContain("descuento").doesNotContain("$");
        }
    }

    /**
     * Lo que ocurre <b>hoy</b>, con el acceso al modelo sin habilitar. Va aparte de
     * los doce casos a proposito: es un estado de la cuenta de AWS, no una lectura
     * del texto del prospecto, y el dia que Bedrock se encienda este bloque es el
     * unico que se toca.
     */
    @Nested
    @DisplayName("Hoy, sin acceso al modelo")
    class SinAccesoAlModelo {

        @Test
        @DisplayName("el invocador declara que no esta disponible, asi que nadie llama a Bedrock")
        void el_invocador_no_esta_disponible() {
            assertThat(new ModelAccessNotEnabledInvoker().isAvailable()).isFalse();
        }

        /**
         * Sin lectura del texto libre, el carrito es el determinista: el nucleo y lo
         * que arrastre su cierre. Es una propuesta correcta, solo que mas pobre.
         */
        @Test
        @DisplayName("un borrador sin lineas produce la propuesta determinista, no una vacia")
        void un_borrador_sin_lineas_produce_la_propuesta_determinista() {
            ProposalDraft sinLineas = ProposalDraft.sinLineas(false, false);

            CartResult carrito = ProposalCart.build(sinLineas.necessaryCodes(),
                    sinLineas.recommendedCodes(), sinLineas.textosDeMotivo(), CATALOGO);

            assertThat(codigos(carrito.aceptadas())).containsExactly("CORE");
            assertThat(linea(carrito, "CORE").source()).isEqualTo(LineSource.DEPENDENCY_CLOSURE);
        }

        /**
         * &#9940; Las tres degradaciones colapsan en una sola pantalla, y eso es el
         * control: un anonimo con {@code curl} no puede distinguir el tope de gasto de
         * la palanca apagada, porque saberlo le diria cuando se agoto el presupuesto
         * diario de la plataforma.
         */
        @ParameterizedTest(name = "GenerationOutcome.{0}")
        @EnumSource(value = GenerationOutcome.class, names = "SUCCEEDED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("cualquier desenlace que no sea exito sale como una sola pantalla degradada")
        void toda_degradacion_sale_igual(GenerationOutcome desenlace) {
            assertThat(ProposalAssembler.presentacion(desenlace,
                    ProposalDraft.sinLineas(false, false)))
                    .isEqualTo(ProposalPresentation.DETERMINISTIC);
        }
    }

    /**
     * El golden set no puede referirse a codigos que el catalogo no tiene: si
     * alguien retira un articulo de las semillas, el caso queda hablando de un
     * producto que ya no existe y su expectativa deja de significar nada.
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("casos")
    @DisplayName("todo codigo que el caso espera cotizado existe en el catalogo publicado")
    void los_codigos_esperados_existen_en_el_catalogo(CasoDorado caso) {
        assertThat(caso.aceptados()).allSatisfy(code -> assertThat(CATALOGO.find(code))
                .as("el catalogo no tiene %s", code).isPresent());
    }

    /**
     * Y no puede haber dos casos con la misma lectura: dos casos identicos son un
     * caso, y el conteo de doce dejaria de medir doce situaciones distintas.
     */
    @Test
    @DisplayName("no hay dos casos con la misma lectura del modelo")
    void no_hay_dos_casos_con_la_misma_lectura() {
        assertThat(casos()).extracting(CasoDorado::lectura).doesNotHaveDuplicates();
    }

    @Test
    @DisplayName("entre las doce propuestas se cotiza todo el catalogo vendible a mano")
    void entre_las_doce_se_cotiza_todo_el_catalogo() {
        List<String> cotizados = casos().stream().map(PropuestaGoldenSetTest::propuesta)
                .flatMap(carrito -> carrito.aceptadas().stream()).map(CartLine::code).distinct()
                .toList();

        assertThat(cotizados).contains("CORE", "SCHEDULING", "CLINICAL_HISTORY",
                "VACCINATION_DEWORMING", "HOSPITALIZATION", "SURGERY", "LAB_IMAGING", "GROOMING",
                "SERVICES", "CASH_REGISTER", "INVENTORY", "PURCHASES", "OPEN_ACCOUNTS",
                "ELECTRONIC_INVOICING", "CAPACITY_TERMINAL");
    }
}
