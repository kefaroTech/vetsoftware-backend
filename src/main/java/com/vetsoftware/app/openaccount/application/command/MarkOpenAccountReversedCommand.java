package com.vetsoftware.app.openaccount.application.command;

import java.time.LocalDateTime;

/**
 * Reverso contable de una cuenta ya facturada. Lo dispara la validacion DIAN de
 * la nota credito que corrige su factura, nunca un cliente REST.
 *
 * @param openAccountId
 *            cuenta a reversar.
 * @param companyId
 *            empresa dueña de la cuenta. Acota la lectura: una cuenta de otro
 *            tenant no se encuentra y por tanto no se escribe.
 * @param reversedAt
 *            instante del reverso; {@code null} deja que el dominio ponga el
 *            suyo.
 */
public record MarkOpenAccountReversedCommand(Long openAccountId, Long companyId,
        LocalDateTime reversedAt) {
}
