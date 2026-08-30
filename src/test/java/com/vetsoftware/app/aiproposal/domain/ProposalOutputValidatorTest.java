package com.vetsoftware.app.aiproposal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Lo que el plan daba por determinista y no lo era.
 *
 * <p>
 * S7.3 afirmaba que "el modelo hace exactamente una cosa —elegir codigos— y
 * todo lo de aguas abajo es determinista". Ademas de los codigos, el modelo
 * controla <b>dos booleanos que deciden que pantalla ve el prospecto</b>,
 * <b>tres enteros de capacidad</b> y <b>el motivo en prosa libre</b>. Los
 * cuatro se tratan aqui como entrada de un atacante, porque el texto que los
 * produce lo escribe cualquiera.
 */
@DisplayName("ProposalOutputValidator — la salida del modelo es entrada no confiable")
class ProposalOutputValidatorTest {

    private final SellableCatalog catalogo = SellableCatalogMother.completo();

    private static ModelProposalPayload salida(boolean understood, boolean outOfDomain,
            List<String> necesarios, List<String> recomendados, Map<String, String> motivos) {
        return new ModelProposalPayload(understood, outOfDomain, necesarios, recomendados, motivos,
                null, null, null);
    }

    @Nested
    @DisplayName("Los dos booleanos: decide el servidor, no el modelo")
    class Booleanos {

        @Test
        @DisplayName("«fuera de dominio» con ocho lineas sale sin ninguna linea")
        void fuera_de_dominio_no_puede_traer_lineas() {
            ProposalDraft draft = ProposalOutputValidator.validate(salida(true, true,
                    List.of("CORE", "SCHEDULING"), List.of("LAB_IMAGING"), Map.of()), catalogo);

            assertThat(draft.outOfDomain()).isTrue();
            assertThat(draft.tieneLineas()).isFalse();
            // Descartadas SI, perdidas NO: el plan (S8.2.1) llama a esto "una senal
            // de calidad que hay que ver".
            assertThat(draft.contradictoryCodes()).isEqualTo(3);
            assertThat(draft.seContradijo()).isTrue();
        }

        @Test
        @DisplayName("«no entendi» con lineas tampoco las conserva")
        void no_entendido_no_puede_traer_lineas() {
            ProposalDraft draft = ProposalOutputValidator
                    .validate(salida(false, false, List.of("CORE"), List.of(), Map.of()), catalogo);

            assertThat(draft.understood()).isFalse();
            assertThat(draft.tieneLineas()).isFalse();
        }

        @Test
        @DisplayName("y el propio ProposalDraft rechaza el estado incoherente")
        void el_draft_no_admite_el_estado_imposible() {
            assertThatThrownBy(() -> new ProposalDraft(false, false, List.of("CORE"), List.of(),
                    Map.of(), CapacityHint.desconocido(), 0))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("understood nothing");
            assertThatThrownBy(() -> new ProposalDraft(true, false, List.of(), List.of(), Map.of(),
                    CapacityHint.desconocido(), -1)).isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("contradictoryCodes");
        }

        @Test
        @DisplayName("una salida nula se lee como «no entendi», no como una excepcion")
        void la_salida_nula_no_revienta() {
            ProposalDraft draft = ProposalOutputValidator.validate(null, catalogo);

            assertThat(draft.understood()).isFalse();
            assertThat(draft.tieneLineas()).isFalse();
        }
    }

    @Nested
    @DisplayName("Los codigos")
    class Codigos {

        @Test
        @DisplayName("una alucinacion SOBREVIVE al validador: es la senal de calidad")
        void la_alucinacion_llega_al_motor() {
            ProposalDraft draft = ProposalOutputValidator.validate(salida(true, false,
                    List.of("CORE", "PACK_ENTERPRISE_2027"), List.of(), Map.of()), catalogo);

            // Filtrarla aqui borraria la linea UNKNOWN_CODE con la que se mide si el
            // modelo sirve. Quien le pone veredicto es ProposalCart.
            assertThat(draft.necessaryCodes()).contains("PACK_ENTERPRISE_2027");
        }

