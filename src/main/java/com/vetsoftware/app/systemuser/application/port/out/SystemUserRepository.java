package com.vetsoftware.app.systemuser.application.port.out;

import com.vetsoftware.app.systemuser.domain.SystemUser;
import java.util.List;
import java.util.Optional;

public interface SystemUserRepository {
    SystemUser save(SystemUser systemUser);
    Optional<SystemUser> findById(Long id);
    List<SystemUser> findAll();
    void delete(Long id);
}
