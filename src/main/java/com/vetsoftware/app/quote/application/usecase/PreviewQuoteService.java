package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.command.PreviewQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.command.SelfServeQuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuotePreviewDto;
import com.vetsoftware.app.quote.application.dto.QuotePreviewLineDto;
import com.vetsoftware.app.quote.application.port.in.PreviewQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.quote.application.port.out.CatalogPriceQueryPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.PublishedCatalogItemQueryPort;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.QuoteLine;
import com.vetsoftware.app.quote.domain.QuoteTotals;
import io.micrometer.observation.annotation.Observed;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * La calculadora publica: <strong>el mismo precio que se va a cobrar, sin crear
 * la oferta</strong>.
 *
 * <p>
 * Existe porque la escalera de tramos no se publica —es la politica de
 * descuento por volumen— y con solo el tramo de entrada un front no puede hacer
 * otra cosa que extrapolar. Quince usuarios le salen 156.000 multiplicando; la
 * cotizacion cobra 141.000 repartiendo por tramos. Este servicio devuelve
 * 141.000 porque usa <strong>literalmente el mismo codigo</strong> que congela
 * una oferta real: {@link QuoteLineFreezer} y {@link QuoteTotals}. No es una
 * segunda opinion.
 *
 * <p>
 * <strong>Aplica el mismo gate que la autocontratacion</strong>, y tiene que
 * hacerlo: si la vista previa tarifara rotulos que la contratacion rechaza,
 * volveria a existir un numero que la portada promete y el contrato niega, que
 * es el defecto que este slice lleva toda la noche cerrando. Traduce solo lo
 * publicado, rechaza el cobro doble y rechaza la cesta incoherente.
 *
 * <p>
 * <strong>No persiste nada.</strong> Ni numero, ni vigencia, ni estado, ni
 * empresa: es de solo lectura y no hay nada que aceptar despues. La oferta de
 * verdad la emite {@code SelfServeQuoteService}, que si exige estar
 * autenticado.
 */
@Observed(name = "quote.preview")
@Service
public class PreviewQuoteService implements PreviewQuoteUseCase {

    /** Mismo texto y mismo motivo que en {@code SelfServeQuoteService}. */
    private static final String ARTICULO_NO_CONTRATABLE = "Unknown or unavailable catalog item code";

    private final PriceListQueryPort priceListQueryPort;
    private final PublishedCatalogItemQueryPort publishedCatalogItemQueryPort;
    private final CatalogItemQueryPort catalogItemQueryPort;
    private final CatalogPriceQueryPort catalogPriceQueryPort;
    private final Clock clock;

    public PreviewQuoteService(PriceListQueryPort priceListQueryPort,
            PublishedCatalogItemQueryPort publishedCatalogItemQueryPort,
            CatalogItemQueryPort catalogItemQueryPort, CatalogPriceQueryPort catalogPriceQueryPort,
            Clock clock) {
        this.priceListQueryPort = priceListQueryPort;
        this.publishedCatalogItemQueryPort = publishedCatalogItemQueryPort;
        this.catalogItemQueryPort = catalogItemQueryPort;
        this.catalogPriceQueryPort = catalogPriceQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public QuotePreviewDto preview(PreviewQuoteCommand command) {
        LocalDateTime ahora = LocalDateTime.now(clock);
        PriceListRef tarifa = tarifaVigente(ahora.toLocalDate())
                .orElseThrow(() -> new IllegalStateException(
                        "No published price list is effective on " + ahora.toLocalDate()));
        BillingCycle ciclo = parseBillingCycle(command.billingCycle());

        List<QuoteLine> lineas = QuoteLineFreezer.freeze(traducirCodigos(command, tarifa, ciclo),
                tarifa, ciclo, ahora, catalogItemQueryPort, catalogPriceQueryPort);
        QuoteTotals totales = QuoteTotals.of(lineas);

        return new QuotePreviewDto(tarifa.currency(), ciclo.name(),
                lineas.stream().map(QuotePreviewLineDto::from).toList(), totales.subtotalAmount(),
                totales.discountAmount(), totales.taxAmount(), totales.totalAmount());
    }

    /**
     * El mismo gate que la autocontratacion, y por el mismo motivo: si la vista
     * previa tarifara rotulos que la contratacion rechaza, volveria a existir un
     * numero que la portada promete y el contrato niega. El descuento se escribe en
     * cero porque aqui tampoco hay quien lo negocie.
     */
    private List<QuoteLineCommand> traducirCodigos(PreviewQuoteCommand command, PriceListRef tarifa,
            BillingCycle ciclo) {
        if (command.lines() == null || command.lines().isEmpty()) {
            throw new IllegalArgumentException("preview requires at least one line");
        }
        List<String> codigos = command.lines().stream().map(SelfServeQuoteLineCommand::code)
                .toList();
        SelfServeCartGuard.assertContractable(codigos, publishedCatalogItemQueryPort);

        List<QuoteLineCommand> lineas = new ArrayList<>();
        for (SelfServeQuoteLineCommand linea : command.lines()) {
            Long catalogItemId = publishedCatalogItemQueryPort
                    .findPublishedIdByCode(linea.code(), tarifa.id(), ciclo)
                    .orElseThrow(() -> new IllegalArgumentException(ARTICULO_NO_CONTRATABLE));
            lineas.add(new QuoteLineCommand(catalogItemId, linea.quantity(), BigDecimal.ZERO));
        }
        return List.copyOf(lineas);
    }

    /** Misma eleccion determinista que {@code SelfServeQuoteService}. */
    private Optional<PriceListRef> tarifaVigente(LocalDate hoy) {
        return priceListQueryPort.findAllPublished().stream().filter(l -> l.isEffectiveOn(hoy))
                .max(Comparator.comparing(PriceListRef::validFrom).thenComparing(PriceListRef::id));
    }

    private static BillingCycle parseBillingCycle(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("billingCycle is required");
        }
        try {
            return BillingCycle.valueOf(raw);
        } catch (IllegalArgumentException exception) {
            throw new IllegalArgumentException("Unknown billingCycle: " + raw);
        }
    }
}
