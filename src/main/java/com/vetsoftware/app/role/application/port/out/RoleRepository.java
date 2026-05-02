package com.vetsoftware.app.role.application.port.out;

import com.vetsoftware.app.role.domain.Role;
import java.util.List;
import java.util.Optional;

public interface RoleRepository {
    Role save(Role role);
    Optional<Role> findById(Long id);
    List<Role> findAll();
    void delete(Long id);
}
