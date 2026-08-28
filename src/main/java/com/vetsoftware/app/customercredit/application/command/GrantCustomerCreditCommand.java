package com.vetsoftware.app.customercredit.application.command;

import com.vetsoftware.app.customercredit.domain.CreditOriginKind;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Alta de saldo a favor.
 *
 * <p>
 * El origen no es decorativo: {@code chk_cce_origin_branch} exige que cada
 * clase de origen apunte a lo suyo y a nada mas, asi que de los tres
 * identificadores viaja <strong>exactamente uno</strong> —o ninguno, en las
 * ramas que no apuntan a nada—. Lo comprueba el constructor de la entidad de
 * dominio, no este record.
 *
 * @param clientRequestId
 *            llave de idempotencia, <strong>obligatoria</strong>: un libro de
 *            dinero sin ella es un doble clic esperando. La respalda
 *            {@code uq_cce_idempotency}
 * @param expiresOn
 *            cuando caduca este lote, o {@code null} si no caduca. Solo un alta
 *            puede llevarla ({@code chk_cce_expiry_only_on_grant})
 */
public record GrantCustomerCreditCommand(Long companyId, BigDecimal amount,
        CreditOriginKind originKind, Long originPaymentId, Long originDocumentId,
        Long originSubscriptionId, LocalDate expiresOn, String clientRequestId) {
}
