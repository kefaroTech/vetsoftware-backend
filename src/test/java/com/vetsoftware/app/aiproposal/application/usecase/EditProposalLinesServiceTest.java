package com.vetsoftware.app.aiproposal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.command.EditProposalLinesCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalLineDto;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalRepository;
import com.vetsoftware.app.aiproposal.application.port.out.LegalConsentPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalLinkEmailSender;
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.AiProposalNotFoundException;
import com.vetsoftware.app.aiproposal.domain.LineAction;
import com.vetsoftware.app.aiproposal.domain.LineSource;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.ProposalVersionConflictException;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.domain.TurnType;
import com.vetsoftware.app.aiproposal.testsupport.ProposalMother;
import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * La edicion manual, que reconstruye el carrito con el motor determinista en
 * vez de parchear la lista a mano.
 *
 * <p>
 * El carrito de partida es el del turno inicial: nucleo, caja y la terminal que
 * la caja arrastra.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("EditProposalLinesService — el carrito que deja el cliente")
class EditProposalLinesServiceTest {

    private static final Long TURNO_INICIAL = 71L;

    private static final Long TURNO_NUEVO = 72L;

    private final SellableCatalog catalog = SellableCatalogMother.sinPaquetes();

    @Mock
    private AiProposalRepository repository;

    @Mock
    private SellableCatalogQueryPort catalogQueryPort;

    @Mock
    private LegalConsentPort legalConsent;

    @Mock
    private ProposalLinkEmailSender enlacePorCorreo;

    private EditProposalLinesService service;

    @BeforeEach
    void montar() {
        service = new EditProposalLinesService(catalogQueryPort, new ProposalTurnWriter(repository,
                legalConsent, enlacePorCorreo, ProposalMother.RELOJ),
                new ProposalReader(repository, catalogQueryPort));
    }

