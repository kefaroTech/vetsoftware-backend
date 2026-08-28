package com.vetsoftware.app.billingdocumentstatushistory.application.command;

import com.vetsoftware.app.billingdocumentstatushistory.domain.BillingDocumentStatus;

/**
 * @param companyId
 *            la empresa dueña del documento. <strong>Nunca llega del
 *            cuerpo</strong>: lo inyecta el controller desde
 *            {@code authz.currentCompanyId()} y el puerto lo revalida con
 *            {@code @authz.isMyCompany(#command.companyId)}
 *            ({@code EMPRESA_NO_VIAJA_EN_EL_CUERPO})
 * @param fromStatus
 *            de donde venia el documento. Obligatorio y distinto de
 *            {@code toStatus}; lo comprueba el dominio
 * @param actor
 *            quien lo movio: el nombre de una persona o el del proceso
 *            automatico. Texto y no una FK a {@code system_users} a proposito
 *            —un proceso no tiene fila alli—
 * @param reason
 *            por que se movio, en lenguaje que se entienda seis meses despues
 */
public record RecordBillingDocumentStatusChangeCommand(Long companyId, Long billingDocumentId,
        BillingDocumentStatus fromStatus, BillingDocumentStatus toStatus, String actor,
        String reason) {
}
