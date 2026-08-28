package com.vetsoftware.app.companybillingprofile.application.dto;

import com.vetsoftware.app.companybillingprofile.domain.CompanyBillingProfile;
import com.vetsoftware.app.companybillingprofile.domain.PersonKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxIdKind;
import com.vetsoftware.app.companybillingprofile.domain.TaxRegime;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La ficha de facturacion tal como la consume la aplicacion.
 *
 * <p>
 * <strong>Sin {@code version}</strong>: el numero de version es la barandilla
 * del bloqueo optimista, no un dato de la ficha. Publicarlo invitaria a un
 * cliente a devolverlo y a construir un control de concurrencia paralelo al que
 * ya hace Hibernate.
 *
 * <p>
 * <strong>Con los cuatro campos de nombre separados</strong>, incluso cuando
 * tres de ellos van vacios. Juntarlos aqui para «ahorrar campos» es exactamente
 * lo que obliga a partirlos otra vez cuando toca reportar la exogena.
 *
 * @param validTo
 *            nulo significa <em>vigente</em>. Es la misma señal que alimenta la
 *            columna generada del esquema
 */
public record CompanyBillingProfileDto(Long id, Long companyId, PersonKind personKind,
        TaxIdKind taxIdKind, String taxId, String verificationDigit, String legalName,
        String firstName, String middleName, String lastName, String secondLastName, String address,
        CitySummaryDto city, String billingEmail, TaxRegime taxRegime, boolean withholdingAgent,
        LocalDate validFrom, LocalDate validTo, LocalDateTime createdDate) {

    public static CompanyBillingProfileDto from(CompanyBillingProfile profile) {
        return new CompanyBillingProfileDto(profile.getId(), profile.getCompanyId(),
                profile.getPersonKind(), profile.getTaxIdKind(), profile.getTaxId(),
                profile.getVerificationDigit(), profile.getLegalName(), profile.getFirstName(),
                profile.getMiddleName(), profile.getLastName(), profile.getSecondLastName(),
                profile.getAddress(), CitySummaryDto.from(profile.getCity()),
                profile.getBillingEmail(), profile.getTaxRegime(), profile.isWithholdingAgent(),
                profile.getValidFrom(), profile.getValidTo(), profile.getCreatedDate());
    }
}
