package com.vetsoftware.app.withholdingcertificate.application.command;

import java.time.LocalDate;

/**
 * Cierra la expectativa: el certificado llego.
 *
 * <p>
 * <strong>Sin {@code companyId}, y eso obliga a que el puerto sea SYSTEM a
 * secas.</strong> Es exactamente el caso que describe
 * {@code OPERACIONES_POR_ID_SIN_EMPRESA_SOLO_SYSTEM}: un command con {@code id}
 * y sin empresa senala una fila concreta que el cliente escribe en la URL. Aqui
 * es correcto porque quien registra la llegada del papel es tesoreria de la
 * plataforma, no la clinica.
 *
 * @param fileRef
 *            referencia del archivo guardado. Obligatoria: un certificado
 *            recibido sin archivo es un certificado que nadie puede ensenar
 */
public record ReceiveWithholdingCertificateCommand(Long id, LocalDate receivedOn, String fileRef) {
}
