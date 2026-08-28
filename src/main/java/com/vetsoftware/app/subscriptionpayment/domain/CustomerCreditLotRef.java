package com.vetsoftware.app.subscriptionpayment.domain;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Companion VO del lote de saldo a favor que vive en {@code customercredit}.
 *
 * <p>
 * <b>Un lote, no "el saldo".</b> El saldo a favor se lleva por lotes porque sin
 * ellos la caducidad no es calculable: cien mil que caducan en diciembre mas
 * cincuenta mil sin fecha, consumidos ciento veinte mil, admite dos respuestas
 * defendibles. Aplicar contra una factura consume <b>de un lote concreto</b>, y
 * por eso lo que este VO trae es el {@code GRANT} y no una suma.
 *
 * @param grantedAmount
 *            el importe con el que nacio el lote. Es el techo de R3: la suma de
 *            lo aplicado desde el no puede pasarse
 * @param expiresOn
 *            nulo si el lote no caduca. Un lote caducado no puede aplicarse: su
 *            remanente ya se dio de baja con un asiento de caducidad, y
 *            aplicarlo despues seria gastar dos veces el mismo dinero
 */
public record CustomerCreditLotRef(Long id, Long companyId, BigDecimal grantedAmount,
        LocalDate expiresOn) {

    public CustomerCreditLotRef {
        if (id == null)
            throw new IllegalArgumentException("credit entry id is required");
        if (companyId == null)
            throw new IllegalArgumentException("credit entry companyId is required");
        if (grantedAmount == null || grantedAmount.signum() <= 0)
            throw new IllegalArgumentException(
                    "a credit lot must have been granted a positive amount");
    }

    /** {@code true} si el lote ya caduco el dia indicado. */
    public boolean haCaducado(LocalDate day) {
        return expiresOn != null && day != null && day.isAfter(expiresOn);
    }
}
