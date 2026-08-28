package com.vetsoftware.app.accountmapping.application.command;

import com.vetsoftware.app.accountmapping.domain.MappingKind;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId}: la tabla no tiene esa columna.</strong> El
 * puente concepto → cuenta es de los libros de VetSoftware.
 *
 * @param mappingKey
 *            la subclave dentro de la clase. Nunca nula: donde no hay subclave
 *            se escribe {@code '-'}
 * @param catalogItemId
 *            solo para {@code REVENUE} y {@code DEFERRED_REVENUE}, igual que
 *            {@code chargeType} y {@code taxTreatment}. El «solo» lo valida el
 *            dominio, que es donde vive porque mira dos campos
 * @param deferredAccountCode
 *            la cuenta donde se aparca el ingreso cobrado y no devengado. Es
 *            clave foranea de verdad contra {@code accounting_accounts(code)},
 *            no texto suelto
 * @param validTo
 *            nulo abre la vigencia; con fecha el mapeo entra ya cerrado
 */
public record CreateAccountMappingCommand(MappingKind mappingKind, String mappingKey,
        Long catalogItemId, String chargeType, String taxTreatment, String debitAccountCode,
        String creditAccountCode, String deferredAccountCode, LocalDate validFrom,
        LocalDate validTo) {
}
