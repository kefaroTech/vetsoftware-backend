package com.vetsoftware.app.testtype.infrastructure.persistence;

import com.vetsoftware.app.testtype.domain.TestType;
import org.springframework.stereotype.Component;

@Component
public class TestTypeJpaMapper {
    public TestTypeJpaEntity toJpa(TestType testType) {
        TestTypeJpaEntity entity = new TestTypeJpaEntity();
        entity.setId(testType.getId());
        entity.setName(testType.getName());
        entity.setDescription(testType.getDescription());
        entity.setCreatedDate(testType.getCreatedDate());
        return entity;
    }

    public TestType toDomain(TestTypeJpaEntity entity) {
        return new TestType(
                entity.getId(),
                entity.getName(),
                entity.getDescription(),
                entity.getCreatedDate());
    }
}
