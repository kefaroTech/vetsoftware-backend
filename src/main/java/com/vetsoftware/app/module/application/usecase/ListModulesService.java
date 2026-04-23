package com.vetsoftware.app.module.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.module.application.dto.ModuleDto;
import com.vetsoftware.app.module.application.port.in.ListModulesUseCase;
import com.vetsoftware.app.module.application.port.out.ModuleRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "module.list")
@Service
public class ListModulesService implements ListModulesUseCase {
    private final ModuleRepository repository;

    public ListModulesService(ModuleRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<ModuleDto> listAll(AuthContext auth) {
        return repository.findAll().stream().map(ModuleDto::from).toList();
    }
}
