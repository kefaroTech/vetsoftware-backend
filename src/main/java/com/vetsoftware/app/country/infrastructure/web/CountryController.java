package com.vetsoftware.app.country.infrastructure.web;

import com.vetsoftware.app.auth.application.dto.AuthContext;
import com.vetsoftware.app.country.application.command.CreateCountryCommand;
import com.vetsoftware.app.country.application.command.UpdateCountryCommand;
import com.vetsoftware.app.country.application.dto.CountryDto;
import com.vetsoftware.app.country.application.port.in.CreateCountryUseCase;
import com.vetsoftware.app.country.application.port.in.DeleteCountryUseCase;
import com.vetsoftware.app.country.application.port.in.FindCountryUseCase;
import com.vetsoftware.app.country.application.port.in.ListCountriesUseCase;
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

    public CountryController(CreateCountryUseCase createUseCase, UpdateCountryUseCase updateUseCase,
                             FindCountryUseCase findUseCase, ListCountriesUseCase listUseCase,
                             DeleteCountryUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CountryResponse create(@Valid @RequestBody CreateCountryRequest request,
                                  @RequestAttribute AuthContext authContext) {
        return toResponse(createUseCase.execute(new CreateCountryCommand(request.name()), authContext));
    }

    @GetMapping
    public List<CountryResponse> listAll(@RequestAttribute AuthContext authContext) {
        return listUseCase.listAll(authContext).stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public CountryResponse findById(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        return toResponse(findUseCase.findById(id, authContext));
    }

    @PutMapping("/{id}")
    public CountryResponse update(@PathVariable Long id, @Valid @RequestBody UpdateCountryRequest request,
                                  @RequestAttribute AuthContext authContext) {
        return toResponse(updateUseCase.execute(new UpdateCountryCommand(id, request.name()), authContext));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id, @RequestAttribute AuthContext authContext) {
        deleteUseCase.execute(id, authContext);
    }

    private CountryResponse toResponse(CountryDto dto) {
        return new CountryResponse(dto.id(), dto.name(), dto.createdDate());
    }
}
