package com.vetsoftware.app.employeebranch.infrastructure.persistence;

import com.vetsoftware.app.employeebranch.application.port.out.EmployeeBranchRepository;
import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Repository;

@Repository
public class JpaEmployeeBranchRepository implements EmployeeBranchRepository {

    private final EmployeeBranchJpaRepository jpaRepository;

    public JpaEmployeeBranchRepository(EmployeeBranchJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public List<Long> findBranchIdsByEmployeeId(Long employeeId) {
        return jpaRepository.findBranchIdsByEmployeeId(employeeId);
    }

    @Override
    public void replaceBranches(Long employeeId, Long companyId, Collection<Long> branchIds) {
        // Set atómico: desactiva todo lo vigente y luego reactiva/inserta el objetivo.
        // Reactivar en vez
        // de insertar
        // ciego respeta el unique (employee_id, branch_id) sobre filas soft-deleted. El
        // caller corre
        // @Transactional.
        jpaRepository.disableAllByEmployeeId(employeeId, companyId);
        for (Long branchId : branchIds) {
            if (jpaRepository.reactivate(employeeId, branchId, companyId) == 0) {
                jpaRepository.insert(employeeId, branchId, companyId);
            }
        }
    }
}
