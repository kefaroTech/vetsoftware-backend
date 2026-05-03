package com.vetsoftware.app.specie.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.specie.application.dto.SpecieDto;
import com.vetsoftware.app.specie.application.port.in.ListSpeciesUseCase;
import com.vetsoftware.app.specie.application.port.out.SpecieRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "specie.list")
@Service
public class ListSpeciesService implements ListSpeciesUseCase {
    private final SpecieRepository repository;

    public ListSpeciesService(SpecieRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<SpecieDto> listAll(AuthContext auth) {
        return repository.findAll().stream().map(SpecieDto::from).toList();
    }
}
