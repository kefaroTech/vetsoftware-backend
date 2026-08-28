package com.vetsoftware.app.documentwithholding.infrastructure.persistence;

import com.vetsoftware.app.documentwithholding.domain.DocumentWithholding;
import org.springframework.stereotype.Component;

/**
 * El unico sitio que conoce a la vez el modelo de dominio y la entidad JPA.
 *
 * <p>
 * <strong>El {@code version} viaja en los dos sentidos, y es lo que hace que el
 * bloqueo optimista funcione.</strong> Si {@code toJpa} lo dejara sin poner,
 * Hibernate veria una entidad con version nula, la trataria como nueva y
 * <em>insertaria una fila duplicada</em> en vez de actualizar la existente —o
 * chocaria con {@code uq_document_withholdings_case}, que es el desenlace
 * afortunado—. Es la trampa clasica del mapper de una entidad versionada que se
 * reconstruye fuera de la sesion.
 *
 * <p>
 * <strong>Un solo {@code toDomain} y sin sobrecarga de camino de
 * escritura</strong>: el dominio no guarda ningun companion VO, solo los ids de
 * las FK, asi que no hay proxy que se pueda disparar al reconstruir la
 * retencion.
 */
@Component
public class DocumentWithholdingJpaMapper {

    public DocumentWithholdingJpaEntity toJpa(DocumentWithholding withholding) {
        DocumentWithholdingJpaEntity entity = new DocumentWithholdingJpaEntity();
        entity.setId(withholding.getId());
        entity.setCompanyId(withholding.getCompanyId());
        entity.setBillingDocumentId(withholding.getBillingDocumentId());
        entity.setType(withholding.getType());
        entity.setTaxableBase(withholding.getTaxableBase());
        entity.setRatePercent(withholding.getRatePercent());
        entity.setAmount(withholding.getAmount());
        entity.setMunicipalityCode(withholding.getMunicipalityCode());
        // El estrechamiento es seguro porque el dominio ya acoto el ano a
        // 2020..2100, muy dentro de lo que cabe en un short.
        entity.setFiscalYear((short) withholding.getFiscalYear());
        entity.setFiscalPeriodKey(withholding.getFiscalPeriodKey());
        entity.setPracticedOn(withholding.getPracticedOn());
        entity.setCertificateId(withholding.getCertificateId());
        entity.setCreatedDate(withholding.getCreatedDate());
        entity.setVersion(withholding.getVersion());
        return entity;
    }

    public DocumentWithholding toDomain(DocumentWithholdingJpaEntity entity) {
        return new DocumentWithholding(entity.getId(), entity.getCompanyId(),
                entity.getBillingDocumentId(), entity.getType(), entity.getTaxableBase(),
                entity.getRatePercent(), entity.getAmount(), entity.getMunicipalityCode(),
                entity.getFiscalYear(), entity.getFiscalPeriodKey(), entity.getPracticedOn(),
                entity.getCertificateId(), entity.getCreatedDate(), entity.getVersion());
    }
}
