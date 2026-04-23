package com.vetsoftware.app.submodule.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.submodule.application.port.in.DeleteSubModuleUseCase;
import com.vetsoftware.app.submodule.application.port.out.SubModuleRepository;
import com.vetsoftware.app.submodule.domain.SubModuleNotFoundException;
import io.micrometer.observation.annotation.Observed;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Observed(name = "submodule.delete")
@Service
public class DeleteSubModuleService implements DeleteSubModuleUseCase {
    private final SubModuleRepository repository;

    public DeleteSubModuleService(SubModuleRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public void execute(Long id, AuthContext auth) {
        repository.findById(id).orElseThrow(() -> new SubModuleNotFoundException(id));
        repository.delete(id);
    }
}
