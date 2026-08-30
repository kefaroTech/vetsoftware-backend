package com.vetsoftware.app.aiproposal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.aiproposal.application.command.GenerateProposalCommand;
import com.vetsoftware.app.aiproposal.application.command.LegalAcceptanceCommand;
import com.vetsoftware.app.aiproposal.application.dto.ProposalGenerationRequest;
import com.vetsoftware.app.aiproposal.application.dto.ProposalLineDto;
import com.vetsoftware.app.aiproposal.application.dto.ProposalViewDto;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalRepository;
import com.vetsoftware.app.aiproposal.application.port.out.LegalConsentPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalGeneratorPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalLinkEmailSender;
import com.vetsoftware.app.aiproposal.application.port.out.ResponsePacingPort;
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.domain.GenerationOutcome;
import com.vetsoftware.app.aiproposal.domain.LegalDocumentVersionRef;
import com.vetsoftware.app.aiproposal.domain.LineVerdict;
import com.vetsoftware.app.aiproposal.domain.ProposalBillingCycle;
import com.vetsoftware.app.aiproposal.domain.ProposalDraft;
import com.vetsoftware.app.aiproposal.domain.ProposalLine;
import com.vetsoftware.app.aiproposal.domain.ProposalPresentation;
import com.vetsoftware.app.aiproposal.domain.ProposalTurn;
import com.vetsoftware.app.aiproposal.domain.ProspectText;
import com.vetsoftware.app.aiproposal.domain.SellableCatalog;
import com.vetsoftware.app.aiproposal.testsupport.ProposalMother;
import com.vetsoftware.app.aiproposal.testsupport.SellableCatalogMother;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * La propuesta inicial: TX1, la invocacion fuera de transaccion y TX2.
 *
 * <p>
 * <b>{@link ProposalReader} y {@link ProposalTurnWriter} son reales, no
 * dobles.</b> Solo se mockean los puertos de salida. Un doble del lector
 * dejaria que el propio test definiera la regla de fusion y el tope de
 * refinamientos, que es justo lo que estos tests vienen a comprobar.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GenerateProposalService — la propuesta inicial de un prospecto anonimo")
class GenerateProposalServiceTest {

    private static final Long ID_TURNO = 70L;

    private static final String DESCRIPCION = "somos una veterinaria de barrio en Chapinero";

    private static final LegalDocumentVersionRef AVISO = new LegalDocumentVersionRef(
            ProposalMother.ID_AVISO, "PRIVACY_NOTICE", 3, true);

    private static final LegalDocumentVersionRef TERMINOS = new LegalDocumentVersionRef(11L,
            "TERMS", 2, false);

    private final SellableCatalog catalog = SellableCatalogMother.sinPaquetes();

    @Mock
    private AiProposalRepository repository;

    @Mock
    private SellableCatalogQueryPort catalogQueryPort;

    @Mock
    private LegalConsentPort legalConsent;

    @Mock
    private ProposalGeneratorPort generator;

    @Mock
    private ResponsePacingPort pacing;

    @Mock
    private ProposalLinkEmailSender enlacePorCorreo;

    @Mock
    private AiProposalMetrics metrics;

    private GenerateProposalService service;

    @BeforeEach
    void montar() {
        service = new GenerateProposalService(catalogQueryPort, legalConsent, generator,
                new ProposalTurnWriter(repository, legalConsent, enlacePorCorreo,
                        ProposalMother.RELOJ),
                new ProposalReader(repository, catalogQueryPort), pacing, metrics,
                ProposalMother.RELOJ, ProposalMother.MODELO, ProposalMother.PROMPT, 14, "es-CO");
    }

    private GenerateProposalCommand comando(String clave) {
        return new GenerateProposalCommand(ProposalMother.CORREO, DESCRIPCION, clave,
                List.of(new LegalAcceptanceCommand("PRIVACY_NOTICE", 3),
                        new LegalAcceptanceCommand("TERMS", 2)),
                "iphash", "uahash");
    }

