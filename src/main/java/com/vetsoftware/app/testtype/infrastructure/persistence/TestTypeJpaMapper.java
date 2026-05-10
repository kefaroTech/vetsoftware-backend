package com.vetsoftware.app.testtype.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.testtype.domain.CompanyRef;
import com.vetsoftware.app.testtype.domain.TestType;
import org.springframework.stereotype.Component;

@Component
public class TestTypeJpaMapper {
    public TestTypeJpaEntity toJpa(TestType testType, CompanyJpaEntity company) {
        TestTypeJpaEntity entity = new TestTypeJpaEntity();
        entity.setId(testType.getId());
        entity.setName(testType.getName());
        entity.setDescription(testType.getDescription());
        entity.setCompany(company);
        entity.setGeneral(testType.isGeneral());
        entity.setCreatedDate(testType.getCreatedDate());
        return entity;
    }

    public TestType toDomain(TestTypeJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
                c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public TestType toDomain(TestTypeJpaEntity entity, CompanyRef companyRef) {
        return new TestType(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                companyRef,
                Boolean.TRUE.equals(entity.getGeneral()),
                entity.getCreatedDate());
    }
}
