package com.vetsoftware.app.baserole.application.port.out;

import com.vetsoftware.app.baserole.domain.BaseRole;
import java.util.List;
import java.util.Optional;

public interface BaseRoleRepository {
    BaseRole save(BaseRole baseRole);
    Optional<BaseRole> findById(Long id);
    List<BaseRole> findAll();
    void delete(Long id);
    int reactivate(Long id);
}
