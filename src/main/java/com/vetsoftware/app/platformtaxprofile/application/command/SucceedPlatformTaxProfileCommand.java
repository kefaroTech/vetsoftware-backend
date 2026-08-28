package com.vetsoftware.app.platformtaxprofile.application.command;

import com.vetsoftware.app.platformtaxprofile.domain.PlatformDocumentType;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxRegime;
import java.time.LocalDate;

/**
 * Cambio de identidad fiscal de VetSoftware. <strong>No es una
 * actualizacion</strong>: cierra la ficha vigente y abre una nueva con estos
 * datos, en una sola transaccion.
 *
 * <p>
 * <strong>Trae la ficha entera y no solo lo que cambia, y eso es
 * deliberado.</strong> Un comando de cambio parcial obligaria a copiar de la
 * ficha vieja los campos que no vienen, y ese arrastre silencioso es como se
 * acaba imprimiendo en las facturas la razon social nueva con el NIT de la
 * anterior. La sucesora es una ficha completa y quien la abre la declara
 * completa.
 *
 * <p>
 * <strong>Sin {@code companyId}</strong>: la tabla no tiene esa columna. Ver
 * {@link OpenPlatformTaxProfileCommand}.
 *
 * @param effectiveFrom
 *            desde cuando rige la nueva. Es tambien la fecha con la que se
 *            cierra la anterior: el intervalo es semiabierto
 *            {@code [valid_from, valid_to)}, asi que no hay hueco ni solape.
 *            Tiene que ser <strong>estrictamente posterior</strong> al
 *            {@code validFrom} de la vigente, y esa fecha es exactamente la que
 *            decide que razon social se imprime en una factura emitida en el
 *            intervalo
 */
public record SucceedPlatformTaxProfileCommand(PlatformDocumentType documentType, String documentId,
        String verificationDigit, String legalName, PlatformTaxRegime taxRegime, String fiscalEmail,
        String commercialName, Long economicActivityId, boolean selfWithholder,
        LocalDate effectiveFrom) {
}
