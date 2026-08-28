package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.AddSubscriptionItemCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Anadir un articulo abre una fila nueva. Nunca reescribe una existente.
 *
 * <p>
 * <strong>El precio ya NO llega en el cuerpo (R-QUOTE-02).</strong> La peticion
 * declaraba {@code unitAmount} —admitiendo el cero explicitamente—,
 * {@code itemName}, {@code itemType}, {@code capacityUnit},
 * {@code includedQuantity} sin techo y {@code taxRate}, y el servicio los
 * copiaba tal cual a {@code subscription_items} con una sola comprobacion: que
 * el articulo existiera. Se podia abrir una linea a cero pesos, con nombre
 * inventado, o con nueve mil novecientas noventa y nueve unidades incluidas que
 * iban directas al techo del contador sin pasar por ninguna tarifa. Y el
 * prorrateo «que calcula el servidor» se calculaba sobre ese importe del
 * cuerpo, asi que la proteccion que su comentario reclamaba era hueca.
 *
 * <p>
 * Hoy {@code AddSubscriptionItemService} resuelve el snapshot completo contra
 * la tarifa publicada <strong>del propio contrato</strong> y vigente por fecha
 * (D-73), exactamente como {@code CreateRequestedSubscriptionService} y como
 * dice {@code CreateQuoteService}: <i>«el precio, el nombre y la tarifa de IVA
 * se leen aqui del catalogo y se COPIAN a la linea. Si el cliente pudiera
 * enviarlos, cotizar a cero seria un campo de formulario»</i>. El cuerpo trae
 * seleccion —articulo, cantidad y fechas— y nada mas.
 *
 * <p>
 * <strong>Se mantiene {@code hasRole('SYSTEM')} y ahora es una decision de
 * politica comercial, no una barandilla tecnica.</strong> El motivo por el que
 * este puerto estaba cerrado —que abrirlo al tenant era regalar el producto—
 * desaparecio con el precio del cuerpo. Abrirlo es el flujo «Ampliar mi plan»
 * del documento de diseno y hace falta decidir antes que exige: R-SUB-24 pide
 * que toda linea que cobra referencie una aceptacion probada, y este caso de
 * uso todavia no la enlaza. Cambiar la anotacion sin eso abriria un cobro que
 * nadie acepto.
 *
 * <p>
 * El cuerpo tampoco trae precio en
 * {@link ChangeSubscriptionItemQuantityUseCase} ni en
 * {@link RemoveSubscriptionItemUseCase}, y por eso esos dos si estan abiertos
 * al tenant.
 */
public interface AddSubscriptionItemUseCase {
    @PreAuthorize("hasRole('SYSTEM')")
    SubscriptionItemDto execute(AddSubscriptionItemCommand command);
}
