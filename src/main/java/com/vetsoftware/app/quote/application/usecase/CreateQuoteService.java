package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.CreateQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.CatalogItemQueryPort;
import com.vetsoftware.app.quote.application.port.out.CatalogPriceQueryPort;
import com.vetsoftware.app.quote.application.port.out.CompanyQueryPort;
import com.vetsoftware.app.quote.application.port.out.PriceListQueryPort;
import com.vetsoftware.app.quote.application.port.out.QuoteNumberPort;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.domain.BillingCycle;
import com.vetsoftware.app.quote.domain.CompanyRef;
import com.vetsoftware.app.quote.domain.PriceListRef;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteLine;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
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
    private final Clock clock;

    public CreateQuoteService(QuoteRepository repository, QuoteNumberPort quoteNumberPort,
            CompanyQueryPort companyQueryPort, PriceListQueryPort priceListQueryPort,
            CatalogItemQueryPort catalogItemQueryPort, CatalogPriceQueryPort catalogPriceQueryPort,
            Clock clock) {
        this.repository = repository;
        this.quoteNumberPort = quoteNumberPort;
        this.companyQueryPort = companyQueryPort;
        this.priceListQueryPort = priceListQueryPort;
        this.catalogItemQueryPort = catalogItemQueryPort;
        this.catalogPriceQueryPort = catalogPriceQueryPort;
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

        Quote quote = Quote.create(quoteNumberPort.next(now.getYear()), company,
                command.prospectName(), command.prospectEmail(), command.prospectDocument(),
                command.prospectPhone(), priceList.id(), billingCycle, command.validUntil(),
                command.trialDays(), command.clientRequestId(), lines, now);
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
     * Delega en {@link QuoteLineFreezer}, que es el unico sitio del proyecto donde
     * una cesta se convierte en lineas con precio. Salio de aqui cuando la vista
     * previa publica necesito la misma cuenta: con dos implementaciones, lo que se
     * muestra y lo que se cobra volverian a poder discrepar.
     */
    private List<QuoteLine> freezeLines(CreateQuoteCommand command, PriceListRef priceList,
            BillingCycle billingCycle, LocalDateTime now) {
        return QuoteLineFreezer.freeze(command.lines(), priceList, billingCycle, now,
                catalogItemQueryPort, catalogPriceQueryPort);
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
