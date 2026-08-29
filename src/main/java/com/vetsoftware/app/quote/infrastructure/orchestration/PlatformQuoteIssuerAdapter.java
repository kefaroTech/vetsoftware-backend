package com.vetsoftware.app.quote.infrastructure.orchestration;

import com.vetsoftware.app.auth.infrastructure.security.SystemAuthRunner;
import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.command.SendQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;
import com.vetsoftware.app.quote.application.port.in.CreateQuoteUseCase;
import com.vetsoftware.app.quote.application.port.in.SendQuoteUseCase;
import com.vetsoftware.app.quote.application.port.out.PlatformQuoteIssuerPort;
import com.vetsoftware.app.quote.domain.QuoteStatus;
import org.springframework.stereotype.Component;

/**
 * La escalada a plataforma, acotada a dos llamadas.
 *
 * <p>
 * <strong>Que se escala y que no.</strong> {@link SystemAuthRunner} intercambia
 * el principal del hilo mientras corre la accion y lo devuelve en el
 * {@code finally}. Aqui dentro se ejecutan exactamente dos casos de uso, los
 * dos con la empresa <em>ya decidida</em> y viajando dentro del command:
 * ninguno de los dos lee el principal para saber de quien es la cotizacion, asi
 * que la escalada no cambia sobre que filas se actua, solo permite actuar. La
 * empresa que entra la revalido antes {@code SelfServeQuoteUseCase} con
 * {@code @authz.isMyCompany}.
 *
 * <p>
 * <strong>Por que la escalada es legitima y no un rodeo del gate.</strong>
 * Enviar es, por escrito, «el acto por el que la plataforma publica su propia
 * oferta y la vuelve vinculante»; el cliente no se envia una oferta a si mismo.
 * En este flujo <em>tampoco lo hace</em>: el cliente pide, y quien compone la
 * oferta con precios de lista y la emite es el servidor. Abrir
 * {@code SendQuoteUseCase} al tenant habria sido el rodeo; esto es la
 * plataforma respondiendo.
 *
 * <p>
 * <strong>Lo que hay que vigilar al mantener.</strong> Si algun dia
 * {@code CreateQuoteService} o {@code SendQuoteService} empiezan a resolver la
 * empresa desde el principal en vez de recibirla en el command, esta escalada
 * les daria el de plataforma y la cotizacion se crearia sin empresa. Hoy los
 * dos la reciben.
 */
@Component
public class PlatformQuoteIssuerAdapter implements PlatformQuoteIssuerPort {

    private final CreateQuoteUseCase createUseCase;
    private final SendQuoteUseCase sendUseCase;
    private final SystemAuthRunner systemAuthRunner;

    public PlatformQuoteIssuerAdapter(CreateQuoteUseCase createUseCase,
            SendQuoteUseCase sendUseCase, SystemAuthRunner systemAuthRunner) {
        this.createUseCase = createUseCase;
        this.sendUseCase = sendUseCase;
        this.systemAuthRunner = systemAuthRunner;
    }

    @Override
    public QuoteDto issue(CreateQuoteCommand command) {
        return systemAuthRunner.call(() -> {
            QuoteDto quote = createUseCase.execute(command);
            // La idempotencia de CreateQuoteService puede devolver una que ya se emitio o
            // que el cliente ya acepto. Ver el javadoc del puerto.
            if (!QuoteStatus.DRAFT.name().equals(quote.status())) {
                return quote;
            }
            return sendUseCase.execute(new SendQuoteCommand(quote.id(), command.companyId()));
        });
    }
}
