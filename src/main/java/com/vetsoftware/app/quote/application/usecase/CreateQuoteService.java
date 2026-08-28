package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.QuoteAnswerCommand;
import com.vetsoftware.app.quote.application.command.QuoteLineCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.CreateQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.quote.application.port.out.CatalogPriceQueryPort;
import com.vetsoftware.app.quote.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.quote.application.port.out.ConfiguratorQuestionQueryPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.QuoteNumberPort;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CatalogItemRef;
import com.vetsoftware.app.quote.domain.CatalogPriceRef;
import com.vetsoftware.app.quote.domain.CompanyRef;
import com.vetsoftware.app.quote.domain.ConfiguratorQuestionRef;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteAnswer;
import com.vetsoftware.app.quote.domain.QuoteLine;
import com.vetsoftware.app.quote.domain.TieredPrice;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Emite la cotizacion CONGELANDO el catalogo y la tarifa en el mismo acto.
 *
 * <p>
 * Dos invariantes gobiernan esta clase, y las dos existen porque su ausencia
 * cuesta dinero de verdad:
 *
 * <ul>
 * <li><b>Idempotencia (R13).</b> Se busca por {@code clientRequestId} ANTES de
 * insertar, dentro de la transaccion, y si ya existe se devuelve la cotizacion
 * que nacio la primera vez. La constraint unica sigue estando como red, pero
 * apoyarse solo en ella convierte un doble clic en un 500 en la cara del
 * cliente, que no es una respuesta idempotente.
 * <li><b>Cuadre de totales (R5).</b> Los cuatro totales NO llegan de fuera: los
 * calcula {@code Quote.create} sumando las lineas. Asi el numero que firma el
 * cliente es exactamente el que suman los renglones que leyo, y no hay ningun
 * camino de codigo capaz de separarlos.
 * </ul>
 *
 * <p>
 * Y una tercera cosa que este servicio hace y conviene no perder: el precio, el
 * nombre y la tarifa de IVA se leen aqui del catalogo y se COPIAN a la linea.
 * Nunca vuelven a leerse. Si el cliente pudiera enviarlos, cotizar a cero seria
 * un campo de formulario.
 */
@Observed(name = "quote.create")
@Service
public class CreateQuoteService implements CreateQuoteUseCase {

    private final QuoteRepository repository;
    private final QuoteNumberPort quoteNumberPort;
    private final CompanyQueryPort companyQueryPort;
    private final PriceListQueryPort priceListQueryPort;
    private final CatalogItemQueryPort catalogItemQueryPort;
    private final CatalogPriceQueryPort catalogPriceQueryPort;
    private final ConfiguratorQuestionQueryPort configuratorQuestionQueryPort;
    private final Clock clock;

    public CreateQuoteService(QuoteRepository repository, QuoteNumberPort quoteNumberPort,
            CompanyQueryPort companyQueryPort, PriceListQueryPort priceListQueryPort,
            CatalogItemQueryPort catalogItemQueryPort, CatalogPriceQueryPort catalogPriceQueryPort,
            ConfiguratorQuestionQueryPort configuratorQuestionQueryPort, Clock clock) {
        this.repository = repository;
        this.quoteNumberPort = quoteNumberPort;
        this.companyQueryPort = companyQueryPort;
        this.priceListQueryPort = priceListQueryPort;
        this.catalogItemQueryPort = catalogItemQueryPort;
        this.catalogPriceQueryPort = catalogPriceQueryPort;
        this.configuratorQuestionQueryPort = configuratorQuestionQueryPort;
        this.clock = clock;
    }

