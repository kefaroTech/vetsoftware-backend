package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.CLIENT_REQUEST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.empresa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.infrastructure.config.ClockConfig;
import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.out.PlatformQuoteIssuerPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.ProposalReferencePort;
import com.vetsoftware.app.quote.application.port.out.PublishedCatalogItemQueryPort;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import com.vetsoftware.app.quote.testsupport.QuoteMother;
import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>La barandilla: nadie compra algo que no va a poder usar.</b>
 *
 * <p>
 * {@code catalog_item_dependencies} declara nueve arcos {@code REQUIRES} desde
 * el changeset 309 —facturar electronicamente necesita Caja— y hasta hoy no los
 * evaluaba nadie. El configurador ahora los completa, pero eso es el camino
 * amable: quien llame directo a este endpoint, o componga la cesta a mano desde
 * el catalogo publico, no pasa por alli. <b>Una garantia que solo vive en el
 * camino amable no es una garantia</b>, y esta clase es la mitad que lo
 * convierte en una.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SelfServeQuoteService — una cesta incoherente no se cotiza")
class SelfServeQuoteRequirementsTest {

    private static final Clock RELOJ = Clock
            .fixed(AHORA.atZone(ClockConfig.BUSINESS_ZONE).toInstant(), ClockConfig.BUSINESS_ZONE);

    private static final Long TARIFA = 70L;

    private static final String FACTURACION = "ELECTRONIC_INVOICING";
    private static final String CAJA = "CASH_REGISTER";
    private static final String PACK = "PACK_FULL";

    @Mock
    private PlatformQuoteIssuerPort issuer;
    @Mock
    private PriceListQueryPort priceListQueryPort;
    @Mock
    private PublishedCatalogItemQueryPort publishedCatalogItemQueryPort;

    /** DC-2: sin propuesta detras. El doble responde Optional.empty(). */
    @Mock
    private ProposalReferencePort proposalReferencePort;
    @Captor
    private ArgumentCaptor<CreateQuoteCommand> emitido;

    private SelfServeQuoteService servicio() {
        return new SelfServeQuoteService(issuer, priceListQueryPort, publishedCatalogItemQueryPort,
                proposalReferencePort, RELOJ);
    }

    private static SelfServeQuoteCommand comando(String... codigos) {
        return new SelfServeQuoteCommand(CLIENT_REQUEST_ID, empresa().id(), "MONTHLY",
                List.of(codigos).stream().map(c -> new SelfServeQuoteLineCommand(c, 1)).toList());
    }

    private void hayTarifaVigente() {
        when(priceListQueryPort.findAllPublished()).thenReturn(List.of(
                new PriceListRef(TARIFA, "LISTA-2026-08", "COP", LocalDate.of(2026, 8, 1), null)));
    }

    private void sinCobroDoble() {
        when(publishedCatalogItemQueryPort.findComponentCodesOfBundles(any()))
                .thenReturn(List.of());
    }

    private void elCatalogoResuelve(String codigo, Long id) {
        when(publishedCatalogItemQueryPort.findPublishedIdByCode(codigo, TARIFA,
                BillingCycle.MONTHLY)).thenReturn(Optional.of(id));
    }

    /**
     * El caso que el modelo declaraba y nadie comprobaba: Facturacion Electronica
     * sin Caja. Se cotizaba tal cual y el cliente compraba algo inservible.
     */
    @Test
    @DisplayName("un articulo sin lo que necesita se rechaza, nombrando lo que falta")
    void un_articulo_sin_lo_que_necesita_se_rechaza() {
        hayTarifaVigente();
        sinCobroDoble();
        when(publishedCatalogItemQueryPort.findMissingRequirements(any()))
                .thenReturn(List.of(CAJA));

        assertThatThrownBy(() -> servicio().execute(comando(FACTURACION)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("missing catalog items required").hasMessageContaining(CAJA);

        verifyNoInteractions(issuer);
    }

    /**
     * Se rechaza <b>antes</b> de traducir un solo rotulo: una cesta invalida no
     * gasta consultas que un cliente puede repetir a voluntad.
     */
    @Test
    @DisplayName("no traduce ningun codigo cuando falta un requisito")
    void no_traduce_ningun_codigo_cuando_falta_un_requisito() {
        hayTarifaVigente();
        sinCobroDoble();
        when(publishedCatalogItemQueryPort.findMissingRequirements(any()))
                .thenReturn(List.of(CAJA));

        assertThatThrownBy(() -> servicio().execute(comando(FACTURACION)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(publishedCatalogItemQueryPort, never()).findPublishedIdByCode(anyString(), any(),
                any());
    }

    /**
     * <b>La sutileza que hace correcto el caso normal.</b> {@code PACK_FULL} trae
     * dentro Facturacion Electronica y Caja, asi que comprar el paquete satisface
     * el requisito aunque ninguno de los dos rotulos viaje en la peticion. La
     * cobertura la calcula el puerto expandiendo los paquetes — expansion que el
     * precio, en cambio, no hace: son dos preguntas distintas sobre la misma cesta.
     */
    @Test
    @DisplayName("comprar el paquete entero satisface los requisitos de lo que trae dentro")
    void el_paquete_satisface_los_requisitos_de_sus_piezas() {
        hayTarifaVigente();
        sinCobroDoble();
        when(publishedCatalogItemQueryPort.findMissingRequirements(any())).thenReturn(List.of());
        elCatalogoResuelve(PACK, 90L);
        when(issuer.issue(any()))
                .thenReturn(QuoteDto.from(QuoteMother.persistida(1L, QuoteStatus.SENT)));

        servicio().execute(comando(PACK));

        verify(issuer).issue(emitido.capture());
        assertThat(emitido.getValue().lines()).extracting(QuoteLineCommand::catalogItemId)
                .containsExactly(90L);
    }

    @Test
    @DisplayName("una cesta coherente pasa: los dos articulos se cotizan")
    void una_cesta_coherente_pasa() {
        hayTarifaVigente();
        sinCobroDoble();
        when(publishedCatalogItemQueryPort.findMissingRequirements(any())).thenReturn(List.of());
        elCatalogoResuelve(FACTURACION, 31L);
        elCatalogoResuelve(CAJA, 32L);
        when(issuer.issue(any()))
                .thenReturn(QuoteDto.from(QuoteMother.persistida(1L, QuoteStatus.SENT)));

        servicio().execute(comando(FACTURACION, CAJA));

        verify(issuer).issue(emitido.capture());
        assertThat(emitido.getValue().lines()).extracting(QuoteLineCommand::catalogItemId)
                .containsExactly(31L, 32L);
    }
}
