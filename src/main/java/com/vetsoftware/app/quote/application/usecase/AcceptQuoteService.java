package com.vetsoftware.app.quote.application.usecase;

import com.vetsoftware.app.quote.application.command.AcceptQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.AcceptQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.QuoteAuditPort;
import com.vetsoftware.app.quote.application.port.out.QuoteRepository;
import com.vetsoftware.app.quote.application.port.out.SubscriptionProvisioningPort;
import com.vetsoftware.app.quote.domain.Quote;
import com.vetsoftware.app.quote.domain.QuoteNotFoundException;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import io.micrometer.observation.annotation.Observed;
import java.time.Clock;
import java.time.LocalDateTime;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * SENT -> ACCEPTED, sellando la prueba de la aceptacion <strong>y firmando el
 * contrato que de ella se deriva</strong> (DC-2).
 *
 * <p>
 * El reloj se inyecta: la fecha de aceptacion es la prueba, y una prueba que
 * depende del reloj de la maquina no se puede fijar en un test.
 *
 * <p>
 * <strong>La aceptacion y el contrato ocurren en el mismo commit.</strong>
 * Hasta DC-2 aceptar solo movia un estado y el contrato habia que pedirlo
 * despues por otro endpoint: entre las dos cosas existia una ventana en la que
 * el cliente habia firmado y no tenia nada, y nada garantizaba que el segundo
 * paso llegara a darse. Provisionar aqui dentro deja exactamente dos
 * desenlaces: aceptada y contratada, o ninguna de las dos. Ver
 * {@link SubscriptionProvisioningPort}.
 */
@Observed(name = "quote.accept")
@Service
public class AcceptQuoteService implements AcceptQuoteUseCase {

    /** El documento de cobro no modela todavia la divisa: siempre "COP". */
    private static final String CURRENCY = "COP";

    private final QuoteRepository repository;
    private final SubscriptionProvisioningPort subscriptionProvisioningPort;
    private final QuoteAuditPort audit;
    private final Clock clock;

    public AcceptQuoteService(QuoteRepository repository,
            SubscriptionProvisioningPort subscriptionProvisioningPort, QuoteAuditPort audit,
            Clock clock) {
        this.repository = repository;
        this.subscriptionProvisioningPort = subscriptionProvisioningPort;
        this.audit = audit;
        this.clock = clock;
    }

    @Override
    @Transactional
    public QuoteDto execute(AcceptQuoteCommand command) {
        Quote quote = (command.companyId() == null
                ? repository.findById(command.id())
                : repository.findByIdAndCompanyId(command.id(), command.companyId()))
                .orElseThrow(() -> new QuoteNotFoundException(command.id()));

        // Reintento tras una respuesta HTTP perdida: la aceptacion y el contrato ya
        // nacieron en el commit anterior, asi que se devuelve el mismo documento sin
        // volver a firmar. Aceptada por OTRO correo sigue cayendo en Quote.accept, que
        // la rechaza con 409 al no estar en SENT.
        if (quote.getStatus() == QuoteStatus.ACCEPTED
                && command.acceptedByEmail().equals(quote.getAcceptedByEmail())) {
            return QuoteDto.from(quote);
        }

        LocalDateTime now = LocalDateTime.now(clock);
        quote.accept(command.acceptedByEmail(), command.acceptedIp(), now, now.toLocalDate());
        Quote accepted = repository.save(quote);

        // El contrato nace aqui, en esta misma transaccion. Nada se captura: un fallo
        // al provisionar tiene que revertir tambien la aceptacion, o quedaria una
        // cotizacion firmada sin contrato detras y solo se arreglaria a mano.
        //
        // Una oferta a un PROSPECTO no tiene empresa (chk_quotes_party: o empresa, o
        // al menos nombre del prospecto) y por tanto no tiene donde poner un
        // contrato: se acepta igual —la aceptacion es la prueba de que el prospecto
        // dijo que si, y el embudo la necesita— y el contrato se firmara cuando esa
        // empresa exista. No es un caso degradado, es el orden real del embudo.
        Long subscriptionId = accepted.getCompany() == null
                ? null
                : subscriptionProvisioningPort.provisionFromAcceptedQuote(accepted.getId(),
                        accepted.getCompany().id());
        audit.quoteAccepted(accepted.getId(), accepted.getQuoteNumber(), accepted.getCompanyId(),
                subscriptionId, accepted.getTotalAmount(), CURRENCY, accepted.getAcceptedByEmail(),
                accepted.getAcceptedIp());
        return QuoteDto.from(accepted);
    }
}
