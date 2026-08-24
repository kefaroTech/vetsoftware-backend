package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.AddSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Anadir un articulo abre una fila nueva. Nunca reescribe una existente.
 *
 * <p>
 * <strong>Sigue cerrado a la plataforma, y NO por inercia: mientras el precio
 * llegue en el cuerpo, abrirlo al tenant es regalar el producto.</strong>
 * {@code SubscriptionItemLineRequest} declara {@code unitAmount},
 * {@code taxRate} e {@code includedQuantity}, y
 * {@code AddSubscriptionItemService} los copia tal cual a
 * {@code subscription_items} sin volver a mirar la tarifa. Con este puerto
 * abierto a una autoridad de empresa, una administradora de clinica anade
 * cualquier articulo del catalogo con {@code unitAmount = 0}, el
 * {@code SubscriptionChangedPort} recalcula y le concede el modulo, y el otrosi
 * queda firmado con su nombre como si fuera legitimo. No es un 403 de menos: es
 * una alta gratuita autoservida.
 *
 * <p>
 * El repositorio ya tiene escrito el criterio, en {@code CreateQuoteService}:
 * <i>«el precio, el nombre y la tarifa de IVA se leen aqui del catalogo y se
 * COPIAN a la linea. Si el cliente pudiera enviarlos, cotizar a cero seria un
 * campo de formulario»</i>. Aqui lo es.
 *
 * <p>
 * <strong>Que hace falta para abrirlo</strong> —y es una feature, no un cambio
 * de anotacion—: un caso de uso hermano para el tenant que reciba solo
 * {@code catalogItemId} y {@code quantity} y resuelva precio, impuesto y lo
 * incluido en servidor contra la lista de precios publicada del contrato, igual
 * que {@code CreateQuoteService.freezeLines}. Ese es el flujo «Ampliar mi plan»
 * del documento de diseno. Cambiar solo el {@code @PreAuthorize} lo abriria
 * roto.
 *
 * <p>
 * El cuerpo no trae precio en {@link ChangeSubscriptionItemQuantityUseCase} ni
 * en {@link RemoveSubscriptionItemUseCase}, y por eso esos dos si estan
 * abiertos al tenant.
 */
public interface AddSubscriptionItemUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionItemDto execute(AddSubscriptionItemCommand command);
}
