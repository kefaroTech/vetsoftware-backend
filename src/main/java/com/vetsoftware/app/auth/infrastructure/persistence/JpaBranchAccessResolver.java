package com.vetsoftware.app.auth.infrastructure.persistence;

import com.vetsoftware.app.auth.application.port.out.BranchAccessResolver;
import com.vetsoftware.app.employeebranch.infrastructure.persistence.EmployeeBranchJpaRepository;
import java.util.HashSet;
import java.util.Set;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Repository;

/**
 * Resuelve y cachea (por employeeId) el set de sedes asignadas a un empleado,
 * en paralelo a {@link JpaPermissionResolver}. {@code employee_branches} es la
 * única fuente del alcance por sede — "todas" se materializa como una fila por
 * sede, así que no hay flag que leer por request. Se cachea un {@link HashSet}
 * (no un {@code Set.copyOf(...)} inmutable) para round-trippear de forma fiable
 * con {@code GenericJackson2JsonRedisSerializer}, igual que
 * {@code employee-permissions}.
 *
 * <p>
 * El {@code @Cacheable} vive en el método público invocado desde otro bean
 * ({@code
 * ResolveAuthContextService}), así la llamada cruza el proxy de cache de
 * Spring. Invalidar con {@link #evict(Long)} tras reasignar sedes.
 */
@Repository
public class JpaBranchAccessResolver implements BranchAccessResolver {

    private final EmployeeBranchJpaRepository employeeBranchJpaRepository;

    public JpaBranchAccessResolver(EmployeeBranchJpaRepository employeeBranchJpaRepository) {
        this.employeeBranchJpaRepository = employeeBranchJpaRepository;
    }

    @Cacheable(value = "employee-branch-ids", key = "#employeeId")
    @Override
    public Set<Long> resolveFor(Long employeeId) {
        return new HashSet<>(employeeBranchJpaRepository.findBranchIdsByEmployeeId(employeeId));
    }

    @CacheEvict(value = "employee-branch-ids", key = "#employeeId")
    public void evict(Long employeeId) {
    }
}
