package com.vetsoftware.app.accountingexport.application.command;

import com.vetsoftware.app.accountingexport.domain.AccountingExportKind;
import java.math.BigDecimal;

/**
 * Registrar un fichero de exportacion recien generado.
 *
 * <p>
 * <strong>No trae {@code attemptNumber}</strong>: lo calcula el caso de uso
 * como el siguiente al ultimo intento del mismo mes y clase. Dejarlo en manos
 * del llamador seria invitar a reutilizar un numero y chocar contra
 * {@code uq_accounting_exports_attempt} por un motivo que no tiene nada que ver
 * con el negocio.
 *
 * @param generatedBySystemUserId
 *            quien firma la exportacion. <strong>Lo pone el controller desde
 *            {@code authz.currentSystemUserId()}, nunca el cuerpo</strong>:
 *            aceptarlo por HTTP dejaria firmar un fichero a nombre de otro
 *            superadministrador, y la firma es lo que sostiene la trazabilidad
 *            de quien entrego que al contador
 * @param totalsHash
 *            SHA-256 en minusculas del contenido del fichero. Es lo que permite
 *            demostrar que lo que el contador tiene es lo que se genero
 */
public record GenerateAccountingExportCommand(String periodKey, AccountingExportKind exportKind,
        Long generatedBySystemUserId, BigDecimal totalDebit, BigDecimal totalCredit,
        String totalsHash, String fileRef) {
}
