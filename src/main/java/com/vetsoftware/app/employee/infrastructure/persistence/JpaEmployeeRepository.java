package com.vetsoftware.app.employee.infrastructure.persistence;

import com.vetsoftware.app.employee.application.port.out.EmployeeRepository;
import com.vetsoftware.app.employee.domain.Employee;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmployeeRepository implements EmployeeRepository {
    private final EmployeeJpaRepository jpaRepository;
    private final EmployeeJpaMapper mapper;

    public JpaEmployeeRepository(EmployeeJpaRepository jpaRepository, EmployeeJpaMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public Employee save(Employee employee) {
        return mapper.toDomain(jpaRepository.save(mapper.toJpa(employee)));
    }

    @Override
    public Optional<Employee> findById(Long id) {
        return jpaRepository.findById(id).map(mapper::toDomain);
    }

    @Override
    public List<Employee> findAll() {
        return jpaRepository.findAll().stream().map(mapper::toDomain).toList();
    }

    @Override
    public void delete(Long id) {
        jpaRepository.deleteById(id);
    }
}
