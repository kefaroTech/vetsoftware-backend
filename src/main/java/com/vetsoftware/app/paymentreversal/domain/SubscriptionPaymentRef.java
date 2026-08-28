package com.vetsoftware.app.paymentreversal.domain;

import java.math.BigDecimal;

/**
 * Companion VO del pago que vive en {@code subscriptionpayment}.
 *
 * <p>
 * Es una <strong>copia</strong> de los tres datos que esta feature necesita, no
 * una referencia al agregado ajeno: el vertical slicing prohibe importar el
 * dominio de otra feature, y {@code SubscriptionPaymentQueryPort} es el unico
 * punto que conoce la otra rodaja.
 *
 * <p>
 * <strong>{@code companyId} no es decorativo.</strong> Es la mitad de la clave
 * compuesta {@code (company_id, id)} con la que {@code fk_prr_payment} impide
 * que el expediente de una clinica cuelgue del pago de otra, y el constructor
 * la exige para que un {@code Ref} construido a mano no pueda saltarse la
 * comprobacion que el servicio hace despues.
 *
 * @param amount
 *            importe original del pago. Es el techo de lo que una reversion
 *            aceptada puede llegar a aplicar
 */
public record SubscriptionPaymentRef(Long id, Long companyId, BigDecimal amount) {

    public SubscriptionPaymentRef {
        if (id == null)
            throw new IllegalArgumentException("payment id is required");
        if (companyId == null)
            throw new IllegalArgumentException("payment company id is required");
        if (amount == null)
            throw new IllegalArgumentException("payment amount is required");
        if (amount.signum() <= 0)
            throw new IllegalArgumentException("payment amount must be greater than zero");
    }
}
