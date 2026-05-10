package com.vetsoftware.app.testtype.infrastructure.web;

import com.vetsoftware.app.testtype.application.command.CreateTestTypeCommand;
import com.vetsoftware.app.testtype.application.command.UpdateTestTypeCommand;
import com.vetsoftware.app.testtype.application.dto.CompanySummaryDto;
import com.vetsoftware.app.testtype.application.dto.TestTypeDto;
import com.vetsoftware.app.testtype.application.port.in.CreateTestTypeUseCase;
import com.vetsoftware.app.testtype.application.port.in.DeleteTestTypeUseCase;
import com.vetsoftware.app.testtype.application.port.in.FindTestTypeUseCase;
import com.vetsoftware.app.testtype.application.port.in.ListTestTypesUseCase;
import com.vetsoftware.app.testtype.application.port.in.UpdateTestTypeUseCase;
import com.vetsoftware.app.testtype.infrastructure.web.request.CreateTestTypeRequest;
import com.vetsoftware.app.testtype.infrastructure.web.request.UpdateTestTypeRequest;
import com.vetsoftware.app.testtype.infrastructure.web.response.CompanySummary;
import com.vetsoftware.app.testtype.infrastructure.web.response.TestTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test-types")
public class TestTypeController {
    private final CreateTestTypeUseCase createUseCase;
    private final UpdateTestTypeUseCase updateUseCase;
    private final FindTestTypeUseCase findUseCase;
    private final ListTestTypesUseCase listUseCase;
    private final DeleteTestTypeUseCase deleteUseCase;

    public TestTypeController(CreateTestTypeUseCase createUseCase,
                              UpdateTestTypeUseCase updateUseCase,
                              FindTestTypeUseCase findUseCase,
                              ListTestTypesUseCase listUseCase,
                              DeleteTestTypeUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TestTypeResponse create(@Valid @RequestBody CreateTestTypeRequest request) {
        return toResponse(createUseCase.execute(
                new CreateTestTypeCommand(request.name(), request.description(),
                        request.companyId(), request.general())));
    }

    @GetMapping
    public List<TestTypeResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public TestTypeResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public TestTypeResponse update(@PathVariable Long id,
                                   @Valid @RequestBody UpdateTestTypeRequest request) {
        return toResponse(updateUseCase.execute(
                new UpdateTestTypeCommand(id, request.name(), request.description(),
                        request.companyId(), request.general())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private TestTypeResponse toResponse(TestTypeDto dto) {
        CompanySummaryDto c = dto.company();
        return new TestTypeResponse(
                dto.id(), dto.name(), dto.description(),
                c == null ? null : new CompanySummary(c.id(), c.name(), c.identifier()),
                dto.general(),
                dto.createdDate());
    }
}
