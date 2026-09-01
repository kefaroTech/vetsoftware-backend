package com.vetsoftware.app.aiproposal.application.usecase;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
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
import com.vetsoftware.app.aiproposal.application.port.out.PaidInvocationSignalPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalGeneratorPort;
import com.vetsoftware.app.aiproposal.application.port.out.ProposalLinkEmailSender;
import com.vetsoftware.app.aiproposal.application.port.out.SellableCatalogQueryPort;
import com.vetsoftware.app.aiproposal.domain.AiProposal;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics.Outcome;
import com.vetsoftware.app.aiproposal.application.port.out.AiProposalMetrics.ServedProposal;
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
    private ProposalLinkEmailSender enlacePorCorreo;

    @Mock
    private AiProposalMetrics metrics;

    @Mock
    private PaidInvocationSignalPort paidInvocationSignal;

    private GenerateProposalService service;

    @BeforeEach
    void montar() {
        service = new GenerateProposalService(catalogQueryPort, legalConsent, generator,
                new ProposalTurnWriter(repository, legalConsent, enlacePorCorreo,
                        ProposalMother.RELOJ),
                new ProposalReader(repository, catalogQueryPort, ProposalMother.RELOJ), metrics,
                paidInvocationSignal, ProposalMother.RELOJ, ProposalMother.MODELO,
                ProposalMother.PROMPT, 14, "es-CO");
    }

    private GenerateProposalCommand comandoMensual(String clave) {
        return comando(clave, ProposalBillingCycle.MONTHLY);
    }

    private GenerateProposalCommand comandoAnual(String clave) {
        return comando(clave, ProposalBillingCycle.ANNUAL);
    }

    /**
     * &#9940; <strong>El ciclo es obligatorio y no tiene defecto AQUI a
     * proposito.</strong> Un ayudante que lo omitiera devolveria el trabajo al
     * estado del que se sale: catorce tests contra la rama mensual y cero contra la
     * anual, sin que nada lo señalara.
     */
    private GenerateProposalCommand comando(String clave, ProposalBillingCycle ciclo) {
        return new GenerateProposalCommand(ProposalMother.CORREO, DESCRIPCION, clave,
                List.of(new LegalAcceptanceCommand("PRIVACY_NOTICE", 3),
                        new LegalAcceptanceCommand("TERMS", 2)),
                "iphash", "uahash", ciclo);
    }

    private void conTarifaPublicada() {
        when(catalogQueryPort.findPublishedPriceListId())
                .thenReturn(Optional.of(ProposalMother.ID_TARIFA));
        when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA, ProposalBillingCycle.MONTHLY))
                .thenReturn(Optional.of(catalog));
    }

    private void conTarifaPublicadaAnual() {
        when(catalogQueryPort.findPublishedPriceListId())
                .thenReturn(Optional.of(ProposalMother.ID_TARIFA));
        when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA, ProposalBillingCycle.ANNUAL))
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

            ProposalViewDto vista = service.generate(comandoMensual(ProposalMother.CLAVE));

            assertThat(vista.version()).isEqualTo(2L);
            assertThat(vista.recalculated()).isFalse();
            assertThat(vista.lines()).extracting(ProposalLineDto::code).containsExactly("CORE");
            verifyNoInteractions(generator, enlacePorCorreo);
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

            ProposalViewDto vista = service.generate(comandoMensual(ProposalMother.CLAVE));

            assertThat(vista.version()).isEqualTo(1L);
            assertThat(vista.recalculated()).isFalse();
            assertThat(vista.lines()).extracting(ProposalLineDto::code).containsExactly("CORE");
            verifyNoInteractions(generator);
        }

        /**
         * <b>La ventana que quedaba abierta, y ahora cerrada.</b> Entre el rechazo del
         * índice único y el commit del ganador hay un instante en el que la fila
         * todavía no es visible. Antes, una sola relectura con {@code orElseThrow}
         * convertía ahí la carrera ya manejada en un {@code NoSuchElementException}: un
         * 500 para quien hizo doble clic sobre una propuesta que sí existe. Ahora el
         * perdedor reintenta la lectura y solo se rinde cuando el ganador no aparece en
         * ninguno de los intentos.
         */
        @Test
        @DisplayName("el perdedor reintenta la lectura mientras el ganador no ha commiteado")
        void el_perdedor_reintenta_mientras_el_ganador_no_ha_commiteado() {
            AiProposal ganadora = ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA,
                    1L);
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty(), Optional.empty(), Optional.of(ganadora));
            conTarifaPublicada();
            conConsentimientoResoluble();
            when(repository.save(any()))
                    .thenThrow(new DataIntegrityViolationException("uq_ai_proposals_idempotency"));
            when(repository.findTurnsByProposalId(ProposalMother.ID_PROPUESTA))
                    .thenReturn(List.of(ProposalMother.turnoInicial(ID_TURNO, DESCRIPCION)));
            when(repository.findLinesByTurnId(ID_TURNO)).thenReturn(
                    List.of(ProposalMother.lineaDelModelo(ID_TURNO, "CORE", "69000.00", 0)));

            ProposalViewDto vista = service.generate(comandoMensual(ProposalMother.CLAVE));

            assertThat(vista.version()).isEqualTo(1L);
            assertThat(vista.lines()).extracting(ProposalLineDto::code).containsExactly("CORE");
            verifyNoInteractions(generator);
        }

        /**
         * Agotados los reintentos se relanza <b>la violación original</b>, no un
         * {@code NoSuchElementException}: el fallo que se reporta es el que de verdad
         * ocurrió y nombra la restricción que lo causó.
         */
        @Test
        @DisplayName("si el ganador no aparece en ningún intento, se relanza la violación original")
        void si_el_ganador_no_aparece_se_relanza_la_violacion_original() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            conConsentimientoResoluble();
            when(repository.save(any()))
                    .thenThrow(new DataIntegrityViolationException("uq_ai_proposals_idempotency"));

            GenerateProposalCommand command = comandoMensual(ProposalMother.CLAVE);

            assertThatThrownBy(() -> service.generate(command))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("uq_ai_proposals_idempotency");
            verifyNoInteractions(generator);
        }

        @Test
        @DisplayName("sin clave declarada una violacion de integridad no se disfraza de reintento")
        void sin_clave_una_violacion_de_integridad_no_se_disfraza() {
            conTarifaPublicada();
            conConsentimientoResoluble();
            when(repository.save(any()))
                    .thenThrow(new DataIntegrityViolationException("otra restriccion"));

            GenerateProposalCommand command = comandoMensual(null);

            assertThatThrownBy(() -> service.generate(command))
                    .isInstanceOf(DataIntegrityViolationException.class)
                    .hasMessageContaining("otra restriccion");
            verifyNoInteractions(generator);
        }

        /**
         * &#9940; <strong>La clave la genera el front al montar la pantalla, asi que la
         * peticion mensual y la anual llegan con la MISMA.</strong> Devolver la previa
         * mirando solo la clave hacia que conmutar a anual respondiera la propuesta
         * mensual -con sus precios mensuales- y con toda la cara de haber funcionado:
         * 200, lineas y ni un error en ningun sitio.
         *
         * <p>
         * Y la nueva se escribe con clave <strong>nula</strong>: la fila mensual ya
         * ocupa ese par en {@code uq_ai_proposals_idempotency}, asi que reusarla
         * estrellaria el INSERT contra el unico.
         */
        @Test
        @DisplayName("una previa mensual no vale para una peticion anual con la misma clave:"
                + " se vuelve a generar y se escribe con clave nula")
        void una_previa_mensual_no_vale_para_una_peticion_anual() {
            AiProposal yaVista = ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA,
                    2L);
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.of(yaVista));
            conTarifaPublicadaAnual();
            conConsentimientoResoluble();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())));

            ProposalViewDto vista = service.generate(comandoAnual(ProposalMother.CLAVE));

            assertThat(vista.recalculated()).as("no puede ser la vista guardada").isTrue();
            verify(generator).generate(any());

            ArgumentCaptor<AiProposal> nueva = ArgumentCaptor.captor();
            verify(repository, atLeastOnce()).save(nueva.capture());
            assertThat(nueva.getValue().getBillingCycle()).isEqualTo(ProposalBillingCycle.ANNUAL);
            assertThat(nueva.getValue().getIdempotencyKey())
                    .as("la mensual ya ocupa ese par en el unico de idempotencia").isNull();
        }
    }

    /**
     * &#9940; <strong>El ciclo llegaba a {@code loadCatalog} clavado a
     * {@code MONTHLY}</strong> por una constante, asi que el prospecto que pedia
     * anual cotizaba contra la escalera mensual de {@code catalog_prices}: se
     * llevaba precios que no son los suyos y la cabecera quedaba escrita como
     * mensual, que es el dato que {@code ProposalReader.catalogo} vuelve a leer en
     * cada refinamiento.
     */
    @Nested
    @DisplayName("Ciclo de facturacion")
    class CicloDeFacturacion {

        @Test
        @DisplayName("una peticion anual cotiza contra la escalera anual y persiste ANNUAL")
        void una_peticion_anual_cotiza_contra_la_escalera_anual() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicadaAnual();
            conConsentimientoResoluble();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(
                    ProposalMother.exito(ProposalMother.borrador(List.of("CORE"), List.of())));

            service.generate(comandoAnual(ProposalMother.CLAVE));

            verify(catalogQueryPort).loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.ANNUAL);
            verify(catalogQueryPort, never()).loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY);

            ArgumentCaptor<AiProposal> nueva = ArgumentCaptor.captor();
            verify(repository, atLeastOnce()).save(nueva.capture());
            assertThat(nueva.getValue().getBillingCycle()).isEqualTo(ProposalBillingCycle.ANNUAL);
        }
    }

    @Nested
    @DisplayName("Sin catalogo que cotizar")
    class SinCatalogo {

        /**
         * &#9940; <b>NO_CATALOG y no DETERMINISTIC.</b> Por aqui no corrio ni el
         * determinista ni el modelo -el {@code return} es anterior al generador-, asi
         * que anunciar la pantalla determinista decia "hubo degradacion del modelo y
         * estas son sus lineas" sin una sola linea. Es el defecto del #692 y esta es su
         * prueba.
         */
        @Test
        @DisplayName("sin tarifa publicada se responde NO_CATALOG y no se persiste nada")
        void sin_tarifa_publicada_no_se_persiste_nada() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            when(catalogQueryPort.findPublishedPriceListId()).thenReturn(Optional.empty());

            ProposalViewDto vista = service.generate(comandoMensual(ProposalMother.CLAVE));

            assertThat(vista.presentation()).isEqualTo(ProposalPresentation.NO_CATALOG);
            assertThat(vista.publicToken()).isNull();
            assertThat(vista.lines()).isEmpty();
            verifyNoInteractions(generator, legalConsent);
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

            assertThat(service.generate(comandoMensual(ProposalMother.CLAVE)).presentation())
                    .isEqualTo(ProposalPresentation.NO_CATALOG);
            verifyNoInteractions(generator, legalConsent);
            noEscribioNada();
        }

        /**
         * &#9940; <b>La otra mitad del #692: el valor tiene que SEPARAR.</b> Sin esta
         * asercion, cambiar {@code sinCatalogo()} de vuelta a {@code DETERMINISTIC}
         * dejaria verdes las dos pruebas de arriba el dia que alguien "unifique" los
         * dos caminos, que es exactamente como nacio el defecto.
         */
        @Test
        @DisplayName("NO_CATALOG no se confunde con la degradacion del modelo, que si lleva"
                + " lineas")
        void no_catalog_no_es_la_pantalla_de_la_degradacion() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            when(catalogQueryPort.findPublishedPriceListId()).thenReturn(Optional.empty());

            ProposalViewDto vista = service.generate(comandoMensual(ProposalMother.CLAVE));

            assertThat(vista.presentation()).isNotEqualTo(ProposalPresentation.DETERMINISTIC);
            assertThat(vista.presentation()).isNotNull();
            assertThat(vista.lines()).isEmpty();
        }

        /**
         * &#9940; <b>Los dos caminos mudos NO comparten desenlace, y la diferencia solo
         * se ve en la metrica.</b> El prospecto ve lo mismo -200 con cero lineas- pero
         * la accion es la contraria: sin tarifa hay que publicarla, con la tarifa ya
         * publicada hay que mirar por que no cuelga de ella ningun articulo. Con los
         * dos colapsados, la alerta mandaba a publicar una tarifa que ya estaba
         * publicada.
         */
        @Test
        @DisplayName("sin tarifa cuenta no_catalog; con tarifa publicada y vacia cuenta"
                + " empty_catalog")
        void los_dos_caminos_mudos_no_comparten_desenlace() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            when(catalogQueryPort.findPublishedPriceListId()).thenReturn(Optional.empty());

            service.generate(comandoMensual(ProposalMother.CLAVE));

            ArgumentCaptor<ServedProposal> sinTarifa = ArgumentCaptor.captor();
            verify(metrics).proposalServed(sinTarifa.capture());
            assertThat(sinTarifa.getValue().outcome()).isEqualTo(Outcome.NO_CATALOG);

            reset(metrics);
            when(catalogQueryPort.findPublishedPriceListId())
                    .thenReturn(Optional.of(ProposalMother.ID_TARIFA));
            when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY)).thenReturn(Optional.empty());

            service.generate(comandoMensual(ProposalMother.CLAVE));

            ArgumentCaptor<ServedProposal> vacio = ArgumentCaptor.captor();
            verify(metrics).proposalServed(vacio.capture());
            assertThat(vacio.getValue().outcome()).isEqualTo(Outcome.EMPTY_CATALOG);
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
                    DESCRIPCION, ProposalMother.CLAVE, List.of(), "iphash", "uahash",
                    ProposalBillingCycle.MONTHLY);

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

            GenerateProposalCommand command = comandoMensual(ProposalMother.CLAVE);

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
                    List.of(new LegalAcceptanceCommand("TERMS", 2)), "iphash", "uahash",
                    ProposalBillingCycle.MONTHLY);

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

            service.generate(comandoMensual(ProposalMother.CLAVE));

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

            ProposalViewDto vista = service.generate(comandoMensual(ProposalMother.CLAVE));

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

            ProposalViewDto vista = service.generate(comandoMensual(ProposalMother.CLAVE));

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

            service.generate(comandoMensual(ProposalMother.CLAVE));

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

    /**
     * &#9940; El suelo de latencia de la ruta degradada <b>se retiro</b>: el bit
     * que ocultaba lo publica la respuesta en {@code presentation}. Estas dos
     * pruebas son las que impiden que vuelva de buena fe — la primera se pone roja
     * si alguien reintroduce el {@code Thread.sleep}, la segunda dice por que no
     * servia de nada.
     */
    @Nested
    @DisplayName("Respuesta degradada")
    class RespuestaDegradada {

        @ParameterizedTest
        @CsvSource({"DEGRADED_SPEND_CAP", "DEGRADED_NO_HINTS", "DEGRADED_MODEL_UNAVAILABLE"})
        @DisplayName("una degradacion responde de inmediato: no hay suelo de latencia que pagar")
        void una_degradacion_responde_de_inmediato(GenerationOutcome outcome) {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            conConsentimientoResoluble();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(ProposalMother.resultadoDe(outcome,
                    ProposalMother.borrador(List.of("CORE"), List.of())));

            long empezo = System.nanoTime();
            service.generate(comandoMensual(ProposalMother.CLAVE));
            long transcurrido = (System.nanoTime() - empezo) / 1_000_000;

            // El suelo retirado dormia entre 2.500 y 4.500 ms. Un segundo es holgado
            // para una prueba unitaria con todo doblado y a la vez inalcanzable si
            // alguien repone el sleep.
            assertThat(transcurrido).as("ms hasta responder una degradacion").isLessThan(1_000L);
        }

        @ParameterizedTest
        @CsvSource({"DEGRADED_SPEND_CAP", "DEGRADED_NO_HINTS", "DEGRADED_MODEL_UNAVAILABLE",
                "MODEL_FAILED"})
        @DisplayName("la respuesta publica el estado degradado en presentation: por eso el suelo"
                + " de latencia no ocultaba nada")
        void la_respuesta_publica_el_estado_degradado(GenerationOutcome outcome) {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            conConsentimientoResoluble();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(ProposalMother.resultadoDe(outcome,
                    ProposalMother.borrador(List.of("CORE"), List.of())));

            assertThat(service.generate(comandoMensual(ProposalMother.CLAVE)).presentation())
                    .isEqualTo(ProposalPresentation.DETERMINISTIC);
        }

        @Test
        @DisplayName("la matriz de arriba cubre todos los desenlaces que el dominio declara")
        void la_matriz_cubre_todos_los_desenlaces() {
            assertThat(GenerationOutcome.values()).extracting(Enum::name).containsExactlyInAnyOrder(
                    "SUCCEEDED", "DEGRADED_SPEND_CAP", "DEGRADED_NO_HINTS",
                    "DEGRADED_MODEL_UNAVAILABLE", "MODEL_FAILED");
        }
    }

    /**
     * &#9940; <b>El defecto que este bloque cierra, con su evidencia.</b> Tres
     * {@code POST /assistant/proposal} murieron en "no hay lista de precios", sin
     * invocar al modelo y con coste cero, y aun asi agotaron el cupo del dia de su
     * autor. {@code LoginRateLimitFilter} dice en negrita que lo que reparte son
     * <em>llamadas de pago, no peticiones</em>, pero es un filtro de servlet: cobra
     * antes de que exista un desenlace y no lo devuelve nunca. Este caso de uso es
     * el emisor del bit que le faltaba.
     *
     * <p>
     * <b>Las dos ramas se prueban, y la segunda es la que importa.</b> Un test que
     * solo comprobara la devolucion pasaria igual con un {@code signal(false)}
     * incondicional, que es exactamente el fallo que convierte el cupo en
     * decorativo.
     */
    @Nested
    @DisplayName("Cupo diario: quien no invoca al modelo recupera su intento")
    class CupoDiario {

        @Test
        @DisplayName("sin tarifa publicada consta que NO hubo invocacion de pago")
        void sin_tarifa_publicada_consta_que_no_hubo_invocacion() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            when(catalogQueryPort.findPublishedPriceListId()).thenReturn(Optional.empty());

            service.generate(comandoMensual(ProposalMother.CLAVE));

            verify(paidInvocationSignal).signal(false);
        }

        @Test
        @DisplayName("una tarifa publicada y vacia tambien: el modelo no llego a arrancar")
        void una_tarifa_vacia_tambien_consta_como_sin_invocacion() {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            when(catalogQueryPort.findPublishedPriceListId())
                    .thenReturn(Optional.of(ProposalMother.ID_TARIFA));
            when(catalogQueryPort.loadCatalog(ProposalMother.ID_TARIFA,
                    ProposalBillingCycle.MONTHLY))
                    .thenReturn(Optional.of(new SellableCatalog(Map.of(), Map.of(), List.of())));

            service.generate(comandoMensual(ProposalMother.CLAVE));

            verify(paidInvocationSignal).signal(false);
        }

        @ParameterizedTest
        @CsvSource({"DEGRADED_SPEND_CAP", "DEGRADED_NO_HINTS", "DEGRADED_MODEL_UNAVAILABLE"})
        @DisplayName("las tres degradaciones se deciden antes de llamar: el intento se devuelve")
        void las_tres_degradaciones_devuelven_el_intento(GenerationOutcome outcome) {
            conUnaGeneracionQueTermina(outcome);

            service.generate(comandoMensual(ProposalMother.CLAVE));

            verify(paidInvocationSignal).signal(false);
        }

        @Test
        @DisplayName("una generacion correcta consume el intento")
        void una_generacion_correcta_consume_el_intento() {
            conUnaGeneracionQueTermina(GenerationOutcome.SUCCEEDED);

            service.generate(comandoMensual(ProposalMother.CLAVE));

            verify(paidInvocationSignal).signal(true);
        }

        /**
         * &#9940; <b>El filo del predicado.</b> Se invoco, se pago y no sirvio:
         * {@code BedrockProposalGenerator} reconcilia el gasto tambien en sus dos
         * {@code catch}, asi que devolver el cupo aqui seria regalar dinero que ya
         * salio. Y no se puede escribir como {@code usage != null}:
         * {@code seInvocoAlModelo()} es esa comparacion y responde {@code false} para
         * este desenlace, porque una invocacion que revienta no trae medidas.
         */
        @Test
        @DisplayName("una invocacion FALLIDA consume igual: se pago lo mismo")
        void una_invocacion_fallida_consume_igual() {
            conUnaGeneracionQueTermina(GenerationOutcome.MODEL_FAILED);

            service.generate(comandoMensual(ProposalMother.CLAVE));

            verify(paidInvocationSignal).signal(true);
        }

        /**
         * Aqui tampoco hay invocacion, y aun asi se cobra: lo que se sirve es la
         * propuesta entera que el prospecto ya tiene. Sin marca el filtro cobra —ese es
         * el estado por defecto y esta prueba lo fija.
         */
        @Test
        @DisplayName("la repeticion idempotente no se marca: sin marca, el filtro cobra")
        void la_repeticion_idempotente_no_se_marca() {
            AiProposal yaVista = ProposalMother.propuestaConVersion(ProposalMother.ID_PROPUESTA,
                    2L);
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.of(yaVista));
            conVistaReleible(yaVista);

            service.generate(comandoMensual(ProposalMother.CLAVE));

            verifyNoInteractions(paidInvocationSignal);
        }

        private void conUnaGeneracionQueTermina(GenerationOutcome outcome) {
            when(repository.findByIdempotency(ProposalMother.CORREO, ProposalMother.CLAVE))
                    .thenReturn(Optional.empty());
            conTarifaPublicada();
            conConsentimientoResoluble();
            conEscrituraQueFunciona();
            when(generator.generate(any())).thenReturn(ProposalMother.resultadoDe(outcome,
                    ProposalMother.borrador(List.of("CORE"), List.of())));
        }
    }
}
