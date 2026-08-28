package com.vetsoftware.app.documentwithholding.application.port.out;

/**
 * La FK compuesta {@code document_withholdings (company_id, certificate_id)}
 * contra {@code withholding_certificates}, que es de otra feature.
 *
 * <p>
 * {@code ValidationPort} y no {@code QueryPort} por el mismo criterio que
 * {@link BillingDocumentValidationPort}: esta rodaja no lee un solo campo del
 * certificado. Tentador seria comprobar aqui que el tipo y el ano del
 * certificado coinciden con los de la retencion —un certificado de IVA de 2025
 * no puede respaldar una retencion de ICA de 2026—, pero esa regla necesita los
 * datos del agregado ajeno y pertenece a quien es dueno de el; importarlos aqui
 * ataria esta feature a la forma de la otra. Queda declarada como pendiente, no
 * como olvido.
 *
 * <p>
 * <strong>Acotado por empresa, y aqui no es una precaucion teorica.</strong> La
 * clave foranea comparte la columna {@code company_id} con la del documento de
 * cobro: un certificado de otra clinica no lo rechazaria una validacion, lo
 * rechazaria el motor como error de integridad, que llega al operador como un
 * 500 sin explicacion.
 */
public interface WithholdingCertificateValidationPort {

    /** {@code true} si el certificado existe y es de esa empresa. */
    boolean existsByIdAndCompanyId(Long certificateId, Long companyId);
}
