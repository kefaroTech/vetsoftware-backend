package com.vetsoftware.app.companybillingprofile.application.command;

import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import java.time.LocalDate;

/**
 * Apertura de la primera ficha de facturacion de una empresa.
 *
 * <p>
 * <strong>El {@code companyId} viaja en el command pero NO en el
 * request.</strong> Lo pone el controller con {@code authz.currentCompanyId()}
 * y lo revalida el {@code @PreAuthorize} del puerto; aceptarlo del cuerpo
 * dejaria que un cliente abriera la ficha de facturacion de otra empresa, que
 * es decidir a quien se le cobra.
 *
 * @param cityId
 *            el municipio se resuelve en el caso de uso via
 *            {@code CityQueryPort}, no en el repositorio: asi la FK inexistente
 *            sale como un 400 con el id delante y no como una violacion de
 *            clave foranea
 * @param withholdingAgent
 *            si el <strong>cliente</strong> es agente de retencion. Que Lumbre
 *            sea autorretenedor es dato propio y vive en
 *            {@code platform_tax_profiles.is_self_withholder} (changeset 367):
 *            coexisten, no se deducen. <b>Antes este javadoc decia
 *            {@code platform_billing_config} y era falso</b> —esa tabla no
 *            tiene ninguna columna fiscal—; la tabla correcta existe desde 367
 *            pero sigue <b>sin sembrar a proposito</b>, porque no habia razon
 *            social ni NIT reales que poner
 * @param validFrom
 *            desde cuando rige. Va explicito y no se toma del reloj porque una
 *            ficha puede firmarse hoy para regir desde el primero del mes que
 *            viene
 */
public record OpenCompanyBillingProfileCommand(PersonKind personKind, TaxIdKind taxIdKind,
        String taxId, String verificationDigit, String legalName, String firstName,
        String middleName, String lastName, String secondLastName, String address, Long cityId,
        String billingEmail, TaxRegime taxRegime, boolean withholdingAgent, LocalDate validFrom,
        Long companyId) {
}
