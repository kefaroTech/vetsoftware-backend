package com.vetsoftware.app.aiproposal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.dto.ProposalLineDto;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalRepository;
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.AiProposalNotFoundException;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.ProposalVersionConflictException;
import com.vetsoftware.app.aiproposal.domain.ProspectText;
import com.vetsoftware.app.aiproposal.testsupport.ProposalMother;
import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Lo que los cuatro casos de uso leen de una propuesta ya escrita.
 *
 * <p>
 * Se mockean <b>solo los dos puertos</b>; las entidades que devuelven se
 * construyen de verdad, que es lo unico que hace creible una afirmacion sobre
 * "lo que el cliente quito a mano".
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProposalReader — el carrito vigente y lo que el cliente decidio")
class ProposalReaderTest {

    private static final Long TURNO_1 = 71L;

    private static final Long TURNO_2 = 72L;

    private static final Long TURNO_3 = 73L;

    @Mock
    private AiProposalRepository repository;

    @Mock
    private SellableCatalogQueryPort catalogQueryPort;

    private ProposalReader reader;

    @BeforeEach
    void montar() {
        reader = new ProposalReader(repository, catalogQueryPort);
    }

    @Nested
    @DisplayName("Lectura por token")
    class LecturaPorToken {

        @Test
        @DisplayName("un token que no existe da el mismo 404 que uno caducado")
        void un_token_que_no_existe_da_404() {
            when(repository.findByPublicToken("noexiste")).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reader.exigir("noexiste"))
                    .isInstanceOf(AiProposalNotFoundException.class)
                    .hasMessageContaining("AI proposal not found");
        }

        @Test
        @DisplayName("devuelve la cabecera que el token senala")
        void devuelve_la_cabecera_que_el_token_senala() {
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            when(repository.findByPublicToken(ProposalMother.TOKEN))
                    .thenReturn(Optional.of(propuesta));

            assertThat(reader.exigir(ProposalMother.TOKEN)).isSameAs(propuesta);
        }

