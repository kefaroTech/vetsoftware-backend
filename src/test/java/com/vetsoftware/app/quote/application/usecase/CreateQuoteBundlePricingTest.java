package com.vetsoftware.app.quote.application.usecase;

import static com.vetsoftware.app.quote.testsupport.QuoteMother.AHORA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.CLIENT_REQUEST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.NUMERO;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.PRICE_LIST_ID;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.VIGENTE_HASTA;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.empresa;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.precioGravado;
import static com.vetsoftware.app.quote.testsupport.QuoteMother.tarifa;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.dto.QuoteLineDto;
import com.vetsoftware.app.quote.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.quote.application.port.out.CatalogPriceQueryPort;
import com.vetsoftware.app.quote.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.quote.application.port.out.ConfiguratorQuestionQueryPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.QuoteNumberPort;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CatalogItemRef;
import com.vetsoftware.app.quote.domain.QuoteItemType;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * <b>La otra mitad de la pregunta del cobro doble, contra el motor de
 * precios.</b>
 *
 * <p>
 * {@link SelfServeQuoteDoubleChargeTest} prueba que una cesta con un paquete y
 * una pieza suya se rechaza. Eso solo es suficiente si ademas es cierto lo que
 * esa prueba da por hecho: que <b>un paquete se cobra por su propio precio y
 * sus componentes no se cotizan aparte</b>. Si {@code freezeLines} expandiera
 * el paquete en sus piezas, el rechazo de la otra clase estaria protegiendo un
 * flanco mientras el cobro doble entraba por el otro — con el agravante de que
 * seria invisible: los totales seguirian cuadrando con la suma de las lineas,
 * porque las lineas de mas tambien suman.
 *
 * <p>
 * Y la direccion contraria, en la misma clase: un modulo comprado suelto se
 * cobra <b>una</b> vez, al precio del modulo, sin que el hecho de pertenecer a
 * un paquete le anada ni le quite nada.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CreateQuoteService — un paquete se cobra por su precio, no por sus piezas")
class CreateQuoteBundlePricingTest {

    private static final Clock RELOJ = Clock.fixed(AHORA.atZone(ZoneId.systemDefault()).toInstant(),
            ZoneId.systemDefault());

    private static final Long PAQUETE_ID = 90L;
    private static final Long PIEZA_ID = 11L;

    @Mock
    private QuoteRepository repository;
    @Mock
    private QuoteNumberPort quoteNumberPort;
    @Mock
    private CompanyQueryPort companyQueryPort;
    @Mock
    private PriceListQueryPort priceListQueryPort;
    @Mock
    private CatalogItemQueryPort catalogItemQueryPort;
    @Mock
    private CatalogPriceQueryPort catalogPriceQueryPort;
    @Mock
    private ConfiguratorQuestionQueryPort configuratorQuestionQueryPort;

    private CreateQuoteService service;

    @BeforeEach
    void crearServicio() {
        service = new CreateQuoteService(repository, quoteNumberPort, companyQueryPort,
                priceListQueryPort, catalogItemQueryPort, catalogPriceQueryPort,
                configuratorQuestionQueryPort, RELOJ);
    }

    private static CatalogItemRef paquete() {
        return new CatalogItemRef(PAQUETE_ID, "PACK_CLINIC", "Paquete Clinica",
                QuoteItemType.BUNDLE);
    }

    private static CatalogItemRef piezaDelPaquete() {
        return new CatalogItemRef(PIEZA_ID, "SCHEDULING", "Agenda", QuoteItemType.MODULE);
    }

    private static CreateQuoteCommand comando(Long catalogItemId) {
        return new CreateQuoteCommand(CLIENT_REQUEST_ID, empresa().id(), null, null, null, null,
                PRICE_LIST_ID, "MONTHLY", VIGENTE_HASTA, 0,
                List.of(new QuoteLineCommand(catalogItemId, 1, BigDecimal.ZERO)), List.of());
    }

    private void caminoFeliz() {
        when(repository.findByClientRequestIdAndCompanyId(CLIENT_REQUEST_ID, empresa().id()))
                .thenReturn(Optional.empty());
        when(priceListQueryPort.findPublishedById(PRICE_LIST_ID)).thenReturn(Optional.of(tarifa()));
        when(companyQueryPort.findById(empresa().id())).thenReturn(Optional.of(empresa()));
        when(quoteNumberPort.next(2026)).thenReturn(NUMERO);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    /**
     * El caso «compro el paquete». Una linea, la del paquete, a su propio precio.
     * Ninguna de las piezas que trae dentro se busca en el catalogo ni se tarifa:
     * el servicio recorre {@code command.lines()} y nada mas.
     */
    @Test
    @DisplayName("cotizar un paquete emite UNA linea a su precio y no expande sus componentes")
    void cotizar_un_paquete_emite_una_sola_linea() {
        caminoFeliz();
        when(catalogItemQueryPort.findActiveById(PAQUETE_ID)).thenReturn(Optional.of(paquete()));
        when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, PAQUETE_ID, BillingCycle.MONTHLY))
                .thenReturn(List.of(precioGravado("89000.00")));

        QuoteDto resultado = service.execute(comando(PAQUETE_ID));

        assertThat(resultado.lines()).singleElement().satisfies(linea -> {
            assertThat(linea.catalogItemId()).isEqualTo(PAQUETE_ID);
            assertThat(linea.unitAmount()).isEqualByComparingTo("89000.00");
        });
        verify(catalogItemQueryPort, never()).findActiveById(PIEZA_ID);
        verify(catalogPriceQueryPort, never()).findAllTiers(any(), eq(PIEZA_ID), any());
    }

    /**
     * El caso «compro solo la cirugia». Que el modulo tambien viva dentro de un
     * paquete no le anade ni le quita nada: se cobra su propio precio, una vez.
     */
    @Test
    @DisplayName("cotizar un modulo suelto lo cobra una vez, a su propio precio")
    void cotizar_un_modulo_suelto_lo_cobra_una_vez() {
        caminoFeliz();
        when(catalogItemQueryPort.findActiveById(PIEZA_ID))
                .thenReturn(Optional.of(piezaDelPaquete()));
        when(catalogPriceQueryPort.findAllTiers(PRICE_LIST_ID, PIEZA_ID, BillingCycle.MONTHLY))
                .thenReturn(List.of(precioGravado("38000.00")));

        QuoteDto resultado = service.execute(comando(PIEZA_ID));

        assertThat(resultado.lines()).extracting(QuoteLineDto::catalogItemId)
                .containsExactly(PIEZA_ID);
        assertThat(resultado.lines()).singleElement().satisfies(
                linea -> assertThat(linea.unitAmount()).isEqualByComparingTo("38000.00"));
        verify(catalogItemQueryPort, never()).findActiveById(PAQUETE_ID);
    }
}
