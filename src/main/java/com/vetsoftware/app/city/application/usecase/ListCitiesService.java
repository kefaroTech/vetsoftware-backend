package com.vetsoftware.app.city.application.usecase;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.city.application.dto.CityDto;
import com.vetsoftware.app.city.application.port.in.ListCitiesUseCase;
import com.vetsoftware.app.city.application.port.out.CityRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "city.list")
@Service
public class ListCitiesService implements ListCitiesUseCase {
    private final CityRepository repository;

    public ListCitiesService(CityRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<CityDto> listAll(AuthContext auth) {
        return repository.findAll().stream().map(CityDto::from).toList();
    }
}