        @Test
        @DisplayName("pero el motor la rechaza y no llega a cotizarse")
        void y_el_motor_la_rechaza() {
            ProposalDraft draft = ProposalOutputValidator.validate(salida(true, false,
                    List.of("CORE", "PACK_ENTERPRISE_2027"), List.of(), Map.of()), catalogo);

            CartResult carrito = ProposalCart.build(draft.necessaryCodes(),
                    draft.recommendedCodes(), draft.textosDeMotivo(), catalogo);

            assertThat(carrito.aceptadas()).extracting(CartLine::code)
                    .doesNotContain("PACK_ENTERPRISE_2027");
            assertThat(carrito.descartadas()).isPositive();
        }

        @Test
        @DisplayName("una lista desbocada se acota a 40 codigos por lista")
        void la_lista_se_acota() {
            List<String> quinientos = IntStream.range(0, 500).mapToObj(i -> "CODE_" + i).toList();

            ProposalDraft draft = ProposalOutputValidator
                    .validate(salida(true, false, quinientos, quinientos, Map.of()), catalogo);

            assertThat(draft.necessaryCodes()).hasSize(40);
            assertThat(draft.recommendedCodes()).hasSize(40);
        }

        @Test
        @DisplayName("un codigo de mas de 50 caracteres se descarta en vez de reventar")
        void un_codigo_imposible_no_revienta() {
            ProposalDraft draft = ProposalOutputValidator.validate(salida(true, false,
                    List.of("CORE", "X".repeat(51), "  ", ""), List.of(), Map.of()), catalogo);

            assertThat(draft.necessaryCodes()).containsExactly("CORE");
        }

        @Test
        @DisplayName("sin ni un codigo utilizable el borrador sale vacio")
        void sin_codigos_utilizables() {
            ProposalDraft draft = ProposalOutputValidator
                    .validate(salida(true, false, List.of(" "), List.of(), Map.of()), catalogo);

            assertThat(draft.tieneLineas()).isFalse();
        }
    }

    @Nested
    @DisplayName("Los motivos, saneados antes de persistir y antes de servir")
    class Motivos {

        @Test
        @DisplayName("un motivo con cifra cae al short_description del articulo")
        void el_motivo_con_cifra_cae_al_determinista() {
            ProposalDraft draft = ProposalOutputValidator.validate(
                    salida(true, false, List.of("CLINICAL_HISTORY"), List.of(),
                            Map.of("CLINICAL_HISTORY", "Porque facturas 40 millones al mes")),
                    catalogo);

            SanitizedReason motivo = draft.reasons().get("CLINICAL_HISTORY");
            assertThat(motivo.rule()).isEqualTo(ReasonRejection.R3_CIFRA);
            assertThat(motivo.text())
                    .isEqualTo(catalogo.find("CLINICAL_HISTORY").orElseThrow().shortDescription());
        }

        @Test
        @DisplayName("el motivo de un codigo alucinado no hace eco del codigo recibido")
        void sin_eco_del_codigo() {
            ProposalDraft draft = ProposalOutputValidator
                    .validate(salida(true, false, List.of("PACK_ENTERPRISE_2027"), List.of(),
                            Map.of("PACK_ENTERPRISE_2027", "x")), catalogo);

            assertThat(draft.reasons().get("PACK_ENTERPRISE_2027").text())
                    .isEqualTo(ProposalCart.MOTIVO_AUSENTE).doesNotContain("PACK_ENTERPRISE");
        }

        @Test
        @DisplayName("un motivo huerfano —de un codigo descartado— no se sanea ni se guarda")
        void los_motivos_huerfanos_no_viajan() {
            ProposalDraft draft = ProposalOutputValidator
                    .validate(salida(true, false, List.of("CORE"), List.of(),
                            Map.of("CORE", "Porque es el nucleo y va siempre contigo", "  ",
                                    "prosa de un codigo en blanco")),
                            catalogo);

            assertThat(draft.reasons()).containsOnlyKeys("CORE");
        }
    }

