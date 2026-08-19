package com.vetsoftware.app.medicament.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.medicament.domain.CompanyRef;
import com.vetsoftware.app.medicament.domain.Medicament;
import org.springframework.stereotype.Component;

@Component
public class MedicamentJpaMapper {
    public MedicamentJpaEntity toJpa(Medicament medicament, CompanyJpaEntity company) {
        MedicamentJpaEntity entity = new MedicamentJpaEntity();
        entity.setId(medicament.getId());
        entity.setName(medicament.getName());
        entity.setDescription(medicament.getDescription());
        entity.setCompany(company);
        entity.setGeneral(medicament.isGeneral());
        entity.setCreatedDate(medicament.getCreatedDate());
        entity.setVersion(medicament.getVersion());
        entity.setEnabled(medicament.isEnabled());
        return entity;
    }

    public Medicament toDomain(MedicamentJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        return toDomain(entity,
                c == null ? null : new CompanyRef(c.getId(), c.getName(), c.getIdentifier()));
    }

    public Medicament toDomain(MedicamentJpaEntity entity, CompanyRef companyRef) {
        return new Medicament(entity.getId(), entity.getName(), entity.getDescription(), companyRef,
                Boolean.TRUE.equals(entity.getGeneral()), entity.getCreatedDate(),
                entity.getVersion(), entity.isEnabled());
    }
}
