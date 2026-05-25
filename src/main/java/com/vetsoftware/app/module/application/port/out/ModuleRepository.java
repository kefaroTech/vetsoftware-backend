package com.vetsoftware.app.module.application.port.out;

import com.vetsoftware.app.module.domain.Module;
import java.util.List;
import java.util.Optional;

public interface ModuleRepository {
    Module save(Module module);
    Optional<Module> findById(Long id);
    List<Module> findAll();
    void delete(Long id);
    int reactivate(Long id);
}
