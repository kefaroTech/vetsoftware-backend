package com.vetsoftware.app.owner.infrastructure.persistence;

import com.vetsoftware.app.city.infrastructure.persistence.CityJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.owner.domain.CityRef;
import com.vetsoftware.app.owner.domain.CompanyRef;
import com.vetsoftware.app.owner.domain.Owner;
import org.springframework.stereotype.Component;

@Component
public class OwnerJpaMapper {

    public OwnerJpaEntity toJpa(Owner owner, CityJpaEntity city, CompanyJpaEntity company) {
        OwnerJpaEntity entity = new OwnerJpaEntity();
        entity.setId(owner.getId());
        entity.setName(owner.getName());
        entity.setEmail(owner.getEmail());
        entity.setDocument(owner.getDocument());
        entity.setDocumentType(owner.getDocumentType());
        entity.setPersonType(owner.getPersonType());
        entity.setVerificationDigit(owner.getVerificationDigit());
        entity.setLegalName(owner.getLegalName());
        entity.setAddress(owner.getAddress());
        entity.setPhone(owner.getPhone());
        entity.setCity(city);
        entity.setCompany(company);
        entity.setWithholdingAgent(owner.isWithholdingAgent());
        entity.setTaxRegime(owner.getTaxRegime());
        entity.setCreatedDate(owner.getCreatedDate());
        entity.setEnabled(owner.isEnabled());
        return entity;
    }

    public Owner toDomain(OwnerJpaEntity entity) {
        CityJpaEntity c = entity.getCity();
        CompanyJpaEntity co = entity.getCompany();
        return toDomain(entity,
            new CityRef(c.getId(), c.getName()),
            new CompanyRef(co.getId(), co.getName(), co.getIdentifier()));
    }

    public Owner toDomain(OwnerJpaEntity entity, CityRef cityRef, CompanyRef companyRef) {
        return new Owner(
            entity.getId(), entity.getName(), entity.getEmail(), entity.getDocument(),
            entity.getDocumentType(), entity.getPersonType(), entity.getVerificationDigit(),
            entity.getLegalName(), entity.getAddress(), entity.getPhone(), cityRef, companyRef,
            entity.isWithholdingAgent(), entity.getTaxRegime(), entity.getCreatedDate(), entity.isEnabled()
        );
    }
}
