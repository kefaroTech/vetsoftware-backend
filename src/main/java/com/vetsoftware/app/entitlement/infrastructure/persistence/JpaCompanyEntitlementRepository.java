package com.vetsoftware.app.entitlement.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.entitlement.application.port.out.CompanyEntitlementRepository;
import com.vetsoftware.app.entitlement.domain.CompanyEntitlement;
import com.vetsoftware.app.entitlement.domain.EntitlementSource;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaEntity;
import com.vetsoftware.app.submodule.infrastructure.persistence.SubModuleJpaRepository;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

/** Adaptador de salida de los permisos derivados. */
@Repository
public class JpaCompanyEntitlementRepository implements CompanyEntitlementRepository {

    private final CompanyEntitlementJpaRepository jpaRepository;
    private final CompanyEntitlementJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;
    private final SubModuleJpaRepository subModuleJpaRepository;

    public JpaCompanyEntitlementRepository(CompanyEntitlementJpaRepository jpaRepository,
            CompanyEntitlementJpaMapper mapper, CompanyJpaRepository companyJpaRepository,
            SubModuleJpaRepository subModuleJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
        this.subModuleJpaRepository = subModuleJpaRepository;
    }

    @Override
    public List<CompanyEntitlement> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompany_Id(companyId).stream().map(mapper::toDomain).toList();
    }

    /**
     * Orden total: por submodulo y con desempate por id. Sin el desempate, dos
     * paginas consecutivas pueden repetir u omitir filas.
     */
    @Override
    public PageResult<CompanyEntitlement> findPageByCompanyId(Long companyId, int page,
            int pageSize) {
        Sort order = Sort.by(Sort.Direction.ASC, "subModule.id")
                .and(Sort.by(Sort.Direction.ASC, "id"));
        return Pages.result(
                jpaRepository.findAllByCompany_Id(companyId, Pages.request(page, pageSize, order)),
                mapper::toDomain);
    }

    @Override
    public List<CompanyEntitlement> saveAll(List<CompanyEntitlement> entitlements) {
        if (entitlements.isEmpty()) {
            return List.of();
        }
        List<CompanyEntitlementJpaEntity> rows = new ArrayList<>(entitlements.size());
        for (CompanyEntitlement entitlement : entitlements) {
            CompanyJpaEntity company = companyJpaRepository
                    .getReferenceById(entitlement.getCompanyId());
            SubModuleJpaEntity subModule = subModuleJpaRepository
                    .getReferenceById(entitlement.getSubModule().id());
            rows.add(mapper.toJpa(entitlement, company, subModule));
        }
        List<CompanyEntitlementJpaEntity> saved = jpaRepository.saveAll(rows);
        List<CompanyEntitlement> result = new ArrayList<>(saved.size());
        for (int index = 0; index < saved.size(); index++) {
            CompanyEntitlement source = entitlements.get(index);
            // Reusa la referencia precargada: leerla del proxy dispararia una consulta
            // de hidratacion por fila, justo despues de haber escrito la tabla entera.
            result.add(mapper.toDomain(saved.get(index), source.getCompanyId(),
                    source.getSubModule()));
        }
        return List.copyOf(result);
    }

    /**
     * Las concesiones manuales de la empresa. El recalculo las lee antes de borrar
     * para dejar sus submodulos fuera del calculo: emitir una fila derivada para un
     * submodulo que ya tiene la manual reventaria {@code uq_company_entitlements}.
     */
    @Override
    public List<CompanyEntitlement> findManualGrantsByCompanyId(Long companyId) {
        return jpaRepository
                .findAllByCompany_IdAndSource(companyId, EntitlementSource.MANUAL_GRANT.name())
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public int deleteDerivedByCompanyId(Long companyId) {
        return jpaRepository.deleteDerivedByCompanyId(companyId);
    }
}
