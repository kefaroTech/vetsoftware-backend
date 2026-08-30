package com.vetsoftware.app.aiproposal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.vetsoftware.app.aiproposal.domain.CartLine;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.domain.SellableItemKind;
import com.vetsoftware.app.aiproposal.testsupport.ProposalMother;
import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Las tres traducciones que comparten los cuatro casos de uso.
 *
 * <p>
 * Sin Spring y sin un solo mock: {@code ProposalAssembler} no tiene estado y
 * solo toca dominio, asi que un doble aqui no probaria nada que el propio
 * catalogo no pruebe mejor.
 */
@DisplayName("ProposalAssembler — rehacer el carrito de lo ya escrito")
class ProposalAssemblerTest {

    private static final Long TURNO = 70L;

    private final SellableCatalog catalog = SellableCatalogMother.completo();

    @Nested
    @DisplayName("Reconstruccion")
    class Reconstruccion {

        @Test
        @DisplayName("ordena por sortOrder aunque la consulta las devuelva desordenadas")
        void ordena_por_sort_order() {
            CartResult carrito = ProposalAssembler.reconstruir(List.of(
                    ProposalMother.lineaDelModelo(TURNO, "SCHEDULING", "35000.00", 2),
                    ProposalMother.lineaDelModelo(TURNO, "CORE", "69000.00", 0),
                    ProposalMother.lineaDelModelo(TURNO, "CLINICAL_HISTORY", "49000.00", 1)),
                    catalog);

            assertThat(carrito.lineas()).extracting(CartLine::code).containsExactly("CORE",
                    "CLINICAL_HISTORY", "SCHEDULING");
        }

        @Test
        @DisplayName("el importe sale de la linea congelada y el copy del catalogo vigente")
        void el_importe_sale_de_la_linea_y_el_copy_del_catalogo() {
            CartResult carrito = ProposalAssembler.reconstruir(
                    List.of(ProposalMother.lineaDelModelo(TURNO, "CORE", "51000.00", 0)), catalog);

            CartLine linea = carrito.lineas().getFirst();
            assertThat(linea.unitAmount()).isEqualByComparingTo(new BigDecimal("51000.00"));
            assertThat(linea.name()).isEqualTo("Nucleo: clientes y mascotas");
            assertThat(linea.trialDays()).isEqualTo(30);
            assertThat(linea.kind()).isEqualTo(SellableItemKind.MODULE);
        }

        @Test
        @DisplayName("una alucinacion del modelo se reconstruye sin precio y con el codigo por"
                + " nombre")
        void una_alucinacion_se_reconstruye_sin_precio() {
            CartResult carrito = ProposalAssembler.reconstruir(List
                    .of(ProposalMother.lineaDelModelo(TURNO, "CORE", "69000.00", 0), ProposalMother
                            .lineaRechazada(TURNO, "TELEMEDICINA", LineVerdict.UNKNOWN_CODE, 1)),
                    catalog);

            CartLine alucinada = carrito.lineas().get(1);
            assertThat(alucinada.name()).isEqualTo("TELEMEDICINA");
            assertThat(alucinada.unitAmount()).isNull();
            assertThat(alucinada.currency()).isNull();
            assertThat(alucinada.kind()).isNull();
            assertThat(carrito.descartadas()).isEqualTo(1);
            assertThat(carrito.aceptadas()).extracting(CartLine::code).containsExactly("CORE");
        }

        @Test
        @DisplayName("la huella de lo que el cliente retiro no vuelve al carrito al releer")
        void la_huella_de_lo_retirado_no_vuelve_al_carrito() {
            CartResult carrito = ProposalAssembler.reconstruir(
                    List.of(ProposalMother.lineaAnadidaPorElCliente(TURNO, "CORE", "69000.00", 0),
                            ProposalMother.lineaRetiradaPorElCliente(TURNO, "CASH_REGISTER", 1)),
                    catalog);

            assertThat(carrito.lineas()).extracting(CartLine::code).containsExactly("CORE");
            assertThat(carrito.aceptadas()).extracting(CartLine::code)
                    .doesNotContain("CASH_REGISTER");
        }
    }

    @Nested
    @DisplayName("Divisa")
    class Divisa {

        @Test
        @DisplayName("un carrito sin ninguna linea aceptada toma la divisa del catalogo")
        void sin_lineas_aceptadas_toma_la_divisa_del_catalogo() {
            CartResult carrito = ProposalAssembler.reconstruir(List.of(ProposalMother
                    .lineaRechazada(TURNO, "TELEMEDICINA", LineVerdict.UNKNOWN_CODE, 0)), catalog);

            assertThat(carrito.currency()).isEqualTo(SellableCatalogMother.COP);
        }

        @Test
        @DisplayName("el carrito vacio de OUT_OF_DOMAIN no lleva ni una linea de ejemplo")
        void el_carrito_vacio_no_lleva_lineas() {
            CartResult carrito = ProposalAssembler.vacio(catalog);

            assertThat(carrito.lineas()).isEmpty();
            assertThat(carrito.currency()).isEqualTo(SellableCatalogMother.COP);
            assertThat(carrito.total()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        @DisplayName("un catalogo sin articulos no puede cotizar y lo dice")
        void un_catalogo_sin_articulos_no_puede_cotizar() {
            SellableCatalog vacio = new SellableCatalog(Map.of(), Map.of(), List.of());

            assertThatThrownBy(() -> ProposalAssembler.vacio(vacio))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("an empty catalog cannot price a proposal");
        }
    }

    @Nested
    @DisplayName("Presentacion")
    class Presentacion {

        @ParameterizedTest
        @EnumSource(value = GenerationOutcome.class, names = "SUCCEEDED", mode = EnumSource.Mode.EXCLUDE)
        @DisplayName("las tres degradaciones y el fallo del modelo colapsan en DETERMINISTIC")
        void las_degradaciones_y_el_fallo_colapsan_en_deterministic(GenerationOutcome outcome) {
            assertThat(
                    ProposalAssembler.presentacion(outcome, ProposalDraft.sinLineas(true, false)))
                    .isEqualTo(ProposalPresentation.DETERMINISTIC);
        }

        @Test
        @DisplayName("un negocio ajeno da OUT_OF_DOMAIN")
        void un_negocio_ajeno_da_out_of_domain() {
            assertThat(ProposalAssembler.presentacion(GenerationOutcome.SUCCEEDED,
                    ProposalDraft.sinLineas(true, true)))
                    .isEqualTo(ProposalPresentation.OUT_OF_DOMAIN);
        }

        @Test
        @DisplayName("un texto que el modelo no entendio da NOT_UNDERSTOOD, no OUT_OF_DOMAIN")
        void un_texto_no_entendido_da_not_understood() {
            assertThat(ProposalAssembler.presentacion(GenerationOutcome.SUCCEEDED,
                    ProposalDraft.sinLineas(false, false)))
                    .isEqualTo(ProposalPresentation.NOT_UNDERSTOOD);
        }

        @Test
        @DisplayName("un borrador entendido y en dominio da PROPOSAL")
        void un_borrador_entendido_da_proposal() {
            assertThat(ProposalAssembler.presentacion(GenerationOutcome.SUCCEEDED,
                    ProposalMother.borrador(List.of("CORE"), List.of())))
                    .isEqualTo(ProposalPresentation.PROPOSAL);
        }
    }
}
