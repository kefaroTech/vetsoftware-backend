package com.vetsoftware.app.aiproposal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationResult;
import com.vetsoftware.app.aiproposal.application.dto.ProposalLinkEmail;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalRepository;
import com.vetsoftware.app.aiproposal.application.port.out.LegalConsentPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalLinkEmailSender;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.CartResult;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.LegalDocumentVersionRef;
import com.vetsoftware.app.aiproposal.domain.LineAction;
import com.vetsoftware.app.aiproposal.domain.LineSource;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalCart;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalStatus;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.domain.TurnStatus;
import com.vetsoftware.app.aiproposal.domain.TurnType;
import com.vetsoftware.app.aiproposal.testsupport.ProposalMother;
import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Las dos transacciones de la generacion, y el correo que sale despues.
 *
 * <p>
 * &#9940; <b>La evidencia de consentimiento se afirma contra el id que devolvio
 * el {@code save}</b>, no contra la entidad que entro: es la unica forma de que
 * el test note si alguien saca la aceptacion de TX1 y la deja en el caso de
 * uso, donde una propuesta podria quedar persistida sin autorizacion probable.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProposalTurnWriter — TX1, TX2 y el enlace por correo")
class ProposalTurnWriterTest {

    private static final Long ID_TURNO = 70L;

    private static final LegalDocumentVersionRef AVISO = new LegalDocumentVersionRef(
            ProposalMother.ID_AVISO, "PRIVACY_NOTICE", 3, true);

    private static final LegalDocumentVersionRef TERMINOS = new LegalDocumentVersionRef(11L,
            "TERMS", 2, false);

    private final SellableCatalog catalog = SellableCatalogMother.sinPaquetes();

    @Mock
    private AiProposalRepository repository;

    @Mock
    private LegalConsentPort legalConsent;

    @Mock
    private ProposalLinkEmailSender enlacePorCorreo;

    private ProposalTurnWriter writer;

    @BeforeEach
    void montar() {
        writer = new ProposalTurnWriter(repository, legalConsent, enlacePorCorreo,
                ProposalMother.RELOJ);
    }

    private void conCabeceraGuardada() {
        when(repository.save(any())).thenAnswer(invocacion -> {
            AiProposal propuesta = invocacion.getArgument(0);
            propuesta.setId(ProposalMother.ID_PROPUESTA);
            return propuesta;
        });
    }

    private void conTurnoGuardado() {
        when(repository.saveTurn(any())).thenAnswer(invocacion -> {
            ProposalTurn turno = invocacion.getArgument(0);
            turno.setId(ID_TURNO);
            return turno;
        });
    }

    private ProposalTurn turnoPendiente(TurnType tipo, int numero) {
        ProposalTurn turno = ProposalTurn.pendienteDeModelo(ProposalMother.ID_PROPUESTA, numero,
                tipo, "somos una veterinaria de barrio", ProposalMother.MODELO,
                ProposalMother.PROMPT, null, ProposalMother.RELOJ);
        turno.setId(ID_TURNO);
        return turno;
    }

    private CartResult carritoConNucleo() {
        return ProposalCart.build(List.of("CORE"), List.of(),
                Map.of("CORE", "Es el nucleo del producto"), catalog);
    }

    private List<ProposalLine> lineasEscritas() {
        ArgumentCaptor<List<ProposalLine>> lineas = ArgumentCaptor.captor();
        verify(repository).saveLines(lineas.capture());
        return lineas.getValue();
    }

    @Nested
    @DisplayName("TX1 — cabecera, consentimiento y turno pendiente")
    class AperturaDePropuesta {

