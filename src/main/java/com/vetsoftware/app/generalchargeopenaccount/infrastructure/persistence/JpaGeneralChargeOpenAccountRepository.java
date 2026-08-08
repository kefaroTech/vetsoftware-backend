package com.vetsoftware.app.generalchargeopenaccount.infrastructure.persistence;

import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaEntity;
import com.vetsoftware.app.employee.infrastructure.persistence.EmployeeJpaRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.port.out.GeneralChargeOpenAccountRepository;
import com.vetsoftware.app.generalchargeopenaccount.application.dto.PageResult;
import com.vetsoftware.app.generalchargeopenaccount.domain.GeneralChargeOpenAccount;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaEntity;
import com.vetsoftware.app.openaccount.infrastructure.persistence.OpenAccountJpaRepository;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaEntity;
import com.vetsoftware.app.tax.infrastructure.persistence.TaxJpaRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;

@Repository
public class JpaGeneralChargeOpenAccountRepository implements GeneralChargeOpenAccountRepository {

    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 200;
    private final GeneralChargeOpenAccountJpaRepository jpaRepository;
    private final GeneralChargeOpenAccountJpaMapper mapper;
    private final TaxJpaRepository taxJpaRepository;
    private final OpenAccountJpaRepository openAccountJpaRepository;
    private final EmployeeJpaRepository employeeJpaRepository;

    public JpaGeneralChargeOpenAccountRepository(
            GeneralChargeOpenAccountJpaRepository jpaRepository,
            GeneralChargeOpenAccountJpaMapper mapper, TaxJpaRepository taxJpaRepository,
            OpenAccountJpaRepository openAccountJpaRepository,
            EmployeeJpaRepository employeeJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.taxJpaRepository = taxJpaRepository;
        this.openAccountJpaRepository = openAccountJpaRepository;
        this.employeeJpaRepository = employeeJpaRepository;
    }

    @Override
    public GeneralChargeOpenAccount save(GeneralChargeOpenAccount charge) {
        TaxJpaEntity tax = charge.getTax() == null
                ? null
                : taxJpaRepository.getReferenceById(charge.getTax().id());
        OpenAccountJpaEntity openAccount = openAccountJpaRepository
                .getReferenceById(charge.getOpenAccount().id());
        EmployeeJpaEntity createdBy = employeeJpaRepository
                .getReferenceById(charge.getCreatedBy().id());
        EmployeeJpaEntity voidedBy = charge.getVoidedBy() == null
                ? null
                : employeeJpaRepository.getReferenceById(charge.getVoidedBy().id());
        GeneralChargeOpenAccountJpaEntity saved = jpaRepository
                .save(mapper.toJpa(charge, tax, openAccount, createdBy, voidedBy));
        return mapper.toDomain(saved, charge.getTax(), charge.getOpenAccount(),
                charge.getCreatedBy(), charge.getVoidedBy());
    }

    @Override
    public Optional<GeneralChargeOpenAccount> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<GeneralChargeOpenAccount> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndOpenAccount_Company_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<GeneralChargeOpenAccount> findByOpenAccountIdAndClientRequestId(
            Long openAccountId, String clientRequestId) {
        return jpaRepository.findByOpenAccount_IdAndClientRequestId(openAccountId, clientRequestId)
                .map(mapper::toDomain);
    }

    @Override
    public List<GeneralChargeOpenAccount> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<GeneralChargeOpenAccount> findAllByCompanyId(Long companyId, int page,
            int pageSize) {
        Page<GeneralChargeOpenAccountJpaEntity> result = jpaRepository
                .findAllByOpenAccount_Company_Id(companyId, pageRequest(page, pageSize));
        return new PageResult<>(result.getContent().stream().map(mapper::toDomain).toList(),
                result.getNumber(), result.getSize(), result.getTotalElements(),
                result.getTotalPages());
    }

    /**
     * Normaliza lo que llega del cliente: una pagina negativa o un tamano desmedido
     * no deben poder volver a pedir la tabla entera, que es el fallo que se esta
     * corrigiendo. El orden por id descendente es estable y devuelve primero lo mas
     * reciente; sin orden explicito la paginacion no es determinista y una misma
     * fila puede salir en dos paginas.
     */
    private static PageRequest pageRequest(int page, int pageSize) {
        int safeSize = pageSize <= 0 ? DEFAULT_PAGE_SIZE : Math.min(pageSize, MAX_PAGE_SIZE);
        return PageRequest.of(Math.max(page, 0), safeSize, Sort.by(Sort.Direction.DESC, "id"));
    }

    @Override
    public List<GeneralChargeOpenAccount> findByOpenAccountIdAndCompanyId(Long openAccountId,
            Long companyId) {
        return jpaRepository.findByOpenAccount_IdAndOpenAccount_Company_Id(openAccountId, companyId)
                .stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }

    @Override
    public int reactivate(Long id, Long companyId) {
        return jpaRepository.reactivate(id, companyId);
    }
}
