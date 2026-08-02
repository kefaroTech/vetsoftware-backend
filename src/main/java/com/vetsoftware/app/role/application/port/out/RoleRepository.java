package com.vetsoftware.app.role.application.port.out;

import com.vetsoftware.app.role.domain.Role;
import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Role save(Role role);
    Optional<Role> findById(Long id);
    Optional<Role> findByIdAndCompanyId(Long id, Long companyId);
    List<Role> findAll();
    List<Role> findAllByCompanyId(Long companyId);
    void delete(Long id);
    int reactivate(Long id, Long companyId);
}