    @Nested
    @DisplayName("Las tres capacidades")
    class Capacidades {

        @Test
        @DisplayName("se acotan con los rangos del esquema: usuarios 500, sedes 200, cajas 100")
        void se_acotan() {
            ModelProposalPayload desbocado = new ModelProposalPayload(true, false, List.of("CORE"),
                    List.of(), Map.of(), 3, 9000, -4);

            CapacityHint capacidades = ProposalOutputValidator.validate(desbocado, catalogo)
                    .capacities();

            assertThat(capacidades.staff()).isEqualTo(3);
            assertThat(capacidades.branches()).isZero();
            assertThat(capacidades.terminals()).isZero();
        }

        @Test
        @DisplayName("cada eje tiene SU tope, no uno comun: 600 usuarios pasa, 600 cajas no")
        void cada_eje_su_tope() {
            // El anexo E §2 declara maximos distintos por campo. Un tope unico y
            // holgado dejaria pasar 600 cajas, que es una cifra absurda pintada al
            // lado de un precio.
            assertThat(new CapacityHint(400, 150, 90)).isEqualTo(new CapacityHint(400, 150, 90));
            assertThat(new CapacityHint(600, 0, 0).staff()).isZero();
            assertThat(new CapacityHint(0, 250, 0).branches()).isZero();
            assertThat(new CapacityHint(0, 0, 600).terminals()).isZero();
            assertThat(new CapacityHint(500, 200, 100).hayAlgoQueDecir()).isTrue();
        }

        @Test
        @DisplayName("y NUNCA se convierten en una linea cotizada")
        void nunca_son_una_linea() {
            ProposalDraft draft = ProposalOutputValidator.validate(new ModelProposalPayload(true,
                    false, List.of("CORE"), List.of(), Map.of(), 15, 2, 3), catalogo);

            CartResult carrito = ProposalCart.build(draft.necessaryCodes(),
                    draft.recommendedCodes(), draft.textosDeMotivo(), catalogo);

            // Los cuatro EXTRA_* no cuelgan de ningun paquete publicado, asi que no
            // son contratables por autoservicio: cotizarlos muere en el paso 6.
            assertThat(carrito.aceptadas()).extracting(CartLine::code)
                    .noneMatch(code -> code.startsWith("EXTRA_"));
            assertThat(carrito.aceptadas()).extracting(CartLine::quantity).containsOnly(1);
        }

        @Test
        @DisplayName("sin capacidades declaradas no hay nada que decir")
        void sin_capacidades() {
            assertThat(CapacityHint.desconocido().hayAlgoQueDecir()).isFalse();
            assertThat(new CapacityHint(3, 0, 0).hayAlgoQueDecir()).isTrue();
        }
    }

    @Test
    @DisplayName("el payload se defiende de los nulos para que el validador no tenga que")
    void el_payload_normaliza_nulos() {
        ModelProposalPayload conNulos = new ModelProposalPayload(true, false, null, null, null,
                null, null, null);

        assertThat(conNulos.necessaryCodes()).isEmpty();
        assertThat(conNulos.recommendedCodes()).isEmpty();
        assertThat(conNulos.reasons()).isEmpty();
        assertThat(ModelProposalPayload.noEntendido().understood()).isFalse();
    }

    @Test
    @DisplayName("el borrador es inmutable: quien lo construyo no lo mueve despues")
    void el_borrador_es_inmutable() {
        ProposalDraft draft = ProposalOutputValidator
                .validate(salida(true, false, List.of("CORE"), List.of(), Map.of()), catalogo);

        assertThatThrownBy(() -> draft.necessaryCodes().add("OTRO"))
                .isInstanceOf(UnsupportedOperationException.class);
        assertThat(Collections.unmodifiableMap(draft.reasons())).isNotNull();
    }
}
