package com.vetsoftware.app.withholdingcertificate.infrastructure.persistence;

import com.vetsoftware.app.withholdingcertificate.domain.WithholdingCertificate;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>Un solo {@code toDomain} y sin sobrecarga de camino de
 * escritura</strong>: el dominio no guarda ningun companion VO, solo el
 * {@code companyId}, asi que no hay proxy que se pueda disparar al reconstruir
 * el certificado.
 *
 * <p>
 * <strong>El {@code version} no cruza la frontera.</strong> Es un dato del
 * mecanismo de bloqueo, no del expediente fiscal; publicarlo obligaria al
 * dominio a arrastrar un campo que no significa nada para el, y a los tests de
 * dominio a inventarlo. Como {@code save} vuelve a cargar la entidad gestionada
 * por su {@code id}, Hibernate conserva la version real y el chequeo sigue
 * ocurriendo.
 */
@Component
public class WithholdingCertificateJpaMapper {

    public WithholdingCertificateJpaEntity toJpa(WithholdingCertificate certificate) {
        WithholdingCertificateJpaEntity entity = new WithholdingCertificateJpaEntity();
        apply(certificate, entity);
        return entity;
    }

    /**
     * Vuelca el estado del agregado sobre una entidad <em>ya gestionada</em>, que
     * es lo que hacen las dos segundas escrituras. Volcar sobre una instancia nueva
     * con el mismo id seria un {@code merge} sobre una entidad <em>detached</em>
     * con {@code version} a nulo: Hibernate la trataria como fila nueva y el
     * bloqueo optimista no comprobaria nada.
     */
    public void apply(WithholdingCertificate certificate, WithholdingCertificateJpaEntity entity) {
        entity.setId(certificate.getId());
        entity.setCompanyId(certificate.getCompanyId());
        entity.setIssuedByTaxId(certificate.getIssuedByTaxId());
        entity.setCertificateNumber(certificate.getCertificateNumber());
        entity.setWithholdingType(certificate.getWithholdingType());
        entity.setFiscalYear(certificate.getFiscalYear().shortValue());
        entity.setFiscalPeriodKey(certificate.getFiscalPeriodKey());
        entity.setRatePercent(certificate.getRatePercent());
        entity.setCertifiedAmount(certificate.getCertifiedAmount());
        entity.setIssuedOn(certificate.getIssuedOn());
        entity.setLegalDeadlineOn(certificate.getLegalDeadlineOn());
        entity.setReceivedOn(certificate.getReceivedOn());
        entity.setFileRef(certificate.getFileRef());
        entity.setSubstituteEvidenceKind(certificate.getSubstituteEvidenceKind());
        entity.setSubstituteEvidenceRef(certificate.getSubstituteEvidenceRef());
        entity.setCreatedDate(certificate.getCreatedDate());
    }

    public WithholdingCertificate toDomain(WithholdingCertificateJpaEntity entity) {
        return new WithholdingCertificate(entity.getId(), entity.getCompanyId(),
                entity.getIssuedByTaxId(), entity.getCertificateNumber(),
                entity.getWithholdingType(), (int) entity.getFiscalYear(),
                entity.getFiscalPeriodKey(), entity.getRatePercent(), entity.getCertifiedAmount(),
                entity.getIssuedOn(), entity.getLegalDeadlineOn(), entity.getReceivedOn(),
                entity.getFileRef(), entity.getSubstituteEvidenceKind(),
                entity.getSubstituteEvidenceRef(), entity.getCreatedDate());
    }
}
