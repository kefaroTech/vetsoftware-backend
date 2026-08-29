package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.quote.application.port.out.CatalogPriceQueryPort;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CatalogItemRef;
import com.vetsoftware.app.quote.domain.CatalogPriceRef;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.QuoteLine;
import com.vetsoftware.app.quote.domain.TieredPrice;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * <strong>El unico sitio donde una cesta se convierte en lineas con
 * precio.</strong>
 *
 * <p>
 * Vivia dentro de {@code CreateQuoteService} y salio de ahi cuando aparecio un
 * segundo consumidor: la vista previa que le dice al front cuanto cuesta una
 * seleccion <em>sin</em> crear la oferta. Copiar el bucle habria sido barato y
 * habria reintroducido el defecto que este proyecto acaba de pagar dos veces
 * —el front multiplicando el tramo de entrada, y extrapolando el anual del
 * mensual—: dos cifras plausibles que no eran la que se cobra. Con un solo
 * implementador, <strong>lo que se muestra y lo que se cobra coinciden por
 * construccion</strong>, no por acuerdo.
 *
 * <p>
 * <strong>Y no expande paquetes, a proposito.</strong> Recorre las lineas que
 * recibe, una por una: un {@code BUNDLE} se cobra por su propio precio y sus
 * componentes no se cotizan aparte. Es lo que hace correcta la compra de un
 * paquete, y lo que obliga a que el rechazo del cobro doble viva antes, en
 * quien arma la cesta.
 *
 * <p>
 * Estatico y con los puertos por parametro en vez de ser un bean: los dos
 * servicios que lo usan ya tienen esos puertos inyectados, y convertirlo en
 * colaborador de Spring cambiaria dos constructores sin ganar nada.
 */
final class QuoteLineFreezer {

    private QuoteLineFreezer() {
    }

    /**
     * D-66 / R-PRICE-04: la cantidad se parte ACUMULATIVAMENTE entre los tramos, y
     * cada tramo produce SU PROPIO RENGLON.
     *
     * <p>
     * Quince usuarios con "unidades extra 1 a 8 a 12.000 y de la 9 en adelante a
     * 9.000" salen como dos renglones —ocho a 12.000 y cinco a 9.000, 141.000— y no
     * como uno de trece al precio del tramo alto, que daba 117.000. Que sean dos
     * renglones y no un importe calculado a mano es lo que hace que el cliente vea
     * el mismo desglose con el que se le va a facturar (R-QUOTE-09) y que los
     * totales sigan siendo la suma de las lineas sin ninguna excepcion.
     *
     * <p>
     * El reparto lo hace {@link TieredPrice} y la cantidad de cada renglon la
     * recalcula {@link QuoteLine} en su constructor: este codigo no sabe
     * multiplicar y no puede equivocarse en la cuenta.
     *
     * <p>
     * R15: lo incluido en la tarifa se resta antes de repartir por tramos, y la
     * resta la hace el dominio porque quien resuelve el precio es quien tiene el
     * {@code included_quantity}. Si lo contratado no supera lo incluido, el reparto
     * sale vacio y no se emite ninguna linea.
     */
    static List<QuoteLine> freeze(List<QuoteLineCommand> requestedLines, PriceListRef priceList,
            BillingCycle billingCycle, LocalDateTime now, CatalogItemQueryPort catalogItemQueryPort,
            CatalogPriceQueryPort catalogPriceQueryPort) {
        if (requestedLines == null || requestedLines.isEmpty()) {
            throw new IllegalArgumentException("quote requires at least one line");
        }
        List<QuoteLine> lines = new ArrayList<>();
        int lineNumber = 1;
        for (QuoteLineCommand requested : requestedLines) {
            CatalogItemRef item = catalogItemQueryPort.findActiveById(requested.catalogItemId())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "Catalog item not found or not active: " + requested.catalogItemId()));
            List<CatalogPriceRef> tiers = catalogPriceQueryPort.findAllTiers(priceList.id(),
                    requested.catalogItemId(), billingCycle);
            if (tiers.isEmpty()) {
                throw new IllegalArgumentException(
                        "No price for catalog item " + requested.catalogItemId() + " in price list "
                                + priceList.id() + " for cycle " + billingCycle);
            }
            TieredPrice tiered = TieredPrice.of(item.itemType(), requested.quantity(), tiers);
            for (CatalogPriceRef tier : tiered.tiers()) {
                lines.add(QuoteLine.freeze(lineNumber, item, tier, requested.quantity(),
                        tiered.includedQuantity(), requested.discountPercent(),
                        requested.discountIsConditional(), now));
                lineNumber++;
            }
        }
        return lines;
    }
}