    private void conTarifaPublicada() {
        when(catalogQueryPort.findPublishedPriceListId())
                .thenReturn(Optional.of(ProposalMother.ID_TARIFA));
        when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA, ProposalBillingCycle.MONTHLY))
                .thenReturn(Optional.of(catalog));
    }

    private void conConsentimientoResoluble() {
        when(legalConsent.findVersion("PRIVACY_NOTICE", 3)).thenReturn(Optional.of(AVISO));
        when(legalConsent.findVersion("TERMS", 2)).thenReturn(Optional.of(TERMINOS));
    }

    private void conEscrituraQueFunciona() {
        when(repository.save(any())).thenAnswer(invocacion -> {
            AiProposal propuesta = invocacion.getArgument(0);
            propuesta.setId(ProposalMother.ID_PROPUESTA);
            return propuesta;
        });
        when(repository.saveTurn(any())).thenAnswer(invocacion -> {
            ProposalTurn turno = invocacion.getArgument(0);
            turno.setId(ID_TURNO);
            return turno;
        });
        when(catalogQueryPort.findItemIdsByCode()).thenReturn(ProposalMother.idsPorCodigo());
        when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA))
                .thenReturn(List.of(ProposalMother.turnoInicial(ID_TURNO, DESCRIPCION)));
    }

    private void conVistaReleible(AiProposal propuesta) {
        when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA, ProposalBillingCycle.MONTHLY))
                .thenReturn(Optional.of(catalog));
        when(repository.findTurnsByProposalId(propuesta.getId()))
                .thenReturn(List.of(ProposalMother.turnoInicial(ID_TURNO, DESCRIPCION)));
        when(repository.findLinesByTurnId(ID_TURNO)).thenReturn(
                List.of(ProposalMother.lineaDelModelo(ID_TURNO, "CORE", "69000.00", 0)));
    }

    private void noEscribioNada() {
        verify(repository, never()).save(any());
        verify(repository, never()).saveTurn(any());
        verify(repository, never()).saveLines(any());
    }

    @Nested
    @DisplayName("Idempotencia")
    class Idempotencia {

        @Test
        @DisplayName("una peticion repetida devuelve lo ya visto sin volver a invocar al modelo")
        void una_peticion_repetida_no_vuelve_a_invocar_al_modelo() {
            AiProposal yaVista = ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA,
                    2L);
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.of(yaVista));
            conVistaReleible(yaVista);

            ProposalViewDto vista = service.generate(comando(ProposalMother.CLAVE));

            assertThat(vista.version()).isEqualTo(2L);
            assertThat(vista.recalculated()).isFalse();
            assertThat(vista.lines()).extracting(ProposalLineDto::code).containsExactly("CORE");
            verifyNoInteractions(generator, pacing, enlacePorCorreo);
            noEscribioNada();
        }

        @Test
        @DisplayName("el perdedor de la carrera relee la fila que gano en vez de dar un 500")
        void el_perdedor_de_la_carrera_relee_la_fila_que_gano() {
            AiProposal ganadora = ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA,
                    1L);
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty(), Optional.of(ganadora));
            conTarifaPublicada();
            conConsentimientoResoluble();
            when(repository.save(any()))
                    .thenThrow(new DataIntegrityViolationException("uq_ai_proposals_idempotency"));
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA))
                    .thenReturn(List.of(ProposalMother.turnoInicial(ID_TURNO, DESCRIPCION)));
            when(repository.findLinesByTurnId(ID_TURNO)).thenReturn(
                    List.of(ProposalMother.lineaDelModelo(ID_TURNO, "CORE", "69000.00", 0)));

            ProposalViewDto vista = service.generate(comando(ProposalMother.CLAVE));

            assertThat(vista.version()).isEqualTo(1L);
            assertThat(vista.recalculated()).isFalse();
            assertThat(vista.lines()).extracting(ProposalLineDto::code).containsExactly("CORE");
            verifyNoInteractions(generator, pacing);
        }

        @Test
        @DisplayName("&#9888; ventana abierta: si el ganador aun no ha commiteado, el perdedor se"
                + " lleva un 500 y no un reintento")
        void la_ventana_en_que_el_ganador_no_ha_commiteado_da_un_500() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            conConsentimientoResoluble();
            when(repository.save(any()))
                    .thenThrow(new DataIntegrityViolationException("uq_ai_proposals_idempotency"));

            GenerateProposalCommand command = comando(ProposalMother.CLAVE);

            assertThatThrownBy(() -> service.generate(command))
                    .isInstanceOf(NoSuchElementException.class)
                    .hasMessageContaining("No value present");
            verifyNoInteractions(generator, pacing);
        }

        @Test
        @DisplayName("sin clave declarada una violacion de integridad no se disfraza de reintento")
        void sin_clave_una_violacion_de_integridad_no_se_disfraza() {
            conTarifaPublicada();
            conConsentimientoResoluble();
            when(repository.save(any()))
                    .thenThrow(new DataIntegrityViolationException("otra restriccion"));

            GenerateProposalCommand command = comando(null);

            assertThatThrownBy(() -> service.generate(command))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("otra restriccion");
            verifyNoInteractions(generator);
        }
    }

    @Nested
    @DisplayName("Sin catalogo que cotizar")
    class SinCatalogo {

        @Test
        @DisplayName("sin tarifa publicada se responde la vista deterministica y no se persiste"
                + " nada")
        void sin_tarifa_publicada_no_se_persiste_nada() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            when(catalogQueryPort.findPublishedPriceListId()).thenReturn(Optional.empty());

            ProposalViewDto vista = service.generate(comando(ProposalMother.CLAVE));

            assertThat(vista.presentation()).isEqualTo(ProposalPresentation.DETERMINISTIC);
            assertThat(vista.publicToken()).isNull();
            assertThat(vista.lines()).isEmpty();
            verifyNoInteractions(generator, legalConsent, pacing);
            noEscribioNada();
        }

        @Test
        @DisplayName("una tarifa publicada pero sin articulos tampoco cotiza")
        void una_tarifa_sin_articulos_tampoco_cotiza() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            when(catalogQueryPort.findPublishedPriceListId())
                    .thenReturn(Optional.of(ProposalMother.ID_TARIFA));
            when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY))
                    .thenReturn(Optional.of(new SellableCatalog(Map.of(), Map.of(), List.of())));

            assertThat(service.generate(comando(ProposalMother.CLAVE)).presentation())
                    .isEqualTo(ProposalPresentation.DETERMINISTIC);
            verifyNoInteractions(generator, legalConsent, pacing);
            noEscribioNada();
        }
    }

    @Nested
    @DisplayName("Consentimiento")
    class Consentimiento {

        @Test
        @DisplayName("una peticion sin ninguna aceptacion no recoge ni un dato")
        void sin_ninguna_aceptacion_no_se_recoge_nada() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            GenerateProposalCommand command = new GenerateProposalCommand(ProposalMother.CORREO,
                    DESCRIPCION, ProposalMother.CLAVE, List.of(), "iphash", "uahash");

            assertThatThrownBy(() -> service.generate(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("at least one legal acceptance is required");
            verifyNoInteractions(generator);
            noEscribioNada();
        }

        @Test
        @DisplayName("un par codigo + version que no existe es un 400, no un consentimiento dado"
                + " por bueno")
        void un_par_inexistente_es_un_400() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            when(legalConsent.findVersion("PRIVACY_NOTICE", 3)).thenReturn(Optional.empty());

            GenerateProposalCommand command = comando(ProposalMother.CLAVE);

            assertThatThrownBy(() -> service.generate(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("unknown legal document version: PRIVACY_NOTICE");
            verifyNoInteractions(generator);
            noEscribioNada();
        }

        @Test
        @DisplayName("sin aviso de privacidad entre las aceptaciones no se persiste la cabecera")
        void sin_aviso_de_privacidad_no_se_persiste_la_cabecera() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            when(legalConsent.findVersion("TERMS", 2)).thenReturn(Optional.of(TERMINOS));
            GenerateProposalCommand command = new GenerateProposalCommand(ProposalMother.CORREO,
                    DESCRIPCION, ProposalMother.CLAVE,
                    List.of(new LegalAcceptanceCommand("TERMS", 2)), "iphash", "uahash");

            assertThatThrownBy(() -> service.generate(command))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("the privacy notice acceptance is required");
            verifyNoInteractions(generator);
            noEscribioNada();
        }
    }

    @Nested
    @DisplayName("Generacion")
    class Generacion {

        @Test
        @DisplayName("el turno pendiente se escribe antes de invocar al modelo, no despues")
        void el_turno_pendiente_se_escribe_antes_de_invocar_al_modelo() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            conConsentimientoResoluble();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())));

            service.generate(comando(ProposalMother.CLAVE));

            InOrder orden = inOrder(repository, generator);
            orden.verify(repository).saveTurn(any());
            orden.verify(generator).generate(any());

            ArgumentCaptor<ProposalGenerationRequest> peticion = ArgumentCaptor.captor();
            verify(generator).generate(peticion.capture());
            assertThat(peticion.getValue().customerTexts())
                    .containsExactly(ProspectText.of(DESCRIPCION));
            assertThat(peticion.getValue().currentCartCodes()).isEmpty();
        }

        @Test
        @DisplayName("la vista lleva las lineas aceptadas y de las descartadas solo el conteo")
        void la_vista_lleva_las_aceptadas_y_solo_el_conteo_de_las_descartadas() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            conConsentimientoResoluble();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(ProposalMother
                    .exito(ProposalMother.borrador(List.of("CORE", "TELEMEDICINA"), List.of())));

            ProposalViewDto vista = service.generate(comando(ProposalMother.CLAVE));

            assertThat(vista.presentation()).isEqualTo(ProposalPresentation.PROPOSAL);
            assertThat(vista.recalculated()).isTrue();
            assertThat(vista.lines()).extracting(ProposalLineDto::code).containsExactly("CORE");
            assertThat(vista.discardedLines()).isEqualTo(1);
            assertThat(vista.lines()).extracting(ProposalLineDto::reason)
                    .noneMatch(motivo -> motivo != null && motivo.contains("TELEMEDICINA"));
            assertThat(vista.refinementsLeft()).isEqualTo(3);
        }

        @Test
        @DisplayName("un negocio ajeno no recibe ni una linea, pero su contradiccion queda"
                + " escrita")
        void un_negocio_ajeno_no_recibe_lineas_pero_su_contradiccion_queda_escrita() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            conConsentimientoResoluble();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(ProposalMother
                    .exito(ProposalDraft.sinLineas(true, true, List.of("CASH_REGISTER"))));

            ProposalViewDto vista = service.generate(comando(ProposalMother.CLAVE));

            assertThat(vista.presentation()).isEqualTo(ProposalPresentation.OUT_OF_DOMAIN);
            assertThat(vista.lines()).isEmpty();
            assertThat(vista.discardedLines()).isZero();

            ArgumentCaptor<List<ProposalLine>> lineas = ArgumentCaptor.captor();
            verify(repository).saveLines(lineas.capture());
            assertThat(lineas.getValue()).singleElement().satisfies(linea -> {
                assertThat(linea.getItemCode()).isEqualTo("CASH_REGISTER");
                assertThat(linea.getVerdict()).isEqualTo(LineVerdict.NOT_SELLABLE);
            });
        }

        @Test
        @DisplayName("una alucinacion del modelo se persiste con su veredicto de rechazo")
        void una_alucinacion_se_persiste_con_su_veredicto() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            conConsentimientoResoluble();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(ProposalMother
                    .exito(ProposalMother.borrador(List.of("CORE", "TELEMEDICINA"), List.of())));

            service.generate(comando(ProposalMother.CLAVE));

            ArgumentCaptor<List<ProposalLine>> lineas = ArgumentCaptor.captor();
            verify(repository).saveLines(lineas.capture());
            assertThat(lineas.getValue()).extracting(ProposalLine::getItemCode)
                    .contains("TELEMEDICINA");
            assertThat(lineas.getValue())
                    .filteredOn(linea -> "TELEMEDICINA".equals(linea.getItemCode())).singleElement()
                    .satisfies(linea -> assertThat(linea.getVerdict())
                            .isEqualTo(LineVerdict.UNKNOWN_CODE));
        }
    }

    @Nested
    @DisplayName("Suelo de latencia")
    class SueloDeLatencia {

        @ParameterizedTest
        @CsvSource({"SUCCEEDED,0", "DEGRADED_SPEND_CAP,1", "DEGRADED_NO_HINTS,1",
                "DEGRADED_MODEL_UNAVAILABLE,1", "MODEL_FAILED,0"})
        @DisplayName("el suelo iguala las tres degradaciones sin llamada y deja fuera al fallo del"
                + " modelo, que ya pago la espera")
        void el_suelo_solo_iguala_las_degradaciones_sin_llamada(GenerationOutcome outcome,
                int veces) {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            conConsentimientoResoluble();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(ProposalMother.resultadoDe(outcome,
                    ProposalMother.borrador(List.of("CORE"), List.of())));

            service.generate(comando(ProposalMother.CLAVE));

            verify(pacing, times(veces)).applyDegradedFloor(0L);
        }

        @Test
        @DisplayName("la matriz de arriba cubre todos los desenlaces que el dominio declara")
        void la_matriz_cubre_todos_los_desenlaces() {
            assertThat(GenerationOutcome.values()).extracting(Enum::name).containsExactlyInAnyOrder(
                    "SUCCEEDED", "DEGRADED_SPEND_CAP", "DEGRADED_NO_HINTS",
                    "DEGRADED_MODEL_UNAVAILABLE", "MODEL_FAILED");
        }
    }
}