        @Test
        @DisplayName("la aceptacion se escribe contra el id que devolvio el save de la cabecera")
        void la_aceptacion_se_escribe_contra_el_id_de_la_cabecera() {
            conCabeceraGuardada();
            conTurnoGuardado();
            AiProposal nueva = ProposalMother.propuesta(null);

            writer.abrirPropuesta(nueva, "somos una veterinaria", ProposalMother.MODELO,
                    ProposalMother.PROMPT, ProposalMother.CLAVE, List.of(AVISO, TERMINOS), "ip",
                    "ua");

            ArgumentCaptor<Long> documento = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<Long> sujeto = ArgumentCaptor.forClass(Long.class);
            ArgumentCaptor<LocalDateTime> momento = ArgumentCaptor.forClass(LocalDateTime.class);
            verify(legalConsent, times(2)).recordAcceptance(documento.capture(), sujeto.capture(),
                    momento.capture(), eq("ip"), eq("ua"));
            assertThat(documento.getAllValues()).containsExactly(ProposalMother.ID_AVISO, 11L);
            assertThat(sujeto.getAllValues()).containsOnly(ProposalMother.ID_PROPUESTA);
            assertThat(momento.getAllValues())
                    .containsOnly(LocalDateTime.now(ProposalMother.RELOJ));
        }

        @Test
        @DisplayName("las aceptaciones viajan en el mismo metodo transaccional que la cabecera")
        void las_aceptaciones_viajan_en_la_misma_transaccion() throws NoSuchMethodException {
            assertThat(ProposalTurnWriter.class.getDeclaredMethod("abrirPropuesta",
                    AiProposal.class, String.class, String.class, String.class, String.class,
                    List.class, String.class, String.class).getAnnotation(Transactional.class))
                    .isNotNull();
        }

        @Test
        @DisplayName("el turno inicial nace PENDING, con numero 1 y con el texto del prospecto")
        void el_turno_inicial_nace_pendiente() {
            conCabeceraGuardada();
            conTurnoGuardado();

            ProposalTurnWriter.TurnoAbierto abierto = writer.abrirPropuesta(
                    ProposalMother.propuesta(null), "somos una veterinaria", ProposalMother.MODELO,
                    ProposalMother.PROMPT, ProposalMother.CLAVE, List.of(AVISO), "ip", "ua");

            assertThat(abierto.turn().getStatus()).isEqualTo(TurnStatus.PENDING);
            assertThat(abierto.turn().getTurnNumber()).isEqualTo(1);
            assertThat(abierto.turn().getTurnType()).isEqualTo(TurnType.MODEL_INITIAL);
            assertThat(abierto.turn().getInputText()).isEqualTo("somos una veterinaria");
            assertThat(abierto.turn().getModelId()).isEqualTo(ProposalMother.MODELO);
            assertThat(abierto.turn().getClientRequestId()).isEqualTo(ProposalMother.CLAVE);
            assertThat(abierto.proposal().getId()).isEqualTo(ProposalMother.ID_PROPUESTA);
        }

        @Test
        @DisplayName("el refinamiento solo abre el turno: la cabecera ya existe y no se reescribe")
        void el_refinamiento_solo_abre_el_turno() {
            conTurnoGuardado();
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);

            ProposalTurnWriter.TurnoAbierto abierto = writer.abrirRefinamiento(propuesta, 2,
                    "tambien hacemos peluqueria", ProposalMother.MODELO, ProposalMother.PROMPT,
                    null);

