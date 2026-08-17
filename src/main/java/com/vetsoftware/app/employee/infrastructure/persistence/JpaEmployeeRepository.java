package com.vetsoftware.app.employee.infrastructure.persistence;

import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaEntity;
import com.vetsoftware.app.company.infrastructure.persistence.CompanyJpaRepository;
import com.vetsoftware.app.employee.application.command.SearchEmployeesCommand;
import com.vetsoftware.app.shared.pagination.PageResult;
import com.vetsoftware.app.shared.pagination.Pages;
import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmployeeRepository implements EmployeeRepository {
    private final EmployeeJpaRepository jpaRepository;
    private final EmployeeJpaMapper mapper;
    private final CompanyJpaRepository companyJpaRepository;

    public JpaEmployeeRepository(EmployeeJpaRepository jpaRepository, EmployeeJpaMapper mapper,
            CompanyJpaRepository companyJpaRepository) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
        this.companyJpaRepository = companyJpaRepository;
    }

    @Override
    public Employee save(Employee employee) {
        CompanyJpaEntity company = companyJpaRepository
                .getReferenceById(employee.getCompany().id());
        EmployeeJpaEntity saved = jpaRepository.save(mapper.toJpa(employee, company));
        return mapper.toDomain(saved, employee.getCompany());
    }

    @Override
    public Optional<Employee> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public Optional<Employee> findByIdAndCompanyId(Long id, Long companyId) {
        return jpaRepository.findByIdAndCompany_Id(id, companyId).map(mapper::toDomain);
    }

    @Override
    public Optional<Employee> findByIdIncludingDisabled(Long id) {
        return jpaRepository.findByIdIncludingDisabled(id).map(mapper::toDomain);
    }

    @Override
    public List<Employee> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Employee> findAllByCompanyId(Long companyId) {
        return jpaRepository.findAllByCompanyId(companyId).stream().map(mapper::toDomain).toList();
    }

    @Override
    public List<Employee> findAllByCompanyIdIncludingDisabled(Long companyId) {
        return jpaRepository.findAllByCompanyIdIncludingDisabled(companyId).stream()
                .map(mapper::toDomain).toList();
    }

    @Override
    public PageResult<Employee> search(SearchEmployeesCommand command) {
        String q = command.query() == null || command.query().isBlank()
                ? null
                : "%" + command.query().trim() + "%";
        // El ORDER BY va embebido en la query nativa (evita el manejo de Sort en
        // queries nativas).
        Page<EmployeeJpaEntity> page = jpaRepository.searchByCompanyIncludingDisabled(
                command.companyId(), q, Pages.request(command.page(), command.pageSize()));
        return Pages.result(page, mapper::toDomain);
    }

    @Override
    public void delete(Long id) {
        // Soft-delete vía UPDATE nativo (equivalente al @SQLDelete). No usamos
        // deleteById para no
        // disparar
        // el flush de entidades: si hay employee_roles gestionados del empleado en la
        // sesión,
        // provocaría un
        // TransientObjectException. El UPDATE nativo + clearAutomatically lo evita.
        jpaRepository.deactivate(id);
    }

    @Override
    public int reactivate(Long id) {
        return jpaRepository.reactivate(id);
    }

    @Override
    public boolean codeExists(String employeeCode) {
        return jpaRepository.countByEmployeeCodeAllRows(employeeCode) > 0;
    }
}
