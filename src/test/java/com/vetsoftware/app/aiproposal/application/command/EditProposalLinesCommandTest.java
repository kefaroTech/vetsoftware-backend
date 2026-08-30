package com.vetsoftware.app.aiproposal.application.command;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Las invariantes de la edicion manual, comprobadas <b>en el comando</b> y no
 * solo en el {@code record} de entrada REST.
 *
 * <p>
 * &#9940; La cota de tamano vive en los dos sitios a proposito. En el request
 * la aplica el binder —y solo si el {@code @RequestBody} lleva {@code @Valid},
 * que es la trampa de la incidencia #135—; aqui la aplica el propio tipo, asi
 * que un llamante que construya el comando sin pasar por HTTP tampoco puede
 * mandar una lista sin cota.
 */
@DisplayName("EditProposalLinesCommand — lo que una edicion puede traer")
class EditProposalLinesCommandTest {

    private static final String TOKEN = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQ";

    private static List<String> codigos(int cuantos) {
        return IntStream.range(0, cuantos).mapToObj(i -> "C" + i).toList();
    }

    @Nested
    @DisplayName("Cota de tamano")
    class CotaDeTamano {

        @Test
        @DisplayName("mas de 40 codigos anadidos no es un comando construible")
        void mas_de_cuarenta_anadidos_no_se_construye() {
            List<String> demasiados = codigos(EditProposalLinesCommand.MAX_CODIGOS_POR_LISTA + 1);

            assertThatThrownBy(
                    () -> new EditProposalLinesCommand(TOKEN, demasiados, List.of(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("more than 40 codes");
        }

        @Test
        @DisplayName("y tampoco mas de 40 retirados: cada uno escribe su propia linea REMOVED")
        void mas_de_cuarenta_retirados_no_se_construye() {
            List<String> demasiados = codigos(EditProposalLinesCommand.MAX_CODIGOS_POR_LISTA + 1);

            assertThatThrownBy(
                    () -> new EditProposalLinesCommand(TOKEN, List.of(), demasiados, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("justo en el tope si se construye: la cota no rechaza de mas")
        void justo_en_el_tope_se_construye() {
            List<String> justos = codigos(EditProposalLinesCommand.MAX_CODIGOS_POR_LISTA);

            assertThatCode(() -> new EditProposalLinesCommand(TOKEN, justos, justos, null))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Lo que ya se exigia")
    class LoQueYaSeExigia {

        @Test
        @DisplayName("una edicion que no anade ni quita nada no es una edicion")
        void una_edicion_vacia_no_es_una_edicion() {
            assertThatThrownBy(
                    () -> new EditProposalLinesCommand(TOKEN, List.of(), List.of(), null))
                    .isInstanceOf(IllegalArgumentException.class);
        }

        @Test
        @DisplayName("las listas nulas se normalizan a vacias antes de comprobar nada")
        void las_listas_nulas_se_normalizan() {
            EditProposalLinesCommand comando = new EditProposalLinesCommand(TOKEN, List.of("CORE"),
                    null, 3L);

            assertThat(comando.removedCodes()).isEmpty();
            assertThat(comando.addedCodes()).containsExactly("CORE");
        }
    }
}