    @Override
    @Transactional
    public QuoteDto execute(CreateQuoteCommand command) {
        // La busqueda de idempotencia va ACOTADA en cuanto hay empresa. Sin acotar,
        // el clientRequestId -que lo elige quien llama- convierte este endpoint de
        // escritura en una lectura cross-tenant: reutilizar la llave de otra clinica
        // devolveria su cotizacion entera, con precios, descuentos y la prueba de
        // aceptacion. La rama ancha NO sobra: una oferta a prospecto tiene
        // company_id nulo y ningun WHERE company_id = ? casaria con ella.
        Optional<Quote> alreadyCreated = command.companyId() == null
                ? repository.findByClientRequestId(command.clientRequestId())
                : repository.findByClientRequestIdAndCompanyId(command.clientRequestId(),
                        command.companyId());
        if (alreadyCreated.isPresent()) {
            return QuoteDto.from(alreadyCreated.get());
        }

        LocalDateTime now = LocalDateTime.now(clock);
        BillingCycle billingCycle = parseBillingCycle(command.billingCycle());
        PriceListRef priceList = priceListQueryPort.findPublishedById(command.priceListId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Published price list not found: " + command.priceListId()));
        // D-73 (COT-020/COT-021): publicada NO es lo mismo que vigente. Una lista del
        // ano pasado que nadie archivo sigue en PUBLISHED y ponia precio a la
        // cotizacion de hoy sin que nada lo delatara. El precio sale de la lista
        // vigente POR FECHA, y la fecha es hoy EN BOGOTA: se deriva del reloj inyectado
        // -que es el unico que lleva la zona del negocio (D-81)- y no de un
        // LocalDate.now() pelado, que entre las 19:00 y la medianoche ya contesta
        // manana y rechazaria una cotizacion legitima el ultimo dia de la tarifa.
        // El camino del contrato aplica este mismo predicado antes de firmar; hasta
        // ahora el de la cotizacion, por donde entra el dinero nuevo, no lo aplicaba.
        priceList.requireEffectiveOn(now.toLocalDate());
        CompanyRef company = resolveCompany(command.companyId());

        List<QuoteLine> lines = freezeLines(command, priceList, billingCycle, now);
        List<QuoteAnswer> answers = captureAnswers(command.answers(), now);

        Quote quote = Quote.create(quoteNumberPort.next(now.getYear()), company,
                command.prospectName(), command.prospectEmail(), command.prospectDocument(),
                command.prospectPhone(), priceList.id(), billingCycle, command.validUntil(),
                command.trialDays(), command.clientRequestId(), lines, answers, now);
        return QuoteDto.from(repository.save(quote));
    }

    private CompanyRef resolveCompany(Long companyId) {
        // Sin companyId es una oferta a un prospecto que todavia no es empresa:
        // el caso raro de este modelo, y el motivo de que quotes.company_id sea
        // nulable. El @PreAuthorize del puerto ya lo restringio a SYSTEM.
        if (companyId == null) {
            return null;
        }
        return companyQueryPort.findById(companyId)
                .orElseThrow(() -> new IllegalArgumentException("Company not found: " + companyId));
    }

    /**
     * D-66 / R-PRICE-04: la cantidad se parte ACUMULATIVAMENTE entre los tramos, y
     * cada tramo produce SU PROPIO RENGLON.
     *
     * <p>
     * Quince usuarios con "unidades extra 1 a 8 a 12.000 y de la 9 en adelante a
     * 9.000" salen como dos renglones -ocho a 12.000 y cinco a 9.000, 141.000- y no
     * como uno de trece al precio del tramo alto, que daba 117.000. Que sean dos
     * renglones y no un importe calculado a mano es lo que hace que el cliente vea
     * en la oferta el mismo desglose con el que se le va a facturar (R-QUOTE-09) y
     * que los totales sigan siendo la suma de las lineas sin ninguna excepcion.
     *
     * <p>
     * El reparto lo hace {@link TieredPrice} y la cantidad de cada renglon la
     * recalcula {@link QuoteLine} en su constructor: este servicio no sabe
     * multiplicar y no puede equivocarse en la cuenta.
     */
    private List<QuoteLine> freezeLines(CreateQuoteCommand command, PriceListRef priceList,
            BillingCycle billingCycle, LocalDateTime now) {
        if (command.lines() == null || command.lines().isEmpty()) {
            throw new IllegalArgumentException("quote requires at least one line");
        }
        List<QuoteLine> lines = new ArrayList<>();
        int lineNumber = 1;
        for (QuoteLineCommand requested : command.lines()) {
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
            // R15: lo incluido en la tarifa se resta antes de repartir por tramos, y la
            // resta la hace el dominio -TieredPrice- porque es quien resuelve el precio
            // quien tiene el included_quantity. Si lo contratado no supera a lo incluido
            // no hay nada que cobrar, el reparto sale vacio y no se emite ninguna linea:
            // chk_quote_lines_quantity exige quantity > 0. Las tres cifras -contratada,
            // incluida y cobrada- quedan congeladas en cada renglon que si se emite, para
            // que la oferta se explique sin volver a la tarifa.
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

    private List<QuoteAnswer> captureAnswers(List<QuoteAnswerCommand> requested,
            LocalDateTime now) {
        if (requested == null) {
            return List.of();
        }
        List<QuoteAnswer> answers = new ArrayList<>();
        for (QuoteAnswerCommand answer : requested) {
            ConfiguratorQuestionRef question = configuratorQuestionQueryPort
                    .findById(answer.questionId()).orElseThrow(() -> new IllegalArgumentException(
                            "Configurator question not found: " + answer.questionId()));
            answers.add(
                    QuoteAnswer.capture(question, answer.optionId(), answer.answerValue(), now));
        }
        return answers;
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