            assertThat(abierto.turn().getTurnType()).isEqualTo(TurnType.MODEL_REFINEMENT);
            assertThat(abierto.turn().getTurnNumber()).isEqualTo(2);
            assertThat(abierto.proposal()).isSameAs(propuesta);
            verify(repository, never()).save(any());
            verifyNoInteractions(legalConsent);
        }
    }

    @Nested
    @DisplayName("TX2 — cierre del turno y escritura de las lineas")
    class CierreDeTurno {

        @Test
        @DisplayName("un turno con respuesta se cierra con exito y suma los tokens a la cabecera")
        void un_turno_con_respuesta_se_cierra_con_exito() {
            conCabeceraGuardada();
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            ProposalTurn turno = turnoPendiente(TurnType.MODEL_INITIAL, 1);

            AiProposal guardada = writer.cerrarTurno(propuesta, turno,
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())),
                    carritoConNucleo(), List.of(), ProposalMother.idsPorCodigo());

            assertThat(turno.getStatus()).isEqualTo(TurnStatus.SUCCEEDED);
            assertThat(turno.getInputTokens()).isEqualTo(1200);
            assertThat(turno.getOutputTokens()).isEqualTo(340);
            assertThat(turno.getFailureCode()).isNull();
            assertThat(guardada.getTotalInputTokens()).isEqualTo(1200);
            assertThat(guardada.getTotalOutputTokens()).isEqualTo(340);
            assertThat(guardada.getTurnCount()).isEqualTo(1);
            assertThat(guardada.getStatus()).isEqualTo(ProposalStatus.PROPOSED);
        }

        @Test
        @DisplayName("un fallo del modelo cierra el turno FAILED con su codigo y sin sumar tokens")
        void un_fallo_del_modelo_cierra_el_turno_con_su_codigo() {
            conCabeceraGuardada();
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            ProposalTurn turno = turnoPendiente(TurnType.MODEL_INITIAL, 1);

            AiProposal guardada = writer.cerrarTurno(propuesta, turno,
                    ProposalMother.falloDelModelo(), carritoConNucleo(), List.of(),
                    ProposalMother.idsPorCodigo());

            assertThat(turno.getStatus()).isEqualTo(TurnStatus.FAILED);
            assertThat(turno.getFailureCode()).isEqualTo("TIMEOUT");
            assertThat(turno.getLatencyMs()).isEqualTo(4200);
            assertThat(guardada.getTotalInputTokens()).isZero();
            assertThat(guardada.getTurnCount()).isEqualTo(1);
        }

        @Test
        @DisplayName("una degradacion sin codigo propio deja el nombre del desenlace como codigo")
        void una_degradacion_sin_codigo_deja_el_nombre_del_desenlace() {
            conCabeceraGuardada();
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            ProposalTurn turno = turnoPendiente(TurnType.MODEL_INITIAL, 1);

            writer.cerrarTurno(propuesta, turno,
                    ProposalGenerationResult.degradado(GenerationOutcome.DEGRADED_SPEND_CAP),
                    carritoConNucleo(), List.of(), ProposalMother.idsPorCodigo());

            assertThat(turno.getFailureCode()).isEqualTo("DEGRADED_SPEND_CAP");
        }

        @Test
        @DisplayName("un carrito sin ni una linea aceptada no marca la propuesta como propuesta")
        void un_carrito_vacio_no_marca_la_propuesta() {
            conCabeceraGuardada();
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            ProposalTurn turno = turnoPendiente(TurnType.MODEL_INITIAL, 1);

            AiProposal guardada = writer.cerrarTurno(propuesta, turno,
                    ProposalMother.exito(ProposalDraft.sinLineas(true, true)),
                    ProposalAssembler.vacio(catalog), List.of(), ProposalMother.idsPorCodigo());

            assertThat(guardada.getStatus()).isEqualTo(ProposalStatus.DRAFT);
        }

        @Test
        @DisplayName("las lineas con las que el modelo se contradijo se escriben, sin resolver y"
                + " con un motivo que no hace eco del codigo")
        void las_lineas_contradichas_se_escriben_sin_resolver() {
            conCabeceraGuardada();
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);
            ProposalTurn turno = turnoPendiente(TurnType.MODEL_INITIAL, 1);

            writer.cerrarTurno(propuesta, turno,
                    ProposalMother.exito(ProposalDraft.sinLineas(true, true,
                            List.of("CASH_REGISTER", "LAB_IMAGING"))),
                    ProposalAssembler.vacio(catalog), List.of("CASH_REGISTER", "LAB_IMAGING"),
                    ProposalMother.idsPorCodigo());

            List<ProposalLine> escritas = lineasEscritas();
            assertThat(escritas).extracting(ProposalLine::getItemCode)
                    .containsExactly("CASH_REGISTER", "LAB_IMAGING");
            assertThat(escritas).allSatisfy(linea -> {
                assertThat(linea.getVerdict()).isEqualTo(LineVerdict.NOT_SELLABLE);
                assertThat(linea.getCatalogItemId()).isNull();
                assertThat(linea.getReason()).isEqualTo(ProposalTurnWriter.MOTIVO_CONTRADICCION)
                        .doesNotContain(linea.getItemCode());
            });
        }
    }

    @Nested
    @DisplayName("Enlace por correo sin transaccion activa")
    class EnlaceSinTransaccion {

        @Test
        @DisplayName("sin transaccion que sincronizar el enlace sale igual, no se pierde")
        void sin_transaccion_el_enlace_sale_igual() {
            conCabeceraGuardada();
            ProposalTurn turno = turnoPendiente(TurnType.MODEL_INITIAL, 1);

            writer.cerrarTurno(ProposalMother.propuesta(ProposalMother.ID_PROPUESTA), turno,
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())),
                    carritoConNucleo(), List.of(), ProposalMother.idsPorCodigo());

            ArgumentCaptor<ProposalLinkEmail> enlace = ArgumentCaptor.captor();
            verify(enlacePorCorreo).send(enlace.capture());
            assertThat(enlace.getValue().contactEmail()).isEqualTo(ProposalMother.CORREO);
            assertThat(enlace.getValue().publicToken()).isEqualTo(ProposalMother.TOKEN);
        }

        @Test
        @DisplayName("un refinamiento no manda un segundo correo con el mismo enlace")
        void un_refinamiento_no_manda_un_segundo_correo() {
            conCabeceraGuardada();
            ProposalTurn turno = turnoPendiente(TurnType.MODEL_REFINEMENT, 2);

            writer.cerrarTurno(ProposalMother.propuesta(ProposalMother.ID_PROPUESTA), turno,
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())),
                    carritoConNucleo(), List.of(), ProposalMother.idsPorCodigo());

            verifyNoInteractions(enlacePorCorreo);
        }

        @Test
        @DisplayName("un carrito vacio no manda a nadie a una pantalla que no le dice nada")
        void un_carrito_vacio_no_manda_enlace() {
            conCabeceraGuardada();
            ProposalTurn turno = turnoPendiente(TurnType.MODEL_INITIAL, 1);

            writer.cerrarTurno(ProposalMother.propuesta(ProposalMother.ID_PROPUESTA), turno,
                    ProposalMother.exito(ProposalDraft.sinLineas(true, true)),
                    ProposalAssembler.vacio(catalog), List.of(), ProposalMother.idsPorCodigo());

            verifyNoInteractions(enlacePorCorreo);
        }

        @Test
        @DisplayName("una propuesta sin correo de contacto no tiene a quien escribir")
        void una_propuesta_sin_correo_no_manda_enlace() {
            conCabeceraGuardada();
            ProposalTurn turno = turnoPendiente(TurnType.MODEL_INITIAL, 1);

            writer.cerrarTurno(ProposalMother.propuesta(ProposalMother.ID_PROPUESTA, null), turno,
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())),
                    carritoConNucleo(), List.of(), ProposalMother.idsPorCodigo());

            verifyNoInteractions(enlacePorCorreo);
        }
    }

    @Nested
    @DisplayName("Enlace por correo tras el commit")
    class EnlaceTrasCommit {

        @BeforeEach
        void abrirSincronizacion() {
            TransactionSynchronizationManager.initSynchronization();
        }

        @AfterEach
        void cerrarSincronizacion() {
            TransactionSynchronizationManager.clearSynchronization();
        }

        @Test
        @DisplayName("el correo no sale dentro de la transaccion, solo despues del commit")
        void el_correo_sale_despues_del_commit() {
            conCabeceraGuardada();
            ProposalTurn turno = turnoPendiente(TurnType.MODEL_INITIAL, 1);

            writer.cerrarTurno(ProposalMother.propuesta(ProposalMother.ID_PROPUESTA), turno,
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())),
                    carritoConNucleo(), List.of(), ProposalMother.idsPorCodigo());

            verifyNoInteractions(enlacePorCorreo);

            TransactionSynchronizationManager.getSynchronizations()
                    .forEach(TransactionSynchronization::afterCommit);

            verify(enlacePorCorreo).send(any());
        }

        @Test
        @DisplayName("un correo que revienta no convierte una propuesta ya guardada en un 500")
        void un_correo_que_revienta_no_tumba_la_propuesta() {
            conCabeceraGuardada();
            ProposalTurn turno = turnoPendiente(TurnType.MODEL_INITIAL, 1);
            doThrow(new IllegalStateException("Resend no responde")).when(enlacePorCorreo)
                    .send(any());

            writer.cerrarTurno(ProposalMother.propuesta(ProposalMother.ID_PROPUESTA), turno,
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())),
                    carritoConNucleo(), List.of(), ProposalMother.idsPorCodigo());

            List<TransactionSynchronization> sincronizaciones = TransactionSynchronizationManager
                    .getSynchronizations();

            assertThatCode(() -> sincronizaciones.forEach(TransactionSynchronization::afterCommit))
                    .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Edicion manual")
    class EdicionManual {

        @Test
        @DisplayName("el turno de edicion nace cerrado, sin modelo y sin consumir tokens")
        void el_turno_de_edicion_nace_cerrado() {
            conCabeceraGuardada();
            conTurnoGuardado();
            AiProposal propuesta = ProposalMother.propuesta(ProposalMother.ID_PROPUESTA);

            AiProposal guardada = writer.escribirEdicion(propuesta, 3, carritoConNucleo(),
                    List.of(), ProposalMother.idsPorCodigo(), null);

            ArgumentCaptor<ProposalTurn> turno = ArgumentCaptor.captor();
            verify(repository).saveTurn(turno.capture());
            assertThat(turno.getValue().getTurnType()).isEqualTo(TurnType.CUSTOMER_EDIT);
            assertThat(turno.getValue().getStatus()).isEqualTo(TurnStatus.SUCCEEDED);
            assertThat(turno.getValue().getModelId()).isNull();
            assertThat(turno.getValue().getTurnNumber()).isEqualTo(3);
            assertThat(guardada.getTotalInputTokens()).isZero();
            assertThat(guardada.getTurnCount()).isEqualTo(1);
            verifyNoInteractions(enlacePorCorreo);
        }

        @Test
        @DisplayName("lo retirado queda escrito como REMOVED del cliente detras del carrito")
        void lo_retirado_queda_escrito_como_removed_del_cliente() {
            conCabeceraGuardada();
            conTurnoGuardado();

            writer.escribirEdicion(ProposalMother.propuesta(ProposalMother.ID_PROPUESTA), 3,
                    carritoConNucleo(), List.of("CASH_REGISTER"), ProposalMother.idsPorCodigo(),
                    null);

            List<ProposalLine> escritas = lineasEscritas();
            ProposalLine retirada = escritas.getLast();
            assertThat(retirada.getItemCode()).isEqualTo("CASH_REGISTER");
            assertThat(retirada.getAction()).isEqualTo(LineAction.REMOVED);
            assertThat(retirada.getSource()).isEqualTo(LineSource.CUSTOMER);
            assertThat(retirada.getSortOrder())
                    .isEqualTo(escritas.get(escritas.size() - 2).getSortOrder() + 1);
        }

        @Test
        @DisplayName("una retirada cuyo codigo el catalogo no resuelve no se escribe")
        void una_retirada_sin_articulo_no_se_escribe() {
            conCabeceraGuardada();
            conTurnoGuardado();

            writer.escribirEdicion(ProposalMother.propuesta(ProposalMother.ID_PROPUESTA), 3,
                    carritoConNucleo(), List.of("TELEMEDICINA"), ProposalMother.idsPorCodigo(),
                    null);

            assertThat(lineasEscritas()).extracting(ProposalLine::getAction)
                    .doesNotContain(LineAction.REMOVED);
        }
    }
}
