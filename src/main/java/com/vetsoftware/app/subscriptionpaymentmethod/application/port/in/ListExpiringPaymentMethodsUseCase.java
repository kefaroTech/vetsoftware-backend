package com.vetsoftware.app.subscriptionpaymentmethod.application.port.in;

import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.subscriptionpaymentmethod.application.dto.SubscriptionPaymentMethodDto;
import java.time.LocalDate;
import org.springframework.security.access.prepost.PreAuthorize;

/**
 * Uno de los barridos de plataforma: <strong>las tarjetas por vencer de todas
 * las clinicas</strong>.
 *
 * <p>
 * Su indice ({@code ix_subscription_payment_methods_expiring}) va
 * deliberadamente <em>sin la empresa delante</em>, porque recorre el parque
 * entero; ponersela lo haria inutil. Pero declararlo en el changeset no exime
 * al caso de uso de la regla de aislamiento —esa regla recorre codigo, no
 * documentos—, asi que el barrido nace con puerto declarado y con la
 * autorizacion restringida a plataforma.
 *
 * <p>
 * <strong>La salida facil habria sido peor que el problema:</strong> escribirlo
 * como proceso suelto, sin puerto, lo dejaria invisible a las reglas de
 * aislamiento y sin ninguna autorizacion — y el dia que alguien le pusiera un
 * boton, cualquiera listaria las tarjetas por expirar de las quinientas
 * clinicas.
 *
 * <p>
 * Lo que el cliente necesita ver de las suyas sale por
 * {@link ListSubscriptionPaymentMethodsUseCase}, que si va acotado.
 */
public interface ListExpiringPaymentMethodsUseCase {

    @PreAuthorize("hasRole('SYSTEM')")
    PageResult<SubscriptionPaymentMethodDto> listExpiring(LocalDate before, int page, int pageSize);
}
