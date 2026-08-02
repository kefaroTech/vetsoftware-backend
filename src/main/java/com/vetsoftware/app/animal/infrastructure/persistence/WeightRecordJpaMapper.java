package com.vetsoftware.app.animal.infrastructure.persistence;

import com.vetsoftware.app.animal.domain.AnimalRef;
import com.vetsoftware.app.animal.domain.CompanyRef;
import com.vetsoftware.app.animal.domain.WeightRecord;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class WeightRecordJpaMapper {

    public WeightRecordJpaEntity toJpa(WeightRecord record, AnimalJpaEntity animal,
            CompanyJpaEntity company) {
        WeightRecordJpaEntity entity = new WeightRecordJpaEntity();
        entity.setId(record.getId());
        entity.setAnimal(animal);
        entity.setCompany(company);
        entity.setValue(record.getValue());
        entity.setUnit(record.getUnit());
        entity.setMeasuredAt(record.getMeasuredAt());
        entity.setSource(record.getSource());
        entity.setSourceId(record.getSourceId());
        entity.setNote(record.getNote());
        entity.setCreatedDate(record.getCreatedDate());
        entity.setEnabled(record.isEnabled());
        return entity;
    }

    // Read path — el @EntityGraph ya hidrató animal y company.
    public WeightRecord toDomain(WeightRecordJpaEntity entity) {
        AnimalJpaEntity a = entity.getAnimal();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity, new AnimalRef(a.getId(), a.getName(), a.getCode()),
                new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    // Write path — reusa los refs precargados, evita inicializar los proxies de
    // getReferenceById.
    public WeightRecord toDomain(WeightRecordJpaEntity entity, AnimalRef animalRef,
            CompanyRef companyRef) {
        return new WeightRecord(entity.getId(), animalRef, entity.getValue(), entity.getUnit(),
                entity.getMeasuredAt(), entity.getSource(), entity.getSourceId(), entity.getNote(),
                companyRef, entity.getCreatedDate(), entity.isEnabled());
    }
}
