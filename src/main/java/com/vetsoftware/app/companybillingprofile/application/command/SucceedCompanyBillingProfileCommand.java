package com.vetsoftware.app.companybillingprofile.application.command;

import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import java.time.LocalDate;

/**
 * Cambio de datos de facturacion. <strong>No es una actualizacion</strong>:
 * cierra la ficha vigente y abre una nueva con estos datos, en una sola
 * transaccion.
 *
 * <p>
 * <strong>Trae la ficha entera y no solo lo que cambia, y eso es
 * deliberado.</strong> Un comando de cambio parcial obligaria a copiar de la
 * ficha vieja los campos que no vienen, y ese arrastre silencioso es como se
 * acaba emitiendo una factura a la sociedad nueva con la direccion de la
 * anterior. La sucesora es una ficha completa y quien la abre la declara
 * completa.
 *
 * @param effectiveFrom
 *            desde cuando rige la nueva. Es tambien la fecha con la que se
 *            cierra la anterior: el intervalo es semiabierto
 *            {@code [valid_from, valid_to)}, asi que no hay hueco ni solape.
 *            Tiene que ser <strong>estrictamente posterior</strong> al
 *            {@code validFrom} de la vigente
 */
public record SucceedCompanyBillingProfileCommand(PersonKind personKind, TaxIdKind taxIdKind,
        String taxId, String verificationDigit, String legalName, String firstName,
        String middleName, String lastName, String secondLastName, String address, Long cityId,
        String billingEmail, TaxRegime taxRegime, boolean withholdingAgent, LocalDate effectiveFrom,
        Long companyId) {
}
