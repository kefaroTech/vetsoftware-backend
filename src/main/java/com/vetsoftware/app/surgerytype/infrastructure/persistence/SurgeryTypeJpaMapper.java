package com.vetsoftware.app.surgerytype.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.surgerytype.domain.CompanyRef;
import com.vetsoftware.app.surgerytype.domain.SurgeryType;
import org.springframework.stereotype.Component;

@Component
public class SurgeryTypeJpaMapper {
    public SurgeryTypeJpaEntity toJpa(SurgeryType surgeryType, CompanyJpaEntity company) {
        SurgeryTypeJpaEntity entity = new SurgeryTypeJpaEntity();
        entity.setId(surgeryType.getId());
        entity.setName(surgeryType.getName());
        entity.setDescription(surgeryType.getDescription());
        entity.setCompany(company);
        entity.setGeneral(surgeryType.isGeneral());
        entity.setCreatedDate(surgeryType.getCreatedDate());
        entity.setVersion(surgeryType.getVersion());
        entity.setEnabled(surgeryType.isEnabled());
        return entity;
    }

    public SurgeryType toDomain(SurgeryTypeJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
                c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public SurgeryType toDomain(SurgeryTypeJpaEntity entity, CompanyRef companyRef) {
        return new SurgeryType(entity.getId(), entity.getName(), entity.getDescription(),
                companyRef, Boolean.TRUE.equals(entity.getGeneral()), entity.getCreatedDate(),
                entity.getVersion(), entity.isEnabled());
    }
}
