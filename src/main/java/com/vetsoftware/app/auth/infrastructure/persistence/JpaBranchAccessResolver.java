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
 * (no un {@code Set.copyOf(...)} inmutable) porque una colección inmutable de
 * la JDK no tiene constructor que Jackson pueda usar al volver de Redis, igual
 * que {@code employee-permissions}.
 *
 * <p>
 * <b>Quien garantiza que vuelva como {@code Set} es {@code CacheConfig}</b>, no
 * el tipo que se guarde aquí: el serializador de {@code employee-branch-ids}
 * está declarado con el {@code JavaType} de {@code Set<Long>}. El javadoc
 * anterior afirmaba lo contrario -que bastaba con guardar un {@code HashSet}- y
 * era falso: sin tipo declarado, el JSON {@code [1,2]} vuelve como
 * {@code ArrayList} sea cual sea la implementación que se guardó. Ver la
 * incidencia #464.
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
