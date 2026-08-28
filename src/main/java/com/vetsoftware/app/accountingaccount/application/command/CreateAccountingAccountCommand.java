package com.vetsoftware.app.accountingaccount.application.command;

import com.vetsoftware.app.accountingaccount.domain.AccountClass;
import java.time.LocalDate;

/**
 * <strong>Sin {@code companyId}, y aqui ni siquiera es la regla de siempre: es
 * que no existe la columna.</strong> {@code accounting_accounts} es el plan de
 * cuentas de VetSoftware, no de la clinica.
 *
 * @param parentCode
 *            codigo de la cuenta padre. Vacio si y solo si
 *            {@code accountLevel == 1}; el «si y solo si» lo valida el dominio,
 *            que es donde vive porque mira dos campos
 * @param accountLevel
 *            1 clase, 2 grupo, 4 cuenta, 6 subcuenta
 * @param postable
 *            solo el nivel 6 admite asiento. Marcar un grupo descuadra el
 *            balance de prueba por arrastre
 * @param validTo
 *            nulo abre la vigencia; con fecha la cuenta entra ya cerrada, que
 *            es como se carga un plan historico
 */
public record CreateAccountingAccountCommand(String code, String name, AccountClass accountClass,
        String parentCode, int accountLevel, boolean postable, boolean requiresThirdParty,
        LocalDate validFrom, LocalDate validTo) {
}
