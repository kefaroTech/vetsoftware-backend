package com.vetsoftware.app.companytaxprofile.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.companytaxprofile.domain.CompanyRef;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfile;
import com.vetsoftware.app.companytaxprofile.domain.CompanyTaxProfileResponsibility;
import com.vetsoftware.app.companytaxprofile.domain.EconomicActivityRef;
import com.vetsoftware.app.economicactivity.infrastructure.persistence.EconomicActivityJpaEntity;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CompanyTaxProfileJpaMapper {

    public CompanyTaxProfileJpaEntity toJpa(CompanyTaxProfile profile, CompanyJpaEntity company,
            EconomicActivityJpaEntity economicActivity) {
        CompanyTaxProfileJpaEntity entity = new CompanyTaxProfileJpaEntity();
        entity.setId(profile.getId());
        entity.setCompany(company);
        entity.setCompanyDocumentType(profile.getCompanyDocumentType());
        entity.setCompanyDocumentId(profile.getCompanyDocumentId());
        entity.setCompanyDocumentVerificationDigit(profile.getCompanyDocumentVerificationDigit());
        entity.setLegalName(profile.getLegalName());
        entity.setTaxRegime(profile.getTaxRegime());
        entity.setFiscalEmail(profile.getFiscalEmail());
        entity.setCommercialName(profile.getCommercialName());
        entity.setEconomicActivity(economicActivity);
        entity.setCreatedDate(profile.getCreatedDate());
        entity.setVersion(profile.getVersion());
        entity.setEnabled(profile.isEnabled());

        List<CompanyTaxProfileResponsibilityJpaEntity> children = profile.getResponsibilities()
                .stream().map(r -> {
                    CompanyTaxProfileResponsibilityJpaEntity child = new CompanyTaxProfileResponsibilityJpaEntity();
                    child.setCompanyTaxProfile(entity);
                    child.setCode(r.code());
                    child.setCreatedDate(profile.getCreatedDate() == null
                            ? LocalDateTime.now()
                            : profile.getCreatedDate());
                    child.setEnabled(true);
                    return child;
                }).toList();
        entity.getResponsibilities().clear();
        entity.getResponsibilities().addAll(children);
        return entity;
    }

    public CompanyTaxProfile toDomain(CompanyTaxProfileJpaEntity entity) {
        CompanyJpaEntity c = entity.getCompany();
        CompanyRef companyRef = c == null
                ? null
                : new CompanyRef(c.getId(), c.getName(), c.getIdentifier());
        EconomicActivityJpaEntity ea = entity.getEconomicActivity();
        EconomicActivityRef economicActivityRef = ea == null
                ? null
                : new EconomicActivityRef(ea.getId(), ea.getCode(), ea.getName());
        return toDomain(entity, companyRef, economicActivityRef);
    }

    public CompanyTaxProfile toDomain(CompanyTaxProfileJpaEntity entity, CompanyRef companyRef,
            EconomicActivityRef economicActivityRef) {
        List<CompanyTaxProfileResponsibility> responsibilities = entity.getResponsibilities()
                .stream().filter(CompanyTaxProfileResponsibilityJpaEntity::isEnabled)
                .map(r -> new CompanyTaxProfileResponsibility(r.getCode())).toList();
        return new CompanyTaxProfile(entity.getId(), companyRef, entity.getCompanyDocumentType(),
                entity.getCompanyDocumentId(), entity.getCompanyDocumentVerificationDigit(),
                entity.getLegalName(), entity.getTaxRegime(), entity.getFiscalEmail(),
                entity.getCommercialName(), economicActivityRef, responsibilities,
                entity.getCreatedDate(), entity.getVersion(), entity.isEnabled());
    }
}
