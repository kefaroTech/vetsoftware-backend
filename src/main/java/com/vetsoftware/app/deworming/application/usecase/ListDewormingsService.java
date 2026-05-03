package com.vetsoftware.app.deworming.application.usecase;

import com.vetsoftware.app.deworming.application.dto.DewormingDto;
import com.vetsoftware.app.deworming.application.port.in.ListDewormingsUseCase;
import com.vetsoftware.app.deworming.application.port.out.DewormingRepository;
import io.micrometer.observation.annotation.Observed;
import java.util.List;
import org.springframework.stereotype.Service;

@Observed(name = "deworming.list")
@Service
public class ListDewormingsService implements ListDewormingsUseCase {
    private final DewormingRepository repository;

    public ListDewormingsService(DewormingRepository repository) {
        this.repository = repository;
    }

    @Override
    public List<DewormingDto> listAll() {
        return repository.findAll().stream().map(DewormingDto::from).toList();
    }
}
