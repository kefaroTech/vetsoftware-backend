package com.vetsoftware.app.subscriptionpaymentmethod.application.command;

import com.vetsoftware.app.subscriptionpaymentmethod.domain.PaymentMethodKind;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Alta de un medio de pago.
 *
 * <p>
 * {@code companyId} viaja aqui pero <strong>no en el request HTTP</strong>: lo
 * inyecta el controller desde el principal autenticado. Son datos de la
 * clinica, y quien firma la peticion es quien decide de que clinica son.
 *
 * @param token
 *            el testigo de la pasarela. <strong>Nunca el numero de la
 *            tarjeta</strong>: si algun dia llega un PAN por este campo, el
 *            problema no es de validacion sino de integracion
 * @param mandateEvidence
 *            constancia de la autorizacion expresa. Obligatoria: sin ella la
 *            autorizacion es una afirmacion propia y no se puede exhibir ante
 *            un tercero
 */
public record RegisterSubscriptionPaymentMethodCommand(Long companyId, PaymentMethodKind methodKind,
        String gateway, String token, String brand, String lastFour, LocalDate expiresOn,
        String mandateEvidence, LocalDateTime authorizedAt) {
}
