package com.vetsoftware.app.country.infrastructure.web;

import com.vetsoftware.app.country.application.command.CreateCountryCommand;
import com.vetsoftware.app.country.application.command.UpdateCountryCommand;
import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.in.CreateCountryUseCase;
import com.vetsoftware.app.country.application.port.in.DeleteCountryUseCase;
import com.vetsoftware.app.country.application.port.in.FindCountryUseCase;
import com.vetsoftware.app.country.application.port.in.ListCountriesUseCase;
import com.vetsoftware.app.country.application.port.in.ReactivateCountryUseCase;
import com.vetsoftware.app.country.application.port.in.UpdateCountryUseCase;
import com.vetsoftware.app.country.infrastructure.web.request.CreateCountryRequest;
import com.vetsoftware.app.country.infrastructure.web.request.UpdateCountryRequest;
import com.vetsoftware.app.country.infrastructure.web.response.CountryResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/countries")
public class CountryController {

    private final CreateCountryUseCase createUseCase;
    private final UpdateCountryUseCase updateUseCase;
    private final FindCountryUseCase findUseCase;
    private final ListCountriesUseCase listUseCase;
    private final DeleteCountryUseCase deleteUseCase;
    private final ReactivateCountryUseCase reactivateUseCase;

    public CountryController(CreateCountryUseCase createUseCase, UpdateCountryUseCase updateUseCase,
            FindCountryUseCase findUseCase, ListCountriesUseCase listUseCase,
            DeleteCountryUseCase deleteUseCase, ReactivateCountryUseCase reactivateUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
        this.reactivateUseCase = reactivateUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CountryResponse create(@Valid @RequestBody CreateCountryRequest request) {
        return toResponse(createUseCase.execute(new CreateCountryCommand(request.name())));
    }

    @GetMapping
    public List<CountryResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CountryResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public CountryResponse update(@PathVariable Long id,
            @Valid @RequestBody UpdateCountryRequest request) {
        return toResponse(updateUseCase.execute(new UpdateCountryCommand(id, request.name())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    @PatchMapping("/{id}/enable")
    public CountryResponse enable(@PathVariable Long id) {
        return toResponse(reactivateUseCase.execute(id));
    }

    private CountryResponse toResponse(CountryDto dto) {
        return new CountryResponse(dto.id(), dto.name(), dto.createdDate(), dto.enabled());
    }
}
