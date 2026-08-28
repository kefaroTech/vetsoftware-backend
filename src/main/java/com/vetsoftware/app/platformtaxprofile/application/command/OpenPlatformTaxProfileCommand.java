package com.vetsoftware.app.platformtaxprofile.application.command;

import com.vetsoftware.app.platformtaxprofile.domain.PlatformDocumentType;
import com.vetsoftware.app.platformtaxprofile.domain.PlatformTaxRegime;
import java.time.LocalDate;

/**
 * Apertura de la <strong>primera</strong> identidad fiscal de VetSoftware.
 *
 * <p>
 * <strong>Sin {@code companyId}, y aqui no es la regla de siempre: es que no
 * existe la columna.</strong> {@code platform_tax_profiles} es global —una sola
 * identidad fiscal para toda la plataforma— y añadirle una empresa seria
 * modelar mal, no proteger mejor. Lo que si depende del cliente —si <em>el
 * cliente</em> es agente de retencion— vive en
 * {@code company_billing_profiles}.
 *
 * <p>
 * <strong>Este comando es el que la tabla espera y todavia nadie ha
 * ejecutado.</strong> El changeset 367 dejo {@code platform_tax_profiles} sin
 * sembrar a proposito: no habia razon social ni NIT reales y no se inventaron,
 * porque una identidad fiscal inventada acaba impresa en la factura de cada
 * cliente. Quien tenga los datos reales abre la primera ficha por aqui.
 *
 * @param economicActivityId
 *            <strong>opcional</strong>: la columna es nulable en 367. Si viene,
 *            se resuelve en el caso de uso via
 *            {@code EconomicActivityQueryPort} —no en el repositorio—, para que
 *            un id inexistente salga como un 400 con el id delante y no como
 *            una violacion de clave foranea
 * @param selfWithholder
 *            si <strong>VetSoftware</strong> es autorretenedor. No se deduce de
 *            que sus clientes le retengan: los dos hechos coexisten
 * @param validFrom
 *            desde cuando rige. Va explicito y no se toma del reloj porque una
 *            identidad puede registrarse hoy para regir desde el primero del
 *            mes que viene
 */
public record OpenPlatformTaxProfileCommand(PlatformDocumentType documentType, String documentId,
        String verificationDigit, String legalName, PlatformTaxRegime taxRegime, String fiscalEmail,
        String commercialName, Long economicActivityId, boolean selfWithholder,
        LocalDate validFrom) {
}
