package com.vetsoftware.app.electronicdocument.infrastructure.persistence;

import com.vetsoftware.app.companytaxprofile.infrastructure.persistence.CompanyTaxProfileJpaEntity;
import com.vetsoftware.app.companytaxprofile.infrastructure.persistence.CompanyTaxProfileJpaRepository;
import com.vetsoftware.app.electronicdocument.application.port.out.CompanyFiscalProfileQueryPort;
import com.vetsoftware.app.electronicdocument.domain.IssuerSnapshot;
import com.vetsoftware.app.withholdingconfig.infrastructure.persistence.WithholdingConfigJpaEntity;
import com.vetsoftware.app.withholdingconfig.infrastructure.persistence.WithholdingConfigJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Adapter: arma el snapshot del emisor desde CompanyTaxProfile y carga las
 * tarifas de retencion desde WithholdingConfig. Si la empresa no tiene perfil
 * fiscal, devuelve vacio (el caso de uso decide el error).
 */
@Component
public class JpaCompanyFiscalProfileQueryPort implements CompanyFiscalProfileQueryPort {
    private final CompanyTaxProfileJpaRepository companyTaxProfileRepository;
    private final WithholdingConfigJpaRepository withholdingConfigRepository;

    public JpaCompanyFiscalProfileQueryPort(
            CompanyTaxProfileJpaRepository companyTaxProfileRepository,
            WithholdingConfigJpaRepository withholdingConfigRepository) {
        this.companyTaxProfileRepository = companyTaxProfileRepository;
        this.withholdingConfigRepository = withholdingConfigRepository;
    }

    @Override
    public Optional<CompanyFiscalProfile> findByCompany(Long companyId) {
        return companyTaxProfileRepository.findByCompany_Id(companyId).map(profile -> {
            IssuerSnapshot issuer = toIssuer(profile);
            WithholdingConfigJpaEntity wc = withholdingConfigRepository.findByCompany_Id(companyId)
                    .orElse(null);
            return new CompanyFiscalProfile(issuer, wc == null ? null : wc.getReteFuenteRate(),
                    wc == null ? null : wc.getReteIvaRate(),
                    wc == null ? null : wc.getReteIcaRate());
        });
    }

    private static IssuerSnapshot toIssuer(CompanyTaxProfileJpaEntity profile) {
        // Congela los códigos de responsabilidad fiscal del RUT (ya filtrados por
        // enabled vía
        // @SQLRestriction).
        // NOTA: el proveedor MATIAS toma la identidad del emisor (NIT, régimen,
        // responsabilidades) de
        // su propia
        // config de empresa, no del payload; estos códigos quedan en el snapshot para
        // la inmutabilidad
        // fiscal
        // y la representación gráfica del documento.
        List<String> responsibilities = profile.getResponsibilities().stream().map(r -> r.getCode())
                .toList();
        return new IssuerSnapshot(
                profile.getCompanyDocumentType() == null
                        ? null
                        : profile.getCompanyDocumentType().name(),
                profile.getCompanyDocumentId(), profile.getCompanyDocumentVerificationDigit(),
                profile.getLegalName(),
                profile.getTaxRegime() == null ? null : profile.getTaxRegime().name(),
                profile.getFiscalEmail(), responsibilities);
    }
}
