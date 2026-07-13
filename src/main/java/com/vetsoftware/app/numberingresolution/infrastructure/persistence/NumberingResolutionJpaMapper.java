package com.vetsoftware.app.numberingresolution.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.numberingresolution.domain.CompanyRef;
import com.vetsoftware.app.numberingresolution.domain.NumberingResolution;
import org.springframework.stereotype.Component;

@Component
public class NumberingResolutionJpaMapper {
    public NumberingResolutionJpaEntity toJpa(NumberingResolution resolution, CompanyJpaEntity company) {
        NumberingResolutionJpaEntity entity = new NumberingResolutionJpaEntity();
        entity.setId(resolution.getId());
        entity.setCompany(company);
        entity.setBranchId(resolution.getBranchId());
        entity.setDocumentType(resolution.getDocumentType());
        entity.setResolutionNumber(resolution.getResolutionNumber());
        entity.setResolutionDate(resolution.getResolutionDate());
        entity.setPrefix(resolution.getPrefix());
        entity.setRangeFrom(resolution.getRangeFrom());
        entity.setRangeTo(resolution.getRangeTo());
        entity.setValidFrom(resolution.getValidFrom());
        entity.setValidTo(resolution.getValidTo());
        entity.setTechnicalKey(resolution.getTechnicalKey());
        entity.setCurrentNumber(resolution.getCurrentNumber());
        entity.setCreatedDate(resolution.getCreatedDate());
        entity.setEnabled(resolution.isEnabled());
        return entity;
    }

    public NumberingResolution toDomain(NumberingResolutionJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
                c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public NumberingResolution toDomain(NumberingResolutionJpaEntity entity, CompanyRef companyRef) {
        return new NumberingResolution(
                entity.getId(),
                companyRef,
                entity.getDocumentType(),
                entity.getResolutionNumber(),
                entity.getResolutionDate(),
                entity.getPrefix(),
                entity.getRangeFrom(),
                entity.getRangeTo(),
                entity.getValidFrom(),
                entity.getValidTo(),
                entity.getTechnicalKey(),
                entity.getCurrentNumber(),
                entity.getCreatedDate(),
                entity.isEnabled(),
                entity.getBranchId());
    }
}