        @Test
        @DisplayName("una tarifa que ya no se puede leer es un estado imposible, no un carrito"
                + " vacio")
        void una_tarifa_ilegible_es_un_estado_imposible() {
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> reader.catalogo(propuesta))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("no longer readable");
        }
    }

    @Nested
    @DisplayName("Bloqueo optimista")
    class BloqueoOptimista {

        @Test
        @DisplayName("una peticion sin version declarada no se rechaza")
        void sin_version_declarada_no_se_rechaza() {
            AiProposal propuesta = ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA,
                    3L);

            assertThatCode(() -> reader.exigirVersion(propuesta, null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("una peticion con la version vigente pasa")
        void con_la_version_vigente_pasa() {
            AiProposal propuesta = ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA,
                    3L);

            assertThatCode(() -> reader.exigirVersion(propuesta, 3L)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("una peticion con una version vieja es un conflicto, no una sobreescritura")
        void con_una_version_vieja_es_conflicto() {
            AiProposal propuesta = ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA,
                    3L);

            assertThatThrownBy(() -> reader.exigirVersion(propuesta, 2L))
                    .isInstanceOf(ProposalVersionConflictException.class)
                    .hasMessageContaining("Reload it and try again");
        }
    }

    @Nested
    @DisplayName("Lineas vigentes")
    class LineasVigentes {

        @Test
        @DisplayName("un turno que fallo sin escribir ni una linea no vacia el carrito anterior")
        void un_turno_que_fallo_no_vacia_el_carrito_anterior() {
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA))
                    .thenReturn(List.of(ProposalMother.turnoInicial(TURNO_1, "una veterinaria"),
                            ProposalMother.turnoDeRefinamiento(TURNO_2, 2, "tambien peluqueria")));
            when(repository.findLinesByTurnId(TURNO_2)).thenReturn(List.of());
            when(repository.findLinesByTurnId(TURNO_1)).thenReturn(
                    List.of(ProposalMother.lineaDelModelo(TURNO_1, "CORE", "69000.00", 0)));

            assertThat(reader.lineasVigentes(propuesta)).extracting(ProposalLine::getItemCode)
                    .containsExactly("CORE");
        }

        @Test
        @DisplayName("una propuesta sin turnos no tiene carrito y no consulta ninguna linea")
        void sin_turnos_no_hay_carrito() {
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA))
                    .thenReturn(List.of());

            assertThat(reader.lineasVigentes(propuesta)).isEmpty();
        }
    }

    @Nested
    @DisplayName("Historial de turnos")
    class HistorialDeTurnos {

        @Test
        @DisplayName("el siguiente numero de turno es el mayor mas uno, no el tamano de la lista")
        void el_siguiente_numero_es_el_mayor_mas_uno() {
            assertThat(reader
                    .siguienteNumeroDeTurno(List.of(ProposalMother.turnoInicial(TURNO_1, "hola"),
                            ProposalMother.turnoDeRefinamiento(TURNO_3, 7, "y esto"))))
                    .isEqualTo(8);
        }

        @Test
        @DisplayName("la primera propuesta abre el turno numero 1")
        void la_primera_propuesta_abre_el_turno_uno() {
            assertThat(reader.siguienteNumeroDeTurno(List.of())).isEqualTo(1);
        }

        @Test
        @DisplayName("una edicion manual no cuenta como turno de modelo")
        void una_edicion_manual_no_cuenta_como_turno_de_modelo() {
            List<ProposalTurn> turnos = List.of(ProposalMother.turnoInicial(TURNO_1, "hola"),
                    ProposalMother.turnoDeEdicion(TURNO_2, 2),
                    ProposalMother.turnoDeRefinamiento(TURNO_3, 3, "y esto"));

            assertThat(reader.turnosDeModelo(turnos)).isEqualTo(2);
        }

        @Test
        @DisplayName("al modelo se le mandan todos los textos del cliente en orden, no solo el"
                + " ultimo")
        void se_mandan_todos_los_textos_en_orden() {
            List<ProposalTurn> turnos = List.of(
                    ProposalMother.turnoInicial(TURNO_1, "somos una veterinaria de barrio"),
                    ProposalMother.turnoDeEdicion(TURNO_2, 2),
                    ProposalMother.turnoDeRefinamiento(TURNO_3, 3, "tambien hacemos peluqueria"));

            assertThat(reader.textosDelCliente(turnos)).containsExactly(
                    ProspectText.of("somos una veterinaria de barrio"),
                    ProspectText.of("tambien hacemos peluqueria"));
        }
    }

    @Nested
    @DisplayName("Edicion soberana del cliente")
    class EdicionSoberana {

        @Test
        @DisplayName("solo lo que el cliente retiro a mano cuenta como retirada")
        void solo_lo_que_el_cliente_retiro_cuenta() {
            List<ProposalTurn> turnos = List.of(ProposalMother.turnoInicial(TURNO_1, "hola"),
                    ProposalMother.turnoDeEdicion(TURNO_2, 2));
            when(repository.findLinesByTurnId(TURNO_1)).thenReturn(List
                    .of(ProposalMother.lineaDelModelo(TURNO_1, "CASH_REGISTER", "46000.00", 0)));
            when(repository.findLinesByTurnId(TURNO_2)).thenReturn(
                    List.of(ProposalMother.lineaAnadidaPorElCliente(TURNO_2, "CORE", "69000.00", 0),
                            ProposalMother.lineaRetiradaPorElCliente(TURNO_2, "CASH_REGISTER", 1)));

            assertThat(reader.retiradasPorElCliente(turnos)).containsExactly("CASH_REGISTER");
        }

        @Test
        @DisplayName("solo lo que el cliente anadio a mano y quedo aceptado se conserva")
        void solo_lo_que_el_cliente_anadio_se_conserva() {
            List<ProposalTurn> turnos = List.of(ProposalMother.turnoDeEdicion(TURNO_2, 2));
            when(repository.findLinesByTurnId(TURNO_2)).thenReturn(List.of(
                    ProposalMother.lineaAnadidaPorElCliente(TURNO_2, "LAB_IMAGING", "45000.00", 0),
                    ProposalMother.lineaDelModelo(TURNO_2, "CORE", "69000.00", 1),
                    ProposalMother.lineaPorCierre(TURNO_2, "SCHEDULING", "35000.00", 2),
                    ProposalMother.lineaRetiradaPorElCliente(TURNO_2, "VACCINATION", 3)));

            assertThat(reader.anadidasPorElCliente(turnos)).containsExactly("LAB_IMAGING");
        }
    }

    @Nested
    @DisplayName("Tope de refinamientos")
    class TopeDeRefinamientos {

        @ParameterizedTest
        @CsvSource({"1,3", "2,2", "3,1", "4,0", "5,0"})
        @DisplayName("quedan tres refinamientos tras el turno inicial y ninguno tras el cuarto")
        void quedan_tres_tras_el_inicial_y_ninguno_tras_el_cuarto(int turnosDeModelo,
                int esperados) {
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA))
                    .thenReturn(turnosDeModelo(turnosDeModelo));

            assertThat(reader.refinamientosRestantes(propuesta)).isEqualTo(esperados);
        }

        private List<ProposalTurn> turnosDeModelo(int cuantos) {
            List<ProposalTurn> turnos = new java.util.ArrayList<>();
            turnos.add(ProposalMother.turnoInicial(TURNO_1, "somos una veterinaria"));
            for (int numero = 2; numero <= cuantos; numero++)
                turnos.add(ProposalMother.turnoDeRefinamiento(TURNO_1 + numero, numero, "y esto"));
            return List.copyOf(turnos);
        }
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("sin clave declarada no se busca nada: no hay carrera que resolver")
        void sin_clave_no_se_busca_nada() {
            assertThat(reader.porIdempotencia(ProposalMother.CORREO, null)).isEmpty();

            verifyNoInteractions(repository);
        }

        @Test
        @DisplayName("con clave se busca por el par correo + clave")
        void con_clave_se_busca_por_el_par() {
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.of(propuesta));

            assertThat(reader.porIdempotencia(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .contains(propuesta);
        }
    }

    @Nested
    @DisplayName("Vista releida")
    class VistaReleida {

        @Test
        @DisplayName("una propuesta con carrito se relee como PROPOSAL y no serializa lo"
                + " descartado")
        void con_carrito_se_relee_como_proposal() {
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY))
                    .thenReturn(Optional.of(SellableCatalogMother.sinPaquetes()));
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA))
                    .thenReturn(List.of(ProposalMother.turnoInicial(TURNO_1, "una veterinaria")));
            when(repository.findLinesByTurnId(TURNO_1)).thenReturn(List.of(
                    ProposalMother.lineaDelModelo(TURNO_1, "CORE", "69000.00", 0), ProposalMother
                            .lineaRechazada(TURNO_1, "TELEMEDICINA", LineVerdict.UNKNOWN_CODE, 1)));

            ProposalViewDto vista = reader.vista(propuesta, false);

            assertThat(vista.presentation()).isEqualTo(ProposalPresentation.PROPOSAL);
            assertThat(vista.lines()).extracting(ProposalLineDto::code).containsExactly("CORE");
            assertThat(vista.discardedLines()).isEqualTo(1);
            assertThat(vista.recalculated()).isFalse();
            assertThat(vista.refinementsLeft()).isEqualTo(3);
        }

        @Test
        @DisplayName("una propuesta sin ni una linea aceptada se relee como OUT_OF_DOMAIN")
        void sin_lineas_aceptadas_se_relee_como_out_of_domain() {
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY))
                    .thenReturn(Optional.of(SellableCatalogMother.sinPaquetes()));
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA))
                    .thenReturn(List.of(ProposalMother.turnoInicial(TURNO_1, "vendo bicicletas")));
            when(repository.findLinesByTurnId(TURNO_1)).thenReturn(List.of());

            ProposalViewDto vista = reader.vista(propuesta, false);

            assertThat(vista.presentation()).isEqualTo(ProposalPresentation.OUT_OF_DOMAIN);
            assertThat(vista.lines()).isEmpty();
        }
    }
}
