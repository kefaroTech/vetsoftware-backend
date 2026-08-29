package com.vetsoftware.app.quote.application.port.out;

import com.vetsoftware.app.quote.application.command.CreateQuoteCommand;
import com.vetsoftware.app.quote.application.dto.QuoteDto;

/**
 * Emite una oferta con los privilegios de la plataforma: la crea y la deja
 * {@code SENT}.
 *
 * <p>
 * <strong>Existe para no duplicar el embudo.</strong> El camino de
 * autocontratacion resuelve en servidor los terminos economicos y despues
 * necesita exactamente lo que {@code CreateQuoteUseCase} y
 * {@code SendQuoteUseCase} ya hacen —congelar las lineas troceando la cantidad
 * por tramos acumulativos (D-66), exigir tarifa vigente por fecha (D-73), ser
 * idempotente por {@code clientRequestId}—. Copiar eso seria copiar dinero: dos
 * implementaciones que nada obliga a mover juntas empiezan a cotizar distinto y
 * el desajuste se descubre facturando.
 *
 * <p>
 * <strong>Por que un puerto y no llamar a los casos de uso
 * directamente.</strong> Los dos estan cerrados a {@code hasRole('SYSTEM')} —y
 * deben seguir estandolo: enviar es el acto por el que la plataforma publica su
 * propia oferta—, asi que la llamada tiene que ir bajo
 * {@code SystemAuthRunner}. Y ese vive en {@code auth.infrastructure}, que
 * {@code ..application..} no puede tocar
 * ({@code APPLICATION_NO_CONOCE_INFRASTRUCTURE}). El adaptador es
 * {@code PlatformQuoteIssuerAdapter}, en {@code infrastructure/orchestration},
 * que es donde el arbol pone siempre esta escalada — igual que
 * {@code registration} con {@code CreateCompanyAdapter}.
 */
public interface PlatformQuoteIssuerPort {

    /**
     * Crea la oferta y la emite. Si el {@code clientRequestId} ya existia devuelve
     * la que habia, <strong>sin reenviarla</strong>: reenviar una ya emitida —o ya
     * aceptada— lanzaria {@code InvalidQuoteStatusTransitionException} y
     * convertiria un reintento inofensivo en un 409 en mitad de una compra.
     */
    QuoteDto issue(CreateQuoteCommand command);
}
