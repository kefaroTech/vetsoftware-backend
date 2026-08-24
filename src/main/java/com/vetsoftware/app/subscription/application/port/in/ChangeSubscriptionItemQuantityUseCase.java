package com.vetsoftware.app.subscription.application.port.in;

import com.vetsoftware.app.subscription.application.command.ChangeSubscriptionItemQuantityCommand;
import com.vetsoftware.app.subscription.application.dto.SubscriptionItemDto;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Cambiar de cantidad es cerrar la linea y abrir otra. Devuelve la sucesora. El
 * precio unitario y lo incluido viajan intactos desde la original.
 *
 * <p>
 * <strong>Es del cliente, y lo es precisamente por esa frase.</strong> El
 * cuerpo de la peticion no trae precio: {@code unitAmount}, {@code taxRate} y
 * {@code includedQuantity} los copia el servidor de la linea original, que las
 * congelo la plataforma cuando se firmo. El cliente elige cuantas unidades, no
 * a cuanto; no hay ningun campo por el que pueda ponerse su propio precio. Ese
 * es el criterio que separa este caso de uso de
 * {@link AddSubscriptionItemUseCase}, que si recibe el precio en el cuerpo y
 * por eso sigue cerrado a la plataforma.
 *
 * <p>
 * {@code companyId} NO viaja en el cuerpo: lo inyecta el controller desde el
 * principal y aqui se revalida.
 *
 * <p>
 * <strong>Lo que este gate NO cubre.</strong> {@code prorationAmount} y
 * {@code monthlyDeltaAmount} si llegan del cuerpo y se persisten tal cual: este
 * slice no tiene motor de prorrateo y esa aritmetica no esta en el modelo. Hoy
 * ninguna cifra de cobro se deriva de esas dos columnas —la base recurrente
 * sale de {@code subscription_items}—, pero el dia que se derive hay que
 * resolverlas en servidor antes de dejarlas en manos del tenant.
 */
public interface ChangeSubscriptionItemQuantityUseCase {
    @PreAuthorize("hasRole('SYSTEM') or (hasAuthority('subscription.update') "
            + "and @authz.isMyCompany(#command.companyId))")
    SubscriptionItemDto execute(ChangeSubscriptionItemQuantityCommand command);
}
