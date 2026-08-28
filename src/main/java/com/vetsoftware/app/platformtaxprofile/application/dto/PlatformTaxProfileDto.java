package com.vetsoftware.app.platformtaxprofile.application.dto;

import com.vetsoftware.app.platformtaxprofile.domain.PlatformDocumentType;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxProfile;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxRegime;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * La identidad fiscal de VetSoftware tal como la consume la aplicacion.
 *
 * <p>
 * <strong>Sin {@code version}</strong>: el numero de version es la barandilla
 * del bloqueo optimista, no un dato de la identidad. Publicarlo invitaria a un
 * cliente a devolverlo y a construir un control de concurrencia paralelo al que
 * ya hace Hibernate.
 *
 * <p>
 * <strong>Sin {@code currentProfileMarker}</strong>: es una columna generada
 * por MySQL que existe solo para que un indice unico pueda restringir lo que
 * con {@code NULL} no restringia. Publicarla invitaria a construir logica sobre
 * un centinela de base de datos; lo que el consumidor necesita es
 * {@code validTo == null}, que es el mismo hecho dicho en el vocabulario del
 * modelo.
 *
 * @param validTo
 *            nulo significa <em>vigente</em>. Es la misma señal que alimenta la
 *            columna generada del esquema
 * @param economicActivity
 *            nulo es legitimo: {@code economic_activity_id} es nulable
 */
public record PlatformTaxProfileDto(Long id, PlatformDocumentType documentType, String documentId,
        String verificationDigit, String legalName, PlatformTaxRegime taxRegime, String fiscalEmail,
        String commercialName, PlatformEconomicActivitySummaryDto economicActivity,
        boolean selfWithholder, LocalDate validFrom, LocalDate validTo, LocalDateTime createdDate) {

    public static PlatformTaxProfileDto from(PlatformTaxProfile profile) {
        return new PlatformTaxProfileDto(profile.getId(), profile.getDocumentType(),
                profile.getDocumentId(), profile.getVerificationDigit(), profile.getLegalName(),
                profile.getTaxRegime(), profile.getFiscalEmail(), profile.getCommercialName(),
                PlatformEconomicActivitySummaryDto.from(profile.getEconomicActivity()),
                profile.isSelfWithholder(), profile.getValidFrom(), profile.getValidTo(),
                profile.getCreatedDate());
    }
}
