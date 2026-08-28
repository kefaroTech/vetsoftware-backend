package com.vetsoftware.app.documentwithholding.infrastructure.persistence;

import com.vetsoftware.app.documentwithholding.application.port.out.WithholdingCertificateValidationPort;
import com.vetsoftware.app.withholdingcertificate.infrastructure.persistence.WithholdingCertificateJpaRepository;
import org.springframework.stereotype.Component;

/**
 * El unico archivo de este slice que conoce a {@code withholdingcertificate}.
 *
 * <p>
 * Se apoya en {@code findByIdAndCompanyId} y no lee un solo getter de la
 * entidad ajena. Es deliberado incluso donde parece que haria falta: comprobar
 * que el tipo y el ano del certificado casan con los de la retencion es una
 * regla legitima, pero necesita los datos del agregado ajeno y pertenece a
 * quien es dueno de el. Traerlos aqui ataria esta rodaja a la forma de la otra.
 *
 * <p>
 * <strong>El nombre del bean va cualificado</strong> por el mismo motivo que en
 * {@link JpaBillingDocumentValidationPort}: el vertical slicing repite nombres
 * simples entre features y sin cualificar se pisan en los contextos de test.
 */
@Component("documentWithholdingJpaWithholdingCertificateValidationPort")
public class JpaWithholdingCertificateValidationPort
        implements
            WithholdingCertificateValidationPort {

    private final WithholdingCertificateJpaRepository certificateJpaRepository;

    public JpaWithholdingCertificateValidationPort(
            WithholdingCertificateJpaRepository certificateJpaRepository) {
        this.certificateJpaRepository = certificateJpaRepository;
    }

    @Override
    public boolean existsByIdAndCompanyId(Long certificateId, Long companyId) {
        return certificateId != null && companyId != null && certificateJpaRepository
                .findByIdAndCompanyId(certificateId, companyId).isPresent();
    }
}
