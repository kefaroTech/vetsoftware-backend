package com.vetsoftware.app.consultationtype.infrastructure.web;

import com.vetsoftware.app.consultationtype.application.command.CreateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.command.UpdateConsultationTypeCommand;
import com.vetsoftware.app.consultationtype.application.dto.ConsultationTypeDto;
import com.vetsoftware.app.consultationtype.application.port.in.CreateConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.in.DeleteConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.in.FindConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.application.port.in.ListConsultationTypesUseCase;
import com.vetsoftware.app.consultationtype.application.port.in.UpdateConsultationTypeUseCase;
import com.vetsoftware.app.consultationtype.infrastructure.web.request.CreateConsultationTypeRequest;
import com.vetsoftware.app.consultationtype.infrastructure.web.request.UpdateConsultationTypeRequest;
import com.vetsoftware.app.consultationtype.infrastructure.web.response.ConsultationTypeResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/consultation-types")
public class ConsultationTypeController {
    private final CreateConsultationTypeUseCase createUseCase;
    private final UpdateConsultationTypeUseCase updateUseCase;
    private final FindConsultationTypeUseCase findUseCase;
    private final ListConsultationTypesUseCase listUseCase;
    private final DeleteConsultationTypeUseCase deleteUseCase;

    public ConsultationTypeController(CreateConsultationTypeUseCase createUseCase,
                                      UpdateConsultationTypeUseCase updateUseCase,
                                      FindConsultationTypeUseCase findUseCase,
                                      ListConsultationTypesUseCase listUseCase,
                                      DeleteConsultationTypeUseCase deleteUseCase) {
        this.createUseCase = createUseCase;
        this.updateUseCase = updateUseCase;
        this.findUseCase = findUseCase;
        this.listUseCase = listUseCase;
        this.deleteUseCase = deleteUseCase;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConsultationTypeResponse create(@Valid @RequestBody CreateConsultationTypeRequest request) {
        return toResponse(createUseCase.execute(
                new CreateConsultationTypeCommand(request.name(), request.description())));
    }

    @GetMapping
    public List<ConsultationTypeResponse> listAll() {
        return listUseCase.listAll().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}")
    public ConsultationTypeResponse findById(@PathVariable Long id) {
        return toResponse(findUseCase.findById(id));
    }

    @PutMapping("/{id}")
    public ConsultationTypeResponse update(@PathVariable Long id,
                                           @Valid @RequestBody UpdateConsultationTypeRequest request) {
        return toResponse(updateUseCase.execute(
                new UpdateConsultationTypeCommand(id, request.name(), request.description())));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        deleteUseCase.execute(id);
    }

    private ConsultationTypeResponse toResponse(ConsultationTypeDto dto) {
        return new ConsultationTypeResponse(dto.id(), dto.name(), dto.description(), dto.createdDate());
    }
}
