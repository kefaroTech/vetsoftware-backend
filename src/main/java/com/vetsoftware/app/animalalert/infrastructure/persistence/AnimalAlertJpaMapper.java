package com.vetsoftware.app.animalalert.infrastructure.persistence;

import com.vetsoftware.app.animal.infrastructure.persistence.AnimalJpaEntity;
import com.vetsoftware.app.animalalert.domain.AnimalAlert;
import com.vetsoftware.app.animalalert.domain.AnimalRef;
import com.vetsoftware.app.animalalert.domain.CompanyRef;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import org.springframework.stereotype.Component;

@Component
public class AnimalAlertJpaMapper {

    public AnimalAlertJpaEntity toJpa(AnimalAlert alert, AnimalJpaEntity animal, CompanyJpaEntity company) {
        AnimalAlertJpaEntity entity = new AnimalAlertJpaEntity();
        entity.setId(alert.getId());
        entity.setAnimal(animal);
        entity.setCompany(company);
        entity.setType(alert.getType());
        entity.setDescription(alert.getDescription());
        entity.setSeverity(alert.getSeverity());
        entity.setCreatedDate(alert.getCreatedDate());
        entity.setEnabled(alert.isEnabled());
        return entity;
    }

    public AnimalAlert toDomain(AnimalAlertJpaEntity entity) {
        AnimalJpaEntity a = entity.getAnimal();
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
            new AnimalRef(a.getId(), a.getName(), a.getCode()),
            new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public AnimalAlert toDomain(AnimalAlertJpaEntity entity, AnimalRef animalRef, CompanyRef companyRef) {
        return new AnimalAlert(
            entity.getId(), animalRef, companyRef, entity.getType(),
            entity.getDescription(), entity.getSeverity(),
            entity.getCreatedDate(), entity.isEnabled());
    }
}