    private AiProposal propuesta() {
        return ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA, 5L);
    }

    private EditProposalLinesCommand comando(List<String> anadidos, List<String> retirados,
            Long version) {
        return new EditProposalLinesCommand(ProposalMother.TOKEN, anadidos, retirados, version);
    }

    private void conCarritoInicial() {
        when(repository.findByPublicToken(ProposalMother.TOKEN))
                .thenReturn(Optional.of(propuesta()));
        when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA, ProposalBillingCycle.MONTHLY))
                .thenReturn(Optional.of(catalog));
        when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA)).thenReturn(
                List.of(ProposalMother.turnoInicial(TURNO_INICIAL, "una veterinaria con caja")));
        when(repository.findLinesByTurnId(TURNO_INICIAL)).thenReturn(List.of(
                ProposalMother.lineaDelModelo(TURNO_INICIAL, "CORE", "69000.00", 0),
                ProposalMother.lineaDelModelo(TURNO_INICIAL, "CASH_REGISTER", "46000.00", 1),
                ProposalMother.lineaPorCierre(TURNO_INICIAL, "CAPACITY_TERMINAL", "0.00", 2)));
    }

    private void conEscrituraQueFunciona() {
        when(repository.saveTurn(any())).thenAnswer(invocacion -> {
            ProposalTurn turno = invocacion.getArgument(0);
            turno.setId(TURNO_NUEVO);
            return turno;
        });
        when(repository.save(any())).thenAnswer(invocacion -> invocacion.getArgument(0));
        when(catalogQueryPort.findItemIdsByCode()).thenReturn(ProposalMother.idsPorCodigo());
    }

    private List<String> codigosDeLaVista(ProposalViewDto vista) {
        return vista.lines().stream().map(ProposalLineDto::code).toList();
    }

    private List<ProposalLine> lineasEscritas() {
        ArgumentCaptor<List<ProposalLine>> lineas = ArgumentCaptor.captor();
        verify(repository).saveLines(lineas.capture());
        return lineas.getValue();
    }

    @Nested
    @DisplayName("Reconstruccion determinista")
    class ReconstruccionDeterminista {

        @Test
        @DisplayName("anadir una linea se trae sus dependencias, no solo la linea pedida")
        void anadir_una_linea_se_trae_sus_dependencias() {
            conCarritoInicial();
            conEscrituraQueFunciona();

            ProposalViewDto vista = service.edit(comando(List.of("LAB_IMAGING"), List.of(), null));

            assertThat(codigosDeLaVista(vista)).contains("LAB_IMAGING", "CLINICAL_HISTORY",
                    "SCHEDULING");
        }

        @Test
        @DisplayName("el turno queda con origen CUSTOMER y la respuesta no se marca recalculada")
        void el_turno_queda_con_origen_customer() {
            conCarritoInicial();
            conEscrituraQueFunciona();

            ProposalViewDto vista = service.edit(comando(List.of("VACCINATION"), List.of(), null));

            assertThat(vista.presentation()).isEqualTo(ProposalPresentation.PROPOSAL);
            assertThat(vista.recalculated()).isFalse();

            ArgumentCaptor<ProposalTurn> turno = ArgumentCaptor.captor();
            verify(repository).saveTurn(turno.capture());
            assertThat(turno.getValue().getTurnType()).isEqualTo(TurnType.CUSTOMER_EDIT);
            assertThat(lineasEscritas()).filteredOn(linea -> linea.getAction() == LineAction.ADDED)
                    .allSatisfy(linea -> assertThat(linea.getSource()).isIn(LineSource.CUSTOMER,
                            LineSource.DEPENDENCY_CLOSURE));
        }

        @Test
        @DisplayName("una linea que sigue en el carrito no pierde el motivo del turno anterior")
        void una_linea_que_sigue_no_pierde_su_motivo() {
            conCarritoInicial();
            conEscrituraQueFunciona();

            ProposalViewDto vista = service.edit(comando(List.of("VACCINATION"), List.of(), null));

            assertThat(vista.lines()).filteredOn(linea -> "CORE".equals(linea.code()))
                    .singleElement().satisfies(linea -> assertThat(linea.reason())
                            .isEqualTo("El asistente propuso CORE"));
        }
    }

    @Nested
    @DisplayName("Retiradas efectivas")
    class RetiradasEfectivas {

        @Test
        @DisplayName("retirar algo que estaba en el carrito deja su huella de veto")
        void retirar_algo_del_carrito_deja_su_huella() {
            conCarritoInicial();
            conEscrituraQueFunciona();

            ProposalViewDto vista = service
                    .edit(comando(List.of(), List.of("CASH_REGISTER"), null));

            assertThat(codigosDeLaVista(vista)).doesNotContain("CASH_REGISTER");
            assertThat(lineasEscritas())
                    .filteredOn(linea -> linea.getAction() == LineAction.REMOVED).singleElement()
                    .satisfies(linea -> {
                        assertThat(linea.getItemCode()).isEqualTo("CASH_REGISTER");
                        assertThat(linea.getSource()).isEqualTo(LineSource.CUSTOMER);
                    });
        }

        @Test
        @DisplayName("retirar un codigo que nadie le ofrecio no le deja vetar su propuesta"
                + " futura")
        void retirar_un_codigo_que_no_estaba_no_deja_huella() {
            conCarritoInicial();
            conEscrituraQueFunciona();

            service.edit(comando(List.of(), List.of("LAB_IMAGING"), null));

            assertThat(lineasEscritas()).extracting(ProposalLine::getAction)
                    .doesNotContain(LineAction.REMOVED);
        }

        @Test
        @DisplayName("lo retirado en una edicion anterior no reaparece en la siguiente")
        void lo_retirado_antes_no_reaparece() {
            when(repository.findByPublicToken(ProposalMother.TOKEN))
                    .thenReturn(Optional.of(propuesta()));
            when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY)).thenReturn(Optional.of(catalog));
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA)).thenReturn(
                    List.of(ProposalMother.turnoInicial(TURNO_INICIAL, "una veterinaria con caja"),
                            ProposalMother.turnoDeEdicion(TURNO_NUEVO, 2)));
            when(repository.findLinesByTurnId(TURNO_NUEVO)).thenReturn(List.of(
                    ProposalMother.lineaAnadidaPorElCliente(TURNO_NUEVO, "CORE", "69000.00", 0),
                    ProposalMother.lineaRetiradaPorElCliente(TURNO_NUEVO, "CASH_REGISTER", 1)));
            conEscrituraQueFunciona();

            ProposalViewDto vista = service.edit(comando(List.of("VACCINATION"), List.of(), null));

            assertThat(codigosDeLaVista(vista)).contains("VACCINATION", "CORE")
                    .doesNotContain("CASH_REGISTER", "CAPACITY_TERMINAL");
        }
    }

    @Nested
    @DisplayName("Validaciones")
    class Validaciones {

        @Test
        @DisplayName("una edicion que no anade ni quita nada no llega a existir")
        void una_edicion_vacia_no_llega_a_existir() {
            assertThatThrownBy(() -> comando(List.of(), List.of(), null))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("an edit must add or remove at least one line");
        }

        @Test
        @DisplayName("un token desconocido no llega ni a mirar el catalogo")
        void un_token_desconocido_no_llega_al_catalogo() {
            when(repository.findByPublicToken(ProposalMother.TOKEN)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.edit(comando(List.of("CORE"), List.of(), null)))
                    .isInstanceOf(AiProposalNotFoundException.class);
            verifyNoInteractions(catalogQueryPort);
            verify(repository, never()).saveTurn(any());
        }

        @Test
        @DisplayName("una version vieja es un 409 y no escribe ni un turno")
        void una_version_vieja_no_escribe_nada() {
            when(repository.findByPublicToken(ProposalMother.TOKEN))
                    .thenReturn(Optional.of(propuesta()));

            assertThatThrownBy(() -> service.edit(comando(List.of("CORE"), List.of(), 4L)))
                    .isInstanceOf(ProposalVersionConflictException.class);
            verifyNoInteractions(catalogQueryPort);
            verify(repository, never()).saveTurn(any());
            verify(repository, never()).saveLines(any());
        }
    }
}
